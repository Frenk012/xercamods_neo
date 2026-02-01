package xerca.xercamusic.common.packets.serverbound;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import xerca.xercamusic.common.Mod;


public record MusicEndedPacket(int playerId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MusicEndedPacket> TYPE = new CustomPacketPayload.Type<>(Mod.id("music_ended"));
    public static final StreamCodec<FriendlyByteBuf, MusicEndedPacket> STREAM_CODEC = StreamCodec.ofMember(MusicEndedPacket::encode, MusicEndedPacket::decode);

    public static MusicEndedPacket decode(FriendlyByteBuf buf) {
        int playerId = buf.readInt();
        return new MusicEndedPacket(playerId);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(playerId);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
