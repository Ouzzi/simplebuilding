package com.simplevisuals.client;

import net.minecraft.client.renderer.block.BlockModelRenderState;

/** Duck interface on {@code ItemFrameRenderState} (FR23): the framed item is drawn as a flat block model seen from above. */
public interface TiltedFrameState {
    boolean simplevisuals$tilted();

    /** True for a pane: its model already faces the viewer, a flat block is first turned top-forward. */
    boolean simplevisuals$pane();

    BlockModelRenderState simplevisuals$model();

    void simplevisuals$setTilt(boolean tilted, boolean pane);
}
