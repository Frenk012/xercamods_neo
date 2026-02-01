package xerca.xercapaint.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import xerca.xercapaint.Mod;

public record CloseGuiPacket() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CloseGuiPacket> TYPE = new CustomPacketPayload.Type<>(Mod.id("close_gui"));
    public static final StreamCodec<FriendlyByteBuf, CloseGuiPacket> STREAM_CODEC = StreamCodec.ofMember(CloseGuiPacket::encode, CloseGuiPacket::decode);

    public FriendlyByteBuf encode(FriendlyByteBuf buf) {
        return buf;
    }

    public static CloseGuiPacket decode(FriendlyByteBuf buf) {
        return new CloseGuiPacket();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
