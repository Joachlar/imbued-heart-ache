package imbuedHeartache;

import imbuedHeartache.service.SlayerKillCountService;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@PluginDescriptor(
        name = "Imbued heartache"
)
public class ImbuedHeartachePlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private ChatCommandManager chatCommandManager;

    private @Inject SlayerKillCountService slayerKillCountService;

    @Override
    protected void startUp() throws Exception {
        chatCommandManager.registerCommand("!heart", this::handlePossibleHeartacheMessage);
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

    private void handlePossibleHeartacheMessage(ChatMessage message, String query) {
        if (query.isBlank()) {
            return;
        }
        String otherPlayerName = getStringBetweenQuotes(query);
        if (otherPlayerName != null) {
            query = query.replace("\"" + otherPlayerName + "\"", "").trim();
        }
        String heart = query;
        boolean elite = false;
        if (query.split(" ").length > 1) {
            heart = query.split(" ")[0];
            elite = query.split(" ")[1].equalsIgnoreCase("elite");
        }
        if ("!heart".equalsIgnoreCase(heart)) {
            String heartAcheAndKC = slayerKillCountService.getHeartacheAndKC(elite);
            updateChatMessage(message, heartAcheAndKC);
        }
    }

    public String getStringBetweenQuotes(String commandArg) {
        Pattern pattern = Pattern.compile("\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(commandArg);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private void updateChatMessage(ChatMessage chatMessage, String text) {
        chatMessage.getMessageNode().setValue(text);
        client.refreshChat();
    }

}
