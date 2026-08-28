package com.simplebuilding.util;

import java.util.UUID;

public interface LockedFrameExtensions {
    boolean simplebuilding$isLocked();
    void simplebuilding$setLocked(boolean locked);

    UUID simplebuilding$getBrushingPlayer();
    void simplebuilding$setBrushingPlayer(UUID uuid);

    int simplebuilding$getBrushingTicks();
    void simplebuilding$incrementBrushingTicks();
    void simplebuilding$resetBrushing();
}