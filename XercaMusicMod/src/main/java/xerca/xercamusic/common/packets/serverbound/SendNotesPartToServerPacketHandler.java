package xerca.xercamusic.common.packets.serverbound;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import xerca.xercamusic.common.MusicManager;
import xerca.xercamusic.common.packets.clientbound.NotesPartAckFromServerPacket;

import static xerca.xercamusic.common.Mod.sendToClient;

public class SendNotesPartToServerPacketHandler {
    private static void processMessage(SendNotesPartToServerPacket pkt, ServerPlayer sender) {
        if (MusicManager.addNotesPart(pkt)) {
            sendToClient(sender, new NotesPartAckFromServerPacket(pkt.uuid()));
        }
    }

    public static void handle(SendNotesPartToServerPacket packet, IPayloadContext context) {
        if (packet != null) {
            context.enqueueWork(() -> processMessage(packet, (ServerPlayer) context.player()));
        }
    }
}
