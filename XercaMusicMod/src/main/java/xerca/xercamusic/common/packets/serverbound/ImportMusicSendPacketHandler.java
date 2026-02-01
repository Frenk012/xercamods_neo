package xerca.xercamusic.common.packets.serverbound;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import xerca.xercamusic.common.CommandImport;

public class ImportMusicSendPacketHandler {
    private static void processMessage(ImportMusicSendPacket msg, ServerPlayer sender) {
        CommandImport.doImport(msg.tag(), msg.notes(), sender);
    }

    public static void handle(ImportMusicSendPacket packet, IPayloadContext context) {
        if (packet != null) {
            context.enqueueWork(() -> processMessage(packet, (ServerPlayer) context.player()));
        }
    }
}
