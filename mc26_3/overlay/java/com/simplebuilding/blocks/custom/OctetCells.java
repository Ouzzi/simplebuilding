package com.simplebuilding.blocks.custom;

/**
 * 26.3 (split stage 6): reading and writing octet cells (parts, mixed cell, placing, taking out) is Simple Octets' API
 * ({@link com.simpleoctets.block.OctetCells}); the shared code calls it through the inherited statics of this class.
 * The 26.2 twin (common/src/mc26_2/java) keeps SimpleBuilding's own code without the mixed cell.
 */
public final class OctetCells extends com.simpleoctets.block.OctetCells {
    private OctetCells() {
    }
}
