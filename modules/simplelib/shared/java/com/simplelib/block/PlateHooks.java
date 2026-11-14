package com.simplelib.block;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/**
 * N48 (b-circ46): switches of the material pressure plates that a partner mod may set; SimpleLib names none of them. Simple Pads
 * sets both so its config ({@code pads.enableFilterPlates}, the stranger break time of the plates) keeps ruling the Netherite and
 * Enderite plates; without such a mod the defaults apply (filter plates on, stranger needs 10 s).
 */
public final class PlateHooks {
    /** Whether the Netherite and Enderite plates (the barrel filter) work at all. */
    public static volatile BooleanSupplier filterPlatesEnabled = () -> true;
    /** Break progress per tick of a stranger on an owned plate (default 1/200 = 10 s). */
    public static volatile DoubleSupplier strangerPlateProgress = () -> 1.0 / 200.0;
    /** Break progress per tick of the owner on an owned plate (1/30 = 1.5 s). */
    public static final float OWNER_PLATE = 1.0f / 30.0f;

    private PlateHooks() {}
}
