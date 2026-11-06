package com.simpleores;

import com.mojang.serialization.MapCodec;
import com.simpleores.config.OresConfig;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.BlockReplacement;
import net.minecraft.world.level.levelgen.feature.OreFeature;

/**
 * A Nether gold vein that may hold ONE Nether Rose Gold Ore (06.11., owner wish): the vein is the Vanilla ore vein with the targets
 * {@code [rose gold ore, Nether gold ore]} on the same rule. Each block the vein would turn into gold ore first rolls the chance
 * ({@link OresConfig#roseGoldOreChance} percent) for the rose gold ore; that is only allowed below the lava ocean ({@code y < 31}) and
 * only while this vein has none yet, so a vein never holds more than one. The state lives per thread and per {@link #place} call.
 */
public class RoseGoldVeinFeature extends OreFeature {
    public static final MapCodec<RoseGoldVeinFeature> CODEC = makeCodec(RoseGoldVeinFeature::new);
    /** First Y that is NOT hot enough: the lava ocean of the Nether reaches up to Y 31. */
    public static final int LAVA_LEVEL = 31;
    private static final ThreadLocal<boolean[]> PLACED = ThreadLocal.withInitial(() -> new boolean[1]);

    public RoseGoldVeinFeature(List<BlockReplacement> targetStates, int size, float discardChanceOnAirExposure) {
        super(targetStates, size, discardChanceOnAirExposure);
    }

    /** The rule alone (tested without a world): may the next candidate block of a vein become rose gold ore? */
    public static boolean mayBeRoseGold(boolean veinHasOne, int y, double chancePercent, RandomSource random) {
        return !veinHasOne && y < LAVA_LEVEL && chancePercent > 0.0 && random.nextDouble() * 100.0 < chancePercent;
    }

    /** Starts a new vein without placing one (tests drive {@link #canPlaceOre} directly; {@link #place} does the same). */
    public static void beginVein() {
        PLACED.get()[0] = false;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        boolean[] placed = PLACED.get();
        placed[0] = false;
        try {
            return super.place(level, chunkGenerator, random, origin);
        } finally {
            placed[0] = false;
        }
    }

    @Override
    public boolean canPlaceOre(BlockState state, Function<BlockPos, BlockState> blockGetter, RandomSource random, BlockReplacement targetState, BlockPos.MutableBlockPos orePos) {
        if (!targetState.state().is(OresRegistry.NETHER_ROSE_GOLD_ORE)) return super.canPlaceOre(state, blockGetter, random, targetState, orePos);
        boolean[] placed = PLACED.get();
        if (!targetState.target().test(state, orePos, random) || !mayBeRoseGold(placed[0], orePos.getY(), OresConfig.roseGoldOreChance, random)) return false;
        if (!super.canPlaceOre(state, blockGetter, random, targetState, orePos)) return false;
        placed[0] = true;
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public MapCodec<OreFeature> codec() {
        return (MapCodec<OreFeature>) (MapCodec<?>) CODEC;
    }

    /** Loader hook: the feature type must exist before the data packs load (Fabric: mod initializer). */
    public static void register() {
        Registry.register(BuiltInRegistries.FEATURE_TYPE, SimpleOres.id("rose_gold_vein"), CODEC);
    }
}
