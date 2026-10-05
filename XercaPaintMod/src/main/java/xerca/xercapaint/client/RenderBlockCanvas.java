package xerca.xercapaint.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import xerca.xercapaint.block.BlockCanvas;
import xerca.xercapaint.block_entity.TileEntityCanvas;
import xerca.xercapaint.entity.EntityCanvas;

@OnlyIn(Dist.CLIENT)
public class RenderBlockCanvas implements BlockEntityRenderer<TileEntityCanvas> {

    private final Font font;

    public RenderBlockCanvas(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(TileEntityCanvas canvas, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (canvas.getCanvasId().isEmpty()) {
            return;
        }

        poseStack.pushPose();

        Direction facing = canvas.getBlockState().getValue(BlockCanvas.FACING);
        int rotation = canvas.getRotation();
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Position at center of block
        poseStack.translate(0.5, 0.5, 0.5);

        // Rotate based on facing direction
        applyFacingRotation(poseStack, facing);

        // Apply canvas rotation (for square canvases)
        if (rotation > 0) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(-90 * rotation));
        }

        // Get or create canvas texture instance
        CanvasTextureManager.CanvasInstance canvasInstance = null;
        String canvasId = canvas.getCanvasId();
        int version = canvas.getVersion();

        EntityCanvas.Picture picture = EntityCanvas.PICTURES.get(canvasId);
        if (picture != null && picture.pixels() != null) {
            canvasInstance = CanvasTextureManager.INSTANCE.getCanvasInstance(canvasId, version, width, height);
        }

        // Position canvas so the block is the bottom-left corner (like vanilla paintings):
        // it extends to the left (-X) and up (+Y) from the block position, with the front flush at z = 0.
        float wBlocks = width / 16.0f;
        poseStack.translate(0.5 - wBlocks, -0.5, 1.0 / 32.0);
        poseStack.scale(1.0f / 32.0f, 1.0f / 32.0f, 1.0f / 32.0f);
        CanvasTextureManager.INSTANCE.drawModel(poseStack.last(), buffer, packedLight,
                canvasInstance != null ? canvasInstance.location : null, width, height,
                canvas.isGlass(), canvas.isSidesActive(), canvas.getSidePixels(), CanvasTextureManager.NO_TINT);

        poseStack.popPose();

        // Render name tag if applicable
        renderNameTagIfNeeded(canvas, poseStack, buffer, packedLight, facing);
    }

    private void applyFacingRotation(PoseStack poseStack, Direction facing) {
        // Move canvas to be flush against the supporting block (0.5 - thickness)
        final double offset = 0.5 - 0.03125;
        switch (facing) {
            case NORTH -> {
                // Canvas faces north, attached to block to the south
                poseStack.translate(0, 0, offset);
            }
            case SOUTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
                poseStack.translate(0, 0, offset);
            }
            case WEST -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
                poseStack.translate(0, 0, offset);
            }
            case EAST -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(-90));
                poseStack.translate(0, 0, offset);
            }
            case UP -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                poseStack.translate(0, 0, offset);
            }
            case DOWN -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                poseStack.translate(0, 0, offset);
            }
        }
    }

    private void renderNameTagIfNeeded(TileEntityCanvas canvas, PoseStack poseStack,
                                        MultiBufferSource buffer, int packedLight, Direction facing) {
        if (!canvas.hasTitle()) {
            return;
        }

        // Only show name when looking at the canvas
        Minecraft mc = Minecraft.getInstance();
        HitResult hitResult = mc.hitResult;
        if (!(hitResult instanceof BlockHitResult blockHit)) {
            return;
        }

        if (!blockHit.getBlockPos().equals(canvas.getBlockPos())) {
            return;
        }

        if (!Minecraft.renderNames()) {
            return;
        }

        // Build the label
        String title = canvas.getTitle();
        String author = canvas.getAuthor();
        Component label;
        if (!author.isEmpty()) {
            label = Component.literal(title + " ")
                    .append(Component.translatable("canvas.byAuthor", author));
        } else {
            label = Component.literal(title);
        }

        poseStack.pushPose();

        // Position above the canvas, centered on the canvas dimensions
        // The canvas center position depends on the facing direction
        float wBlocks = canvas.getWidth() / 16.0f;
        float hBlocks = canvas.getHeight() / 16.0f;

        // Calculate center offset based on facing
        // Canvas extends in the counter-clockwise direction from the block
        double centerX = 0.5;
        double centerZ = 0.5;
        double centerY = hBlocks + 0.2;

        switch (facing) {
            case NORTH -> centerX = 0.5 - (wBlocks - 1) * 0.5;
            case SOUTH -> centerX = 0.5 + (wBlocks - 1) * 0.5;
            case WEST -> centerZ = 0.5 + (wBlocks - 1) * 0.5;
            case EAST -> centerZ = 0.5 - (wBlocks - 1) * 0.5;
            case UP, DOWN -> {} // Keep centered at 0.5, 0.5
        }

        poseStack.translate(centerX, centerY, centerZ);

        // Face the player
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(0.025f, -0.025f, 0.025f);

        float textWidth = font.width(label);
        float x = -textWidth / 2;

        // Background
        int bgColor = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        font.drawInBatch(label, x, 0, 0xFFFFFF, false, poseStack.last().pose(), buffer,
                Font.DisplayMode.SEE_THROUGH, bgColor, packedLight);
        font.drawInBatch(label, x, 0, 0xFFFFFF, false, poseStack.last().pose(), buffer,
                Font.DisplayMode.NORMAL, 0, packedLight);

        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(TileEntityCanvas blockEntity) {
        return true;
    }
}
