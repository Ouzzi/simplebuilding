package com.simplecrucibles.mixin;

import com.simplecrucibles.fluid.CrucibleFluids;
import com.simplecrucibles.fluid.SoulLava;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.SpringFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Soul lava world generation (owner addition 11, answer 31 A+B): {@link SoulLava#springChance()} of the Nether's lava
 * springs - hidden ({@code spring_closed}, {@code _double}) and open ({@code spring_open}) - become soul lava. Only
 * newly generated chunks; nothing is renewed.
 */
@Mixin(SpringFeature.class)
public abstract class SoulLavaSpringMixin {
    @Inject(method = "place", at = @At("RETURN"))
    private void simplecrucibles$soulLavaSpring(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin,
            CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || CrucibleFluids.SOUL_LAVA == null || level.getLevel().dimension() != Level.NETHER) return;
        var fluid = level.getFluidState(origin);
        if (!fluid.isSource() || !fluid.is(FluidTags.LAVA) || fluid.getType() == CrucibleFluids.SOUL_LAVA) return;
        if (random.nextFloat() >= SoulLava.springChance()) return;
        level.setBlock(origin, CrucibleFluids.SOUL_LAVA.defaultFluidState().createLegacyBlock(), Block.UPDATE_CLIENTS);
        level.scheduleTick(origin, CrucibleFluids.SOUL_LAVA, 0);
    }
}
