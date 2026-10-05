package com.simplebuilding.mixin;

import com.simplebuilding.fluid.ModFluids;
import com.simplebuilding.fluid.SoulLava;
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
    private void simplebuilding$soulLavaWell(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
            BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos, CallbackInfo ci) {
        if (ModFluids.SOUL_LAVA == null) return;
        BlockPos well = ((StructurePieceAccessor) this).simplebuilding$worldPos(6, 5, 6).immutable();
        if (!chunkBB.isInside(well) || !level.getBlockState(well).is(Blocks.LAVA)) return;
        if (new LegacyRandomSource(level.getSeed() ^ well.asLong() ^ 0x50554C4156414CL).nextFloat() >= SoulLava.fortressChance()) return;
        level.setBlock(well, ModFluids.SOUL_LAVA.defaultFluidState().createLegacyBlock(), Block.UPDATE_CLIENTS);
        level.scheduleTick(well, ModFluids.SOUL_LAVA, 0);
    }
}
