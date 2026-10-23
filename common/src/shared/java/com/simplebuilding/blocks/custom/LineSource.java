package com.simplebuilding.blocks.custom;

/**
 * A signal source that belongs to one dust line (B23-M1): the Astral/Nihil/Bridge Redstone Block and the Astral/Nihil
 * Redstone Torch. Dust connects to it and reads it only when the lines link ({@link SignalDustBlock.Line#links}); every
 * device takes its signal.
 */
public interface LineSource {
    SignalDustBlock.Line line();
}
