package xerca.xercapaint.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import xerca.xercapaint.CanvasSides;
import xerca.xercapaint.CanvasType;
import xerca.xercapaint.Config;
import xerca.xercapaint.PaletteUtil;
import xerca.xercapaint.SoundEvents;
import xerca.xercapaint.entity.EntityEasel;
import xerca.xercapaint.item.ItemCanvas;
import xerca.xercapaint.item.Items;
import xerca.xercapaint.packets.CanvasMiniUpdatePacket;
import xerca.xercapaint.packets.CanvasUpdatePacket;
import xerca.xercapaint.packets.EaselLeftPacket;
import xerca.xercapaint.packets.PaletteUpdatePacket;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

import static org.lwjgl.glfw.GLFW.*;

@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
public class GuiCanvasEdit extends BasePalette {
    private double canvasX;
    private double canvasY;
    private static final double[] canvasXs = {-1000, -1000, -1000, -1000};
    private static final double[] canvasYs = {-1000, -1000, -1000, -1000};
    private final int canvasWidth;
    private final int canvasHeight;
    private int brushMeterX;
    private int brushMeterY;
    private int brushOpacityMeterX;
    private int brushOpacityMeterY;
    private final int canvasPixelScale;
    private final int canvasPixelWidth;
    private final int canvasPixelHeight;
    private int brushSize = 0;
    private boolean touchedCanvas = false;
    private boolean undoStarted = false;
    private boolean gettingSigned;
    private boolean isCarryingCanvas;
    private Button buttonSign;
    private Button buttonCancel;
    private Button buttonFinalize;
    private int updateCount;
    private BrushSound brushSound = null;
    private final int canvasHolderHeight = 10;
    private static int brushOpacitySetting = 0;
    private static final float[] brushOpacities = {1.f, 0.75f, 0.5f, 0.25f};
    private static boolean showHelp = false;
    private final Set<Integer> draggedPoints = new HashSet<>();

    private static final int SIDES_TOGGLE_SIZE = 8;
    private boolean sidesActive;
    private int[] sidePixels;
    private int sidesToggleX;
    private int sidesToggleY;

    private final Player editingPlayer;

    private final CanvasType canvasType;
    private final boolean glass;
    private boolean isSigned = false;
    private int[] pixels;
    private String canvasTitle = "";
    private final String canvasId;
    private int version = 0;
    private final EntityEasel easel;
    private int timeSinceLastUpdate = 0;
    private boolean skippedUpdate = false;

    private static final Vec2[] outlinePoss1 = {
            new Vec2(0.f, 199.0f),
            new Vec2(12.f, 199.0f),
            new Vec2(34.f, 199.0f),
            new Vec2(76.f, 199.0f),
    };

    private static final Vec2[] outlinePoss2 = {
            new Vec2(128.f, 199.0f),
            new Vec2(135.f, 199.0f),
            new Vec2(147.f, 199.0f),
            new Vec2(169.f, 199.0f),
    };

    private static final int maxUndoLength = 16;
    private final Deque<Snapshot> undoStack = new ArrayDeque<>(maxUndoLength);

    /**
     * A snapshot of the editable canvas state for both front and side pixels
     */
    private record Snapshot(int[] pixels, int[] sidePixels) {
    }

    private static final int[][][] BRUSH_OFFSETS = {
            {{0, 0}},
            {{0, 0}, {-1, 0}, {0, -1}, {-1, -1}},
            {{-1, 1}, {0, 1}, {-2, 0}, {-1, 0}, {0, 0}, {1, 0}, {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {-1, -2}, {0, -2}},
            {{-1, 2}, {0, 2}, {1, 2}, {-2, 1}, {-1, 1}, {0, 1}, {1, 1}, {2, 1}, {-2, 0}, {-1, 0}, {0, 0}, {1, 0}, {2, 0}, {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {2, -1}, {-1, -2}, {0, -2}, {1, -2}}
    };
    /**
     * Whether each brush anchors on the nearest grid corner (true) or the cell under the cursor (false).
     */
    private static final boolean[] BRUSH_CORNER_ANCHOR = {false, true, true, false};
    /**
     * The smallest offset and the span (in cells) of each brush's bounding box, used to draw the outline.
     */
    private static final int[] BRUSH_MIN_OFFSET = {0, -1, -2, -2};
    private static final int[] BRUSH_SPAN = {1, 2, 4, 5};

    protected GuiCanvasEdit(Player player, ItemStack canvasStack, ItemStack paletteStack, Component title, CanvasType canvasType, EntityEasel easel) {
        super(title, paletteStack, Config.dyeCostEnabled() && !player.isCreative());
        updateCount = 0;

        this.canvasType = canvasType;
        this.glass = canvasStack.getItem() instanceof ItemCanvas itemCanvas && itemCanvas.isGlass();
        this.canvasPixelScale = canvasType == CanvasType.SMALL ? 10 : 5;
        this.canvasPixelWidth = CanvasType.getWidth(canvasType);
        this.canvasPixelHeight = CanvasType.getHeight(canvasType);
        int canvasPixelArea = canvasPixelHeight * canvasPixelWidth;
        this.canvasWidth = this.canvasPixelWidth * this.canvasPixelScale;
        this.canvasHeight = this.canvasPixelHeight * this.canvasPixelScale;
        this.easel = easel;

        this.editingPlayer = player;
        List<Integer> stackPixels = canvasStack.get(Items.CANVAS_PIXELS.get());
        String canvasId = canvasStack.get(Items.CANVAS_ID.get());
        if (stackPixels != null && canvasId != null) {
            this.pixels = stackPixels.stream().mapToInt(i -> i).toArray();
            this.canvasId = canvasId;
            this.version = canvasStack.getOrDefault(Items.CANVAS_VERSION.get(), 1);

            canvasTitle = canvasStack.getOrDefault(Items.CANVAS_TITLE.get(), "");
            isSigned = !canvasTitle.isEmpty();
        } else {
            this.pixels = new int[canvasPixelArea];
            Arrays.fill(this.pixels, glass ? 0 : basicColors[15].rgbVal());

            long secs = System.currentTimeMillis() / 1000;
            this.canvasId = player.getUUID() + "_" + secs;
        }

        this.sidesActive = canvasStack.getOrDefault(Items.CANVAS_SIDES_ACTIVE.get(), false);
        List<Integer> stackSidePixels = canvasStack.get(Items.CANVAS_SIDE_PIXELS.get());
        if (stackSidePixels != null && stackSidePixels.size() == CanvasSides.count(canvasType)) {
            this.sidePixels = stackSidePixels.stream().mapToInt(i -> i).toArray();
        }
    }

    private void ensureSidePixels() {
        if (sidePixels == null || sidePixels.length != CanvasSides.count(canvasType)) {
            sidePixels = CanvasSides.defaultPixels(canvasType, glass);
        }
    }

    private int sideMargin() {
        return sidesActive ? canvasPixelScale : 0;
    }

    @Override
    public void init() {
        if (minecraft == null) {
            return;
        }
        canvasX = canvasXs[canvasType.ordinal()];
        canvasY = canvasYs[canvasType.ordinal()];
        paletteX = paletteXs[canvasType.ordinal()];
        paletteY = paletteYs[canvasType.ordinal()];
        if (canvasX == -1000 || canvasY == -1000 || paletteX == -1000 || paletteY == -1000) {
            resetPositions();
        }

        updateCanvasPos(0, 0);
        updatePalettePos(0, 0);

        Window window = minecraft.getWindow();

        // Hide mouse cursor
        GLFW.glfwSetInputMode(window.getWindow(), GLFW_CURSOR, GLFW_CURSOR_HIDDEN);

        int x = window.getGuiScaledWidth() - 120;
        int y = window.getGuiScaledHeight() - 30;
        this.buttonSign = this.addRenderableWidget(Button.builder(Component.translatable("canvas.signButton"), button -> {
            if (!isSigned) {
                gettingSigned = true;
                resetPositions();
                updateButtons();

                GLFW.glfwSetInputMode(window.getWindow(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            }
        }).bounds(x, y, 98, 20).build());
        this.buttonFinalize = this.addRenderableWidget(Button.builder(Component.translatable("canvas.finalizeButton"), button -> {
            if (!isSigned) {
                canvasDirty = true;
                isSigned = true;
                if (minecraft != null) {
                    minecraft.setScreen(null);
                }
            }

        }).bounds((int) canvasX - 100, 100, 98, 20).build());
        this.buttonCancel = this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> {
            if (!isSigned) {
                gettingSigned = false;
                updateButtons();

                GLFW.glfwSetInputMode(window.getWindow(), GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
            }
        }).bounds((int) canvasX - 100, 130, 98, 20).build());

        x = (int) (window.getGuiScaledWidth() * 0.95) - 21;
        y = (int) (window.getGuiScaledHeight() * 0.05);
        this.addRenderableWidget(new ToggleHelpButton(x, y, 21, 21, 197, 0, 21,
                paletteTextures, 256, 256, button -> showHelp = !showHelp, Tooltip.create(Component.translatable("canvas.help.toggleHelp"))));

        updateButtons();
    }

    private void updateButtons() {
        if (!this.isSigned) {
            this.buttonSign.visible = !this.gettingSigned;
            this.buttonCancel.visible = this.gettingSigned;
            this.buttonFinalize.visible = this.gettingSigned;
            this.buttonFinalize.active = !this.canvasTitle.trim().isEmpty();

            this.buttonFinalize.setX((int) canvasX - 100);
            this.buttonCancel.setX((int) canvasX - 100);
        }
    }

    private int brushAnchor(int mousePos, int origin, boolean corner) {
        int rel = mousePos - origin + (corner ? canvasPixelScale / 2 : 0);
        return Math.floorDiv(rel, canvasPixelScale);
    }

    /**
     * Maps a grid cell that lies outside the canvas to its side-pixel index, or -1 if it is not a side cell.
     */
    private int sideIndexFor(int col, int row) {
        int w = canvasPixelWidth;
        int h = canvasPixelHeight;
        if (row == -1 && col >= 0 && col < w) {
            return CanvasSides.topOffset() + col;
        }
        if (row == h && col >= 0 && col < w) {
            return CanvasSides.bottomOffset(canvasType) + col;
        }
        if (col == -1 && row >= 0 && row < h) {
            return CanvasSides.leftOffset(canvasType) + row;
        }
        if (col == w && row >= 0 && row < h) {
            return CanvasSides.rightOffset(canvasType) + row;
        }
        return -1;
    }

    private void paintCell(int col, int row, PaletteUtil.Color color, float opacity, boolean erase) {
        // Erasing never needs a selected colour and never consumes dye.
        if (!erase && !hasSelectedColor()) {
            return;
        }
        boolean onCanvas = col >= 0 && col < canvasPixelWidth && row >= 0 && row < canvasPixelHeight;
        int sideIndex = -1;
        if (!onCanvas) {
            if (!sidesActive) {
                return;
            }
            sideIndex = sideIndexFor(col, row);
            if (sideIndex < 0) {
                return;
            }
        }
        int key = onCanvas ? row * canvasPixelWidth + col : canvasPixelWidth * canvasPixelHeight + sideIndex;
        if (!draggedPoints.add(key)) {
            return;
        }
        int[] target;
        int index;
        if (onCanvas) {
            target = pixels;
            index = row * canvasPixelWidth + col;
        } else {
            ensureSidePixels();
            target = sidePixels;
            index = sideIndex;
        }
        int newColor = blendPixel(target[index], color, opacity, erase);
        if (newColor != target[index]) {
            target[index] = newColor;
            if (!erase) {
                useDyeCharge(opacity);
            }
        }
    }

    /**
     * Combines a brush stroke with an existing pixel; glass uses binary transparency (erase clears, paint is opaque).
     */
    private int blendPixel(int old, PaletteUtil.Color color, float opacity, boolean erase) {
        if (glass) {
            if (erase) {
                return 0;
            }
            if (((old >> 24) & 0xFF) == 0) {
                return color.rgbVal();
            }
        }
        return PaletteUtil.Color.mix(color, new PaletteUtil.Color(old), opacity).rgbVal();
    }

    private void paintAt(int mouseX, int mouseY, PaletteUtil.Color color, float opacity, boolean erase) {
        boolean corner = BRUSH_CORNER_ANCHOR[brushSize];
        int anchorCol = brushAnchor(mouseX, (int) canvasX, corner);
        int anchorRow = brushAnchor(mouseY, (int) canvasY, corner);
        for (int[] offset : BRUSH_OFFSETS[brushSize]) {
            paintCell(anchorCol + offset[0], anchorRow + offset[1], color, opacity, erase);
        }
    }

    private boolean overSide(int mouseX, int mouseY) {
        if (!sidesActive) {
            return false;
        }
        int col = Math.floorDiv(mouseX - (int) canvasX, canvasPixelScale);
        int row = Math.floorDiv(mouseY - (int) canvasY, canvasPixelScale);
        return sideIndexFor(col, row) >= 0;
    }

    private boolean inPaintable(int mouseX, int mouseY) {
        return inCanvas(mouseX, mouseY) || overSide(mouseX, mouseY);
    }

    /**
     * Returns the color of the canvas/side cell under the cursor, or null if it is not a paintable cell.
     */
    private Integer cellColorAt(int mouseX, int mouseY) {
        int col = Math.floorDiv(mouseX - (int) canvasX, canvasPixelScale);
        int row = Math.floorDiv(mouseY - (int) canvasY, canvasPixelScale);
        if (col >= 0 && col < canvasPixelWidth && row >= 0 && row < canvasPixelHeight) {
            return pixels[row * canvasPixelWidth + col];
        }
        if (sidesActive) {
            int sideIndex = sideIndexFor(col, row);
            if (sideIndex >= 0) {
                return CanvasGuiDrawing.sidePixel(sidePixels, sideIndex);
            }
        }
        return null;
    }

    private void resetPositions() {
        final int padding = 40;
        final int paletteCanvasX = (this.width - (paletteWidth + canvasWidth + padding)) / 2;
        canvasX = paletteCanvasX + paletteWidth + padding;
        if (canvasType.equals(CanvasType.LONG)) {
            canvasY = 80;
        } else {
            canvasY = 40;
        }

        paletteX = paletteCanvasX;
        paletteY = 40;
    }

    @Override
    public void tick() {
        ++this.updateCount;
        ++this.timeSinceLastUpdate;

        if (easel != null) {
            if (easel.getItem().isEmpty() || easel.isRemoved() || easel.distanceToSqr(editingPlayer) > 64) {
                this.onClose();
            }
            if (skippedUpdate && timeSinceLastUpdate > 20 && canvasDirty) {
                updateCanvas(false);
                skippedUpdate = false;
            }
        }

        super.tick();
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
        // Skip vanilla's world-blur behind the painting GUI so the scene stays crisp
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float f) {
        if (!gettingSigned) {
            super.render(guiGraphics, mouseX, mouseY, f);
        } else {
            super.superRender(guiGraphics, mouseX, mouseY, f);
        }

        CanvasGuiDrawing drawing = new CanvasGuiDrawing(guiGraphics, canvasPixelScale);

        // Draw the canvas holder
        int holderMargin = sideMargin();
        drawing.fill((int) (canvasX + canvasWidth * 0.25), (int) canvasY - canvasHolderHeight - holderMargin, (int) (canvasX + canvasWidth * 0.75), (int) canvasY - holderMargin, 0xffe1e1e1);

        // Draw the canvas
        drawing.drawCanvas((int) canvasX, (int) canvasY, canvasPixelWidth, canvasPixelHeight, pixels, glass);

        if (!gettingSigned) {
            // Draw the paintable sides and the toggle button
            if (sidesActive) {
                drawing.drawSides((int) canvasX, (int) canvasY, canvasType, sidePixels, glass);
            }
            drawSidesToggle(guiGraphics);

            // Draw brush meter
            for (int i = 0; i < 4; i++) {
                int y = brushMeterY + i * brushSpriteSize;
                guiGraphics.fill(brushMeterX, y, brushMeterX + 3, y + 3, getCurrentOrEmptyBrushColor().rgbVal());
            }
            guiGraphics.blit(paletteTextures, brushMeterX, brushMeterY + (3 - brushSize) * brushSpriteSize, 15, 246, 10, 10, 256, 256);
            guiGraphics.blit(paletteTextures, brushMeterX, brushMeterY, brushSpriteX, brushSpriteY - brushSpriteSize * 3, brushSpriteSize, brushSpriteSize * 4, 256, 256);

            // Draw opacity meter
            guiGraphics.blit(paletteTextures, brushOpacityMeterX, brushOpacityMeterY, brushOpacitySpriteX, brushOpacitySpriteY, brushOpacitySpriteSize, brushOpacitySpriteSize * 4 + 3, 256, 256);
            guiGraphics.blit(paletteTextures, brushOpacityMeterX - 1, brushOpacityMeterY - 1 + brushOpacitySetting * (brushOpacitySpriteSize + 1), 212, 240, 16, 16, 256, 256);

            // Draw brush and outline
            renderCursor(guiGraphics, mouseX, mouseY);

            if (showHelp) {
                if (inBrushMeter(mouseX, mouseY)) {
                    int selectedSize = 3 - (mouseY - brushMeterY) / brushSpriteSize;
                    if (selectedSize <= 3 && selectedSize >= 0) {
                        guiGraphics.renderTooltip(font, Component.translatable("canvas.help.brushSize", selectedSize + 1), mouseX, mouseY);
                    }
                } else if (inBrushOpacityMeter(mouseX, mouseY)) {
                    int relativeY = mouseY - brushOpacityMeterY;
                    int selectedOpacity = relativeY / (brushOpacitySpriteSize + 1);
                    if (selectedOpacity >= 0 && selectedOpacity <= 3) {
                        int percentage = 100 - 25 * selectedOpacity;
                        guiGraphics.renderTooltip(font, Component.translatable("canvas.help.brushOpacity", percentage), mouseX, mouseY);
                    }
                } else if (inColorPicker(mouseX - (int) paletteX, mouseY - (int) paletteY)) {
                    guiGraphics.renderComponentTooltip(font, Arrays.asList(Component.translatable("canvas.help.colorPicker"),
                            Component.translatable("canvas.help.colorPicker.desc").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
                } else if (inWater(mouseX - (int) paletteX, mouseY - (int) paletteY)) {
                    guiGraphics.renderComponentTooltip(font, Arrays.asList(Component.translatable("canvas.help.colorRemover"),
                            Component.translatable("canvas.help.colorRemover.desc").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
                } else if (inCanvasHolder(mouseX, mouseY)) {
                    guiGraphics.renderComponentTooltip(font, Arrays.asList(Component.translatable("canvas.help.canvasHolder"),
                            Component.translatable("canvas.help.canvasHolder.desc").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
                } else if (inSidesToggle(mouseX, mouseY)) {
                    guiGraphics.renderTooltip(font, Component.translatable("canvas.help.toggleSides"), mouseX, mouseY);
                }
            }
        } else {
            drawSigning(guiGraphics);
        }
    }

    private void renderCursor(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isCarryingColor) {
            // Apply color tint using RenderSystem (NeoForge 1.21.1 doesn't support tinted blit with int color)
            float r = ((carriedColor.rgbVal() >> 16) & 0xFF) / 255.0f;
            float g = ((carriedColor.rgbVal() >> 8) & 0xFF) / 255.0f;
            float b = (carriedColor.rgbVal() & 0xFF) / 255.0f;
            RenderSystem.setShaderColor(r, g, b, 1.0f);
            guiGraphics.blit(paletteTextures, mouseX - brushSpriteSize / 2, mouseY - brushSpriteSize / 2, brushSpriteX + brushSpriteSize, brushSpriteY, dropSpriteWidth, brushSpriteSize, 256, 256);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        } else if (isCarryingWater) {
            // Apply water color tint
            float r = ((waterColor.rgbVal() >> 16) & 0xFF) / 255.0f;
            float g = ((waterColor.rgbVal() >> 8) & 0xFF) / 255.0f;
            float b = (waterColor.rgbVal() & 0xFF) / 255.0f;
            RenderSystem.setShaderColor(r, g, b, 1.0f);
            guiGraphics.blit(paletteTextures, mouseX - brushSpriteSize / 2, mouseY - brushSpriteSize / 2, brushSpriteX + brushSpriteSize, brushSpriteY, dropSpriteWidth, brushSpriteSize, 256, 256);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        } else if (isPickingColor) {
            drawOutline(guiGraphics, mouseX, mouseY, 0);
            guiGraphics.blit(paletteTextures, mouseX, mouseY - colorPickerSize, colorPickerSpriteX, colorPickerSpriteY, colorPickerSize, colorPickerSize, 256, 256);
        } else {
            if (hasSelectedColor()) {
                drawOutline(guiGraphics, mouseX, mouseY, brushSize);
            }

            guiGraphics.fill(mouseX, mouseY, mouseX + 3, mouseY + 3, getCurrentOrEmptyBrushColor().rgbVal());

            int trueBrushY = brushSpriteY - brushSpriteSize * brushSize;
            guiGraphics.blit(paletteTextures, mouseX, mouseY, brushSpriteX, trueBrushY, brushSpriteSize, brushSpriteSize, 256, 256);
        }
    }

    private void drawOutline(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, int brushSize) {
        // The outline is the brush stamp's bounding box, so it follows the cursor onto the sides too.
        if (!inPaintable(mouseX, mouseY)) {
            return;
        }
        boolean corner = BRUSH_CORNER_ANCHOR[brushSize];
        int anchorCol = brushAnchor(mouseX, (int) canvasX, corner);
        int anchorRow = brushAnchor(mouseY, (int) canvasY, corner);
        int minOffset = BRUSH_MIN_OFFSET[brushSize];
        int outlineSize = BRUSH_SPAN[brushSize] * canvasPixelScale + 2;
        int x = (anchorCol + minOffset) * canvasPixelScale + (int) canvasX - 1;
        int y = (anchorRow + minOffset) * canvasPixelScale + (int) canvasY - 1;

        Vec2 textureVec = (canvasPixelScale == 10) ? outlinePoss1[brushSize] : outlinePoss2[brushSize];
        // Draw outline without color tint (NeoForge 1.21.1 doesn't support tinted blit with int color)
        guiGraphics.blit(paletteTextures, x, y, (int) textureVec.x, (int) textureVec.y, outlineSize, outlineSize, 256, 256);
    }

    private void drawSigning(@NotNull GuiGraphics guiGraphics) {
        int i = (int) canvasX;
        int j = (int) canvasY;

        guiGraphics.fill(i + 10, j + 10, i + 150, j + 150, 0xFFEEEEEE);
        String s = this.canvasTitle;

        if (!this.isSigned) {
            if (this.updateCount / 6 % 2 == 0) {
                s = s + ChatFormatting.BLACK + "_";
            } else {
                s = s + ChatFormatting.GRAY + "_";
            }
        }
        String s1 = I18n.get("canvas.editTitle");
        int k = this.font.width(s1);
        guiGraphics.drawString(this.font, s1, (int) (i + 26 + (116 - k) / 2.0f), (j + 16 + 16), 0, false);
        int l = this.font.width(s);
        guiGraphics.drawString(this.font, s, (int) (i + 26 + (116 - l) / 2.0f), j + 48, 0, false);
        String s2 = I18n.get("canvas.byAuthor", this.editingPlayer.getName().getString());
        int i1 = this.font.width(s2);
        guiGraphics.drawString(this.font, ChatFormatting.DARK_GRAY + s2, (int) (i + 26 + (116 - i1) / 2.0f), j + 48 + 10, 0, false);
        guiGraphics.drawWordWrap(this.font, Component.translatable("canvas.finalizeWarning"), i + 26, j + 80, 116, 0);
    }

    private void playBrushSound() {
        brushSound = new BrushSound();
        playSound(brushSound);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.gettingSigned) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    if (!this.canvasTitle.isEmpty()) {
                        this.canvasTitle = this.canvasTitle.substring(0, this.canvasTitle.length() - 1);
                        this.updateButtons();
                    }
                }
                case GLFW.GLFW_KEY_ENTER -> {
                    if (!this.canvasTitle.isEmpty()) {
                        canvasDirty = true;
                        this.isSigned = true;
                        if (this.minecraft != null) {
                            this.minecraft.setScreen(null);
                        }
                    }
                }
                default -> {
                }
            }
            return true;
        } else {
            if (keyCode == GLFW.GLFW_KEY_Z && (modifiers & GLFW.GLFW_MOD_CONTROL) == GLFW.GLFW_MOD_CONTROL) {
                if (!undoStack.isEmpty()) {
                    Snapshot snapshot = undoStack.pop();
                    pixels = snapshot.pixels();
                    sidePixels = snapshot.sidePixels();
                    if (sidesActive) {
                        ensureSidePixels();
                    }
                    canvasDirty = true;
                    if (easel != null) {
                        updateCanvas(false);
                    }
                }
                return true;
            } else {
                if (keyCode == GLFW_KEY_O) {
                    brushOpacitySetting += 1;
                    if (brushOpacitySetting >= 4) {
                        brushOpacitySetting = 0;
                    }
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }

    private static boolean isAllowedChatCharacter(char var0) {
        return var0 != 167 && var0 >= ' ' && var0 != 127;
    }

    @Override
    public boolean charTyped(char typedChar, int something) {
        super.charTyped(typedChar, something);

        if (!this.isSigned) {
            if (this.gettingSigned) {
                if (this.canvasTitle.length() < 16 && isAllowedChatCharacter(typedChar)) {
                    this.canvasTitle = this.canvasTitle + typedChar;
                    this.updateButtons();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double posX, double posY, double scrollX, double scrollY) {
        int mouseX = (int) Math.floor(posX);
        int mouseY = (int) Math.floor(posY);
        if (!gettingSigned && scrollY != 0.d) {
            if (inBrushOpacityMeter(mouseX, mouseY)) {
                final int maxBrushOpacity = 3;
                brushOpacitySetting += scrollY < 0 ? 1 : -1;
                if (brushOpacitySetting > maxBrushOpacity) brushOpacitySetting = 0;
                else if (brushOpacitySetting < 0) brushOpacitySetting = maxBrushOpacity;
                return true;
            } else {
                final int maxBrushSize = 3;
                brushSize += scrollY > 0 ? 1 : -1;
                if (brushSize > maxBrushSize) brushSize = 0;
                else if (brushSize < 0) brushSize = maxBrushSize;
                return true;
            }
        }
        return super.mouseScrolled(posX, posY, scrollX, scrollY);
    }

    // Mouse button 0: left, 1: right
    @Override
    public boolean mouseClicked(double posX, double posY, int mouseButton) {
        if (gettingSigned) {
            return super.superMouseClicked(posX, posY, mouseButton);
        }

        int mouseX = (int) Math.floor(posX);
        int mouseY = (int) Math.floor(posY);

        undoStarted = true;
        touchedCanvas = false;
        if (undoStack.size() >= maxUndoLength) {
            undoStack.removeLast();
        }
        undoStack.push(new Snapshot(pixels.clone(), sidePixels != null ? sidePixels.clone() : null));

        if (inSidesToggle(mouseX, mouseY)) {
            toggleSides();
            return super.superMouseClicked(mouseX, mouseY, mouseButton);
        }

        if (inPaintable(mouseX, mouseY)) {
            if (isPickingColor) {
                Integer color = cellColorAt(mouseX, mouseY);
                // Transparent glass cells hold no colour to pick up
                if (color != null && ((color >> 24) & 0xFF) != 0) {
                    carriedColor = new PaletteUtil.Color(color);
                    setCarryingColor();
                    playSound(SoundEvents.COLOR_PICKER_SUCK.get());
                }
            } else {
                clickedCanvas(mouseX, mouseY, mouseButton);
                playBrushSound();
            }
            return super.superMouseClicked(mouseX, mouseY, mouseButton);
        }

        if (inBrushMeter(mouseX, mouseY)) {
            int selectedSize = 3 - (mouseY - brushMeterY) / brushSpriteSize;
            if (selectedSize <= 3 && selectedSize >= 0) {
                brushSize = selectedSize;
            }
            return super.superMouseClicked(mouseX, mouseY, mouseButton);
        }
        if (inBrushOpacityMeter(mouseX, mouseY)) {
            int relativeY = mouseY - brushOpacityMeterY;
            int selectedOpacity = relativeY / (brushOpacitySpriteSize + 1);
            if (selectedOpacity >= 0 && selectedOpacity <= 3) {
                brushOpacitySetting = selectedOpacity;
            }
            return super.superMouseClicked(mouseX, mouseY, mouseButton);
        }
        if (inCanvasHolder(mouseX, mouseY)) {
            isCarryingCanvas = true;
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void clickedCanvas(int mouseX, int mouseY, int mouseButton) {
        touchedCanvas = true;
        if (mouseButton == GLFW_MOUSE_BUTTON_LEFT && hasSelectedColor()) {
            paintAt(mouseX, mouseY, currentColor, brushOpacities[brushOpacitySetting], false);
        } else if (mouseButton == GLFW_MOUSE_BUTTON_RIGHT && Config.allowErase()) {
            // "Erase" with right click
            paintAt(mouseX, mouseY, PaletteUtil.Color.WHITE, 1.0f, true);
        }
        canvasDirty = true;
    }

    @Override
    public boolean mouseReleased(double posX, double posY, int mouseButton) {
        isCarryingCanvas = false;
        if (gettingSigned) {
            return super.superMouseReleased(posX, posY, mouseButton);
        }
        draggedPoints.clear();

        if (undoStarted && !touchedCanvas) {
            undoStarted = false;
            undoStack.removeFirst();
        }

        if (brushSound != null) {
            brushSound.stopSound();
        }

        if (easel != null) {
            updateCanvas(false);
        }

        return super.mouseReleased(posX, posY, mouseButton);
    }

    @Override
    public boolean mouseDragged(double posX, double posY, int mouseButton, double deltaX, double deltaY) {
        if (gettingSigned) {
            return super.superMouseDragged(posX, posY, mouseButton, deltaX, deltaY);
        }
        if (!isCarryingColor && !isCarryingWater && !isPickingColor && !isCarryingPalette && !isCarryingCanvas) {
            int mouseX = (int) Math.floor(posX);
            int mouseY = (int) Math.floor(posY);
            if (inPaintable(mouseX, mouseY)) {
                clickedCanvas(mouseX, mouseY, mouseButton);
            }

            if (brushSound != null) {
                brushSound.refreshFade();
            }
        } else if (isCarryingCanvas) {
            updateCanvasPos(deltaX, deltaY);
            return super.superMouseDragged(posX, posY, mouseButton, deltaX, deltaY);
        } else if (isCarryingPalette) {
            boolean ret = super.mouseDragged(posX, posY, mouseButton, deltaX, deltaY);
            updatePalettePos(deltaX, deltaY);
            return ret;
        }
        return super.mouseDragged(posX, posY, mouseButton, deltaX, deltaY);
    }

    private void updateCanvasPos(double deltaX, double deltaY) {
        canvasX += deltaX;
        canvasY += deltaY;

        int margin = sideMargin();
        brushMeterX = (int) canvasX + canvasWidth + 2 + margin;
        brushMeterY = (int) canvasY + canvasHeight / 2 + 30;

        brushOpacityMeterX = (int) canvasX + canvasWidth + 2 + margin;
        brushOpacityMeterY = (int) canvasY;

        sidesToggleX = (int) (canvasX + canvasWidth * 0.25) - SIDES_TOGGLE_SIZE - 3;
        sidesToggleY = (int) canvasY - SIDES_TOGGLE_SIZE - 1 - margin;

        canvasXs[canvasType.ordinal()] = canvasX;
        canvasYs[canvasType.ordinal()] = canvasY;
    }

    private void updatePalettePos(double deltaX, double deltaY) {
        paletteX += deltaX;
        paletteY += deltaY;

        paletteXs[canvasType.ordinal()] = paletteX;
        paletteYs[canvasType.ordinal()] = paletteY;
    }

    private boolean inCanvas(int x, int y) {
        return x < canvasX + canvasWidth && x >= canvasX && y < canvasY + canvasHeight && y >= canvasY;
    }

    private boolean inCanvasHolder(int x, int y) {
        int margin = sideMargin();
        return x < canvasX + ((double) canvasWidth) * 0.75 && x >= canvasX + ((double) canvasWidth) * 0.25 && y < canvasY - margin && y >= canvasY - canvasHolderHeight - margin;
    }

    private boolean inSidesToggle(int x, int y) {
        return x >= sidesToggleX && x < sidesToggleX + SIDES_TOGGLE_SIZE && y >= sidesToggleY && y < sidesToggleY + SIDES_TOGGLE_SIZE;
    }

    private void drawSidesToggle(GuiGraphics guiGraphics) {
        int x = sidesToggleX;
        int y = sidesToggleY;
        int s = SIDES_TOGGLE_SIZE;
        guiGraphics.fill(x - 1, y - 1, x + s + 1, y + s + 1, 0xFF000000);
        guiGraphics.fill(x, y, x + s, y + s, sidesActive ? 0xFF6699FF : 0xFFB0B0B0);
        int edge = sidesActive ? 0xFFFFFFFF : 0xFF808080;
        guiGraphics.fill(x + 1, y + 1, x + s - 1, y + 2, edge);
        guiGraphics.fill(x + 1, y + s - 2, x + s - 1, y + s - 1, edge);
        guiGraphics.fill(x + 1, y + 2, x + 2, y + s - 2, edge);
        guiGraphics.fill(x + s - 2, y + 2, x + s - 1, y + s - 2, edge);
    }

    private void toggleSides() {
        sidesActive = !sidesActive;
        if (sidesActive) {
            ensureSidePixels();
        }
        touchedCanvas = true;
        canvasDirty = true;
        updateCanvasPos(0, 0);
        playSound(SoundEvents.MIX.get(), 0.5f);
        if (easel != null) {
            updateCanvas(false);
        }
    }

    private boolean inBrushMeter(int x, int y) {
        return x < brushMeterX + brushSpriteSize && x >= brushMeterX && y < brushMeterY + brushSpriteSize * 4 && y >= brushMeterY;
    }

    private boolean inBrushOpacityMeter(int x, int y) {
        return x < brushOpacityMeterX + brushOpacitySpriteSize && x >= brushOpacityMeterX && y < brushOpacityMeterY + brushOpacitySpriteSize * 4 + 3 && y >= brushOpacityMeterY;
    }

    @Override
    public void removed() {
        updateCanvas(true);
    }

    private void updateCanvas(boolean closing) {
        int[] sideData = sidePixels == null ? new int[0] : sidePixels;
        if (closing) {
            if (canvasDirty) {
                version++;
                int easelId = easel == null ? -1 : easel.getId();
                PacketDistributor.sendToServer(new CanvasUpdatePacket(pixels, isSigned, canvasTitle, canvasId, version, easelId, customColors, canvasType, getBasicColorCharges(), sidesActive, sideData));
            } else {
                if (easel != null) {
                    PacketDistributor.sendToServer(new EaselLeftPacket(easel.getId()));
                }
                if (paletteDirty) {
                    PaletteUpdatePacket pack = new PaletteUpdatePacket(customColors);
                    PacketDistributor.sendToServer(pack);
                }
            }
        } else {
            if (canvasDirty) {
                if (timeSinceLastUpdate < 10) {
                    skippedUpdate = true;
                } else {
                    version++;
                    PacketDistributor.sendToServer(new CanvasMiniUpdatePacket(pixels, canvasId, version, easel.getId(), canvasType, getBasicColorCharges(), sidesActive, sideData));
                    canvasDirty = false;
                    timeSinceLastUpdate = 0;
                }
            }
        }
    }

    public static class ToggleHelpButton extends Button {
        protected final ResourceLocation resourceLocation;
        protected final int xTexStart;
        protected final int yTexStart;
        protected final int yDiffText;
        protected final int texWidth;
        protected final int texHeight;

        public ToggleHelpButton(int x, int y, int width, int height, int xTexStart, int yTexStart, int yDiffText, ResourceLocation texture, int texWidth, int texHeight, OnPress onClick, Tooltip tooltip) {
            super(x, y, width, height, Component.empty(), onClick, Button.DEFAULT_NARRATION);
            this.texWidth = texWidth;
            this.texHeight = texHeight;
            this.xTexStart = xTexStart;
            this.yTexStart = yTexStart;
            this.yDiffText = yDiffText;
            this.resourceLocation = texture;
            setTooltip(tooltip);
        }

        protected void postRender() {
            GlStateManager._enableDepthTest();
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics guiGraphics, int p_230431_2_, int p_230431_3_, float p_230431_4_) {
            RenderSystem.setShaderTexture(0, this.resourceLocation);
            GlStateManager._disableDepthTest();
            int yTexStartNew = this.yTexStart;
            if (this.isHovered) {
                yTexStartNew += this.yDiffText;
            }
            int xTexStartNew = this.xTexStart + (showHelp ? 0 : this.width);
            guiGraphics.blit(resourceLocation, this.getX(), this.getY(), (float) xTexStartNew, (float) yTexStartNew, this.width, this.height, this.texWidth, this.texHeight);
            postRender();
        }
    }
}