package com.simplevisuals.client;

/** Duck interface on {@code ItemFrameRenderState} (TZ22-7): the framed item is drawn tilted, pushed this far off the wall. */
public interface TiltedFrameState {
    boolean simplevisuals$tilted();

    float simplevisuals$push();

    void simplevisuals$setTilt(boolean tilted, float push);
}
