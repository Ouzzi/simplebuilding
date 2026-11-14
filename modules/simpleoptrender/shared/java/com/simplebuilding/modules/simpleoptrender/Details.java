package com.simplebuilding.modules.simpleoptrender;

/**
 * O9 detail switches, own implementation (Sodium Extra describes "toggles for animations, particles and fog"; here without
 * a Sodium dependency). Every switch defaults to true = Vanilla. {@code fog false} lifts only the air/render-distance fog:
 * fog inside water, lava or powder snow and the fog of blindness and darkness stay, since they carry gameplay information.
 */
public final class Details {
    public record Switches(boolean particles, boolean textureAnimations, boolean fog) {
        public static final Switches DEFAULTS = new Switches(true, true, true);
    }

    /** Fog distance that never reaches a pixel. */
    public static final float NO_FOG = Float.MAX_VALUE;

    private Details() {}

    /**
     * simpleoptrender-4 (PLAN40): whether the atmospheric (environmental) fog goes with it. The Nether's and End's thick fog is
     * part of their look (and of the dragon fight), so only a dimension with sky light and no ceiling loses it; the render-distance
     * fog (the far haze) is lifted everywhere.
     */
    public static boolean liftEnvironmentalFog(boolean dimensionHasSkyLight, boolean dimensionHasCeiling) {
        return dimensionHasSkyLight && !dimensionHasCeiling;
    }

    /** Whether the air fog may be lifted: switch off, camera not in a fluid or snow, no blindness or darkness. */
    public static boolean liftFog(Switches s, boolean cameraInFluidOrSnow, boolean blindOrDark) {
        return !s.fog() && !cameraInFluidOrSnow && !blindOrDark;
    }
}
