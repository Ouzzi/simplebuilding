package com.simplebuilding.modules.simpleinterfaces.client;

import org.jetbrains.annotations.Nullable;

/** Duck interface on {@code AbstractContainerScreen}: lay out the linked panel again; the screen background band it leaves. */
public interface LinkedScreen {
    void qol$layout();

    /** Window rectangle {x0, y0, x1, y1} the screen's own background stays visible in, or null (all of it). */
    int @Nullable [] qol$background();
}
