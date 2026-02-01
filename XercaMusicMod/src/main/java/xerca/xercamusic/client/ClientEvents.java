package xerca.xercamusic.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import xerca.xercamusic.common.Mod;

@OnlyIn(Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void onPlayerJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        Mod.LOGGER.debug("ClientPacketListener Join Event");
        MusicManagerClient.load();
    }
}
