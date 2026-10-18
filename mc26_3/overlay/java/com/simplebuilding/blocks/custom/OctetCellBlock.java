package com.simplebuilding.blocks.custom;

import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 26.3 (split stage 6, docs/ai/PLAN-S6-OCTETS-2026-10-17.md): the octet cell logic lives in Simple Octets
 * ({@link com.simpleoctets.block.OctetCellBlock}, required sub-mod). SimpleBuilding's chess octets
 * ({@link CheckerOctetBlock}) are cells of it, so they mix with the material octets; the shared code reaches the bits,
 * shapes and masks through the inherited statics of this class. Type checks use {@code isCell}: material octets are
 * cells of the module class, not of this one. The 26.2 twin (common/src/mc26_2/java) keeps SimpleBuilding's own code.
 */
public abstract class OctetCellBlock extends com.simpleoctets.block.OctetCellBlock {
    protected OctetCellBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
