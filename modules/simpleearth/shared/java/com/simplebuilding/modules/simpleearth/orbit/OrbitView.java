package com.simplebuilding.modules.simpleearth.orbit;

/**
 * Whether this client shows the earth right now (camera above the render switch). Kept free of client classes so the weather
 * mixin on {@code Level} can read it without loading client code.
 */
public final class OrbitView {
    /** WE25-1: while true the client level reports no rain or thunder (no rain, snow or their sounds in orbit). */
    public static volatile boolean active;

    private OrbitView() {}
}
