package com.simplebuilding.mixin;

import org.spongepowered.asm.mixin.Mixin;

/** 26.2 twin: unused (soul lava world generation is 26.3 only). */
@Mixin(net.minecraft.world.level.levelgen.structure.StructurePiece.class)
public interface StructurePieceAccessor {}
