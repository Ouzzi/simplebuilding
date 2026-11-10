package com.simpleores.block;

import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The silver sheet (05.11.): placed like the {@link LeadPlateBlock} on any of the six sides of its cell, but one pixel thin. Pure building block.
 * N25: stacks like the plate, one pixel per layer (8 layers = half a block; 3 ingots give 12 sheets or 6 lead plates - the same metal per pixel).
 */
public class SilverSheetBlock extends LeadPlateBlock {
    /** Thickness of one layer in pixels. */
    public static final double SHEET_THICKNESS = 1.0;

    public SilverSheetBlock(BlockBehaviour.Properties properties) {
        super(properties, SHEET_THICKNESS);
    }
}
