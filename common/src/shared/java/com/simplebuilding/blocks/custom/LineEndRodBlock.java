package com.simplebuilding.blocks.custom;

import net.minecraft.world.level.block.EndRodBlock;

/**
 * Astralit and Nihilit End Rod (W24-4, owner 2026-10-23): Vanilla's End rod - light 14, the spark at its tip, placed
 * facing away from the clicked face (and turned against an End rod it is put on) - in the colours of its redstone line
 * (texture only, like the line torches). The properties are copied from {@code Blocks.END_ROD}.
 */
public class LineEndRodBlock extends EndRodBlock {
    private final SignalDustBlock.Line line;

    public LineEndRodBlock(SignalDustBlock.Line line, Properties properties) {
        super(properties);
        this.line = line;
    }

    public SignalDustBlock.Line line() {
        return line;
    }
}
