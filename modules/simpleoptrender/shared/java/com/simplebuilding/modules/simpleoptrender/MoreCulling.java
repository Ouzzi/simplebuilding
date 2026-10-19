package com.simplebuilding.modules.simpleoptrender;

/**
 * O3 more culling, own rules (technique as published by More Culling: "sign text, item frames, beacon beam" culling;
 * implemented from the Vanilla renderers only). Pure geometry, so the server-side gametests can check it:
 * <ul>
 * <li>Sign text: Vanilla extracts both text sides every frame. The board is opaque, so the side facing away from the camera
 * cannot be seen once the camera is more than {@link #SIGN_MARGIN} behind the board's center along its normal.</li>
 * <li>Item frames: a frame (with its item or map) is hidden behind its wall once the camera is more than
 * {@link #FRAME_MARGIN} behind the frame plane, i.e. on the far side of the supporting block, and that block is opaque.</li>
 * <li>Beacon beams are rendered globally without a view-cone check; a beam whose column lies outside the frustum is skipped.</li>
 * </ul>
 */
public final class MoreCulling {
    /** Half a block: the text planes of standing, wall and hanging signs lie within it around the hitbox center. */
    public static final double SIGN_MARGIN = 0.5;
    /** The supporting block spans 0.03..1.03 behind a frame's position; 1.1 keeps the camera clearly behind it. */
    public static final double FRAME_MARGIN = 1.1;
    /** Top of the beam column (Vanilla BeaconRenderer.MAX_RENDER_Y). */
    public static final double BEAM_TOP = 2048;

    /** Config switches (all default on). */
    public record Parts(boolean signText, boolean itemFrames, boolean beaconBeams) {
        public static final Parts DEFAULTS = new Parts(true, true, true);
    }

    public enum SignSides { BOTH, FRONT, BACK }

    private MoreCulling() {}

    /**
     * Which text sides can be seen. {@code dx}/{@code dz} = camera minus the sign's hitbox center, {@code yRotDegrees} =
     * {@code SignBlock#getYRotationDegrees}: the front faces yaw {@code yRot}, i.e. along (-sin, cos) like Vanilla's
     * {@code getSlotPlayerIsFacing}.
     */
    public static SignSides signSides(double dx, double dz, float yRotDegrees) {
        double r = Math.toRadians(yRotDegrees);
        double along = dx * -Math.sin(r) + dz * Math.cos(r);
        if (along > SIGN_MARGIN) return SignSides.FRONT;
        if (along < -SIGN_MARGIN) return SignSides.BACK;
        return SignSides.BOTH;
    }

    /** {@code d*} = camera minus the frame position, {@code step*} = the frame's facing (away from its wall). */
    public static boolean behindFrame(double dx, double dy, double dz, int stepX, int stepY, int stepZ) {
        return dx * stepX + dy * stepY + dz * stepZ < -FRAME_MARGIN;
    }
}
