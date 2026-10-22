package com.simplebuilding.modules.simpleend.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.modules.simpleend.EndRegistry;
import com.simplebuilding.modules.simpleend.EndWood;
import com.simplebuilding.modules.simpleend.SimpleEnd;
import com.simplebuilding.modules.simpleend.block.EndLeavesBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Feature type {@code simpleend:end_flora} for the biomes: {@code kind} = stick_tree (a big tree of End sticks),
 * giant_stick_tree (2x2 trunk), hedge (fringe leaf wall), scrub (stick columns), vegetation (End grass, bushes and a few buds) or grass. The End
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
            case "hedge" -> hedge(level, origin, w, random);
            case "vegetation" -> vegetation(level, origin, w, random, true);
            case "grass" -> vegetation(level, origin, w, random, false);
            default -> false;
        };
    }

    /**
     * Fringe hedge (N1 A, 2026-10-16): a dense wall of persistent leaves, 4-7 blocks high and 5-10 long, so the rim of the
     * forest blocks the view from outside. Columns follow the ground; only the 3x3 chunks around the origin are touched.
     */
    static boolean hedge(WorldGenLevel level, BlockPos origin, EndWood w, RandomSource random) {
        Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int length = 5 + random.nextInt(6), height = 4 + random.nextInt(4);
        BlockState leaf = w.leaves.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        List<BlockPos> placed = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            BlockPos col = origin.relative(dir, i);
            if (Math.abs(SectionPos.blockToSectionCoord(col.getX()) - SectionPos.blockToSectionCoord(origin.getX())) > 1
                    || Math.abs(SectionPos.blockToSectionCoord(col.getZ()) - SectionPos.blockToSectionCoord(origin.getZ())) > 1) break;
            BlockPos base = new BlockPos(col.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING, col.getX(), col.getZ()), col.getZ());
            if (Math.abs(base.getY() - origin.getY()) > 3 || !level.getBlockState(base.below()).is(EndRegistry.BUD_MAY_PLACE_ON)) continue;
            int h = Math.max(4, Math.min(7, height + random.nextInt(3) - 1));
            for (int y = 0; y < h; y++) {
                BlockPos at = base.above(y);
                BlockState here = level.getBlockState(at);
                if (here.isAir() || here.canBeReplaced() && here.getFluidState().isEmpty()) {
                    level.setBlock(at, leaf, 2);
                    placed.add(at);
                }
            }
        }
        for (BlockPos p : placed) {
            BlockState s = level.getBlockState(p);
            if (s.is(w.leaves)) level.setBlock(p, s.setValue(EndLeavesBlock.DENSITY, EndLeavesBlock.density(level, p)), 2);
        }
        return !placed.isEmpty();
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
