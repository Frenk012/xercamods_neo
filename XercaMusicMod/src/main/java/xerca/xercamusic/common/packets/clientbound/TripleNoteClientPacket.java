package xerca.xercamusic.common.packets.clientbound;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import xerca.xercamusic.common.Mod;
import xerca.xercamusic.common.item.IItemInstrument;
import xerca.xercamusic.common.item.Items;

import java.util.List;


public record TripleNoteClientPacket(int note1, int note2, int note3, IItemInstrument instrumentItem,
                                     int entityId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TripleNoteClientPacket> TYPE = new CustomPacketPayload.Type<>(Mod.id("triple_note_client"));
    public static final StreamCodec<FriendlyByteBuf, TripleNoteClientPacket> STREAM_CODEC = StreamCodec.ofMember(TripleNoteClientPacket::encode, TripleNoteClientPacket::decode);

    public TripleNoteClientPacket(int note1, int note2, int note3, IItemInstrument instrumentItem, Entity entity) {
        this(note1, note2, note3, instrumentItem, entity.getId());
    }

    public static TripleNoteClientPacket decode(FriendlyByteBuf buf) {
        int note1 = buf.readInt();
        int note2 = buf.readInt();
        int note3 = buf.readInt();
        int instrumentId = buf.readInt();
        int entityId = buf.readInt();

        List<IItemInstrument> instruments = Items.getInstruments();
        if (instrumentId < 0 || instrumentId >= instruments.size()) {
            Mod.LOGGER.warn("Invalid instrumentId: {}", instrumentId);
            instrumentId = 0;
        }

        IItemInstrument instrumentItem = instruments.get(instrumentId);
        return new TripleNoteClientPacket(note1, note2, note3, instrumentItem, entityId);
    }

    public void encode(FriendlyByteBuf buf) {
        int instrumentId = instrumentItem.getInstrumentId();

        buf.writeInt(note1);
        buf.writeInt(note2);
        buf.writeInt(note3);
        buf.writeInt(instrumentId);
        buf.writeInt(entityId);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
