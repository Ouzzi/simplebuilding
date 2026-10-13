package com.simplebuilding.modules.simpleend.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.modules.simpleend.EndRegistry;
import com.simplebuilding.modules.simpleend.EndWood;
import com.simplebuilding.modules.simpleend.SimpleEnd;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Feature type {@code simpleend:end_flora} for the biomes: {@code kind} = stick_tree (a big tree of End sticks),
 * giant_stick_tree (2x2 trunk), scrub (stick columns), vegetation (End grass, bushes and a few buds) or grass. The End
 * grows no wood trees (owner 2026-10-13: wood only from buds planted outside the End); {@code wood}
 * picks Ashwood or Duskwood. Placed on End stone (or the bud ground tag) at the heightmap position.
 */
public record EndFlora(String kind, String wood) implements Feature {
    public static final MapCodec<EndFlora> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("kind").forGetter(EndFlora::kind),
            Codec.STRING.optionalFieldOf("wood", "ashwood").forGetter(EndFlora::wood)).apply(i, EndFlora::new));

    @Override
    public MapCodec<EndFlora> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!SimpleEnd.on()) return false;
        EndWood w = EndWood.byName(wood);
        BlockState ground = level.getBlockState(origin.below());
        if (!level.isEmptyBlock(origin) || !ground.is(EndRegistry.BUD_MAY_PLACE_ON)) return false;
        return switch (kind) {
            case "stick_tree" -> EndTrees.grow(level, origin, w, EndTrees.Size.BIG, true, random) != null;
            case "giant_stick_tree" -> EndTrees.grow(level, origin, w, EndTrees.Size.HUGE, true, random) != null;
            case "scrub" -> EndTrees.growScrub(level, origin, w, random, false);
            case "vegetation" -> vegetation(level, origin, w, random, true);
            case "grass" -> vegetation(level, origin, w, random, false);
            default -> false;
        };
    }

    /** Tufts of End grass, some bushes and (in the biomes) a few buds around the origin. */
    static boolean vegetation(WorldGenLevel level, BlockPos origin, EndWood w, RandomSource random, boolean buds) {
        boolean any = false;
        for (int i = 0; i < 40; i++) {
            BlockPos at = origin.offset(random.nextInt(9) - 4, random.nextInt(5) - 2, random.nextInt(9) - 4);
            if (!level.isEmptyBlock(at)) continue;
            int roll = random.nextInt(100);
            BlockState plant = buds && random.nextInt(300) == 0 ? w.bud.defaultBlockState()
                    : roll < 10 ? EndRegistry.END_BUSH.defaultBlockState() : EndRegistry.END_GRASS.defaultBlockState();
            if (plant.canSurvive(level, at)) {
                level.setBlock(at, plant, 2);
                any = true;
            }
        }
        return any;
    }
}
