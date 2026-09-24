package com.simplebuilding.client.render;

import com.simplebuilding.items.custom.OreDetectorItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

import java.util.function.LongSupplier;

/**
 * Randschimmer des Erzdetektors in Inventar und Schnellleiste: ein 1-px-Funke mit zwei
 * verblassenden Schweifpixeln laeuft auf dem aeussersten Pixelrand des 16x16-Itemfelds um, in der
 * Farbe des kalibrierten Blocks ({@link OreDetectorItem#targetColor}). Ohne kalibrierten Block
 * wird nichts gezeichnet.
 *
 * <p>Gezeichnet wird aus {@code ItemDecorationsMixin} am Ende von Vanillas Item-Dekorationen
 * (Haltbarkeitsbalken, Abklingzeit, Anzahl) - derselbe Aufruf auf Fabric, NeoForge und Forge.
 */
public final class OreDetectorGlint {
    /** Pixel auf dem Rand eines 16x16-Felds: 4 * 16 - 4. */
    public static final int PERIMETER = 60;
    /** Millisekunden je Schritt: eine Runde dauert 60 * 50 ms = 3 s. */
    public static final long MILLIS_PER_STEP = 50;
    /** Deckkraft von Kopf und Schweif (von vorn nach hinten). */
    public static final int[] TRAIL_ALPHA = {0xD0, 0x80, 0x40};

    /** Uhr der Animation. Client-Tests frieren sie ein, damit ein Screenshot reproduzierbar ist. */
    public static volatile LongSupplier clock = Util::getMillis;

    private OreDetectorGlint() {
    }

    /**
     * Versatz {dx, dy} des Randpixels {@code index} (0..59) innerhalb des 16x16-Felds, im
     * Uhrzeigersinn ab der oberen linken Ecke.
     */
    public static int[] perimeterPixel(int index) {
        int i = Math.floorMod(index, PERIMETER);
        if (i < 15) return new int[]{i, 0};
        if (i < 30) return new int[]{15, i - 15};
        if (i < 45) return new int[]{15 - (i - 30), 15};
        return new int[]{0, 15 - (i - 45)};
    }

    /** Randindex des Kopfpixels zur Zeit {@code millis}. */
    public static int headIndex(long millis) {
        return (int) Math.floorMod(millis / MILLIS_PER_STEP, (long) PERIMETER);
    }

    public static void render(GuiGraphics graphics, ItemStack stack, int x, int y) {
        int rgb = OreDetectorItem.targetColor(stack);
        if (rgb < 0) return;
        int head = headIndex(clock.getAsLong());
        for (int i = 0; i < TRAIL_ALPHA.length; i++) {
            int[] p = perimeterPixel(head - i);
            int px = x + p[0];
            int py = y + p[1];
            graphics.fill(px, py, px + 1, py + 1, (TRAIL_ALPHA[i] << 24) | (rgb & 0xFFFFFF));
        }
    }
}
