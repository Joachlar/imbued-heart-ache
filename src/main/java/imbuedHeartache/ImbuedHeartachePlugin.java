package imbuedHeartache;

import imbuedHeartache.service.SlayerKillCountService;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Objects;

@Slf4j
@PluginDescriptor(
        name = "Imbued heartache",
        internalName = "imbued-heartache"
)
public class ImbuedHeartachePlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private SlayerKillCountService slayerKillCountService;

    private NavigationButton navButton;

    @Override
    protected void startUp() {
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "panel-icon.png");
        LootLoggerPanel panel = new LootLoggerPanel(client, slayerKillCountService);
        navButton = NavigationButton.builder()
                .tooltip("Imbued heartache")
                .icon(icon)
                .priority(20)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navButton);

        log.debug("Imbued heartache started!");
    }

    @Override
    protected void shutDown() {
        clientToolbar.removeNavigation(navButton);
        log.debug("Imbued heartache stopped!");
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        slayerKillCountService.onWidget(event);
    }

    @Subscribe
    public void onCommandExecuted(CommandExecuted commandExecuted) {
        boolean elite = false;
        if ("heart".equalsIgnoreCase(commandExecuted.getCommand())) {
            if (Objects.nonNull(commandExecuted.getArguments()) && commandExecuted.getArguments().length > 0) {
                elite = "elite".equalsIgnoreCase(commandExecuted.getArguments()[0]);
            }
            String heartAcheAndKC = slayerKillCountService.getHeartacheAndKC(elite);
            client.addChatMessage(ChatMessageType.PLAYERRELATED, "Imbued heartache plugin", heartAcheAndKC, "Imbued heartache");
        }
    }

    @Subscribe
    public void onLootReceived(final LootReceived event) {
        Map<String, Integer> monsterMap = slayerKillCountService.getMonstersAndRates();

        Integer monsterValue = monsterMap.get(event.getName());

        if (Objects.nonNull(monsterValue) && monsterValue >= 0) {
            boolean isSuperior = monsterValue == 0;
            if (isSuperior) {
                slayerKillCountService.upsertSlayerKc("superior creatures", -1);
                return;
            }
            slayerKillCountService.upsertSlayerKc(event.getName().toLowerCase(), -1);
        }
    }
}
