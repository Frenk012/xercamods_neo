package xerca.xercapaint.client;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.NativeImage;
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
import xerca.xercapaint.CanvasSides;
import xerca.xercapaint.Mod;
import xerca.xercapaint.PaletteUtil;
import xerca.xercapaint.entity.EntityCanvas;
import xerca.xercapaint.item.Items;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class CanvasTextureManager {
    public static final CanvasTextureManager INSTANCE = new CanvasTextureManager();

    public static final ResourceLocation BACK_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/birch_planks.png");
    private static final ResourceLocation GLASS_FRAME_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/glass.png");
    private static final ResourceLocation EMPTY_CANVAS_TEXTURE = Mod.id("textures/block/empty.png");
    public static final int NO_TINT = 0xFFFFFFFF;
    /**
     * Alpha of the faint glass sheet drawn behind a tinted glass painting so transparent pixels are tinted too
     */
    private static final int GLASS_TINT_OVERLAY_ALPHA = 0x40;
    private static final int[] EMPTY_PIXELS;

    static {
        EMPTY_PIXELS = new int[1024];
        for (int i = 0; i < 1024; i++) {
            EMPTY_PIXELS[i] = PaletteUtil.Color.WHITE.rgbVal();
        }
    }

    private TextureManager textureManager;
    private final Map<String, CanvasInstance> loadedCanvases = Maps.newHashMap();
    /**
     * 1x1 white texture used to render painted sides and the glass tint sheet
     */
    private ResourceLocation whiteTexture;

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

    private ResourceLocation whiteTexture() {
        if (whiteTexture == null) {
            ensureInitialized();
            DynamicTexture texture = new DynamicTexture(1, 1, false);
            NativeImage image = texture.getPixels();
            if (image != null) {
                image.setPixelRGBA(0, 0, 0xFFFFFFFF);
                texture.upload();
            }
            whiteTexture = textureManager.register("canvas_side_white", texture);
        }
        return whiteTexture;
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
                EntityCanvas.PICTURES.put(canvasId, new EntityCanvas.Picture(version, pixels.stream().mapToInt(i -> i).toArray(),
                        sidesActive(canvasStack), sidePixels(canvasStack)));
            }
        }
        return getCanvasInstance(canvasId, version, width, height);
    }

    public static boolean sidesActive(ItemStack canvasStack) {
        return canvasStack.getOrDefault(Items.CANVAS_SIDES_ACTIVE.get(), false);
    }

    public static int[] sidePixels(ItemStack canvasStack) {
        List<Integer> sideList = canvasStack.get(Items.CANVAS_SIDE_PIXELS.get());
        return sideList != null ? sideList.stream().mapToInt(i -> i).toArray() : new int[0];
    }

    /**
     * Render an empty canvas for easel display (matches the positioning of canvases with content)
     */
    public void renderEmptyForEasel(PoseStack ms, MultiBufferSource buffer, int width, int height, int packedLight, boolean glass) {
        ms.pushPose();
        applyEaselTransform(ms, width, height);
        drawModel(ms.last(), buffer, packedLight, null, width, height, glass, false, null, NO_TINT);
        ms.popPose();
    }

    /**
     * Render an empty canvas as an item (BEWLR)
     */
    public void renderEmptyForItem(PoseStack ms, MultiBufferSource buffer, int width, int height, int packedLight, boolean glass, int tint) {
        ms.pushPose();
        applyItemTransform(ms, width, height);
        drawModel(ms.last(), buffer, packedLight, null, width, height, glass, false, null, tint);
        ms.popPose();
    }

    private static void applyEaselTransform(PoseStack ms, int width, int height) {
        final float wScale = width / 16.0f;
        final float hScale = height / 16.0f;
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
    }

    private static void applyItemTransform(PoseStack ms, int width, int height) {
        final float wScale = width / 16.0f;
        final float hScale = height / 16.0f;
        // Item rendering (BEWLR): center in unit space
        float scale = 1.0f / (32.0f * Math.max(wScale, hScale));
        ms.translate(0.5, 0.5, 0.5);
        ms.scale(scale, scale, scale);
        ms.translate(-16.0f * wScale, -16.0f * hScale, 0);
    }

    /**
     * Draws the canvas model in its canonical space: x in [0, 2*width], y in [0, 2*height] and z in [-1, 1],
     * with the painted front at z = -1 facing -Z and the back at z = +1.
     *
     * @param front       the painting texture, or null for an empty canvas
     * @param sidePixels  painted side pixels in {@link CanvasSides} order (used when sidesActive)
     * @param tint        tint for the glass sheet behind transparent pixels ({@link #NO_TINT} for none)
     */
    public void drawModel(PoseStack.Pose pose, MultiBufferSource buffer, int light, @Nullable ResourceLocation front,
                          int width, int height, boolean glass, boolean sidesActive, @Nullable int[] sidePixels, int tint) {
        final float w32 = 2.0F * width;
        final float h32 = 2.0F * height;
        final float sideWidth = 1.0F / 16.0F;

        if (glass) {
            if (front != null) {
                // FRONT (facing -Z): single-sided cutout so transparent pixels are see-through
                VertexConsumer vb = buffer.getBuffer(RenderType.entityCutout(front));
                vertex(vb, pose, 0, h32, -1, 1, 0, light, 0, 0, -1, NO_TINT);
                vertex(vb, pose, w32, h32, -1, 0, 0, light, 0, 0, -1, NO_TINT);
                vertex(vb, pose, w32, 0, -1, 0, 1, light, 0, 0, -1, NO_TINT);
                vertex(vb, pose, 0, 0, -1, 1, 1, light, 0, 0, -1, NO_TINT);

                // BACK (facing +Z): the front image seen through the glass appears mirrored
                vb = buffer.getBuffer(RenderType.entityCutout(front));
                vertex(vb, pose, 0, 0, 1, 1, 1, light, 0, 0, 1, NO_TINT);
                vertex(vb, pose, w32, 0, 1, 0, 1, light, 0, 0, 1, NO_TINT);
                vertex(vb, pose, w32, h32, 1, 0, 0, light, 0, 0, 1, NO_TINT);
                vertex(vb, pose, 0, h32, 1, 1, 0, light, 0, 0, 1, NO_TINT);
            }

            if (tint != NO_TINT) {
                // Tint the transparent pixels so the glass is visible in inventories
                int overlay = (GLASS_TINT_OVERLAY_ALPHA << 24) | (tint & 0xFFFFFF);
                VertexConsumer sheet = buffer.getBuffer(RenderType.entityTranslucent(whiteTexture()));
                vertex(sheet, pose, 0, h32, 0, 0, 0, light, 0, 0, -1, overlay);
                vertex(sheet, pose, w32, h32, 0, 0, 0, light, 0, 0, -1, overlay);
                vertex(sheet, pose, w32, 0, 0, 0, 0, light, 0, 0, -1, overlay);
                vertex(sheet, pose, 0, 0, 0, 0, 0, light, 0, 0, -1, overlay);
            }

            if (sidesActive) {
                // Painted side pixels (no-cull so they are visible from inside the canvas too)
                VertexConsumer sides = buffer.getBuffer(RenderType.entityCutoutNoCull(whiteTexture()));
                drawPaintedSides(sides, pose, width, height, sidePixels, light, true);
            } else {
                drawGlassFrame(buffer.getBuffer(RenderType.entityTranslucent(GLASS_FRAME_TEXTURE)), pose, w32, h32, light);
            }
            return;
        }

        // FRONT (facing -Z)
        ResourceLocation frontTexture = front != null ? front : EMPTY_CANVAS_TEXTURE;
        VertexConsumer vb = buffer.getBuffer(RenderType.entitySolid(frontTexture));
        vertex(vb, pose, 0, h32, -1, 1, 0, light, 0, 0, -1, NO_TINT);
        vertex(vb, pose, w32, h32, -1, 0, 0, light, 0, 0, -1, NO_TINT);
        vertex(vb, pose, w32, 0, -1, 0, 1, light, 0, 0, -1, NO_TINT);
        vertex(vb, pose, 0, 0, -1, 1, 1, light, 0, 0, -1, NO_TINT);

        // BACK (facing +Z)
        vb = buffer.getBuffer(RenderType.entitySolid(BACK_TEXTURE));
        vertex(vb, pose, 0, 0, 1, 0, 0, light, 0, 0, 1, NO_TINT);
        vertex(vb, pose, w32, 0, 1, 1, 0, light, 0, 0, 1, NO_TINT);
        vertex(vb, pose, w32, h32, 1, 1, 1, light, 0, 0, 1, NO_TINT);
        vertex(vb, pose, 0, h32, 1, 0, 1, light, 0, 0, 1, NO_TINT);

        if (sidesActive) {
            // No-cull so painted sides are visible from inside the canvas too
            VertexConsumer sides = buffer.getBuffer(RenderType.entityCutoutNoCull(whiteTexture()));
            drawPaintedSides(sides, pose, width, height, sidePixels, light, false);
            return;
        }

        // LEFT SIDE (x = 0, normal -X)
        vertex(vb, pose, 0, 0, 1, sideWidth, 0, light, -1, 0, 0, NO_TINT);
        vertex(vb, pose, 0, h32, 1, sideWidth, 1, light, -1, 0, 0, NO_TINT);
        vertex(vb, pose, 0, h32, -1, 0, 1, light, -1, 0, 0, NO_TINT);
        vertex(vb, pose, 0, 0, -1, 0, 0, light, -1, 0, 0, NO_TINT);

        // TOP SIDE (y = h32, normal +Y)
        vertex(vb, pose, 0, h32, 1, 0, 0, light, 0, 1, 0, NO_TINT);
        vertex(vb, pose, w32, h32, 1, 1, 0, light, 0, 1, 0, NO_TINT);
        vertex(vb, pose, w32, h32, -1, 1, sideWidth, light, 0, 1, 0, NO_TINT);
        vertex(vb, pose, 0, h32, -1, 0, sideWidth, light, 0, 1, 0, NO_TINT);

        // RIGHT SIDE (x = w32, normal +X)
        vertex(vb, pose, w32, 0, -1, 0, 0, light, 1, 0, 0, NO_TINT);
        vertex(vb, pose, w32, h32, -1, 0, 1, light, 1, 0, 0, NO_TINT);
        vertex(vb, pose, w32, h32, 1, sideWidth, 1, light, 1, 0, 0, NO_TINT);
        vertex(vb, pose, w32, 0, 1, sideWidth, 0, light, 1, 0, 0, NO_TINT);

        // BOTTOM SIDE (y = 0, normal -Y)
        vertex(vb, pose, 0, 0, -1, 0, 1, light, 0, -1, 0, NO_TINT);
        vertex(vb, pose, w32, 0, -1, 1, 1, light, 0, -1, 0, NO_TINT);
        vertex(vb, pose, w32, 0, 1, 1, 1 - sideWidth, light, 0, -1, 0, NO_TINT);
        vertex(vb, pose, 0, 0, 1, 0, 1 - sideWidth, light, 0, -1, 0, NO_TINT);
    }

    private static void vertex(VertexConsumer vb, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                               int light, float nx, float ny, float nz, int color) {
        vb.addVertex(pose, x, y, z)
                .setColor((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >>> 24) & 0xFF)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    /**
     * Renders the four glass-canvas edges using the vanilla glass texture.
     */
    private static void drawGlassFrame(VertexConsumer vb, PoseStack.Pose pose, float w32, float h32, int light) {
        final float eps = 0.001f;
        final float depth = 1.0F / 16.0F;
        // LEFT (x = 0, normal -X)
        vertex(vb, pose, eps, eps, 1, 0, 0, light, -1, 0, 0, NO_TINT);
        vertex(vb, pose, eps, h32 - eps, 1, 0, 1, light, -1, 0, 0, NO_TINT);
        vertex(vb, pose, eps, h32 - eps, -1, depth, 1, light, -1, 0, 0, NO_TINT);
        vertex(vb, pose, eps, eps, -1, depth, 0, light, -1, 0, 0, NO_TINT);
        // TOP (y = h32, normal +Y)
        vertex(vb, pose, eps, h32 - eps, 1, 0, 0, light, 0, 1, 0, NO_TINT);
        vertex(vb, pose, w32 - eps, h32 - eps, 1, 1, 0, light, 0, 1, 0, NO_TINT);
        vertex(vb, pose, w32 - eps, h32 - eps, -1, 1, depth, light, 0, 1, 0, NO_TINT);
        vertex(vb, pose, eps, h32 - eps, -1, 0, depth, light, 0, 1, 0, NO_TINT);
        // RIGHT (x = w32, normal +X)
        vertex(vb, pose, w32 - eps, eps, -1, 0, 0, light, 1, 0, 0, NO_TINT);
        vertex(vb, pose, w32 - eps, h32 - eps, -1, 0, 1, light, 1, 0, 0, NO_TINT);
        vertex(vb, pose, w32 - eps, h32 - eps, 1, depth, 1, light, 1, 0, 0, NO_TINT);
        vertex(vb, pose, w32 - eps, eps, 1, depth, 0, light, 1, 0, 0, NO_TINT);
        // BOTTOM (y = 0, normal -Y)
        vertex(vb, pose, eps, eps, -1, 0, 0, light, 0, -1, 0, NO_TINT);
        vertex(vb, pose, w32 - eps, eps, -1, 1, 0, light, 0, -1, 0, NO_TINT);
        vertex(vb, pose, w32 - eps, eps, 1, 1, depth, light, 0, -1, 0, NO_TINT);
        vertex(vb, pose, eps, eps, 1, 0, depth, light, 0, -1, 0, NO_TINT);
    }

    private static int sidePixelColor(@Nullable int[] sidePixels, int index) {
        if (sidePixels != null && index >= 0 && index < sidePixels.length) {
            return sidePixels[index];
        }
        return CanvasSides.DEFAULT_COLOR;
    }

    /**
     * Renders the four canvas edges as a strip of solid-colored, one-pixel quads. The pixel
     * ordering matches {@link CanvasSides} so painted sides line up with the adjacent front pixels.
     * When skipTransparent is set (glass), fully transparent side pixels are skipped.
     */
    private static void drawPaintedSides(VertexConsumer vb, PoseStack.Pose pose, int width, int height,
                                         @Nullable int[] sidePixels, int light, boolean skipTransparent) {
        final float eps = 0.001f;
        final float unit = 2.0F; // one image pixel spans two local units along an edge
        final float w32 = unit * width;
        final float h32 = unit * height;
        final int topOffset = 0;
        final int bottomOffset = width;
        final int leftOffset = bottomOffset + width;
        final int rightOffset = leftOffset + height;

        // TOP edge (y = h32, normal +Y): pixel k maps to image column k -> x in [w32-(k+1)u, w32-k*u]
        for (int k = 0; k < width; k++) {
            int c = sidePixelColor(sidePixels, topOffset + k);
            if (skipTransparent && ((c >> 24) & 0xFF) == 0) continue;
            int color = c | 0xFF000000;
            float x0 = w32 - (k + 1) * unit;
            float x1 = w32 - k * unit;
            vertex(vb, pose, x0, h32 - eps, 1, 0, 0, light, 0, 1, 0, color);
            vertex(vb, pose, x1, h32 - eps, 1, 0, 0, light, 0, 1, 0, color);
            vertex(vb, pose, x1, h32 - eps, -1, 0, 0, light, 0, 1, 0, color);
            vertex(vb, pose, x0, h32 - eps, -1, 0, 0, light, 0, 1, 0, color);
        }
        // BOTTOM edge (y = 0, normal -Y): same x mapping as the top
        for (int k = 0; k < width; k++) {
            int c = sidePixelColor(sidePixels, bottomOffset + k);
            if (skipTransparent && ((c >> 24) & 0xFF) == 0) continue;
            int color = c | 0xFF000000;
            float x0 = w32 - (k + 1) * unit;
            float x1 = w32 - k * unit;
            vertex(vb, pose, x0, eps, -1, 0, 0, light, 0, -1, 0, color);
            vertex(vb, pose, x1, eps, -1, 0, 0, light, 0, -1, 0, color);
            vertex(vb, pose, x1, eps, 1, 0, 0, light, 0, -1, 0, color);
            vertex(vb, pose, x0, eps, 1, 0, 0, light, 0, -1, 0, color);
        }
        // LEFT edge (image left, x = w32, normal +X): pixel i maps to image row i -> y in [h32-(i+1)u, h32-i*u]
        for (int i = 0; i < height; i++) {
            int c = sidePixelColor(sidePixels, leftOffset + i);
            if (skipTransparent && ((c >> 24) & 0xFF) == 0) continue;
            int color = c | 0xFF000000;
            float y0 = h32 - (i + 1) * unit;
            float y1 = h32 - i * unit;
            vertex(vb, pose, w32 - eps, y0, -1, 0, 0, light, 1, 0, 0, color);
            vertex(vb, pose, w32 - eps, y1, -1, 0, 0, light, 1, 0, 0, color);
            vertex(vb, pose, w32 - eps, y1, 1, 0, 0, light, 1, 0, 0, color);
            vertex(vb, pose, w32 - eps, y0, 1, 0, 0, light, 1, 0, 0, color);
        }
        // RIGHT edge (image right, x = 0, normal -X): same y mapping as the left
        for (int i = 0; i < height; i++) {
            int c = sidePixelColor(sidePixels, rightOffset + i);
            if (skipTransparent && ((c >> 24) & 0xFF) == 0) continue;
            int color = c | 0xFF000000;
            float y0 = h32 - (i + 1) * unit;
            float y1 = h32 - i * unit;
            vertex(vb, pose, eps, y0, 1, 0, 0, light, -1, 0, 0, color);
            vertex(vb, pose, eps, y1, 1, 0, 0, light, -1, 0, 0, color);
            vertex(vb, pose, eps, y1, -1, 0, 0, light, -1, 0, 0, color);
            vertex(vb, pose, eps, y0, -1, 0, 0, light, -1, 0, 0, color);
        }
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
                            // Convert from ARGB to ABGR (swap red and blue channels), keeping alpha for glass
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
            render(canvas, yaw, pitch, ms, buffer, facing, packedLight, false, false, null, NO_TINT);
        }

        public void renderForItem(PoseStack ms, MultiBufferSource buffer, int packedLight, boolean glass,
                                  boolean sidesActive, int[] sidePixels, int tint) {
            ms.pushPose();
            applyItemTransform(ms, width, height);
            drawModel(ms.last(), buffer, packedLight, location, width, height, glass, sidesActive, sidePixels, tint);
            ms.popPose();
        }

        public void renderForEasel(PoseStack ms, MultiBufferSource buffer, int packedLight, boolean glass,
                                   boolean sidesActive, int[] sidePixels) {
            ms.pushPose();
            applyEaselTransform(ms, width, height);
            drawModel(ms.last(), buffer, packedLight, location, width, height, glass, sidesActive, sidePixels, NO_TINT);
            ms.popPose();
        }

        /**
         * Renders a legacy wall canvas entity.
         */
        public void render(@Nullable EntityCanvas canvas, float yaw, float pitch, PoseStack ms, MultiBufferSource buffer,
                           Direction facing, int packedLight, boolean glass, boolean sidesActive, @Nullable int[] sidePixels, int tint) {
            final float wScale = width / 16.0f;
            final float hScale = height / 16.0f;

            ms.pushPose();

            if (canvas == null) {
                applyEaselTransform(ms, width, height);
            } else {
                float xOffset = facing.getStepX();
                float yOffset = facing.getStepY();
                float zOffset = facing.getStepZ();

                int rotation = canvas.getRotation();
                if (rotation > 0) {
                    ms.mulPose(Axis.XP.rotationDegrees(pitch));
                    ms.mulPose(Axis.YP.rotationDegrees(180 - yaw));
                    ms.mulPose(Axis.ZP.rotationDegrees(90 * rotation));
                    ms.mulPose(Axis.YP.rotationDegrees(-180 + yaw));
                    ms.mulPose(Axis.XP.rotationDegrees(-pitch));
                }

                // Entity canvas rendering (wall placement)
                if (facing.getAxis().isHorizontal()) {
                    ms.translate(zOffset * 0.5d * wScale, -0.5d * hScale, -xOffset * 0.5d * wScale);
                } else {
                    ms.translate(0.5 * wScale, 0 * hScale, (yOffset > 0 ? 0.5 : -0.5) * wScale);
                }
                float f = 1.0f / 32.0f;
                ms.mulPose(Axis.XP.rotationDegrees(pitch));
                ms.mulPose(Axis.YP.rotationDegrees(180 - yaw));
                ms.scale(f, f, f);
            }

            drawModel(ms.last(), buffer, packedLight, location, width, height, glass, sidesActive, sidePixels, tint);

            ms.popPose();
        }

        @Override
        public void close() {
            this.canvasTexture.close();
        }
    }
}
