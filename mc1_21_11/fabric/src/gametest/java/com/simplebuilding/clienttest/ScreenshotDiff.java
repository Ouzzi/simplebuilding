package com.simplebuilding.clienttest;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import javax.imageio.ImageIO;

/**
 * Pixel level comparison of two screenshots taken by the client game test harness.
 *
 * <p>The client game test API only offers {@code assertScreenshotEquals} against a stored template
 * image. These tests have no reference images (and reference images would have to be regenerated on
 * every texture or lighting change), so they compare two screenshots taken in the same run against
 * each other instead: one without the renderer's trigger, one with it. The trigger is the only
 * thing that changed, so any pixel difference is the renderer's output.
 *
 * <p>To make that argument airtight every test also measures a <em>noise floor</em>: two screenshots
 * of the very same state a few ticks apart. In the frozen scene from {@link RendererTestScene} that
 * difference must be essentially zero. It is asserted (so a scene that is not actually static fails
 * as a setup error instead of silently passing every renderer test), and it is used as the
 * comparison baseline for the real signal.
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
        BufferedImage a = read(first);
        BufferedImage b = read(second);

        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            throw new AssertionError("Screenshot sizes differ: " + first + " is " + a.getWidth() + "x" + a.getHeight()
                    + ", " + second + " is " + b.getWidth() + "x" + b.getHeight());
        }

        int changed = 0;

        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
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

        Diff diff = new Diff(label, changed, a.getWidth() * a.getHeight());
        System.out.println("[simplebuilding-test] " + diff);
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
     * measured noise floor and an absolute minimum. The absolute minimum is scaled to the window
     * size so the thresholds hold at any resolution. For reference, at 854x480 the minimum is
     * ~273 pixels while the thinnest effect under test (two octant box outlines) covers well over
     * a thousand.
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

        System.out.println("[simplebuilding-test] " + renderer + " drew: " + signal
                + " vs. required " + required);
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
