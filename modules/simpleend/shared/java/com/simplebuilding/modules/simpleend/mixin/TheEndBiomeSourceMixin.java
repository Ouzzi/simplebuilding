package com.simplebuilding.modules.simpleend.mixin;

import com.simplebuilding.modules.simpleend.SimpleEnd;
import com.simplebuilding.modules.simpleend.SimpleEndConfig;
import com.simplebuilding.modules.simpleend.world.EndBiomes;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
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
 * Ash Valley and Dawnlands (core) with their fringes replace large patches of the outer highlands/midlands/barrens
 * (see {@link EndBiomes}). All four biomes are added to the possible biomes so their features are sorted and placed.
 */
@Mixin(TheEndBiomeSource.class)
public abstract class TheEndBiomeSourceMixin {
    @Shadow @Final private Holder<Biome> highlands;
    @Shadow @Final private Holder<Biome> midlands;
    @Shadow @Final private Holder<Biome> barrens;
    @Unique private Holder<Biome> simpleend$ash;
    @Unique private Holder<Biome> simpleend$dawn;
    @Unique private Holder<Biome> simpleend$ashFringe;
    @Unique private Holder<Biome> simpleend$dawnFringe;

    /**
     * The holders always come from the running server's biome registry (set when it starts, cleared when it stops):
     * a source built on the create-world screen sees a temporary registry whose holders have no network id on the
     * server (chunk packets would carry biome id -1).
     */
    @Unique private Object simpleend$from;

    @Unique
    private void simpleend$resolve() {
        net.minecraft.core.Registry<Biome> biomes = EndBiomes.serverBiomes;
        if (biomes == null || biomes == simpleend$from) return;
        simpleend$from = biomes;
        simpleend$ash = biomes.get(EndBiomes.ASH_VALLEY).orElse(null);
        simpleend$dawn = biomes.get(EndBiomes.DAWNLANDS).orElse(null);
        simpleend$ashFringe = biomes.get(EndBiomes.ASH_VALLEY_FRINGE).orElse(null);
        simpleend$dawnFringe = biomes.get(EndBiomes.DAWNLANDS_FRINGE).orElse(null);
    }

    @Inject(method = "collectPossibleBiomes", at = @At("RETURN"), cancellable = true)
    private void simpleend$possible(CallbackInfoReturnable<Stream<Holder<Biome>>> cir) {
        simpleend$resolve();
        if (simpleend$ash == null && simpleend$dawn == null) return;
        cir.setReturnValue(Stream.concat(cir.getReturnValue(), Stream.of(simpleend$ash, simpleend$dawn, simpleend$ashFringe, simpleend$dawnFringe).filter(Objects::nonNull)));
    }

    /**
     * The patches wrap the End's biome resolver instead of hooking {@code getNoiseBiome}: on Fabric, Fabric API's own
     * hook on that method re-picks every End biome after other mods and drops a replaced one. Wrapping the resolver
     * runs after all of it, on every loader alike.
     */
    @Inject(method = "createResolver", at = @At("RETURN"), cancellable = true)
    private void simpleend$wrap(Climate.Sampler sampler, CallbackInfoReturnable<BiomeResolver> cir) {
        BiomeResolver inner = cir.getReturnValue();
        cir.setReturnValue((x, y, z) -> simpleend$patch(inner.getNoiseBiome(x, y, z), x, z, sampler));
    }

    @Unique
    private Holder<Biome> simpleend$patch(Holder<Biome> result, int quartX, int quartZ, Climate.Sampler sampler) {
        // by key as well as identity: other hooks may hand back holders of another registry instance
        if (result != highlands && result != midlands && result != barrens && !result.is(Biomes.END_HIGHLANDS)
                && !result.is(Biomes.END_MIDLANDS) && !result.is(Biomes.END_BARRENS)) return result;
        simpleend$resolve();
        if (simpleend$ash == null || simpleend$dawn == null || simpleend$ashFringe == null || simpleend$dawnFringe == null
                || !SimpleEndConfig.biomes || !SimpleEnd.on()) return result;
        double chunkX = (QuartPos.toBlock(quartX) + 2) / 16.0;
        double chunkZ = (QuartPos.toBlock(quartZ) + 2) / 16.0;
        return switch (EndBiomes.pick(chunkX, chunkZ, (x, z) -> sampler.erosion().sampleValue(x, 0, z), SimpleEndConfig.biomeChance)) {
            case EndBiomes.ASH -> simpleend$ash;
            case EndBiomes.DAWN -> simpleend$dawn;
            case EndBiomes.ASH_FRINGE -> simpleend$ashFringe;
            case EndBiomes.DAWN_FRINGE -> simpleend$dawnFringe;
            default -> result;
        };
    }
}
