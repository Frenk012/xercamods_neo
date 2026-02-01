package xerca.xercapaint.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import xerca.xercapaint.Mod;
import xerca.xercapaint.item.ItemCanvas;
import xerca.xercapaint.item.Items;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class CanvasItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation backLocation = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/birch_planks.png");
    private static final ResourceLocation emptyCanvasLocation = Mod.id("textures/block/empty.png");

    public CanvasItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    private static boolean loggedOnce = false;

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack matrixStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        if (!loggedOnce) {
            Mod.LOGGER.info("XercaPaint: CanvasItemRenderer.renderByItem called for {}", stack.getItem());
            loggedOnce = true;
        }

        if (stack.getItem() instanceof ItemCanvas itemCanvas) {
            boolean rendered = false;
            if (stack.get(Items.CANVAS_PIXELS.get()) != null) {
                CanvasTextureManager.CanvasInstance canvasIns = CanvasTextureManager.INSTANCE.getCanvasInstance(stack, itemCanvas.getWidth(), itemCanvas.getHeight());
                if (canvasIns != null) {
                    canvasIns.renderForItem(matrixStack, buffer, combinedLight);
                    rendered = true;
                }
            }

            if (!rendered) {
                renderEmptyCanvas(matrixStack, buffer, itemCanvas.getWidth(), itemCanvas.getHeight(), combinedLight);
            }
        }
    }

    private void addVertex(VertexConsumer vb, Matrix4f m, PoseStack.Pose pose, double x, double y, double z, float tx, float ty, int lightmap, float xOff, float yOff, float zOff) {
        vb.addVertex(m, (float) x, (float) y, (float) z).setColor(255, 255, 255, 255).setUv(tx, ty).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(pose, xOff, yOff, zOff);
    }

    private void renderEmptyCanvas(PoseStack ms, MultiBufferSource buffer, float width, float height, int packedLight) {
        final float wScale = width / 16.0f;
        final float hScale = height / 16.0f;

        ms.pushPose();

        float xOffset = 0;
        float yOffset = 0;
        float zOffset = -1;

        // Item rendering: center in unit space for BEWLR compatibility
        float scale = 1.0f / (32.0f * Math.max(wScale, hScale));
        ms.translate(0.5, 0.5, 0.5);
        ms.scale(scale, scale, scale);
        // Center the canvas
        ms.translate(-16.0f * wScale, -16.0f * hScale, 0);

        Matrix4f m = ms.last().pose();
        PoseStack.Pose pose = ms.last();
        VertexConsumer vb = buffer.getBuffer(RenderType.entitySolid(emptyCanvasLocation));

        // Draw the front
        addVertex(vb, m, pose, 0.0F, 32.0F * hScale, -1.0F, 1.0F, 0.0F, packedLight, xOffset, yOffset, zOffset);
        addVertex(vb, m, pose, 32.0F * wScale, 32.0F * hScale, -1.0F, 0.0F, 0.0F, packedLight, xOffset, yOffset, zOffset);
        addVertex(vb, m, pose, 32.0F * wScale, 0.0F, -1.0F, 0.0F, 1.0F, packedLight, xOffset, yOffset, zOffset);
        addVertex(vb, m, pose, 0.0F, 0.0F, -1.0F, 1.0F, 1.0F, packedLight, xOffset, yOffset, zOffset);

        vb = buffer.getBuffer(RenderType.entitySolid(backLocation));
        // Draw the back and sides
        final float sideWidth = 1.0F / 16.0F;

        RenderSystem.setShaderTexture(0, backLocation);
        addVertex(vb, m, pose, 0.0D, 0.0D, 1.0D, 0.0F, 0.0F, packedLight, xOffset, yOffset, zOffset);
        addVertex(vb, m, pose, 32.0D * wScale, 0.0D, 1.0D, 1.0F, 0.0F, packedLight, xOffset, yOffset, zOffset);
        addVertex(vb, m, pose, 32.0D * wScale, 32.0D * hScale, 1.0D, 1.0F, 1.0F, packedLight, xOffset, yOffset, zOffset);
        addVertex(vb, m, pose, 0.0D, 32.0D * hScale, 1.0D, 0.0F, 1.0F, packedLight, xOffset, yOffset, zOffset);

        // Sides
        addVertex(vb, m, pose, 0.0D, 0.0D, 1.0D, sideWidth, 0.0F, packedLight, xOffset, yOffset, zOffset);
        addVertex(vb, m, pose, 0.0D, 32.0D * hScale, 1.0D, sideWidth, 1.0F, packedLight, xOffset, yOffset, zOffset);
        addVertex(vb, m, pose, 0.0D, 32.0D * hScale, -1.0D, 0.0F, 1.0F, packedLight, xOffset, yOffset, zOffset);
        addVertex(vb, m, pose, 0.0D, 0.0D, -1.0D, 0.0F, 0.0F, packedLight, xOffset, yOffset, zOffset);

        addVertex(vb, m, pose, 0.0D, 32.0D * hScale, 1.0F, 0.0F, 0.0F, packedLight, xOffset, yOffset, zOffset);
        addVertex(vb, m, pose, 32.0D * wScale, 32.0D * hScale, 1.0F, 1.0F, 0.0F, packedLight, xOffset, yOffset, zOffset);
        addVertex(vb, m, pose, 32.0D * wScale, 32.0D * hScale, -1.0F, 1.0F, sideWidth, packedLight, xOffset, yOffset, zOffset);
        addVertex(vb, m, pose, 0.0D, 32.0D * hScale, -1.0F, 0.0F, sideWidth, packedLight, xOffset, yOffset, zOffset);

        addVertex(vb, m, pose, 32.0D * wScale, 0.0D, -1.0F, 0.0F, 0.0F, packedLight, xOffset, yOffset, zOffset);
        addVertex(vb, m, pose, 32.0D * wScale, 32.0D * hScale, -1.0F, 0.0F, 1.0F, packedLight, xOffset, yOffset, zOffset);
        addVertex(vb, m, pose, 32.0D * wScale, 32.0D * hScale, 1.0F, sideWidth, 1.0F, packedLight, xOffset, yOffset, zOffset);
        addVertex(vb, m, pose, 32.0D * wScale, 0.0D, 1.0F, sideWidth, 0.0F, packedLight, xOffset, yOffset, zOffset);

        addVertex(vb, m, pose, 0.0D, 0.0D, -1.0F, 0.0F, 1.0F, packedLight, xOffset, yOffset, zOffset);
        addVertex(vb, m, pose, 32.0D * wScale, 0.0D, -1.0F, 1.0F, 1.0F, packedLight, xOffset, yOffset, zOffset);
        addVertex(vb, m, pose, 32.0D * wScale, 0.0D, 1.0F, 1.0F, 1.0F - sideWidth, packedLight, xOffset, yOffset, zOffset);
        addVertex(vb, m, pose, 0.0D, 0.0D, 1.0F, 0.0F, 1.0F - sideWidth, packedLight, xOffset, yOffset, zOffset);

        ms.popPose();
    }
}
