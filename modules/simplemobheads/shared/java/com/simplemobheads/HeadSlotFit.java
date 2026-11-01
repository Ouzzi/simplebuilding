package com.simplemobheads;

import org.joml.Matrix4f;

/**
 * IG31-10 / IG31-39: the arithmetic of fitting a head into an inventory slot, free of client classes so a server game test can
 * check it. Slot units: the slot is 1.0 wide with its centre at 0 (the frame the GUI draws an item in, before the item's own
 * GUI transform). From the head's on-screen box (after GUI transform and local transform) a uniform scale makes the larger
 * extent {@value #TARGET} (a Vanilla 8 px head measures 0.71; owner choice IG31C head A) and a shift puts the box's centre on the
 * slot's centre. The matrix has to be multiplied in on the RIGHT of the slot pose (before the item transform): multiplied on the
 * left it scales the slot's position in the GUI atlas too and the head lands in another slot (IG31-39).
 */
public final class HeadSlotFit {
    public static final float TARGET = 0.74F;
    /** Guards only: every head, tadpole to giant, is brought to {@value #TARGET} (IG31-39: "all heads like Vanilla heads"). */
    public static final float MIN_SCALE = 0.05F;
    public static final float MAX_SCALE = 8.0F;

    private HeadSlotFit() {
    }

    /** {scale, shiftX, shiftY} for a head whose screen box (slot units, centre 0) is the given one. */
    public static float[] fit(float minX, float minY, float maxX, float maxY) {
        if (!(minX <= maxX) || !(minY <= maxY)) {
            return new float[] {1.0F, 0.0F, 0.0F};
        }
        float extent = Math.max(maxX - minX, maxY - minY);
        float scale = extent <= 0.0F ? 1.0F : Math.max(MIN_SCALE, Math.min(MAX_SCALE, TARGET / extent));
        return new float[] {scale, -scale * (minX + maxX) / 2.0F, -scale * (minY + maxY) / 2.0F};
    }

    /** The fit as a matrix in slot units: p' = scale * p + shift. Multiply it on the right of the slot pose. */
    public static Matrix4f matrix(float[] fit) {
        return new Matrix4f().translation(fit[1], fit[2], 0.0F).scale(fit[0]);
    }
}
