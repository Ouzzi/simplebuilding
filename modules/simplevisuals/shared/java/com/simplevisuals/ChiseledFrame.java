package com.simplevisuals;

/** Duck interface on {@code ItemFrame} (W23-5, mixin ItemFrameChiselMixin): the chisel step, a synced entity datum. */
public interface ChiseledFrame {
    /** 0 = flat on the back plate ... {@link FrameTilt#STEPS} = 90 degrees. */
    int simplevisuals$tiltStep();

    void simplevisuals$setTiltStep(int step);

    /** Vanilla's {@code Fixed} flag: a locked frame does not change. */
    boolean simplevisuals$fixed();
}
