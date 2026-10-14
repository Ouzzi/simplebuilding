package com.simplebuilding.util;

/**
 * Duck interface on the Vanilla {@code SculkShriekerBlockEntity} (see {@code SculkShriekerSilenceMixin}): a Sculk
 * Shrieker silenced with a Silent Dandelion keeps its flag in the block entity data, so it holds for every player
 * and survives saving. Silent means no shriek and no warden-reply sounds; warning levels, Darkness and the Warden
 * summon are untouched.
 */
public interface ShriekerSilence {
    boolean simplebuilding$isSilenced();

    void simplebuilding$setSilenced(boolean silenced);
}
