package xerca.xercapaint.packets;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import xerca.xercapaint.Config;
import xerca.xercapaint.Mod;
import xerca.xercapaint.entity.EntityEasel;
import xerca.xercapaint.item.ItemCanvas;
import xerca.xercapaint.item.ItemPalette;
import xerca.xercapaint.item.Items;

import java.util.Arrays;

public class CanvasUpdatePacketHandler {

    public static void processMessage(CanvasUpdatePacket msg, ServerPlayer pl) {
        EntityEasel entityEasel = CanvasUpdateUtils.getEntityEasel(pl, msg.easelId());
        if (msg.easelId() > -1 && entityEasel == null) {
            Mod.LOGGER.error("CanvasUpdatePacketHandler: Easel entity not found! easelId: {}", msg.easelId());
            return;
        }

        ItemStack canvas = CanvasUpdateUtils.getCanvas(pl, entityEasel);
        ItemStack palette = CanvasUpdateUtils.getPalette(pl);

        if (canvas != null && !canvas.isEmpty() && canvas.getItem() instanceof ItemCanvas) {
            // Feature 10: a waxed (protected) canvas can no longer be edited.
            if (canvas.getOrDefault(Items.CANVAS_WAXED.get(), false)) {
                pl.displayClientMessage(Component.translatable("xercapaint.protected").withStyle(ChatFormatting.RED), true);
                return;
            }

            boolean paletteIsReal = palette != null && !palette.isEmpty() && palette.getItem() == Items.ITEM_PALETTE.get();

            // Feature 1: consume per-colour paint charge from the palette based on which pixels changed.
            if (Config.dyeCostEnabled() && paletteIsReal && !pl.isCreative()) {
                palette.set(Items.PALETTE_CHARGES.get(), new Items.PaletteCharges(msg.basicColorsCharges()));
            }

            canvas.set(Items.CANVAS_PIXELS.get(), Arrays.stream(msg.pixels()).boxed().toList());
            canvas.set(Items.CANVAS_ID.get(), msg.canvasId());
            canvas.set(Items.CANVAS_VERSION.get(), msg.version());
            canvas.set(Items.CANVAS_GENERATION.get(), 0);
            if (msg.signed()) {
                canvas.set(Items.CANVAS_AUTHOR.get(), pl.getName().getString());
                canvas.set(Items.CANVAS_TITLE.get(), msg.title().trim());
                canvas.set(Items.CANVAS_GENERATION.get(), 1);
            }

            if (palette != null && !palette.isEmpty() && palette.getItem() == Items.ITEM_PALETTE.get()) {
                palette.set(Items.PALETTE_CUSTOM_COLORS.get(), new ItemPalette.ComponentCustomColor(msg.paletteColors()));
            }

            if (entityEasel instanceof EntityEasel easel) {
                easel.setItem(canvas, false);
                easel.setPainter(null);
            }

            Mod.LOGGER.debug("Handling canvas update: Name: {} V: {}", msg.canvasId(), msg.version());
        }
    }

    public static void handle(CanvasUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> processMessage(packet, (ServerPlayer) context.player()));
    }
}
