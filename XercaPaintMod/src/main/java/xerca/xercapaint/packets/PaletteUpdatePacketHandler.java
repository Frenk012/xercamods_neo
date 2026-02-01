package xerca.xercapaint.packets;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import xerca.xercapaint.item.ItemPalette;
import xerca.xercapaint.item.Items;

public class PaletteUpdatePacketHandler {

    private static void processMessage(PaletteUpdatePacket msg, ServerPlayer pl) {
        ItemStack palette = pl.getMainHandItem();

        if (palette.isEmpty() || palette.getItem() != Items.ITEM_PALETTE.get()) {
            palette = pl.getOffhandItem();
            if (palette.isEmpty() || palette.getItem() != Items.ITEM_PALETTE.get()) {
                return;
            }
        }

        palette.set(Items.PALETTE_CUSTOM_COLORS.get(), new ItemPalette.ComponentCustomColor(msg.paletteColors()));
    }

    public static void handle(PaletteUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> processMessage(packet, (ServerPlayer) context.player()));
    }
}
