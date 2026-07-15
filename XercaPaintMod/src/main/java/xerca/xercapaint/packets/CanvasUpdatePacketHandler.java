package xerca.xercapaint.packets;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xerca.xercapaint.Config;
import xerca.xercapaint.PaletteUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import xerca.xercapaint.Mod;
import xerca.xercapaint.entity.EntityEasel;
import xerca.xercapaint.item.ItemCanvas;
import xerca.xercapaint.item.ItemPalette;
import xerca.xercapaint.item.Items;

import java.util.Arrays;

public class CanvasUpdatePacketHandler {

    public static void processMessage(CanvasUpdatePacket msg, ServerPlayer pl) {
        ItemStack canvas;
        ItemStack palette;
        Entity entityEasel = null;

        if (msg.easelId() > -1) {
            entityEasel = pl.level().getEntity(msg.easelId());
            if (entityEasel == null) {
                Mod.LOGGER.error("CanvasUpdatePacketHandler: Easel entity not found! easelId: {}", msg.easelId());
                return;
            }
            if (!(entityEasel instanceof EntityEasel easel)) {
                Mod.LOGGER.error("CanvasUpdatePacketHandler: Entity found is not an easel! easelId: {}", msg.easelId());
                return;
            }
            canvas = easel.getItem();
            if (!(canvas.getItem() instanceof ItemCanvas)) {
                Mod.LOGGER.error("CanvasUpdatePacketHandler: Canvas not found inside easel!");
                return;
            }
            ItemStack mainHandItem = pl.getMainHandItem();
            ItemStack offHandItem = pl.getOffhandItem();
            if (mainHandItem.getItem() instanceof ItemPalette) {
                palette = mainHandItem;
            } else if (offHandItem.getItem() instanceof ItemPalette) {
                palette = offHandItem;
            } else {
                Mod.LOGGER.error("CanvasUpdatePacketHandler: Palette not found on player's hands!");
                return;
            }
        } else {
            canvas = pl.getMainHandItem();
            palette = pl.getOffhandItem();
            if (canvas.getItem() instanceof ItemPalette) {
                ItemStack temp = canvas;
                canvas = palette;
                palette = temp;
            }
        }

        if (!canvas.isEmpty() && canvas.getItem() instanceof ItemCanvas) {
            // Feature 10: a waxed (protected) canvas can no longer be edited.
            if (canvas.getOrDefault(Items.CANVAS_WAXED.get(), false)) {
                pl.displayClientMessage(Component.translatable("xercapaint.protected").withStyle(ChatFormatting.RED), true);
                return;
            }

            boolean paletteIsReal = !palette.isEmpty() && palette.getItem() == Items.ITEM_PALETTE.get();

            // Feature 1: consume per-colour paint charge from the palette based on which pixels changed.
            if (Config.dyeCostEnabled() && paletteIsReal && !pl.isCreative()) {
                Items.PaletteCharges charges = palette.getOrDefault(Items.PALETTE_CHARGES.get(), Items.PaletteCharges.empty());
                if (charges.total() <= 0) {
                    pl.displayClientMessage(Component.translatable("xercapaint.out_of_paint").withStyle(ChatFormatting.RED), true);
                    return;
                }
                int[] newCharges = consumeCharges(charges.charges(),
                        canvas.get(Items.CANVAS_PIXELS.get()), msg.pixels(),
                        palette.getOrDefault(Items.PALETTE_BASIC_COLORS.get(), Items.BasicColors.empty()));
                palette.set(Items.PALETTE_CHARGES.get(), new Items.PaletteCharges(newCharges));
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

            if (!palette.isEmpty() && palette.getItem() == Items.ITEM_PALETTE.get()) {
                palette.set(Items.PALETTE_CUSTOM_COLORS.get(), new ItemPalette.ComponentCustomColor(msg.paletteColors()));
            }

            if (entityEasel instanceof EntityEasel easel) {
                easel.setItem(canvas, false);
                easel.setPainter(null);
            }

            Mod.LOGGER.debug("Handling canvas update: Name: {} V: {}", msg.canvasId(), msg.version());
        }
    }

    /**
     * Consumes per-colour charge for every pixel that changed. For each changed, non-transparent pixel we estimate
     * which basic colours compose its colour (see {@link PaletteUtil#estimateComposition}) and spread one unit of
     * cost across those colours proportionally. Fractional consumption is accumulated and applied once at the end.
     */
    private static int[] consumeCharges(int[] charges, java.util.List<Integer> oldPixels, int[] newPixels,
                                        Items.BasicColors basic) {
        boolean[] available = new boolean[16];
        for (int i = 0; i < 16; i++) {
            available[i] = basic.get(i) > 0;
        }

        double[] acc = new double[16];
        // Cache composition per distinct colour so a big single-colour fill isn't re-estimated thousands of times.
        java.util.HashMap<Integer, int[]> cache = new java.util.HashMap<>();

        boolean hadOld = oldPixels != null && !oldPixels.isEmpty();
        for (int i = 0; i < newPixels.length; i++) {
            int now = newPixels[i];
            if ((now >>> 24) == 0) {
                continue; // transparent pixel: erased or blank, no paint used
            }
            int old = hadOld && i < oldPixels.size() ? oldPixels.get(i) : 0;
            if (now == old) {
                continue; // unchanged pixel costs nothing
            }
            int[] counts = cache.computeIfAbsent(now & 0xFFFFFF, rgb -> PaletteUtil.estimateComposition(rgb, available));
            int totalUnits = 0;
            for (int c : counts) {
                totalUnits += c;
            }
            if (totalUnits == 0) {
                continue;
            }
            for (int c = 0; c < 16; c++) {
                if (counts[c] > 0) {
                    acc[c] += (double) counts[c] / totalUnits;
                }
            }
        }

        int[] result = charges.clone();
        for (int c = 0; c < 16; c++) {
            int cost = (int) Math.round(acc[c]);
            if (cost > 0) {
                result[c] = Math.max(0, result[c] - cost);
            }
        }
        return result;
    }

    public static void handle(CanvasUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> processMessage(packet, (ServerPlayer) context.player()));
    }
}
