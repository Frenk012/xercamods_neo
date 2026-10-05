package xerca.xercapaint.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import xerca.xercapaint.Mod;

public record PictureSendPacket(String canvasId, int version, int[] pixels, boolean sidesActive,
                                int[] sidePixels) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PictureSendPacket> TYPE = new CustomPacketPayload.Type<>(Mod.id("picture_send"));
    public static final StreamCodec<FriendlyByteBuf, PictureSendPacket> STREAM_CODEC = StreamCodec.ofMember(PictureSendPacket::encode, PictureSendPacket::decode);

    public FriendlyByteBuf encode(FriendlyByteBuf buf) {
        buf.writeUtf(canvasId);
        buf.writeInt(version);
        buf.writeVarIntArray(pixels);
        buf.writeBoolean(sidesActive);
        buf.writeVarIntArray(sidePixels);
        return buf;
    }

    public static PictureSendPacket decode(FriendlyByteBuf buf) {
        String canvasId = buf.readUtf(64);
        int version = buf.readInt();
        int[] pixels = buf.readVarIntArray(1024);
        boolean sidesActive = buf.readBoolean();
        // A canvas has at most 2*(32+32) = 128 side pixels.
        int[] sidePixels = buf.readVarIntArray(128);
        return new PictureSendPacket(canvasId, version, pixels, sidesActive, sidePixels);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
