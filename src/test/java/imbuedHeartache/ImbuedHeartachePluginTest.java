package imbuedHeartache;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ImbuedHeartachePluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(ImbuedHeartachePlugin.class);
        RuneLite.main(args);
    }
}