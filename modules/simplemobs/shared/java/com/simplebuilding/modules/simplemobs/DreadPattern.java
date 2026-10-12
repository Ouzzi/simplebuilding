package com.simplebuilding.modules.simplemobs;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import org.jspecify.annotations.Nullable;

/**
 * The Dread figure (owner 12.10.: summoned like a golem, never spawns naturally), seen from the front - the Iron Golem's
 * T, but with an anvil in the middle of the crossbar and a skeleton skull as the head:
 * <pre>
 *  . S .    S = Skeleton Skull (floor or wall skull)
 *  I A I    I = Block of Iron, A = any anvil (#minecraft:anvil)
 *  . I .    . = air
 * </pre>
 * The figure wakes when an Iron Helmet is put on the skull (right-click the skull with it): the helmet, the skull and
 * the blocks are used up and the Dread rises where the bottom block stood. With a wrong figure the helmet stays in the
 * hand and smoke rises from the skull. Not on Peaceful, switchable with {@code dreadSummonEnabled}.
 */
public final class DreadPattern {
    private static @Nullable BlockPattern pattern;

    private DreadPattern() {}

    public static BlockPattern pattern() {
        if (pattern == null) {
            pattern = BlockPatternBuilder.start()
                    .aisle("~^~", "IAI", "~I~")
                    .where('^', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.SKELETON_SKULL)
                            .or(BlockStatePredicate.forBlock(Blocks.SKELETON_WALL_SKULL))))
                    .where('A', b -> b.getState().is(BlockTags.ANVIL))
                    .where('I', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.IRON_BLOCK)))
                    .where('~', b -> b.getState().isAir())
                    .build();
        }
        return pattern;
    }

    /** Whether an Iron Helmet put on this skull would wake a Dread (client and server). */
    public static boolean matches(Level level, BlockPos skull) {
        if (!MobsRegistry.on() || !MobsConfig.dreadSummonEnabled) return false;
        if (level.getDifficulty() == Difficulty.PEACEFUL || skull.getY() < level.getMinY() + 2) return false;
        return pattern().find(level, skull) != null;
    }

    /** An Iron Helmet is put on the skull at {@code skull}: summons the Dread if the figure is complete (server). */
    public static boolean trySummon(Level level, BlockPos skull) {
        if (!(level instanceof ServerLevel server) || !matches(level, skull)) return false;
        BlockPattern.BlockPatternMatch match = pattern().find(level, skull);
        if (match == null) return false;
        DreadEntity dread = FearMobs.DREAD.create(server, EntitySpawnReason.TRIGGERED);
        if (dread == null) return false;
        BlockPos foot = match.getBlock(1, 2, 0).getPos();
        CarvedPumpkinBlock.clearPatternBlocks(level, match);
        float yaw = match.getForwards().getAxis() == Direction.Axis.X ? 90.0F : 0.0F;
        dread.snapTo(foot.getX() + 0.5, foot.getY() + 0.05, foot.getZ() + 0.5, yaw, 0.0F);
        dread.yBodyRot = yaw;
        dread.yHeadRot = yaw;
        dread.setPersistenceRequired();
        dread.startRising();
        server.addFreshEntity(dread);
        server.playSound(null, foot, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.HOSTILE, 1.5F, 0.6F);
        server.playSound(null, foot, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5F, 0.5F);
        server.playSound(null, foot, SoundEvents.WITHER_SKELETON_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.4F);
        for (ServerPlayer player : server.getEntitiesOfClass(ServerPlayer.class, dread.getBoundingBox().inflate(16.0))) {
            CriteriaTriggers.SUMMONED_ENTITY.trigger(player, dread);
        }
        CarvedPumpkinBlock.updatePatternBlocks(level, match);
        return true;
    }

    /** A helmet put on a skull of a wrong figure: a puff of smoke, the helmet is kept. */
    public static void fizzle(Level level, BlockPos skull) {
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SMOKE, skull.getX() + 0.5, skull.getY() + 0.6, skull.getZ() + 0.5, 8, 0.2, 0.1, 0.2, 0.01);
            server.playSound(null, skull, SoundEvents.ARMOR_EQUIP_CHAIN.value(), SoundSource.BLOCKS, 0.8F, 0.5F);
        }
    }
}
