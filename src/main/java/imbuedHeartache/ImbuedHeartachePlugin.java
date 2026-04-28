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

import java.util.Objects;

@Slf4j
@PluginDescriptor(
        name = "Imbued heartache"
)
public class ImbuedHeartachePlugin extends Plugin {
    @Inject
    private Client client;

    private @Inject SlayerKillCountService slayerKillCountService;

    @Override
    protected void startUp() throws Exception {
        log.debug("Imbued heartache started!");
    }

    @Override
    protected void shutDown() throws Exception {
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
}
