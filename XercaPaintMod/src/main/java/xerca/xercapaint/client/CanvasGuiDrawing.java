package xerca.xercapaint.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import xerca.xercapaint.CanvasSides;
import xerca.xercapaint.CanvasType;

/**
 * Shared drawing of a canvas (front pixels, optional painted sides and the glass transparency checkerboard)
 * for the painting and viewing GUIs.
 */
@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
final class CanvasGuiDrawing {
    private static final int CHECKER_LIGHT = 0xFFBFBFBF;
    private static final int CHECKER_DARK = 0xFF7F7F7F;

    private final VertexConsumer buffer;
    private final Matrix4f matrix;
    private final int scale;

    CanvasGuiDrawing(GuiGraphics guiGraphics, int scale) {
        // Write cells straight into the shared GUI buffer (guiGraphics.fill flushes per quad and tanks the FPS)
        this.matrix = guiGraphics.pose().last().pose();
        this.buffer = guiGraphics.bufferSource().getBuffer(RenderType.gui());
        this.scale = scale;
    }

    /**
     * Writes one coloured quad into the shared GUI buffer, like {@link GuiGraphics#fill} but without flushing.
     */
    void fill(int x1, int y1, int x2, int y2, int color) {
        buffer.addVertex(matrix, x1, y1, 0.0f).setColor(color);
        buffer.addVertex(matrix, x1, y2, 0.0f).setColor(color);
        buffer.addVertex(matrix, x2, y2, 0.0f).setColor(color);
        buffer.addVertex(matrix, x2, y1, 0.0f).setColor(color);
    }

    private void cell(int x, int y, int color) {
        fill(x, y, x + scale, y + scale, color);
    }

    private void checker(int x, int y, int parity) {
        cell(x, y, (parity & 1) == 0 ? CHECKER_LIGHT : CHECKER_DARK);
    }

    void drawCanvas(int canvasX, int canvasY, int pixelWidth, int pixelHeight, int[] pixels, boolean glass) {
        for (int i = 0; i < pixelHeight; i++) {
            for (int j = 0; j < pixelWidth; j++) {
                int x = canvasX + j * scale;
                int y = canvasY + i * scale;
                // For glass canvases, a transparency checkerboard keeps the empty cells visible
                if (glass) {
                    checker(x, y, i + j);
                }
                cell(x, y, pixels[i * pixelWidth + j]);
            }
        }
    }

    void drawSides(int canvasX, int canvasY, CanvasType canvasType, int[] sidePixels, boolean glass) {
        int pixelWidth = CanvasType.getWidth(canvasType);
        int pixelHeight = CanvasType.getHeight(canvasType);
        int canvasWidth = pixelWidth * scale;
        int canvasHeight = pixelHeight * scale;
        for (int k = 0; k < pixelWidth; k++) {
            int x = canvasX + k * scale;
            if (glass) {
                checker(x, canvasY - scale, k - 1);
                checker(x, canvasY + canvasHeight, k + pixelHeight);
            }
            cell(x, canvasY - scale, sidePixel(sidePixels, CanvasSides.topOffset() + k));
            cell(x, canvasY + canvasHeight, sidePixel(sidePixels, CanvasSides.bottomOffset(canvasType) + k));
        }
        for (int i = 0; i < pixelHeight; i++) {
            int y = canvasY + i * scale;
            if (glass) {
                checker(canvasX - scale, y, i - 1);
                checker(canvasX + canvasWidth, y, i + pixelWidth);
            }
            cell(canvasX - scale, y, sidePixel(sidePixels, CanvasSides.leftOffset(canvasType) + i));
            cell(canvasX + canvasWidth, y, sidePixel(sidePixels, CanvasSides.rightOffset(canvasType) + i));
        }
    }

    static int sidePixel(int[] sidePixels, int index) {
        if (sidePixels != null && index >= 0 && index < sidePixels.length) {
            return sidePixels[index];
        }
        return CanvasSides.DEFAULT_COLOR;
    }
}
