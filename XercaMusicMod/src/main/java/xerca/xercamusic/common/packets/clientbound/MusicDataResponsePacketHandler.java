package xerca.xercamusic.common.packets.clientbound;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import xerca.xercamusic.client.MusicManagerClient;
import xerca.xercamusic.common.NoteEvent;

import java.util.List;
import java.util.UUID;

public class MusicDataResponsePacketHandler {
    private static void processMessage(MusicDataResponsePacket msg) {
        UUID id = msg.id();
        int version = msg.version();
        List<NoteEvent> notes = msg.notes();
        MusicManagerClient.setMusicData(id, version, notes);
    }

    public static void handle(MusicDataResponsePacket packet, IPayloadContext context) {
        if (packet != null) {
            context.enqueueWork(() -> processMessage(packet));
        }
    }
}
