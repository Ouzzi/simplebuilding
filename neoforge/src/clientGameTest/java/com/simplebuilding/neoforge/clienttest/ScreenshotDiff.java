package com.simplebuilding.neoforge.clienttest;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import javax.imageio.ImageIO;

/**
 * Pixel level comparison of two screenshots taken during the same client run.
 *
 * <p>Identical in method (and in thresholds) to the Fabric client game test of the same name, so a
 * "the renderer draws" statement means the same thing on both loaders: take a screenshot without
 * the renderer's trigger, take one with it, and require the two to differ. Since the trigger is the
 * only thing that changed, any pixel difference is the renderer's output.
 *
 * <p>Every test also measures a <em>noise floor</em> first: two screenshots of the very same state
 * a few ticks apart. In a frozen scene that difference has to be essentially zero. It is asserted
 * (so a scene that is not actually static fails as a setup error instead of silently passing every
 * renderer test) and it is used as the baseline the real signal has to beat.
 */
final class ScreenshotDiff {

    /**
     * Per channel difference at which a pixel counts as changed. High enough to ignore a stray
     * rounding difference, far below the ~30 levels a 30% black highlight line puts on stone and
     * the ~110 levels an 80% orange octant line puts on it.
     */
    private static final int CHANNEL_TOLERANCE = 12;

    private ScreenshotDiff() {
    }

    /** Result of comparing two screenshots. */
    record Diff(String label, int changedPixels, int totalPixels) {
        double percent() {
            return 100.0 * changedPixels / totalPixels;
        }

        @Override
        public String toString() {
            return String.format("%s: %d/%d changed pixels (%.4f%%)", label, changedPixels, totalPixels, percent());
        }
    }

    static Diff compare(String label, Path first, Path second) {
        return compare(label, first, second, false);
    }

    /**
     * @param rightHalfOnly restricts the comparison to the right half of the frame. Needed for the
     *                      octant highlight: the mod also draws a rangefinder info panel in the
     *                      upper left while an octant is held, and that panel would be counted as
     *                      "the in-world renderer drew something". The right half contains one
     *                      complete corner box and none of the panel, so a difference there can
     *                      only come from the in-world geometry.
     */
    static Diff compare(String label, Path first, Path second, boolean rightHalfOnly) {
        BufferedImage a = read(first);
        BufferedImage b = read(second);

        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            throw new AssertionError("Screenshot sizes differ: " + first + " is " + a.getWidth() + "x" + a.getHeight()
                    + ", " + second + " is " + b.getWidth() + "x" + b.getHeight());
        }

        int fromX = rightHalfOnly ? a.getWidth() / 2 : 0;
        int changed = 0;

        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = fromX; x < a.getWidth(); x++) {
                int pa = a.getRGB(x, y);
                int pb = b.getRGB(x, y);

                if (pa == pb) {
                    continue;
                }

                int dr = Math.abs(((pa >> 16) & 0xFF) - ((pb >> 16) & 0xFF));
                int dg = Math.abs(((pa >> 8) & 0xFF) - ((pb >> 8) & 0xFF));
                int db = Math.abs((pa & 0xFF) - (pb & 0xFF));

                if (Math.max(dr, Math.max(dg, db)) > CHANNEL_TOLERANCE) {
                    changed++;
                }
            }
        }

        Diff diff = new Diff(label, changed, (a.getWidth() - fromX) * a.getHeight());
        Log.info(diff.toString());
        return diff;
    }

    /**
     * Asserts that two screenshots of an unchanged scene really are the same. This is the
     * self-check of the whole approach: if it fails, the scene is not deterministic and no
     * difference test below it means anything.
     */
    static void assertUnchanged(Diff diff) {
        int allowed = Math.max(60, diff.totalPixels() / 20000);

        if (diff.changedPixels() > allowed) {
            throw new AssertionError("Scene is not deterministic - " + diff
                    + " while nothing changed on screen (allowed: " + allowed + " pixels). "
                    + "Renderer difference tests cannot be trusted in this state.");
        }
    }

    /**
     * Asserts that the renderer put something on screen: the signal has to be far above both the
     * measured noise floor and an absolute minimum scaled to the window size. At 854x480 the
     * minimum is ~273 pixels while the thinnest effect under test (two octant box outlines) covers
     * well over a thousand.
     */
    static void assertDrew(String renderer, Diff noiseFloor, Diff signal) {
        int absoluteMinimum = Math.max(400, signal.totalPixels() / 1500);
        int required = Math.max(absoluteMinimum, noiseFloor.changedPixels() * 10 + 200);

        if (signal.changedPixels() < required) {
            throw new AssertionError(renderer + " did not draw anything: " + signal
                    + " but at least " + required + " changed pixels were required (noise floor was "
                    + noiseFloor.changedPixels() + " pixels). The trigger conditions were asserted "
                    + "before the screenshot, so the geometry never reached the screen.");
        }

        Log.info(renderer + " drew: " + signal + " vs. required " + required);
    }

    /**
     * Asserts that taking the trigger away again restores the baseline picture. Without this
     * control, "the picture drifts anyway" would be an equally good explanation for the signal.
     */
    static void assertBackToBaseline(String what, Diff noiseFloor, Diff residual) {
        int allowed = Math.max(noiseFloor.changedPixels() * 4 + 200, residual.totalPixels() / 20000);

        if (residual.changedPixels() > allowed) {
            throw new AssertionError("Control step failed: " + what + " did not restore the baseline image ("
                    + residual + ", allowed " + allowed + " pixels). The measured difference cannot be "
                    + "attributed to the renderer.");
        }

        Log.info("control ok - " + what + ": " + residual + " (allowed " + allowed + ")");
    }

    private static BufferedImage read(Path path) {
        try {
            BufferedImage image = ImageIO.read(path.toFile());

            if (image == null) {
                throw new AssertionError("Could not decode screenshot " + path);
            }

            return image;
        } catch (IOException e) {
            throw new AssertionError("Could not read screenshot " + path, e);
        }
    }
}
