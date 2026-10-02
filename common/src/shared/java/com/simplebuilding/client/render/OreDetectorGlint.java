package com.simplebuilding.client.render;

import com.simplebuilding.items.custom.OreDetectorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.item.properties.numeric.CompassAngle;
import net.minecraft.client.renderer.item.properties.numeric.CompassAngleState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.function.LongSupplier;

/**
 * Auswahl-Schimmer des Erzdetektors in Inventar und Schnellleiste: ein 1-px-Funke mit zwei
 * verblassenden Schweifpixeln laeuft auf der Nadel von der Nabe zur Spitze, in der Farbe des
 * kalibrierten Blocks ({@link OreDetectorItem#targetColor}). Ohne kalibrierten Block wird nichts
 * gezeichnet. (Bis 2026-10-02 lief er am Rand des Itemfelds um; Besitzer: auf die Nadel.)
 *
 * <p>Welche Nadel gerade zu sehen ist, entscheidet dieselbe Rechnung wie das Itemmodell: ohne
 * {@code lodestone_tracker} das Ruhebild (Nadel oben, Bild 16), sonst Vanillas Kompasswinkel zum
 * Ziel ohne Nachschwingen ({@code CompassAngle(false, LODESTONE)}, wie im Modell), gerundet auf
 * die 32 Bilder. Die Pixel der Bilder stehen in {@link OreDetectorNeedlePath}.
 *
 * <p>Gezeichnet wird aus {@code ItemDecorationsMixin} am Ende von Vanillas Item-Dekorationen
 * (Haltbarkeitsbalken, Abklingzeit, Anzahl) - derselbe Aufruf auf Fabric, NeoForge und Forge.
 */
public final class OreDetectorGlint {
    /** Millisekunden je Schritt des Funkens. */
    public static final long MILLIS_PER_STEP = 150;
    /** Deckkraft von Kopf und Schweif (von vorn nach hinten). */
    public static final int[] TRAIL_ALPHA = {0xD0, 0x80, 0x40};
    /** Schritte Pause, nachdem der Schweif die Spitze verlassen hat. */
    public static final int PAUSE_STEPS = 4;
    /** Bild der ruhenden Nadel (ohne Fund): oben. */
    public static final int IDLE_FRAME = 16;

    /** Uhr der Animation. Client-Tests frieren sie ein, damit ein Screenshot reproduzierbar ist. */
    public static volatile LongSupplier clock = Util::getMillis;

    private static final CompassAngle ANGLE = new CompassAngle(false, CompassAngleState.CompassTarget.LODESTONE);

    private OreDetectorGlint() {
    }

    /** Nadelbild 0..31 zum Kompasswert 0..1 - dieselbe Zuordnung wie {@code ModModelProvider#generateOreDetector}. */
    public static int frameForAngle(float angle) {
        return Math.floorMod(Math.round(angle * 32.0F) - 16, 32);
    }

    /** Schritte einer Runde auf dem Nadelbild {@code frame}. */
    public static int lapSteps(int frame) {
        return OreDetectorNeedlePath.length(frame) + TRAIL_ALPHA.length + PAUSE_STEPS;
    }

    /**
     * Die Funkenpixel zur Zeit {@code millis} auf dem Nadelbild {@code frame}: je {dx, dy, alpha}
     * im 16x16-Feld, Kopf zuerst. Liegt ein Schweifpixel nicht (mehr) auf der Nadel, fehlt es.
     */
    public static int[][] sparks(int frame, long millis) {
        int length = OreDetectorNeedlePath.length(frame);
        int head = (int) Math.floorMod(millis / MILLIS_PER_STEP, (long) lapSteps(frame));
        int[][] out = new int[TRAIL_ALPHA.length][];
        int n = 0;
        for (int i = 0; i < TRAIL_ALPHA.length; i++) {
            int k = head - i;
            if (k >= 0 && k < length) {
                int[] p = OreDetectorNeedlePath.pixel(frame, k);
                out[n++] = new int[]{p[0], p[1], TRAIL_ALPHA[i]};
            }
        }
        return Arrays.copyOf(out, n);
    }

    private static int currentFrame(ItemStack stack) {
        if (!stack.has(DataComponents.LODESTONE_TRACKER)) return IDLE_FRAME;
        Minecraft minecraft = Minecraft.getInstance();
        return frameForAngle(ANGLE.get(stack, minecraft.level, minecraft.player, 0));
    }

    public static void render(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        int rgb = OreDetectorItem.targetColor(stack);
        if (rgb < 0) return;
        for (int[] s : sparks(currentFrame(stack), clock.getAsLong())) {
            int px = x + s[0];
            int py = y + s[1];
            graphics.fill(px, py, px + 1, py + 1, (s[2] << 24) | (rgb & 0xFFFFFF));
        }
    }
}
