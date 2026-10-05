package xerca.xercapaint.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import xerca.xercapaint.CanvasSides;
import xerca.xercapaint.CanvasType;
import xerca.xercapaint.entity.EntityEasel;
import xerca.xercapaint.item.ItemCanvas;
import xerca.xercapaint.item.Items;

import java.util.Arrays;
import java.util.List;

@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
public class GuiCanvasView extends Screen {
    private int canvasX;
    private int canvasY = 40;
    private final int canvasWidth;
    private final int canvasPixelScale;
    private final int canvasPixelWidth;
    private final int canvasPixelHeight;
    private final CanvasType canvasType;
    private final boolean glass;

    private int[] pixels;
    private final boolean sidesActive;
    private int[] sidePixels;
    private String authorName = "";
    private String canvasTitle = "";
    private int generation = 0;
    private final EntityEasel easel;
    private final Player player;

    protected GuiCanvasView(ItemStack canvasStack, Component title, CanvasType canvasType, EntityEasel easel) {
        super(title);

        this.canvasType = canvasType;
        this.glass = canvasStack.getItem() instanceof ItemCanvas itemCanvas && itemCanvas.isGlass();
        this.canvasPixelScale = canvasType == CanvasType.SMALL ? 10 : 5;
        this.canvasPixelWidth = CanvasType.getWidth(canvasType);
        this.canvasPixelHeight = CanvasType.getHeight(canvasType);
        this.canvasWidth = this.canvasPixelWidth * this.canvasPixelScale;
        this.easel = easel;
        this.player = Minecraft.getInstance().player;

        List<Integer> stackPixels = canvasStack.get(Items.CANVAS_PIXELS.get());
        if (stackPixels != null) {
            this.authorName = canvasStack.get(Items.CANVAS_AUTHOR.get());
            this.canvasTitle = canvasStack.getOrDefault(Items.CANVAS_TITLE.get(), "");
            this.generation = canvasStack.getOrDefault(Items.CANVAS_GENERATION.get(), 0);

            this.pixels = stackPixels.stream().mapToInt(i -> i).toArray();
        } else {
            this.pixels = new int[canvasPixelWidth * canvasPixelHeight];
            Arrays.fill(this.pixels, glass ? 0 : 0xFFF9FFFE);
        }

        this.sidesActive = canvasStack.getOrDefault(Items.CANVAS_SIDES_ACTIVE.get(), false);
        List<Integer> stackSidePixels = canvasStack.get(Items.CANVAS_SIDE_PIXELS.get());
        if (stackSidePixels != null && stackSidePixels.size() == CanvasSides.count(canvasType)) {
            this.sidePixels = stackSidePixels.stream().mapToInt(i -> i).toArray();
        } else {
            this.sidePixels = CanvasSides.defaultPixels(canvasType, glass);
        }
    }

    @Override
    public void init() {
        canvasX = (this.width - canvasWidth) / 2;
        if (canvasType.equals(CanvasType.LONG)) {
            canvasY += 40;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float f) {
        CanvasGuiDrawing drawing = new CanvasGuiDrawing(guiGraphics, canvasPixelScale);
        drawing.drawCanvas(canvasX, canvasY, canvasPixelWidth, canvasPixelHeight, pixels, glass);
        if (sidesActive) {
            drawing.drawSides(canvasX, canvasY, canvasType, sidePixels, glass);
        }

        if (generation > 0 && !canvasTitle.isEmpty()) {
            String title = canvasTitle + " " + I18n.get("canvas.byAuthor", authorName);
            String gen = "(" + I18n.get("canvas.generation." + (generation - 1)) + ")";

            int titleWidth = this.font.width(title);
            int genWidth = this.font.width(gen);

            float titleX = (canvasX + (canvasWidth - titleWidth) / 2.0f);
            float genX = (canvasX + (canvasWidth - genWidth) / 2.0f);
            float minX = Math.min(genX, titleX);
            float maxX = Math.max(genX + genWidth, titleX + titleWidth);

            guiGraphics.fill((int) (minX - 10), canvasY - 40, (int) (maxX + 10), canvasY - 14, 0xFFEEEEEE);

            guiGraphics.drawString(font, title, (int) titleX, (canvasY - 35), 0xFF111111, false);
            guiGraphics.drawString(font, gen, (int) genX, canvasY - 24, 0xFF444444, false);
        }
    }

    @Override
    public void tick() {
        if (easel != null) {
            if (easel.getItem().isEmpty() || easel.isRemoved() || easel.distanceToSqr(player) > 64) {
                this.onClose();
            }
        }
        super.tick();
    }
}