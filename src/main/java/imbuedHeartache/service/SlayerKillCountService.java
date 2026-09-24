package imbuedHeartache.service;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.http.api.loottracker.LootRecordType;

import static net.runelite.client.RuneLite.RUNELITE_DIR;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.*;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

@Slf4j
@Singleton
public class SlayerKillCountService {

    private static final String FILE_EXTENSION = ".log";
    private static final File LOOT_RECORD_DIR = new File(RUNELITE_DIR, "loots");
    private final File playerFolder = LOOT_RECORD_DIR;

    @Inject
    private ConfigManager configManager;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    public void onWidget(WidgetLoaded event) {
        if (event.getGroupId() == InterfaceID.KILL_LOG) {
            clientThread.invokeAtTickEnd(this::handleSlayerLog);
            clientThread.invokeAtTickEnd(this::handleLootLoggerSlayerMonsters);
        }
    }

    public String getHeartacheAndKC(boolean elite) {
        Double heartRate = calculateHeartRate(elite);
        Integer superiorCreatureKC = getSlayerKc("superior creatures");

        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);
        return "Heartache: " + df.format(heartRate) + " x, with " + superiorCreatureKC + " superiors killed";
    }

    private void handleSlayerLog() {
        var title = client.getWidget(InterfaceID.KillLog.INTERFACE_TITLE);
        if (title == null || !"Slayer Kill Log".equals(title.getText())) {
            return;
        }

        var monsters = client.getWidget(InterfaceID.KillLog.NAME);
        var counts = client.getWidget(InterfaceID.KillLog.KILL);
        if (monsters == null || counts == null) {
            return;
        }

        var mobs = monsters.getChildren();
        var kills = counts.getChildren();
        if (mobs == null || kills == null) {
            return;
        }

        final int n = Math.min(mobs.length, kills.length);
        for (int i = 0; i < n; i++) {
            var mob = mobs[i].getText().replace(":", "");
            var count = kills[i].getText().replace(",", "");
            log.debug(mob + " - " + count);

            int kc;
            try {
                kc = Integer.parseInt(count);
            } catch (NumberFormatException e) {
                if (count.startsWith("Lots")) {
                    kc = 65_535;

                    // avoid overwriting a higher kc
                    Integer oldKc = getSlayerKc(mob);
                    if (oldKc != null && oldKc >= kc) {
                        continue;
                    }
                } else {
                    log.debug("Failed to parse slayer log entry for mob '{}' with kc '{}'", mob, count);
                    continue;
                }
            }

            setSlayerKc(mob, kc);
        }
    }

    private void handleLootLoggerSlayerMonsters() {
        List<String> specifiedMonsters = new ArrayList<>();
        specifiedMonsters.add("Elder custodian stalker");
        specifiedMonsters.add("Venator");

        specifiedMonsters.forEach(monster -> setSlayerKc(monster, getKCFromLootLogger(monster)));
    }

    private void setSlayerKc(String mob, int kc) {
        if (kc <= 0) return;
        configManager.setRSProfileConfiguration("imbuedHeartAchePlugin", "kc_" + mob.toLowerCase(), kc);
    }

    private Integer getSlayerKc(String mob) {
        return configManager.getRSProfileConfiguration("imbuedHeartAchePlugin", "kc_" + mob.toLowerCase(), int.class);
    }

    private Double calculateHeartRate(boolean elite) {
        double superiorRate = elite ? 150.0 : 200.0;
        Map<String, Integer> monsters = getMonstersAndRates();

        ArrayList<Double> rates = new ArrayList<>();

        monsters.forEach((monster, rarity) -> {
            Integer amount = getSlayerKc(monster);
            if (Objects.isNull(amount))
                return;
            double calculatedSuperiors = (double) amount / superiorRate;
            rates.add(calculatedSuperiors / rarity);
        });

        return rates.stream().reduce(0.0, Double::sum);
    }

    @Nonnull
    private static Map<String, Integer> getMonstersAndRates() {
        Map<String, Integer> monsters = new HashMap<>();
        monsters.put("Aberrant spectres", 760);
        monsters.put("Abyssal demons", 352);
        monsters.put("Aquanites", 472);
        monsters.put("Araxytes", 224);
        monsters.put("Banshees", 1288);
        monsters.put("Basilisk knights", 760);
        monsters.put("Basilisks", 1024);
        monsters.put("Bloodvelds", 896);
        monsters.put("Cave crawlers", 1336);
        monsters.put("Cave Horrors", 784);
        monsters.put("Cockatrice", 1192);
        monsters.put("Crawling hands", 1376);
        monsters.put("Elder custodian stalker", 504);
        monsters.put("Dark beasts", 256);
        monsters.put("Drakes", 368);
        monsters.put("Dust devils", 680);
        monsters.put("Gargoyles", 520);
        monsters.put("Gryphons", 888);
        monsters.put("Hydras", 160);
        monsters.put("Infernal mages", 960);
        monsters.put("Jellies", 872);
        monsters.put("Kurask", 600);
        monsters.put("Nechryael", 440);
        monsters.put("Pyrefiends", 1144);
        monsters.put("Rockslugs", 1240);
        monsters.put("Smoke devils", 200);
        monsters.put("Turoth", 832);
        monsters.put("Venator", 536);
        monsters.put("Warped creatures", 816);
        monsters.put("Wyrms", 728);
        return monsters;
    }

    private static String npcNameToFileName(final String npcName) {
        return npcName.toLowerCase().trim() + FILE_EXTENSION;
    }

    // Finds kc from Loot Logger files
    // See https://github.com/TheStonedTurtle/Loot-Logger
    private int getKCFromLootLogger(String npcName) {
        final File userHashFile = new File(playerFolder, String.valueOf(client.getAccountHash()));
        final File npcFile = new File(userHashFile, LootRecordType.NPC.name());
        final String npcFileName = npcNameToFileName(npcName);
        final File file = new File(npcFile, npcFileName);
        int count = 0;

        try (final BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Skips the empty line at end of file
                if (!line.isEmpty()) {
                    count++;
                }
            }

        } catch (FileNotFoundException e) {
            log.debug("File not found: {}", npcFileName);
        } catch (IOException e) {
            log.warn("IOException for file {}: {}", npcFileName, e.getMessage());
        }

        return count;
    }
}
