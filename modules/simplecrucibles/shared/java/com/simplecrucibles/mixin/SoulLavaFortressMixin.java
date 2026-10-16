package com.simplecrucibles.mixin;

import com.simplecrucibles.fluid.CrucibleFluids;
import com.simplecrucibles.fluid.SoulLava;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressPieces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Soul lava in Nether fortresses (owner addition 11): the lava well of a fortress entrance room becomes soul lava
 * with {@link SoulLava#fortressChance()}. The roll depends only on the world seed and the well's position, so every
 * chunk pass of the piece agrees.
 */
@Mixin(NetherFortressPieces.CastleEntrance.class)
public abstract class SoulLavaFortressMixin {
    @Inject(method = "postProcess", at = @At("TAIL"))
    private void simplecrucibles$soulLavaWell(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
            BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos, CallbackInfo ci) {
        if (CrucibleFluids.SOUL_LAVA == null) return;
        BlockPos well = ((StructurePieceAccessor) this).simplecrucibles$worldPos(6, 5, 6).immutable();
        if (!chunkBB.isInside(well) || !level.getBlockState(well).is(Blocks.LAVA)) return;
        if (new LegacyRandomSource(level.getSeed() ^ well.asLong() ^ 0x50554C4156414CL).nextFloat() >= SoulLava.fortressChance()) return;
        level.setBlock(well, CrucibleFluids.SOUL_LAVA.defaultFluidState().createLegacyBlock(), Block.UPDATE_CLIENTS);
        level.scheduleTick(well, CrucibleFluids.SOUL_LAVA, 0);
    }
}
