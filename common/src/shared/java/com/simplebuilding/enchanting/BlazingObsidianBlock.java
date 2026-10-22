package com.simplebuilding.enchanting;

import com.simplebuilding.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Blazing Obsidian (owner 2026-10-09, queue N27): the glowing sibling of crying obsidian, eight blaze powder around one
 * crying obsidian. As a 5x5 floor under the Astral Enchanting Table it unlocks tiers 40 and 50 ({@link AstralEnchanting}).
 * Rework LOHEN17 (owner 2026-10-21, docs/ai/KONZEPT-BLAZING-OBSIDIAN-2026-10-17.md V1-V3):
 * <ul>
 *   <li>V1: sparks rise from every free face, now and then it crackles like fire, and under an Astral table (any block of
 *       the 5x5 below it, not only a complete floor) sparks fly to the book;</li>
 *   <li>V2: fire on top burns forever (block tag {@code minecraft:infiniburn_overworld}, datagen);</li>
 *   <li>V3: high crucible heat (Simple Crucibles' heat source, {@code CrucibleCompat}, config {@code blazingHeat}).</li>
 * </ul>
 */
public class BlazingObsidianBlock extends Block {
    public BlazingObsidianBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction face = Direction.values()[random.nextInt(Direction.values().length)];
        if (free(level, pos, face)) {
            double x = pos.getX() + 0.5 + (face.getStepX() == 0 ? random.nextDouble() - 0.5 : face.getStepX() * 0.52);
            double y = pos.getY() + 0.5 + (face.getStepY() == 0 ? random.nextDouble() - 0.5 : face.getStepY() * 0.52);
            double z = pos.getZ() + 0.5 + (face.getStepZ() == 0 ? random.nextDouble() - 0.5 : face.getStepZ() * 0.52);
            if (face == Direction.UP && random.nextInt(10) == 0) {
                level.addParticle(ParticleTypes.LAVA, x, y, z, 0.0, 0.0, 0.0);
            } else {
                level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, face.getStepX() * 0.01, 0.015, face.getStepZ() * 0.01);
            }
        }
        if (random.nextInt(40) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS,
                    0.3F + random.nextFloat() * 0.3F, random.nextFloat() * 0.7F + 0.3F, false);
        }
        if (random.nextInt(4) == 0 && free(level, pos, Direction.UP)) sparkToBook(level, pos, random);
    }

    /** One spark from the top of this block towards the book of an Astral table standing on the 5x5 around it. */
    private static void sparkToBook(Level level, BlockPos pos, RandomSource random) {
        if (ModBlocks.ASTRAL_ENCHANTING_TABLE == null) return;
        int r = AstralEnchanting.FLOOR_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx == 0 && dz == 0) continue;   // the block right under the table has no free top
                BlockPos table = pos.offset(dx, 1, dz);
                if (!level.getBlockState(table).is(ModBlocks.ASTRAL_ENCHANTING_TABLE)) continue;
                double x = pos.getX() + 0.2 + random.nextDouble() * 0.6;
                double y = pos.getY() + 1.05;
                double z = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
                // the flame drifts with friction 0.96 for 12..44 ticks: about 15 times its start speed in total
                level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, (table.getX() + 0.5 - x) / 15.0,
                        (table.getY() + 1.0 - y) / 15.0, (table.getZ() + 0.5 - z) / 15.0);
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
