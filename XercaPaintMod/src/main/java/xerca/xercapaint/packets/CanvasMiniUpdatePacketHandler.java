package xerca.xercapaint.packets;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import xerca.xercapaint.Config;
import xerca.xercapaint.Mod;
import xerca.xercapaint.entity.EntityEasel;
import xerca.xercapaint.item.ItemCanvas;
import xerca.xercapaint.item.Items;

import java.util.Arrays;

public class CanvasMiniUpdatePacketHandler {
    public static void processMessage(CanvasMiniUpdatePacket msg, ServerPlayer pl) {
        EntityEasel entityEasel = CanvasUpdateUtils.getEntityEasel(pl, msg.easelId());
        if (msg.easelId() > -1 && entityEasel == null) {
            Mod.LOGGER.error("CanvasMiniUpdatePacketHandler: Easel entity not found! easelId: {}", msg.easelId());
            return;
        }

        ItemStack canvas = CanvasUpdateUtils.getCanvas(pl, entityEasel);
        ItemStack palette = CanvasUpdateUtils.getPalette(pl);

        if (canvas != null && !canvas.isEmpty() && canvas.getItem() instanceof ItemCanvas) {
            boolean paletteIsReal = palette != null && !palette.isEmpty() && palette.getItem() == Items.ITEM_PALETTE.get();

            if (Config.dyeCostEnabled() && paletteIsReal && !pl.isCreative()) {
                palette.set(Items.PALETTE_CHARGES.get(), new Items.PaletteCharges(msg.basicColorsCharges()));
            }

            canvas.set(Items.CANVAS_PIXELS.get(), Arrays.stream(msg.pixels()).boxed().toList());
            canvas.set(Items.CANVAS_ID.get(), msg.canvasId());
            canvas.set(Items.CANVAS_VERSION.get(), msg.version());
            canvas.set(Items.CANVAS_GENERATION.get(), 0);

            if (entityEasel instanceof EntityEasel easel) {
                easel.setItem(canvas, false);
            }

            Mod.LOGGER.debug("Handling canvas update: Name: {} V: {}", msg.canvasId(), msg.version());
        }
    }

    public static void handle(CanvasMiniUpdatePacket packet, IPayloadContext context) {
        if (packet != null) {
            context.enqueueWork(() -> processMessage(packet, (ServerPlayer) context.player()));
        }
    }
}
