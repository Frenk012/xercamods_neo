package xerca.xercapaint.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import xerca.xercapaint.item.ItemCanvas;
import xerca.xercapaint.item.Items;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class CanvasItemRenderer extends BlockEntityWithoutLevelRenderer {

    public CanvasItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    /**
     * Glass paintings are mostly transparent, so in inventories they get a faint blue-ish glass tint.
     */
    private static final int GLASS_INVENTORY_TINT = 0xFFDCE6FF;

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack matrixStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        if (stack.getItem() instanceof ItemCanvas itemCanvas) {
            boolean glass = itemCanvas.isGlass();
            int tint = (glass && displayContext == ItemDisplayContext.GUI) ? GLASS_INVENTORY_TINT : CanvasTextureManager.NO_TINT;
            if (stack.get(Items.CANVAS_PIXELS.get()) != null) {
                CanvasTextureManager.CanvasInstance canvasIns = CanvasTextureManager.INSTANCE.getCanvasInstance(stack, itemCanvas.getWidth(), itemCanvas.getHeight());
                if (canvasIns != null) {
                    canvasIns.renderForItem(matrixStack, buffer, combinedLight, glass,
                            CanvasTextureManager.sidesActive(stack), CanvasTextureManager.sidePixels(stack), tint);
                    return;
                }
            }
            CanvasTextureManager.INSTANCE.renderEmptyForItem(matrixStack, buffer, itemCanvas.getWidth(), itemCanvas.getHeight(), combinedLight, glass, tint);
        }
    }
}
