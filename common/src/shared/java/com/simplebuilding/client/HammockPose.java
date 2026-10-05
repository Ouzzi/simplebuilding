package com.simplebuilding.client;

/**
 * Extra lying pose for a player in a hammock (client render state, docs/ai/PLAN-HAENGEMATTE-2026-10-02.md v2): vanilla
 * lays sleepers along one of four directions on the centre of their bed block; a slanted or off-centre hammock needs a
 * turn and a shift on top. Filled by {@code HammockLivingRendererMixin}, stored by {@code HammockRenderStateMixin}.
 */
public interface HammockPose {
    /** Extra turn about the vertical axis in degrees (from the bed direction onto the line, e.g. -45 at 45 degrees), 0 for none. */
    float simplebuilding$hammockYaw();

    /** World-space shift of the lying body (blocks), applied before the sleeping rotation. */
    double simplebuilding$hammockShiftX();

    double simplebuilding$hammockShiftZ();

    void simplebuilding$setHammockPose(float yaw, double shiftX, double shiftZ);
}
