package xerca.xercapaint.packets;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import xerca.xercapaint.Mod;
import xerca.xercapaint.entity.EntityEasel;
import xerca.xercapaint.item.ItemCanvas;
import xerca.xercapaint.item.ItemPalette;
import xerca.xercapaint.item.Items;

import javax.annotation.Nullable;

public class CanvasUpdateUtils {
    @Nullable
    public static EntityEasel getEntityEasel(ServerPlayer pl, int easelId) {
        if (easelId == -1) {
            return null;
        }
        Entity entityEasel = pl.level().getEntity(easelId);
        if (entityEasel == null) {
            Mod.LOGGER.error("CanvasUpdateUtils: Easel entity not found! easelId: {}", easelId);
            return null;
        }
        if (!(entityEasel instanceof EntityEasel easel)) {
            Mod.LOGGER.error("CanvasUpdateUtils: Entity found is not an easel! easelId: {}", easelId);
            return null;
        }
        return easel;
    }

    @Nullable
    public static ItemStack getCanvas(ServerPlayer pl, @Nullable EntityEasel easel) {
        if (easel != null) {
            ItemStack canvas;
            canvas = easel.getItem();
            if (!(canvas.getItem() instanceof ItemCanvas)) {
                Mod.LOGGER.error("CanvasUpdateUtils: Canvas not found inside easel!");
                return null;
            }
            return canvas;
        } else if (pl.getMainHandItem().getItem() instanceof ItemCanvas) {
            return pl.getMainHandItem();
        } else if (pl.getOffhandItem().getItem() instanceof ItemCanvas) {
            return pl.getOffhandItem();
        }

        return null;
    }

    @Nullable
    public static ItemStack getPalette(ServerPlayer pl) {
        if (pl.getMainHandItem().getItem() instanceof ItemPalette) {
            return pl.getMainHandItem();
        } else if (pl.getOffhandItem().getItem() instanceof ItemPalette) {
            return pl.getOffhandItem();
        }

        Mod.LOGGER.error("CanvasUpdateUtils: Palette not found on player's hands!");
        return null;
    }

    /**
     * Paint charges are tracked by the client while painting, so the server must not trust the reported values blindly.
     * A colour can only ever go down while painting: anything above the stored charge (or a malformed array) is ignored.
     */
    public static Items.PaletteCharges sanitizeCharges(Items.PaletteCharges current, int[] reported) {
        if (reported == null || reported.length != Items.PaletteCharges.SIZE) {
            return current;
        }
        int[] result = new int[Items.PaletteCharges.SIZE];
        for (int i = 0; i < Items.PaletteCharges.SIZE; i++) {
            result[i] = Math.max(0, Math.min(current.get(i), reported[i]));
        }
        return new Items.PaletteCharges(result);
    }

    public static void consumePaletteCharges(ItemStack palette, int[] reported) {
        Items.PaletteCharges current = palette.getOrDefault(Items.PALETTE_CHARGES.get(), Items.PaletteCharges.empty());
        palette.set(Items.PALETTE_CHARGES.get(), sanitizeCharges(current, reported));
    }

    /**
     * Signed and waxed canvases are final and must not be overwritten by canvas update packets.
     */
    public static boolean isLocked(ItemStack canvas) {
        return canvas.getOrDefault(Items.CANVAS_WAXED.get(), false)
                || canvas.getOrDefault(Items.CANVAS_GENERATION.get(), 0) > 0;
    }
}
