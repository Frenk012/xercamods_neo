package xerca.xercamusic.common.packets.serverbound;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import xerca.xercamusic.common.packets.clientbound.SingleNoteClientPacket;

import java.util.List;

import static xerca.xercamusic.common.Mod.sendToClient;

public class SingleNotePacketHandler {
    private static void processMessage(SingleNotePacket msg, ServerPlayer pl) {
        // Find players within 24 blocks using AABB
        AABB area = new AABB(pl.position().subtract(24, 24, 24), pl.position().add(24, 24, 24));
        List<ServerPlayer> players = ((ServerLevel) pl.level()).getEntitiesOfClass(ServerPlayer.class, area);
        SingleNoteClientPacket packet = new SingleNoteClientPacket(msg.note(), msg.instrumentItem(), pl, msg.isStop(), msg.volume());
        for (ServerPlayer player : players) {
            sendToClient(player, packet);
        }
    }

    public static void handle(SingleNotePacket packet, IPayloadContext context) {
        if (packet != null) {
            context.enqueueWork(() -> processMessage(packet, (ServerPlayer) context.player()));
        }
    }
}
