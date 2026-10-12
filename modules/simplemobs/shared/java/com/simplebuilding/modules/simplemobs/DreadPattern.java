package com.simplebuilding.modules.simplemobs;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
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
 * The Dread figure (owner 12.10.: summoned like a golem, never spawns naturally), seen from the front:
 * <pre>
 *  . S .    S = Wither Skeleton Skull (placed last; floor or wall skull)
 *  C A C    C = any chain (#minecraft:chains), A = any anvil (#minecraft:anvil)
 *  . I .    I = Block of Iron;  . = air
 * </pre>
 * About 40 iron ingots plus the skull. Placing the skull checks the figure like the Wither's; the blocks are used up
 * and the Dread rises where the anvil stood. Not on Peaceful, switchable with {@code dreadSummonEnabled}.
 */
public final class DreadPattern {
    private static @Nullable BlockPattern pattern;

    private DreadPattern() {}

    public static BlockPattern pattern() {
        if (pattern == null) {
            pattern = BlockPatternBuilder.start()
                    .aisle("~^~", "CAC", "~I~")
                    .where('^', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.WITHER_SKELETON_SKULL)
                            .or(BlockStatePredicate.forBlock(Blocks.WITHER_SKELETON_WALL_SKULL))))
                    .where('C', b -> b.getState().is(BlockTags.CHAINS))
                    .where('A', b -> b.getState().is(BlockTags.ANVIL))
                    .where('I', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.IRON_BLOCK)))
                    .where('~', b -> b.getState().isAir())
                    .build();
        }
        return pattern;
    }

    /** Called when a wither skeleton skull is placed (before the Wither check). True if a Dread was summoned. */
    public static boolean trySummon(Level level, BlockPos skull) {
        if (!(level instanceof ServerLevel server) || !MobsRegistry.on() || !MobsConfig.dreadSummonEnabled) return false;
        if (level.getDifficulty() == Difficulty.PEACEFUL || skull.getY() < level.getMinY() + 2) return false;
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
        server.playSound(null, foot, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5F, 0.5F);
        server.playSound(null, foot, SoundEvents.WITHER_SKELETON_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.4F);
        for (ServerPlayer player : server.getEntitiesOfClass(ServerPlayer.class, dread.getBoundingBox().inflate(16.0))) {
            CriteriaTriggers.SUMMONED_ENTITY.trigger(player, dread);
        }
        CarvedPumpkinBlock.updatePatternBlocks(level, match);
        return true;
    }
}
