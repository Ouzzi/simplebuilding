package com.simplebuilding.clientgametest;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import javax.imageio.ImageIO;

/**
 * Pixel level comparison of two screenshots taken during the same client run.
 *
 * <p><b>Why differences and not reference images.</b> The only comparison the client game test API
 * offers is {@code assertScreenshotEquals} against a stored template image. These tests have no
 * template images, and template images would have to be regenerated on every texture, lighting or
 * resolution change - which turns a broken renderer into a routine "update the references" step,
 * i.e. into a test that never fails. So two screenshots taken in the same run are compared against
 * each other instead: one without the renderer's trigger, one with it. The trigger is the only
 * thing that changed between them, so any pixel difference is the renderer's output.
 *
 * <p><b>The noise floor is what makes that argument airtight.</b> Every test first measures two
 * screenshots of the very same state a few ticks apart. In the frozen scene from {@link TestScene}
 * that difference has to be essentially zero. It is <em>asserted</em> ({@link #assertUnchanged}),
 * so a scene that is not actually static fails as a setup error instead of silently passing every
 * renderer test below it, and it is then used as the baseline the real signal has to beat.
 *
 * <p><b>Method and thresholds are the same on every target</b>, so that "the renderer draws"
 * means the same thing on Fabric and on NeoForge. That was already true when the two loader copies
 * of this class were maintained side by side; it is now true because there is one copy.
 *
 * <p><b>Not covered:</b> the absolute colour of a highlight pixel. A difference proves that
 * something was drawn and that it scales with the option under test, but the blend factor cannot be
 * read back from the framebuffer without knowing the wall texture's per pixel colour, and pinning
 * it would break on any texture pack or lighting change.
 */
public final class ScreenshotDiff {

    /**
     * Per channel difference at which a pixel counts as changed. High enough to ignore a stray
     * rounding difference, far below the ~30 levels a 30% black highlight line puts on stone and
     * the ~110 levels an 80% orange octant line puts on it.
     */
    private static final int CHANNEL_TOLERANCE = 12;

    private ScreenshotDiff() {
    }

    /** Result of comparing two screenshots. */
    public record Diff(String label, int changedPixels, int totalPixels) {

        public double percent() {
            return 100.0 * changedPixels / totalPixels;
        }

        @Override
        public String toString() {
            return String.format("%s: %d/%d changed pixels (%.4f%%)", label, changedPixels, totalPixels, percent());
        }
    }

    /** Compares the whole frame. */
    public static Diff compare(String label, Path first, Path second) {
        return compare(label, first, second, false);
    }

    /**
     * Compares two screenshots, optionally only their right half.
     *
     * @param rightHalfOnly restricts the comparison to the right half of the frame. Needed for the
     *                      octant highlight: the mod also draws a rangefinder info panel in the
     *                      upper left while an octant is held, and that panel would be counted as
     *                      "the in-world renderer drew something". The right half contains one
     *                      complete corner box and none of the panel, so a difference there can
     *                      only come from the in-world geometry. Note that {@code totalPixels} is
     *                      then the size of that half, not of the frame - which is deliberate: the
     *                      thresholds of {@link #assertUnchanged} and {@link #assertDrew} are
     *                      derived from it and have to scale with the area actually looked at.
     */
    public static Diff compare(String label, Path first, Path second, boolean rightHalfOnly) {
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
                if (differs(a.getRGB(x, y), b.getRGB(x, y))) {
                    changed++;
                }
            }
        }

        Diff diff = new Diff(label, changed, (a.getWidth() - fromX) * a.getHeight());
        TestLog.info(diff.toString());
        return diff;
    }

    /**
     * The one pixel rule of this file: two pixels differ when any channel differs by more than
     * {@code CHANNEL_TOLERANCE}.
     *
     * <p>Extracted so that the two readers below cannot drift apart. A count and a position that
     * disagree about what "changed" means would be worse than either on its own.
     */
    private static boolean differs(int first, int second) {
        if (first == second) {
            return false;
        }

        int dr = Math.abs(((first >> 16) & 0xFF) - ((second >> 16) & 0xFF));
        int dg = Math.abs(((first >> 8) & 0xFF) - ((second >> 8) & 0xFF));
        int db = Math.abs((first & 0xFF) - (second & 0xFF));

        return Math.max(dr, Math.max(dg, db)) > CHANNEL_TOLERANCE;
    }

    /**
     * Where the changed pixels sit, not just how many there are.
     *
     * <p>A count answers "did the renderer draw"; it cannot answer "did it draw in the right
     * place". Two drawings of the same size at different positions produce the same number, and
     * that is not a hypothetical distinction: shrinking the building wand ghosts towards the
     * minimum corner of their cell instead of towards the centre moves the whole preview a quarter
     * of a block against the grid it claims to preview, and repaints exactly as many pixels.
     *
     * <p>{@code centreX} and {@code centreY} are the mean of the changed pixels rather than the
     * middle of their bounding box: a mean moves with the whole drawing, a bounding box only with
     * its outermost pixel and is therefore at the mercy of a single stray one.
     */
    public record ChangedArea(String label, int changedPixels, int frameWidth, int frameHeight,
                              int left, int top, int right, int bottom,
                              double centreX, double centreY) {

        @Override
        public String toString() {
            return label + ": " + changedPixels + " changed pixels in " + left + "/" + top
                    + ".." + right + "/" + bottom + ", centre "
                    + String.format(java.util.Locale.ROOT, "%.1f/%.1f", centreX, centreY);
        }
    }

    /** Reads both images and describes WHERE they differ. Same pixel rule as {@link #compare}. */
    public static ChangedArea changedArea(String label, Path first, Path second) {
        BufferedImage a = read(first);
        BufferedImage b = read(second);

        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            throw new AssertionError("Screenshot sizes differ: " + first + " is " + a.getWidth() + "x"
                    + a.getHeight() + ", " + second + " is " + b.getWidth() + "x" + b.getHeight());
        }

        int left = Integer.MAX_VALUE;
        int top = Integer.MAX_VALUE;
        int right = -1;
        int bottom = -1;
        long sumX = 0;
        long sumY = 0;
        int changed = 0;

        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (!differs(a.getRGB(x, y), b.getRGB(x, y))) {
                    continue;
                }

                changed++;
                sumX += x;
                sumY += y;
                left = Math.min(left, x);
                top = Math.min(top, y);
                right = Math.max(right, x);
                bottom = Math.max(bottom, y);
            }
        }

        if (changed == 0) {
            return new ChangedArea(label, 0, a.getWidth(), a.getHeight(), 0, 0, 0, 0,
                    Double.NaN, Double.NaN);
        }

        ChangedArea area = new ChangedArea(label, changed, a.getWidth(), a.getHeight(),
                left, top, right, bottom, sumX / (double) changed, sumY / (double) changed);
        TestLog.info(area.toString());
        return area;
    }

    /**
     * Asserts that two screenshots of an unchanged scene really are the same. This is the
     * self-check of the whole approach: if it fails, the scene is not deterministic and no
     * difference test below it means anything.
     */
    /** The noise floor rule, on its own: at most 60 pixels or one in twenty thousand, whichever is more. */
    public static boolean withinNoise(int changedPixels, int totalPixels) {
        return changedPixels <= Math.max(60, totalPixels / 20000);
    }

    public static void assertUnchanged(Diff diff) {
        int allowed = Math.max(60, diff.totalPixels() / 20000);

        if (diff.changedPixels() > allowed) {
            throw new AssertionError("Scene is not deterministic - " + diff
                    + " while nothing changed on screen (allowed: " + allowed + " pixels). "
                    + "Renderer difference tests cannot be trusted in this state.");
        }
    }

    /**
     * Asserts that the renderer put something on screen: the signal has to be far above both the
     * measured noise floor and an absolute minimum. The absolute minimum is scaled to the compared
     * area so the thresholds hold at any resolution. For reference, at 854x480 the minimum is
     * ~273 pixels while the thinnest effect under test (two octant box outlines) covers well over
     * a thousand.
     */
    public static void assertDrew(String renderer, Diff noiseFloor, Diff signal) {
        int absoluteMinimum = Math.max(400, signal.totalPixels() / 1500);
        int required = Math.max(absoluteMinimum, noiseFloor.changedPixels() * 10 + 200);

        if (signal.changedPixels() < required) {
            throw new AssertionError(renderer + " did not draw anything: " + signal
                    + " but at least " + required + " changed pixels were required (noise floor was "
                    + noiseFloor.changedPixels() + " pixels). The trigger conditions were asserted "
                    + "before the screenshot, so the geometry never reached the screen.");
        }

        TestLog.info(renderer + " drew: " + signal + " vs. required " + required);
    }

    /**
     * Asserts that taking the trigger away again restores the baseline picture. Without this
     * control, "the picture drifts anyway" would be an equally good explanation for the signal.
     *
     * <p>Shares its allowance with {@link #assertLooksIdentical} on purpose - both claims are
     * "these two pictures show the same thing", and the tolerance for that is the measured noise
     * floor with headroom, never zero.
     */
    public static void assertBackToBaseline(String what, Diff noiseFloor, Diff residual) {
        int allowed = allowedForIdentical(noiseFloor, residual);

        if (residual.changedPixels() > allowed) {
            throw new AssertionError("Control step failed: " + what + " did not restore the baseline image ("
                    + residual + ", allowed " + allowed + " pixels). The measured difference cannot be "
                    + "attributed to the renderer.");
        }

        TestLog.info("control ok - " + what + ": " + residual + " (allowed " + allowed + ")");
    }

    /**
     * Two screenshots have to show the same thing, where the claim is that the renderer drew
     * <em>nothing</em> - a wand in the offhand only, a wand without material, an option at zero.
     *
     * <p>Separate from {@link #assertBackToBaseline} only in what it says when it fails: there the
     * claim is "the trigger was taken away again", here it is "the trigger was never sufficient".
     * A failure has to name which of the two was expected, or the message sends the reader looking
     * in the wrong place. The tolerance is the same, and is the measured noise floor with headroom
     * rather than zero, because the comparison is only ever used where the claim is "nothing
     * changed".
     */
    public static void assertLooksIdentical(Diff noiseFloor, Diff diff, String claim) {
        int allowed = allowedForIdentical(noiseFloor, diff);

        if (diff.changedPixels() > allowed) {
            throw new AssertionError(claim + ", but the two pictures differ: " + diff
                    + " (allowed " + allowed + " pixels, noise floor was " + noiseFloor.changedPixels()
                    + " pixels).");
        }

        TestLog.info("unchanged as claimed - " + claim + ": " + diff + " (allowed " + allowed + ")");
    }

    /**
     * The tolerance for "these two pictures are the same": four times the measured noise with a
     * floor of 200 pixels, and never below one pixel in 20000 of the compared area. The absolute
     * terms are what keep it usable when the noise floor measures a clean zero - a strict "no more
     * than the noise" would then demand a bit-identical frame, which no GPU owes anyone.
     */
    private static int allowedForIdentical(Diff noiseFloor, Diff diff) {
        return Math.max(noiseFloor.changedPixels() * 4 + 200, diff.totalPixels() / 20000);
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
