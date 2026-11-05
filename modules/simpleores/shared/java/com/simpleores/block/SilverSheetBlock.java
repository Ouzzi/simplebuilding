package com.simpleores.block;

import net.minecraft.world.level.block.state.BlockBehaviour;

/** The silver sheet (05.11.): placed like the {@link LeadPlateBlock} on any of the six sides of its cell, but one pixel thin. Pure building block. */
public class SilverSheetBlock extends LeadPlateBlock {
    /** Thickness in pixels. */
    public static final double SHEET_THICKNESS = 1.0;

    public SilverSheetBlock(BlockBehaviour.Properties properties) {
        super(properties, shapes(SHEET_THICKNESS));
    }
}
