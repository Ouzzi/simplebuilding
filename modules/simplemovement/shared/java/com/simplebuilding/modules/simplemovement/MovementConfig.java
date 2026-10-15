package com.simplebuilding.modules.simplemovement;

/** {@code config/simplemovement.json} (formerly Simple QoL); server values, sent to clients. */
public final class MovementConfig {
    public static final double MAX_CLIMB_SPEED = 0.4;
    public static final double MAX_SLIDE_SPEED = 0.8;
    public static final double DEFAULT_CLIMB_SPEED = 0.4;
    public static final double DEFAULT_SLIDE_SPEED = 0.8;

    /** Public so Cloth Config can read it. */
    public enum SlideActivationMode {
        CAMERA, ALWAYS
    }

    /** /crawl and the crawl key set the swimming pose. */
    public boolean enableManualCrawl = true;
    /** Server allows the client autowalk key. */
    public boolean enableAutowalk = false;
    /** Upward ladder speed, 0.2-0.4 (Vanilla about 0.2). */
    public double ladderClimbingSpeed = DEFAULT_CLIMB_SPEED;
    public boolean enableFastLadderSlide = true;
    /** Downward slide speed, 0.15-0.8. */
    public double ladderSlideSpeed = DEFAULT_SLIDE_SPEED;
    public SlideActivationMode ladderSlideActivation = SlideActivationMode.CAMERA;
    /** Frost Walker boots walk on powder snow (formerly a top-level Simple QoL option). */
    public boolean frostWalkerWalkOnPowderSnow = true;

    /** Jump Boost lets players step up one block without jumping (never stacks with Step Height boots, the higher value wins). */
    public boolean jumpBoostStepUp = true;

    public static double bounded(double v, double min, double max, double fallback) {
        return Double.isFinite(v) ? Math.clamp(v, min, max) : fallback;
    }

    public void normalizeNumbers() {
        ladderClimbingSpeed = bounded(ladderClimbingSpeed, 0.2, MAX_CLIMB_SPEED, DEFAULT_CLIMB_SPEED);
        ladderSlideSpeed = bounded(ladderSlideSpeed, 0.15, MAX_SLIDE_SPEED, DEFAULT_SLIDE_SPEED);
        if (ladderSlideActivation == null) ladderSlideActivation = SlideActivationMode.CAMERA;
    }

    public void normalize() {
        normalizeNumbers();
    }
}
