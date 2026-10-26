package dev.nova.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Thin wrapper over the GUI drawing calls so version changes only touch this file.
 * Rounded shapes are anti-aliased (soft edge pixels) so corners no longer look blocky.
 */
final class Draw {
    private Draw() {}

    static void rect(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + h, color);
    }

    private static int withAlpha(int color, double f) {
        int a = (int) Math.round(((color >>> 24) & 0xFF) * Math.max(0, Math.min(1, f)));
        return (a << 24) | (color & 0xFFFFFF);
    }

    /** Anti-aliased rounded rectangle. */
    static void round(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) {
            rect(g, x, y, w, h, color);
            return;
        }
        g.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            // exact horizontal extent of the circle at the middle of this pixel row
            double dy = r - i - 0.5;
            double dx = Math.sqrt(r * (double) r - dy * dy);
            double edge = r - dx;               // how far the shape is inset on this row
            int full = (int) Math.ceil(edge);   // first fully covered column
            double cover = full - edge;         // coverage of the partial pixel before it
            int left = x + full, right = x + w - full;
            g.fill(left, y + i, right, y + i + 1, color);
            g.fill(left, y + h - 1 - i, right, y + h - i, color);
            if (cover > 0.02 && full > 0) {
                int c = withAlpha(color, cover);
                g.fill(left - 1, y + i, left, y + i + 1, c);
                g.fill(right, y + i, right + 1, y + i + 1, c);
                g.fill(left - 1, y + h - 1 - i, left, y + h - i, c);
                g.fill(right, y + h - 1 - i, right + 1, y + h - i, c);
            }
        }
    }

    /** Soft shadow under a panel: a few widening, fading rounded layers. */
    static void shadow(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int spread) {
        for (int i = spread; i >= 1; i--) {
            int alpha = (int) (34.0 * (1.0 - i / (spread + 1.0)));
            round(g, x - i, y - i + 2, w + 2 * i, h + 2 * i, r + i, alpha << 24);
        }
    }

    static void text(GuiGraphicsExtractor g, String s, int x, int y, int color) {
        g.text(Minecraft.getInstance().font, s, x, y, color, false);
    }

    static int width(String s) {
        return Minecraft.getInstance().font.width(s);
    }

    static void clipOn(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.enableScissor(x, y, x + w, y + h);
    }

    static void clipOff(GuiGraphicsExtractor g) {
        g.disableScissor();
    }
}
