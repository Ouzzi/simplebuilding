package com.simplebuilding.modules.simpleend.mixin;

import com.simplebuilding.modules.simpleend.SimpleEnd;
import com.simplebuilding.modules.simpleend.SimpleEndConfig;
import com.simplebuilding.modules.simpleend.world.EndBiomes;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ash Valley and Dawnlands replace round patches of the outer highlands/midlands (see {@link EndBiomes}). Both biomes
 * are added to the possible biomes so their features are sorted and placed.
 */
@Mixin(TheEndBiomeSource.class)
public abstract class TheEndBiomeSourceMixin {
    @Shadow @Final private Holder<Biome> highlands;
    @Shadow @Final private Holder<Biome> midlands;
    @Unique private Holder<Biome> simpleend$ash;
    @Unique private Holder<Biome> simpleend$dawn;

    /**
     * The holders always come from the running server's biome registry (set when it starts, cleared when it stops):
     * a source built on the create-world screen sees a temporary registry whose holders have no network id on the
     * server (chunk packets would carry biome id -1).
     */
    @Unique
    private void simpleend$resolve() {
        if (simpleend$ash != null && simpleend$dawn != null) return;
        net.minecraft.core.Registry<Biome> biomes = EndBiomes.serverBiomes;
        if (biomes == null) return;
        simpleend$ash = biomes.get(EndBiomes.ASH_VALLEY).orElse(null);
        simpleend$dawn = biomes.get(EndBiomes.DAWNLANDS).orElse(null);
    }

    @Inject(method = "collectPossibleBiomes", at = @At("RETURN"), cancellable = true)
    private void simpleend$possible(CallbackInfoReturnable<Stream<Holder<Biome>>> cir) {
        simpleend$resolve();
        if (simpleend$ash == null && simpleend$dawn == null) return;
        cir.setReturnValue(Stream.concat(cir.getReturnValue(), Stream.of(simpleend$ash, simpleend$dawn).filter(Objects::nonNull)));
    }

    @Inject(method = "getNoiseBiome", at = @At("RETURN"), cancellable = true)
    private void simpleend$patches(int quartX, int quartY, int quartZ, Climate.Sampler sampler, CallbackInfoReturnable<Holder<Biome>> cir) {
        Holder<Biome> result = cir.getReturnValue();
        if (result != highlands && result != midlands) return;
        simpleend$resolve();
        if (simpleend$ash == null || simpleend$dawn == null || !SimpleEndConfig.biomes || !SimpleEnd.on()) return;
        int chunkX = SectionPos.blockToSectionCoord(QuartPos.toBlock(quartX));
        int chunkZ = SectionPos.blockToSectionCoord(QuartPos.toBlock(quartZ));
        int pick = EndBiomes.pick(chunkX, chunkZ, (x, z) -> sampler.erosion().sampleValue(x, 0, z), SimpleEndConfig.biomeChance);
        if (pick == EndBiomes.ASH) cir.setReturnValue(simpleend$ash);
        else if (pick == EndBiomes.DAWN) cir.setReturnValue(simpleend$dawn);
    }
}
