package xerca.xercapaint.packets;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import xerca.xercapaint.Mod;
import xerca.xercapaint.entity.EntityEasel;
import xerca.xercapaint.item.ItemCanvas;
import xerca.xercapaint.item.ItemPalette;

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
}
