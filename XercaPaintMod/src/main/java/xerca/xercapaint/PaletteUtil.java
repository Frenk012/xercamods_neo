package xerca.xercapaint;

import net.minecraft.network.FriendlyByteBuf;

public class PaletteUtil {
    public static final Color EMPTINESS_COLOR = new Color(255, 236, 229);

    /** The 16 basic palette colors, indexed 0..15. Shared source of truth for client rendering and server charge accounting. */
    public static final Color[] BASIC_COLORS = {
            new Color(0xFF1D1D21),
            new Color(0xFFB02E26),
            new Color(0xFF5E7C16),
            new Color(0xFF835432),
            new Color(0xFF3C44AA),
            new Color(0xFF8932B8),
            new Color(0xFF169C9C),
            new Color(0xFF9D9D97),
            new Color(0xFF474F52),
            new Color(0xFFF38BAA),
            new Color(0xFF80C71F),
            new Color(0xFFFED83D),
            new Color(0xFF3AB3DA),
            new Color(0xFFC74EBD),
            new Color(0xFFF9801D),
            new Color(0xFFF9FFFE)
    };

    private static double dist2(Color c, double r, double g, double b) {
        double dr = c.r - r;
        double dg = c.g - g;
        double db = c.b - b;
        return dr * dr + dg * dg + db * db;
    }

    /**
     * Feature 1 (per-color charge): estimate which basic colors compose a painted pixel colour.
     * <p>
     * Custom colours are stored only as an averaged RGB, so the exact dyes used are unknown. This greedily
     * reconstructs the mix: it repeatedly adds the available basic colour that brings the running average
     * closest to the target, mirroring how custom colours are actually mixed by averaging dyes.
     *
     * @param rgb       the pixel colour (ARGB or RGB; alpha ignored)
     * @param available which of the 16 basic colours are unlocked on the palette
     * @return per-basic-colour unit counts estimating the composition (sums to &ge;1 when any colour is available)
     */
    public static int[] estimateComposition(int rgb, boolean[] available) {
        final int cap = 8;
        Color target = new Color(rgb & 0xFFFFFF);
        int[] counts = new int[16];
        long sr = 0, sg = 0, sb = 0;
        int n = 0;
        double bestErr = Double.MAX_VALUE;

        for (int step = 0; step < cap; step++) {
            int bestI = -1;
            double bestStepErr = bestErr;
            for (int i = 0; i < 16; i++) {
                if (!available[i]) {
                    continue;
                }
                Color bc = BASIC_COLORS[i];
                double err = dist2(target,
                        (double) (sr + bc.r) / (n + 1),
                        (double) (sg + bc.g) / (n + 1),
                        (double) (sb + bc.b) / (n + 1));
                if (err < bestStepErr) {
                    bestStepErr = err;
                    bestI = i;
                }
            }
            if (bestI < 0) {
                break; // adding any colour only makes it worse
            }
            counts[bestI]++;
            sr += BASIC_COLORS[bestI].r;
            sg += BASIC_COLORS[bestI].g;
            sb += BASIC_COLORS[bestI].b;
            n++;
            bestErr = bestStepErr;
        }

        if (n == 0) {
            // Nothing improved from empty (or nothing unlocked); fall back to the single nearest available colour.
            int nearest = -1;
            double nearestErr = Double.MAX_VALUE;
            for (int i = 0; i < 16; i++) {
                if (!available[i]) {
                    continue;
                }
                double err = dist2(target, BASIC_COLORS[i].r, BASIC_COLORS[i].g, BASIC_COLORS[i].b);
                if (err < nearestErr) {
                    nearestErr = err;
                    nearest = i;
                }
            }
            if (nearest >= 0) {
                counts[nearest] = 1;
            }
        }
        return counts;
    }

    public static class Color {
        public static final Color WHITE = new Color(0xFFFFFFFF);

        public int r;
        public int g;
        public int b;

        public Color(int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public Color(int rgb) {
            this.r = (rgb >> 16) & 0xFF;
            this.g = (rgb >> 8) & 0xFF;
            this.b = rgb & 0xFF;
        }

        public int rgbVal() {
            int val = r;
            val = (val << 8) + g;
            val = (val << 8) + b;
            val += 0xFF000000;
            return val;
        }

        public static Color mix(Color a, Color b, float ratio) {
            if (ratio == 1.f) {
                return a;
            } else if (ratio == 0.f) {
                return b;
            }
            Color res = new Color(
                    (int) (a.r * ratio) + (int) (b.r * (1 - ratio)),
                    (int) (a.g * ratio) + (int) (b.g * (1 - ratio)),
                    (int) (a.b * ratio) + (int) (b.b * (1 - ratio))
            );
            int averageMaximum = (int) (Math.max(Math.max(a.r, a.g), a.b) * ratio) + (int) (Math.max(Math.max(b.r, b.g), b.b) * (1 - ratio));

            int maximumOfAverage = Math.max(Math.max(res.r, res.g), res.b);
            int gainFactor = averageMaximum / maximumOfAverage;

            res.r *= gainFactor;
            res.g *= gainFactor;
            res.b *= gainFactor;
            return res;
        }
    }

    public static class CustomColor {
        public int totalRed = 0;
        public int totalGreen = 0;
        public int totalBlue = 0;
        public int totalMaximum = 0;
        public int numberOfColors = 0;

        private Color result;

        public CustomColor() {
            calculateResult();
        }

        public CustomColor(FriendlyByteBuf buf) {
            readFromBuffer(buf);
            calculateResult();
        }

        public CustomColor(int totalRed, int totalGreen, int totalBlue, int totalMaximum, int numberOfColors) {
            this.totalRed = totalRed;
            this.totalGreen = totalGreen;
            this.totalBlue = totalBlue;
            this.totalMaximum = totalMaximum;
            this.numberOfColors = numberOfColors;
            calculateResult();
        }

        public void calculateResult() {
            if (numberOfColors == 0) {
                this.result = EMPTINESS_COLOR;
                return;
            }
            int averageRed = totalRed / numberOfColors;
            int averageGreen = totalGreen / numberOfColors;
            int averageBlue = totalBlue / numberOfColors;
            int averageMaximum = totalMaximum / numberOfColors;

            int maximumOfAverage = Math.max(Math.max(averageRed, averageGreen), averageBlue);
            int gainFactor = averageMaximum / maximumOfAverage;

            int resultRed = averageRed * gainFactor;
            int resultGreen = averageGreen * gainFactor;
            int resultBlue = averageBlue * gainFactor;

            this.result = new Color(resultRed, resultGreen, resultBlue);
        }

        public void mix(Color toBeMixed) {
            totalRed += toBeMixed.r;
            totalGreen += toBeMixed.g;
            totalBlue += toBeMixed.b;
            totalMaximum += Math.max(Math.max(toBeMixed.r, toBeMixed.g), toBeMixed.b);
            numberOfColors += 1;
            calculateResult();
        }

        public void reset() {
            totalRed = 0;
            totalGreen = 0;
            totalBlue = 0;
            totalMaximum = 0;
            numberOfColors = 0;
            calculateResult();
        }

        public Color getColor() {
            return result;
        }

        public int getNumberOfColors() {
            return numberOfColors;
        }

        public void writeToBuffer(FriendlyByteBuf buf) {
            buf.writeInt(totalRed);
            buf.writeInt(totalGreen);
            buf.writeInt(totalBlue);
            buf.writeInt(totalMaximum);
            buf.writeInt(numberOfColors);
        }

        public void readFromBuffer(FriendlyByteBuf buf) {
            totalRed = buf.readInt();
            totalGreen = buf.readInt();
            totalBlue = buf.readInt();
            totalMaximum = buf.readInt();
            numberOfColors = buf.readInt();
        }
    }
}
