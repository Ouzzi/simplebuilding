package com.simplebuilding.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Piece-local to world coordinates (for the soul lava fortress well). */
@Mixin(StructurePiece.class)
public interface StructurePieceAccessor {
    @Invoker("getWorldPos")
    BlockPos.MutableBlockPos simplebuilding$worldPos(int x, int y, int z);
}
