package com.simplebuilding.modules.simplelayers.backrooms.stack;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/**
 * Biome source {@code simplelayers:band_stack} (BACK22 core §2): one biome per level by height ({@code bands} in stack order), so
 * fog, light, music, ambience and mob spawns follow the level through Vanilla's environment attributes - no own render code.
 * Bedrock between levels takes the level above. Level edges lie on section edges, so every section has one biome.
 */
public class BandBiomeSource extends BiomeSource implements BiomeResolver {
    public static final MapCodec<BandBiomeSource> CODEC = Biome.CODEC.listOf().fieldOf("bands").xmap(BandBiomeSource::new, s -> s.bands);
    private final List<Holder<Biome>> bands;

    public BandBiomeSource(List<Holder<Biome>> bands) {
        this.bands = List.copyOf(bands);
    }

    @Override
    protected MapCodec<BandBiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return bands.stream();
    }

    @Override
    public BiomeResolver createResolver(Climate.Sampler sampler) {
        return this;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ) {
        return biome(quartY * 4);
    }

    public Holder<Biome> biome(int y) {
        int i = BandStack.nearestBand(y) - 1;
        return bands.get(Math.max(0, Math.min(bands.size() - 1, i)));
    }
}
