package imbuedHeartache.service;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.*;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

@Slf4j
@Singleton
public class SlayerKillCountService {

    @Inject
    private ConfigManager configManager;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    public void onWidget(WidgetLoaded event) {
        if (event.getGroupId() == InterfaceID.KILL_LOG) {
            clientThread.invokeAtTickEnd(this::handleSlayerLog);
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

            upsertSlayerKc(mob, kc);
        }
    }

    public void upsertSlayerKc(String mob, int kc) {
        if (kc == 0) return;
        int kcCount = configManager.getRSProfileConfiguration("imbuedHeartAchePlugin", "kc_" + mob.toLowerCase(), int.class);
        if (kc < 0 && kcCount > 0) {
            int savedKc = configManager.getRSProfileConfiguration("imbuedHeartAchePlugin", "kc_" + mob.toLowerCase(), int.class);
            savedKc++;
            configManager.setRSProfileConfiguration("imbuedHeartAchePlugin", "kc_" + mob.toLowerCase(), savedKc);
        } else {
            configManager.setRSProfileConfiguration("imbuedHeartAchePlugin", "kc_" + mob.toLowerCase(), kc);
        }
    }

    private Integer getSlayerKc(String mob) {
        return configManager.getRSProfileConfiguration("imbuedHeartAchePlugin", "kc_" + mob.toLowerCase(), int.class);
    }

    private Double calculateHeartRate(boolean elite) {
        double superiorRate = elite ? 150.0 : 200.0;
        Map<String, Integer> monsters = getMonstersAndRates();

        ArrayList<Double> rates = new ArrayList<>();

        monsters.forEach((monster, rarity) -> {
            if (rarity == 0) return;
            Integer amount = getSlayerKc(monster);
            if (Objects.isNull(amount)) return;
            double calculatedSuperiors = (double) amount / superiorRate;
            rates.add(calculatedSuperiors / rarity);
        });

        return rates.stream().reduce(0.0, Double::sum);
    }

    public Map<String, Integer> getMonstersAndRates() {
        Map<String, Integer> monsters = new HashMap<>();
        monsters.put("Aberrant spectres", 760);
        monsters.put("Abhorrent spectre", 0);
        monsters.put("Abyssal demons", 352);
        monsters.put("Greater abyssal demon", 0);
        monsters.put("Aquanites", 472);
        monsters.put("Elder aquanite", 0);
        monsters.put("Araxytes", 224);
        monsters.put("Dreadborn Araxyte", 0);
        monsters.put("Banshees", 1288);
        monsters.put("Screeming banshee", 0);
        monsters.put("Basilisk knights", 760);
        monsters.put("Basilisk Sentinel", 0);
        monsters.put("Basilisks", 1024);
        monsters.put("Monstrous basilisk", 0);
        monsters.put("Bloodvelds", 896);
        monsters.put("Insatiable Bloodveld", 0);
        monsters.put("Cave crawlers", 1336);
        monsters.put("Chasm Crawler", 0);
        monsters.put("Cave Horrors", 784);
        monsters.put("Cave abomination", 0);
        monsters.put("Cockatrice", 1192);
        monsters.put("Cockathrice", 0);
        monsters.put("Crawling hands", 1376);
        monsters.put("Crushing hands", 0);
        monsters.put("Elder custodian stalker", 504);
        monsters.put("Ancient stalker", 0);
        monsters.put("Dark beasts", 256);
        monsters.put("Night beasts", 0);
        monsters.put("Drakes", 368);
        monsters.put("Guardian Drake", 0);
        monsters.put("Dust devils", 680);
        monsters.put("Choke devils", 0);
        monsters.put("Gargoyles", 520);
        monsters.put("Marble gargoyle", 0);
        monsters.put("Gryphons", 888);
        monsters.put("Dire gryphon", 0);
        monsters.put("Hydras", 160);
        monsters.put("Colossal Hydra", 0);
        monsters.put("Malevolent Mage", 960);
        monsters.put("Infernal mages", 0);
        monsters.put("Jellies", 872);
        monsters.put("Vitreous Jelly", 0);
        monsters.put("Kurask", 600);
        monsters.put("King kurask", 0);
        monsters.put("Nechryael", 440);
        monsters.put("Nechryarch", 0);
        monsters.put("Pyrefiends", 1144);
        monsters.put("Flaming pyrelord", 0);
        monsters.put("Infernal pyrelord", 0);
        monsters.put("Rockslugs", 1240);
        monsters.put("Giant Rockslug", 0);
        monsters.put("Smoke devils", 200);
        monsters.put("Nuclear smoke devils", 0);
        monsters.put("Turoth", 832);
        monsters.put("Spiked Turoth", 0);
        monsters.put("Venator", 536);
        monsters.put("Blood-starved venator", 0);
        monsters.put("Warped creatures", 816);
        monsters.put("Mutated Terrorbird", 0);
        monsters.put("Mutated Tortoise", 0);
        monsters.put("Wyrms", 728);
        monsters.put("Shadow Wyrm", 0);
        return monsters;
    }
}
