package xerca.xercamusic.common.packets.serverbound;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import xerca.xercamusic.common.entity.EntityMusicSpirit;

public class MusicEndedPacketHandler {
    private static void processMessage(MusicEndedPacket msg, ServerPlayer pl) {
        Entity ent = pl.level().getEntity(msg.playerId());
        if (ent instanceof EntityMusicSpirit spirit) {
            spirit.setPlaying(false);
        }
    }

    public static void handle(MusicEndedPacket packet, IPayloadContext context) {
        if (packet != null) {
            context.enqueueWork(() -> processMessage(packet, (ServerPlayer) context.player()));
        }
    }
}
