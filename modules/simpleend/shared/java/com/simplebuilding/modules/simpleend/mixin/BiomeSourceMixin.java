package com.simplebuilding.modules.simpleend.mixin;

import com.simplebuilding.modules.simpleend.world.EndBiomes;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The End biome source of a new world is built (and its possible biomes memoised) on the create-world screen, before
 * the server and its biome registry exist. Ash Valley and Dawnlands (cores and fringes) are added here once the running server's registry
 * is known, so /locate, biome search and the feature order see them.
 */
@Mixin(BiomeSource.class)
public abstract class BiomeSourceMixin {
    @Unique private Set<Holder<Biome>> simpleend$possible;
    @Unique private Object simpleend$from;

    @Inject(method = "possibleBiomes", at = @At("RETURN"), cancellable = true)
    private void simpleend$endBiomes(CallbackInfoReturnable<Set<Holder<Biome>>> cir) {
        if (!((Object) this instanceof TheEndBiomeSource)) return;
        Registry<Biome> biomes = EndBiomes.serverBiomes;
        if (biomes == null) return;
        if (simpleend$from != biomes) {
            Set<Holder<Biome>> all = new LinkedHashSet<>(cir.getReturnValue());
            biomes.get(EndBiomes.ASH_VALLEY).ifPresent(all::add);
            biomes.get(EndBiomes.DAWNLANDS).ifPresent(all::add);
            biomes.get(EndBiomes.ASH_VALLEY_FRINGE).ifPresent(all::add);
            biomes.get(EndBiomes.DAWNLANDS_FRINGE).ifPresent(all::add);
            simpleend$possible = Set.copyOf(all);
            simpleend$from = biomes;
        }
        cir.setReturnValue(simpleend$possible);
    }
}
