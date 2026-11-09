package com.simpleblocks.nature;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Treppe, Stufe und Mauer aus normalem Eis (PAL14, Besitzer 09.11.): sie schmelzen wie Vanillas Eis (Blocklicht ueber
 * 11 minus Lichtdaempfung -> Wasser, wo Wasser verdunstet: weg). Pack- und Blaueis-Formen schmelzen nicht (wie ihre
 * Vanilla-Grundbloecke) und nutzen die normalen Klassen. Anders als ein Eisblock hinterlaesst eine abgebaute Form kein
 * Wasser (Teilblock). Rutschigkeit, Klang und Haerte kommen aus den kopierten Eis-Eigenschaften.
 */
public final class IceShapes {
    private IceShapes() {
    }

    /** Vanillas Lichtregel aus {@link IceBlock}: zu hell -> schmelzen. */
    static void meltIfLit(BlockState state, ServerLevel level, BlockPos pos) {
        if (level.getBrightness(LightLayer.BLOCK, pos) > 11 - state.getLightDampening()) {
            melt(level, pos);
        }
    }

    /** Wie {@code IceBlock.melt}: Wasser, oder nichts, wo Wasser verdunstet (Nether). */
    public static void melt(ServerLevel level, BlockPos pos) {
        if (level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, pos)) {
            level.removeBlock(pos, false);
        } else {
            level.setBlockAndUpdate(pos, IceBlock.meltsInto());
            level.neighborChanged(pos, IceBlock.meltsInto().getBlock(), null);
        }
    }

    public static class Stairs extends StairBlock {
        public Stairs(BlockState base, Properties properties) {
            super(base, properties.randomTicks());
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return true;
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            meltIfLit(state, level, pos);
        }
    }

    public static class Slab extends SlabBlock {
        public Slab(Properties properties) {
            super(properties.randomTicks());
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return true;
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            meltIfLit(state, level, pos);
        }
    }

    public static class Wall extends WallBlock {
        public Wall(Properties properties) {
            super(properties.forceSolidOn().randomTicks());
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return true;
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            meltIfLit(state, level, pos);
        }
    }
}
