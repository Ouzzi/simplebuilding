package com.simpleores;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

/**
 * N4 (b-ores13, owner 09.11.): a closed mini geode, a third of the amethyst geode. A 5 x 5 x 5 ball: a core of 19 cells
 * (r&sup2; &le; 2) holds {@code ores} (1 to 8) blocks of {@code ore}, the rest of the core is {@code lining}; the {@code shell}
 * (2 &lt; r&sup2; &le; 6) closes it on every side. It is placed only when every cell up to r&sup2; &le; 11 (the shell and the
 * layer around it) passes {@code host}: no air, water, lava, cave or foreign ore touches it, so it is always fully enclosed and
 * never at the surface. Overworld: geode crust, calcite, zinc ore in base stone; End: End stone, geode crust, End zinc ore in End stone.
 */
public record MiniGeodeFeature(BlockState shell, BlockState lining, BlockState ore, RuleTest host, IntProvider ores) implements Feature {
    public static final MapCodec<MiniGeodeFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockState.CODEC.fieldOf("shell").forGetter(MiniGeodeFeature::shell),
            BlockState.CODEC.fieldOf("lining").forGetter(MiniGeodeFeature::lining),
            BlockState.CODEC.fieldOf("ore").forGetter(MiniGeodeFeature::ore),
            RuleTest.CODEC.fieldOf("host").forGetter(MiniGeodeFeature::host),
            net.minecraft.util.valueproviders.IntProviders.codec(1, 19).fieldOf("ores").forGetter(MiniGeodeFeature::ores)).apply(i, MiniGeodeFeature::new));
    /** Squared radii of the layers and of the host check around them. */
    public static final int CORE = 2, SHELL = 6, CHECK = 11;
    /** Most ores one geode holds (owner: 1 to 8). */
    public static final int MAX_ORES = 8;

    /** Offsets with dx&sup2; + dy&sup2; + dz&sup2; &le; r2, in a fixed order. */
    public static List<BlockPos> ball(int r2) {
        List<BlockPos> out = new ArrayList<>();
        int r = (int) Math.floor(Math.sqrt(r2));
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + y * y + z * z <= r2) out.add(new BlockPos(x, y, z));
                }
            }
        }
        return out;
    }

    /** Is every cell of the check ball host rock (the rule alone, tested without a world)? */
    public boolean enclosed(Function<BlockPos, BlockState> getter, BlockPos origin, RandomSource random) {
        for (BlockPos o : ball(CHECK)) {
            BlockPos p = origin.offset(o);
            if (!host.test(getter.apply(p), p, random)) return false;
        }
        return true;
    }

    /** Number of ores of one geode: the provider's roll, kept within 1 to 8. */
    public int oreCount(RandomSource random) {
        return Math.max(1, Math.min(MAX_ORES, ores.sample(random)));
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        if (!enclosed(level::getBlockState, origin, random)) return false;
        List<BlockPos> core = ball(CORE);
        int count = oreCount(random);
        // Fisher-Yates over the core: the first `count` cells take the ore.
        for (int i = core.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            BlockPos t = core.get(i);
            core.set(i, core.get(j));
            core.set(j, t);
        }
        java.util.Set<BlockPos> oreCells = new java.util.HashSet<>(core.subList(0, count));
        for (BlockPos o : ball(SHELL)) {
            int d = o.getX() * o.getX() + o.getY() * o.getY() + o.getZ() * o.getZ();
            BlockState state = d > CORE ? shell : oreCells.contains(o) ? ore : lining;
            level.setBlock(origin.offset(o), state, 2);
        }
        return true;
    }

    @Override
    public MapCodec<MiniGeodeFeature> codec() {
        return CODEC;
    }

    /** Loader hook: the feature type must exist before the data packs load (Fabric: mod initializer). */
    public static void register() {
        Registry.register(BuiltInRegistries.FEATURE_TYPE, SimpleOres.id("mini_geode"), CODEC);
    }
}
