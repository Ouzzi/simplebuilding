package com.simplebuilding.gametest;

import com.simplebuilding.util.ModWorldGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.DiskFeature;

/**
 * MC 26.3 side of the cracked ice field check (N53; the 26.2 twin has no field). Runs the registered disc of
 * {@code simplebuilding:cracked_ice_field} with a radius of 2 instead of 5..8 (the 8x8 test room), everything else as
 * registered: on a snow plain the surface becomes cracked ice (stage 1) over a pit of cave air four blocks deep, snow
 * layers go, sea ice over water cracks while the water stays, a column covered by other ground stays untouched, and
 * nothing outside the disc changes.
 */
final class CrackedIceFieldChecks {

    private CrackedIceFieldChecks() {
    }

    static void placeSmallField(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var feature = level.registryAccess().lookupOrThrow(Registries.FEATURE).getValue(ModWorldGen.CRACKED_ICE_FIELD_KEY);
        helper.assertTrue(feature instanceof DiskFeature, "cracked_ice_field is a disk feature, is " + feature);
        DiskFeature disc = (DiskFeature) feature;
        helper.assertValueEqual(disc.halfHeight(), ModWorldGen.CRACKED_ICE_FIELD_DEPTH, "pit depth (half height)");
        // Snow plain: dirt y1..4, snow block y5, snow layer y6; the centre column is (4, *, 4).
        for (int x = 1; x <= 7; x++) {
            for (int z = 1; z <= 7; z++) {
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.DIRT);
                }
                helper.setBlock(new BlockPos(x, 5, z), Blocks.SNOW_BLOCK);
                helper.setBlock(new BlockPos(x, 6, z), Blocks.AIR);
            }
        }
        helper.setBlock(new BlockPos(4, 6, 4), Blocks.SNOW);
        BlockPos sea = new BlockPos(2, 0, 4);          // ice over water at the west edge
        helper.setBlock(sea.atY(5), Blocks.ICE);
        helper.setBlock(sea.atY(4), Blocks.WATER);
        BlockPos covered = new BlockPos(4, 0, 6);      // ground higher than the disc: stone on the snow
        helper.setBlock(covered.atY(6), Blocks.STONE);
        BlockPos origin = helper.absolutePos(new BlockPos(4, 5, 4));
        boolean placed = new DiskFeature(disc.stateProvider(), disc.target(), ConstantInt.of(2), disc.halfHeight())
                .place(level, level.getChunkSource().getGenerator(), RandomSource.create(53), origin);
        helper.assertTrue(placed, "the disc placed nothing");
        var top = helper.getBlockState(new BlockPos(4, 5, 4));
        helper.assertTrue(top.is(crackedIce()) && top.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_3) == 1, "surface should be cracked ice stage 1, is " + top);
        helper.assertTrue(helper.getBlockState(new BlockPos(4, 6, 4)).isAir(), "snow layer should be gone");
        for (int y = 1; y <= 4; y++) {
            expect(helper, new BlockPos(4, y, 4), Blocks.CAVE_AIR, "pit under the field");
        }
        expect(helper, new BlockPos(7, 1, 4), Blocks.DIRT, "ground outside the disc");
        expect(helper, sea.atY(5), crackedIce(), "sea ice");
        expect(helper, sea.atY(4), Blocks.WATER, "water under the sea ice");
        expect(helper, sea.atY(3), Blocks.DIRT, "ground under the water");
        expect(helper, covered.atY(5), Blocks.SNOW_BLOCK, "covered snow");
        expect(helper, covered.atY(4), Blocks.DIRT, "covered ground");
        expect(helper, new BlockPos(4, 5, 7), Blocks.SNOW_BLOCK, "snow outside the disc");
    }

    /** Simple Blocks' cracked ice (b-blocksmove), by id. */
    private static Block crackedIce() {
        return com.simplebuilding.util.BlockRefs.block(com.simplebuilding.util.BlockRefs.CRACKED_ICE);
    }

    private static void expect(GameTestHelper helper, BlockPos pos, Block block, String what) {
        helper.assertTrue(helper.getBlockState(pos).is(block), what + " at " + pos + " should be " + block + ", is " + helper.getBlockState(pos));
    }
}
