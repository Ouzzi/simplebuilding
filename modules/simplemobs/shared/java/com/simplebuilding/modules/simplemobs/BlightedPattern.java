package com.simplebuilding.modules.simplemobs;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * Blighted Wither summoning (concept N26, question 1): the Vanilla Wither figure - T of soul sand/soul soil with three
 * wither skeleton skulls - with a Block of Netherite under the foot of the T. Placing the last skull summons the
 * Blighted Wither instead of the Wither and uses up the Netherite block too. Without the block the Vanilla Wither comes
 * as always. Not on Peaceful, switchable with {@code witherSummonEnabled}.
 * <pre>
 *  ^ ^ ^    ^ = wither skeleton skull
 *  # # #    # = #minecraft:wither_summon_base_blocks
 *  . # .    . = air
 *    N      N = Block of Netherite
 * </pre>
 */
public final class BlightedPattern {
    private static @Nullable BlockPattern pattern;

    private BlightedPattern() {}

    /** The Vanilla Wither figure (WitherSkullBlock keeps its own private). */
    public static BlockPattern pattern() {
        if (pattern == null) {
            pattern = BlockPatternBuilder.start()
                    .aisle("^^^", "###", "~#~")
                    .where('#', b -> b.getState().is(BlockTags.WITHER_SUMMON_BASE_BLOCKS))
                    .where('^', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.WITHER_SKELETON_SKULL)
                            .or(BlockStatePredicate.forBlock(Blocks.WITHER_SKELETON_WALL_SKULL))))
                    .where('~', b -> b.getState().isAir())
                    .build();
        }
        return pattern;
    }

    /** The foot block below the T of a match (follows the figure's orientation). */
    public static BlockPos foot(BlockPattern.BlockPatternMatch match) {
        return match.getBlock(1, 3, 0).getPos();
    }

    /** A skull was placed at {@code skull}: summons the Blighted Wither if the figure stands on Netherite (server). */
    public static boolean trySummon(Level level, BlockPos skull) {
        if (!(level instanceof ServerLevel server) || !MobsRegistry.on() || !MobsConfig.witherSummonEnabled) return false;
        if (level.getDifficulty() == Difficulty.PEACEFUL || skull.getY() < level.getMinY()) return false;
        var state = level.getBlockState(skull);
        if (!state.is(Blocks.WITHER_SKELETON_SKULL) && !state.is(Blocks.WITHER_SKELETON_WALL_SKULL)) return false;
        BlockPattern.BlockPatternMatch match = pattern().find(level, skull);
        if (match == null) return false;
        BlockPos foot = foot(match);
        if (!level.getBlockState(foot).is(Blocks.NETHERITE_BLOCK)) return false;
        BlightedWither wither = WitherMobs.BLIGHTED_WITHER.create(server, EntitySpawnReason.TRIGGERED);
        if (wither == null) return false;
        CarvedPumpkinBlock.clearPatternBlocks(level, match);
        level.levelEvent(2001, foot, net.minecraft.world.level.block.Block.getId(level.getBlockState(foot)));
        level.setBlock(foot, Blocks.AIR.defaultBlockState(), 2);
        BlockPos spawn = match.getBlock(1, 2, 0).getPos();
        float yaw = match.getForwards().getAxis() == Direction.Axis.X ? 0.0F : 90.0F;
        wither.snapTo(spawn.getX() + 0.5, spawn.getY() + 0.55, spawn.getZ() + 0.5, yaw, 0.0F);
        wither.yBodyRot = yaw;
        wither.makeInvulnerable();
        for (ServerPlayer player : server.getEntitiesOfClass(ServerPlayer.class, wither.getBoundingBox().inflate(50.0))) {
            CriteriaTriggers.SUMMONED_ENTITY.trigger(player, wither);
        }
        server.addFreshEntity(wither);
        CarvedPumpkinBlock.updatePatternBlocks(level, match);
        level.updateNeighborsAt(foot, Blocks.AIR);
        return true;
    }
}
