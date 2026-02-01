package xerca.xercamusic.common.packets.serverbound;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import xerca.xercamusic.common.MusicManager;
import xerca.xercamusic.common.NoteEvent;
import xerca.xercamusic.common.Triggers;
import xerca.xercamusic.common.item.Items;

import java.util.List;
import java.util.UUID;

public class MusicUpdatePacketHandler {
    private static void processMessage(MusicUpdatePacket msg, ServerPlayer pl) {
        ItemStack note = pl.getMainHandItem();
        if (!note.isEmpty() && note.getItem() == Items.MUSIC_SHEET.get()) {
            MusicUpdatePacket.FieldFlag flag = msg.availability();
            if (flag.hasId) note.set(Items.SHEET_ID.get(), msg.id());
            if (flag.hasVersion) note.set(Items.SHEET_VERSION.get(), msg.version());
            if (flag.hasLength) note.set(Items.SHEET_LENGTH.get(), (int) msg.lengthBeats());
            if (flag.hasBps) note.set(Items.SHEET_BPS.get(), msg.bps());
            if (flag.hasVolume) note.set(Items.SHEET_VOLUME.get(), msg.volume());
            if (flag.hasPrevIns) note.set(Items.SHEET_PREV_INSTRUMENT.get(), msg.prevInstrument());
            if (flag.hasPrevInsLocked) note.set(Items.SHEET_PREV_INSTRUMENT_LOCKED.get(), msg.prevInsLocked());
            if (flag.hasHlInterval) note.set(Items.SHEET_HIGHLIGHT_INTERVAL.get(), msg.highlightInterval());
            if (flag.hasSigned && msg.signed()) {
                if (flag.hasTitle) note.set(Items.SHEET_TITLE.get(), msg.title().trim());
                note.set(Items.SHEET_AUTHOR.get(), pl.getName().getString());
                note.set(Items.SHEET_GENERATION.get(), 1);
                Triggers.BECOME_MUSICIAN.get().trigger(pl);
            }
            if (flag.hasNotes) {
                List<NoteEvent> notes = msg.notes();
                UUID id = note.get(Items.SHEET_ID.get());
                if (notes == null) {
                    // Get if a large sheet was sent in parts
                    notes = MusicManager.getFinishedNotesFromBuffer(id);
                    if (notes == null) {
                        return;
                    }
                }
                MusicManager.setMusicData(id, note.getOrDefault(Items.SHEET_VERSION.get(), 0), notes, pl.server);
                if (note.get(Items.SHEET_BPS.get()) == null) {
                    note.set(Items.SHEET_BPS.get(), (byte) 8);
                }
            }
        }
    }

    public static void handle(MusicUpdatePacket packet, IPayloadContext context) {
        if (packet != null) {
            context.enqueueWork(() -> processMessage(packet, (ServerPlayer) context.player()));
        }
    }
}
