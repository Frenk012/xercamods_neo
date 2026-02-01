package xerca.xercapaint.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import xerca.xercapaint.CanvasType;
import xerca.xercapaint.Mod;
import xerca.xercapaint.entity.EntityEasel;
import xerca.xercapaint.item.ItemCanvas;
import xerca.xercapaint.item.Items;

import javax.annotation.ParametersAreNonnullByDefault;

@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
@ParametersAreNonnullByDefault
public class RenderEntityEasel extends EntityRenderer<EntityEasel> {
    protected final EaselModel model;
    public static RenderEntityEasel theInstance;
    private static final ResourceLocation woodTexture = Mod.id("textures/block/birch_long.png");

    public RenderEntityEasel(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new EaselModel(ctx.bakeLayer(ModClient.EASEL_MAIN_LAYER));
        theInstance = this;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(EntityEasel entity) {
        return woodTexture;
    }

    @Override
    public void render(EntityEasel easel, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(-easel.getYRot()));

        this.model.setupAnim(easel, 0, 0, 0, 0, 0);

        poseStack.mulPose(new Quaternionf().rotationXYZ((float) Math.PI, 0, 0));
        poseStack.translate(0, -1.5, 0);

        RenderType renderType = this.model.renderType(woodTexture);
        VertexConsumer vertexConsumer = buffer.getBuffer(renderType);
        int overlay = OverlayTexture.pack(OverlayTexture.u(0), OverlayTexture.v(false));
        this.model.renderToBuffer(poseStack, vertexConsumer, packedLight, overlay, -1);

        // Render canvas layer
        renderCanvas(easel, poseStack, buffer, packedLight);

        poseStack.popPose();
        super.render(easel, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private void renderCanvas(EntityEasel easel, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        ItemStack itemstack = easel.getItem();
        if (itemstack.getItem() instanceof ItemCanvas itemCanvas) {
            poseStack.pushPose();

            CanvasType canvasType = itemCanvas.getCanvasType();
            switch (canvasType) {
                case SMALL -> {
                    poseStack.scale(1.5F, 1.5f, 1.5f);
                    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
                    poseStack.mulPose(Axis.XP.rotationDegrees(-15.0F));
                    poseStack.translate(-0.5, -1.17, -0.5);
                }
                case LARGE -> {
                    poseStack.scale(2F, 2f, 2f);
                    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
                    poseStack.mulPose(Axis.XP.rotationDegrees(-15.0F));
                    poseStack.translate(-0.5, -1.015, -0.5);
                }
                case LONG -> {
                    poseStack.scale(2F, 2f, 2f);
                    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
                    poseStack.mulPose(Axis.XP.rotationDegrees(-15.0F));
                    poseStack.translate(-0.5, -0.915, -0.5);
                }
                case TALL -> {
                    poseStack.scale(2F, 2f, 2f);
                    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
                    poseStack.mulPose(Axis.XP.rotationDegrees(-15.0F));
                    poseStack.translate(-0.5, -1.015, -0.5);
                }
            }

            // Use CanvasTextureManager directly for rendering
            if (itemstack.get(Items.CANVAS_PIXELS.get()) != null) {
                CanvasTextureManager.CanvasInstance canvasIns = CanvasTextureManager.INSTANCE.getCanvasInstance(itemstack, itemCanvas.getWidth(), itemCanvas.getHeight());
                if (canvasIns != null) {
                    canvasIns.render(null, 0, 0, poseStack, bufferSource, Direction.UP, packedLight);
                }
            } else {
                // Render empty canvas with same positioning as filled canvas
                CanvasTextureManager.INSTANCE.renderEmptyForEasel(poseStack, bufferSource, itemCanvas.getWidth(), itemCanvas.getHeight(), packedLight);
            }

            poseStack.popPose();
        }
    }

    @Override
    protected boolean shouldShowName(EntityEasel easel) {
        HitResult result = Minecraft.getInstance().hitResult;
        if (result instanceof EntityHitResult entityHitResult) {
            if (Minecraft.renderNames() && entityHitResult.getEntity() == easel && !easel.getItem().isEmpty() && ItemCanvas.hasTitle(easel.getItem())) {
                double distanceSquared = this.entityRenderDispatcher.distanceToSqr(easel);
                float range = easel.isDiscrete() ? 32.0F : 64.0F;
                return distanceSquared < (double) (range * range);
            }
        }
        return false;
    }

    @Override
    protected void renderNameTag(EntityEasel easel, Component displayName, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float partialTick) {
        poseStack.pushPose();
        // Move name tag above the easel (positive Y is up)
        poseStack.translate(0, 1.8, 0);
        super.renderNameTag(easel, ItemCanvas.getFullLabel(easel.getItem()), poseStack, buffer, packedLight, partialTick);
        poseStack.popPose();
    }
}
