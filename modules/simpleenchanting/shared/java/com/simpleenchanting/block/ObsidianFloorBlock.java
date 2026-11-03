package com.simpleenchanting.block;

import com.simpleenchanting.EnchantingRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * This module's own Blazing and Nihil Obsidian, registered only without SimpleBuilding (which owns both blocks, ENCH01): the floor
 * of the enchanters. Blazing: small flames and now and then a fire crackle; Nihil (navy/void): reverse-portal motes. Both send a
 * spark from their top to the book of an enchanter standing on the 5x5 around them.
 */
public class ObsidianFloorBlock extends Block {
    private final boolean nihil;

    public ObsidianFloorBlock(boolean nihil, Properties properties) {
        super(properties);
        this.nihil = nihil;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        SimpleParticleType mote = this.nihil ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.SMALL_FLAME;
        Direction face = Direction.values()[random.nextInt(Direction.values().length)];
        if (free(level, pos, face)) {
            double x = pos.getX() + 0.5 + (face.getStepX() == 0 ? random.nextDouble() - 0.5 : face.getStepX() * 0.52);
            double y = pos.getY() + 0.5 + (face.getStepY() == 0 ? random.nextDouble() - 0.5 : face.getStepY() * 0.52);
            double z = pos.getZ() + 0.5 + (face.getStepZ() == 0 ? random.nextDouble() - 0.5 : face.getStepZ() * 0.52);
            level.addParticle(mote, x, y, z, face.getStepX() * 0.01, 0.015, face.getStepZ() * 0.01);
        }
        if (!this.nihil && random.nextInt(40) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS,
                    0.3F + random.nextFloat() * 0.3F, random.nextFloat() * 0.7F + 0.3F, false);
        }
        if (random.nextInt(4) == 0 && free(level, pos, Direction.UP)) sparkToBook(level, pos, random, mote);
    }

    /** One spark from the top of this block towards the book of an enchanter standing on the 5x5 around it. */
    private static void sparkToBook(Level level, BlockPos pos, RandomSource random, SimpleParticleType mote) {
        int r = EnchantingRules.FLOOR_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx == 0 && dz == 0) continue;   // the block right under the table has no free top
                BlockPos table = pos.offset(dx, 1, dz);
                if (!(level.getBlockState(table).getBlock() instanceof EnchanterBlock)) continue;
                double x = pos.getX() + 0.2 + random.nextDouble() * 0.6;
                double y = pos.getY() + 1.05;
                double z = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
                level.addParticle(mote, x, y, z, (table.getX() + 0.5 - x) / 15.0, (table.getY() + 1.0 - y) / 15.0, (table.getZ() + 0.5 - z) / 15.0);
                return;
            }
        }
    }

    private static boolean free(Level level, BlockPos pos, Direction face) {
        BlockPos side = pos.relative(face);
        BlockState next = level.getBlockState(side);
        return !(next.canOcclude() && next.isFaceSturdy(level, side, face.getOpposite()));
    }
}
