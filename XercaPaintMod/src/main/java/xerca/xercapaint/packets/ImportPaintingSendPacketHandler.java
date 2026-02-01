package xerca.xercapaint.packets;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import xerca.xercapaint.CommandImport;

public class ImportPaintingSendPacketHandler {

    private static void processMessage(ImportPaintingSendPacket msg, ServerPlayer sender) {
        CommandImport.doImport(msg.tag(), sender);
    }

    public static void handle(ImportPaintingSendPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> processMessage(packet, (ServerPlayer) context.player()));
    }
}
