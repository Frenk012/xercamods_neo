package xerca.xercapaint.client;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import xerca.xercapaint.Mod;
import xerca.xercapaint.PaletteUtil;
import xerca.xercapaint.entity.EntityCanvas;
import xerca.xercapaint.item.Items;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public class CanvasTextureManager {
    public static final CanvasTextureManager INSTANCE = new CanvasTextureManager();

    public static final ResourceLocation BACK_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/birch_planks.png");
    private static final int[] EMPTY_PIXELS;

    static {
        EMPTY_PIXELS = new int[1024];
        for (int i = 0; i < 1024; i++) {
            EMPTY_PIXELS[i] = PaletteUtil.Color.WHITE.rgbVal();
        }
    }

    private TextureManager textureManager;
    private final Map<String, CanvasInstance> loadedCanvases = Maps.newHashMap();

    private static final ResourceLocation EMPTY_CANVAS_TEXTURE = Mod.id("textures/block/empty.png");

    private CanvasTextureManager() {}

    public void init() {
        if (this.textureManager == null) {
            this.textureManager = Minecraft.getInstance().getTextureManager();
        }
    }

    private void ensureInitialized() {
        if (this.textureManager == null) {
            init();
        }
    }

    public CanvasInstance getCanvasInstance(ItemStack canvasStack, int width, int height) {
        String canvasId = canvasStack.get(Items.CANVAS_ID.get());
        if (canvasId == null) {
            Mod.LOGGER.debug("CanvasTextureManager: canvasId is null for stack");
            return null;
        }

        int version = canvasStack.getOrDefault(Items.CANVAS_VERSION.get(), 1);
        if (!EntityCanvas.PICTURES.containsKey(canvasId) || EntityCanvas.PICTURES.get(canvasId).version() < version) {
            var pixels = canvasStack.get(Items.CANVAS_PIXELS.get());
            if (pixels != null) {
                EntityCanvas.PICTURES.put(canvasId, new EntityCanvas.Picture(version, pixels.stream().mapToInt(i -> i).toArray()));
            }
        }
        return getCanvasInstance(canvasId, version, width, height);
    }

    /**
     * Render an empty canvas for easel display (matches the positioning of canvases with content)
     */
    public void renderEmptyForEasel(PoseStack ms, MultiBufferSource buffer, int width, int height, int packedLight) {
        final float wScale = width / 16.0f;
        final float hScale = height / 16.0f;

        ms.pushPose();

        float f = 1.0f / 32.0f;
        // Easel rendering: center the canvas horizontally
        ms.translate(0.5, 0.5, 0.5);
        if (wScale > 1 || hScale > 1) {
            f /= 3.3f;
        } else {
            f /= 2.0f;
        }
        ms.mulPose(Axis.YP.rotationDegrees(180));
        ms.scale(f, f, f);
        // Center the canvas horizontally: canvas width is 32*wScale, so translate by half
        ms.translate(-16.0f * wScale, 0, 0);

        float xOffset = 0;
        float yOffset = 0;
        float zOffset = -1;

        Matrix4f m = ms.last().pose();
        PoseStack.Pose pose = ms.last();
        VertexConsumer vb = buffer.getBuffer(RenderType.entitySolid(EMPTY_CANVAS_TEXTURE));

        // Draw the front
        addVertex(vb, m, pose, 0.0F, 32.0F * hScale, -1.0F, 1.0F, 0.0F, packedLight, xOffset, yOffset, zOffset);
        addVertex(vb, m, pose, 32.0F * wScale, 32.0F * hScale, -1.0F, 0.0F, 0.0F, packedLight, xOffset, yOffset, zOffset);
        addVertex(vb, m, pose, 32.0F * wScale, 0.0F, -1.0F, 0.0F, 1.0F, packedLight, xOffset, yOffset, zOffset);
        addVertex(vb, m, pose, 0.0F, 0.0F, -1.0F, 1.0F, 1.0F, packedLight, xOffset, yOffset, zOffset);

        vb = buffer.getBuffer(RenderType.entitySolid(BACK_TEXTURE));
        // Draw the back and sides
        final float sideWidth = 1.0F / 16.0F;
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

    private void addVertex(VertexConsumer vb, Matrix4f m, PoseStack.Pose pose, double x, double y, double z, float tx, float ty, int lightmap, float xOff, float yOff, float zOff) {
        Vector3f normal = new Vector3f(xOff, yOff, zOff);
        normal.mul(pose.normal());
        vb.addVertex(m, (float) x, (float) y, (float) z)
                .setColor(255, 255, 255, 255)
                .setUv(tx, ty)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(lightmap)
                .setNormal(normal.x(), normal.y(), normal.z());
    }

    public CanvasInstance getCanvasInstance(String canvasId, int version, int width, int height) {
        ensureInitialized();

        CanvasInstance instance = this.loadedCanvases.get(canvasId);
        if (instance == null) {
            instance = new CanvasInstance(canvasId, version, width, height);
            this.loadedCanvases.put(canvasId, instance);
        } else {
            if (instance.version < version || !instance.loaded) {
                instance.updateCanvasTexture(canvasId, version);
            }
        }
        return instance;
    }

    @OnlyIn(Dist.CLIENT)
    public class CanvasInstance implements AutoCloseable {
        int version = 0;
        final int width;
        final int height;
        boolean loaded;
        boolean started;
        public final DynamicTexture canvasTexture;
        public final ResourceLocation location;

        private CanvasInstance(String canvasId, int version, int width, int height) {
            this.started = false;
            this.loaded = false;
            this.width = width;
            this.height = height;
            this.canvasTexture = new DynamicTexture(width, height, true);
            this.location = CanvasTextureManager.this.textureManager.register("canvas/" + canvasId, this.canvasTexture);

            updateCanvasTexture(canvasId, version);
        }

        void updateCanvasTexture(String canvasId, int version) {
            this.version = version;
            int[] pixels = EMPTY_PIXELS;
            if (EntityCanvas.PICTURES.containsKey(canvasId)) {
                pixels = EntityCanvas.PICTURES.get(canvasId).pixels();
                loaded = true;
            }
            if (loaded || !started) {
                if (pixels.length < height * width) {
                    Mod.LOGGER.warn("Pixels array length ({}) is smaller than canvas area ({})", pixels.length, height * width);
                    return;
                }

                NativeImage image = canvasTexture.getPixels();
                if (image != null) {
                    for (int i = 0; i < height; ++i) {
                        for (int j = 0; j < width; ++j) {
                            int k = j + i * width;
                            // Convert from ARGB to ABGR (swap red and blue channels)
                            int argb = pixels[k];
                            int a = (argb >> 24) & 0xFF;
                            int r = (argb >> 16) & 0xFF;
                            int g = (argb >> 8) & 0xFF;
                            int b = argb & 0xFF;
                            int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                            image.setPixelRGBA(j, i, abgr);
                        }
                    }
                }
                canvasTexture.upload();
            }
            this.started = true;
        }

        public void render(@Nullable EntityCanvas canvas, float yaw, float pitch, PoseStack ms, MultiBufferSource buffer, Direction facing, int packedLight) {
            render(canvas, yaw, pitch, ms, buffer, facing, packedLight, false);
        }

        public void renderForItem(PoseStack ms, MultiBufferSource buffer, int packedLight) {
            render(null, 0, 0, ms, buffer, Direction.UP, packedLight, true);
        }

        public void render(@Nullable EntityCanvas canvas, float yaw, float pitch, PoseStack ms, MultiBufferSource buffer, Direction facing, int packedLight, boolean isItemRendering) {
            final float wScale = width / 16.0f;
            final float hScale = height / 16.0f;

            ms.pushPose();

            float xOffset = facing.getStepX();
            float yOffset = facing.getStepY();
            float zOffset = facing.getStepZ();

            boolean canvasIsNull = canvas == null;
            if (!canvasIsNull) {
                int rotation = canvas.getRotation();
                if (rotation > 0) {
                    ms.mulPose(Axis.XP.rotationDegrees(pitch));
                    ms.mulPose(Axis.YP.rotationDegrees(180 - yaw));
                    ms.mulPose(Axis.ZP.rotationDegrees(90 * rotation));
                    ms.mulPose(Axis.YP.rotationDegrees(-180 + yaw));
                    ms.mulPose(Axis.XP.rotationDegrees(-pitch));
                }
            }

            float f = 1.0f / 32.0f;
            if (!canvasIsNull) {
                // Entity canvas rendering (wall placement)
                if (facing.getAxis().isHorizontal()) {
                    ms.translate(zOffset * 0.5d * wScale, -0.5d * hScale, -xOffset * 0.5d * wScale);
                } else {
                    ms.translate(0.5 * wScale, 0 * hScale, (yOffset > 0 ? 0.5 : -0.5) * wScale);
                }
                xOffset = 0;
                yOffset = 0;
                zOffset = -1;
                ms.mulPose(Axis.XP.rotationDegrees(pitch));
                ms.mulPose(Axis.YP.rotationDegrees(180 - yaw));
                ms.scale(f, f, f);
            } else if (isItemRendering) {
                // Item rendering (BEWLR): center in unit space
                float scale = 1.0f / (32.0f * Math.max(wScale, hScale));
                ms.translate(0.5, 0.5, 0.5);
                ms.scale(scale, scale, scale);
                ms.translate(-16.0f * wScale, -16.0f * hScale, 0);
                xOffset = 0;
                yOffset = 0;
                zOffset = -1;
            } else {
                // Easel rendering: center the canvas horizontally
                ms.translate(0.5, 0.5, 0.5);
                if (wScale > 1 || hScale > 1) {
                    f /= 3.3f;
                } else {
                    f /= 2.0f;
                }
                ms.mulPose(Axis.YP.rotationDegrees(180));
                ms.scale(f, f, f);
                // Center the canvas horizontally: canvas width is 32*wScale, so translate by half
                ms.translate(-16.0f * wScale, 0, 0);
                xOffset = 0;
                yOffset = 0;
                zOffset = -1;
            }

            RenderSystem.setShaderTexture(0, location);
            Matrix4f m = ms.last().pose();
            PoseStack.Pose pose = ms.last();
            VertexConsumer vb = buffer.getBuffer(RenderType.entitySolid(location));

            // Draw the front
            addVertex(vb, m, pose, 0.0F, 32.0F * hScale, -1.0F, 1.0F, 0.0F, packedLight, xOffset, yOffset, zOffset);
            addVertex(vb, m, pose, 32.0F * wScale, 32.0F * hScale, -1.0F, 0.0F, 0.0F, packedLight, xOffset, yOffset, zOffset);
            addVertex(vb, m, pose, 32.0F * wScale, 0.0F, -1.0F, 0.0F, 1.0F, packedLight, xOffset, yOffset, zOffset);
            addVertex(vb, m, pose, 0.0F, 0.0F, -1.0F, 1.0F, 1.0F, packedLight, xOffset, yOffset, zOffset);

            vb = buffer.getBuffer(RenderType.entitySolid(BACK_TEXTURE));
            // Draw the back and sides
            final float sideWidth = 1.0F / 16.0F;
            RenderSystem.setShaderTexture(0, BACK_TEXTURE);
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

        private void addVertex(VertexConsumer vb, Matrix4f m, PoseStack.Pose pose, double x, double y, double z, float tx, float ty, int lightmap, float xOff, float yOff, float zOff) {
            Vector3f normal = new Vector3f(xOff, yOff, zOff);
            normal.mul(pose.normal());
            vb.addVertex(m, (float) x, (float) y, (float) z)
                    .setColor(255, 255, 255, 255)
                    .setUv(tx, ty)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(lightmap)
                    .setNormal(normal.x(), normal.y(), normal.z());
        }

        @Override
        public void close() {
            this.canvasTexture.close();
        }
    }
}
