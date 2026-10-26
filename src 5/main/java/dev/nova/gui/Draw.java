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

    /** Rounded box with a 1px outline. */
    static void roundBorder(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int fill, int line) {
        round(g, x, y, w, h, r, line);
        round(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
    }

    /** Plain 1px outline. */
    static void border(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** Anti-aliased filled circle. */
    static void disc(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
        if (r <= 0) return;
        for (int dy = -r; dy <= r; dy++) {
            double yy = Math.abs(dy) - 0.0;
            double half = Math.sqrt(Math.max(0, (r + 0.5) * (r + 0.5) - yy * yy));
            int full = (int) Math.floor(half - 0.5);
            double cover = (half - 0.5) - full;
            if (full >= 0) g.fill(cx - full, cy + dy, cx + full + 1, cy + dy + 1, color);
            if (cover > 0.05) {
                int c = withAlpha(color, cover);
                g.fill(cx - full - 1, cy + dy, cx - full, cy + dy + 1, c);
                g.fill(cx + full + 1, cy + dy, cx + full + 2, cy + dy + 1, c);
            }
        }
    }

    /** Blend two ARGB colors; t = 0 gives a, t = 1 gives b. */
    static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int out = 0;
        for (int shift = 24; shift >= 0; shift -= 8) {
            int ca = (a >>> shift) & 0xFF, cb = (b >>> shift) & 0xFF;
            out |= (Math.round(ca + (cb - ca) * t) & 0xFF) << shift;
        }
        return out;
    }

    /** Heavier text (drawn twice, one pixel apart). */
    static void bold(GuiGraphicsExtractor g, String s, int x, int y, int color) {
        text(g, s, x, y, color);
        text(g, s, x + 1, y, color);
    }

    static void textCentered(GuiGraphicsExtractor g, String s, int cx, int y, int color) {
        text(g, s, cx - width(s) / 2, y, color);
    }

    /** On/off switch, 24 x 12. */
    static void pill(GuiGraphicsExtractor g, int x, int y, boolean on, int accent) {
        round(g, x, y, 24, 12, 6, on ? accent : 0xFF3A3A44);
        disc(g, on ? x + 18 : x + 6, y + 6, 4, 0xFFFFFFFF);
    }

    /** Small label tag; returns its width so several can be laid out in a row. */
    static int chip(GuiGraphicsExtractor g, String label, int x, int y, int bg, int fg) {
        int w = width(label) + 12;
        round(g, x, y, w, 14, 7, bg);
        text(g, label, x + 6, y + 3, fg);
        return w;
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
