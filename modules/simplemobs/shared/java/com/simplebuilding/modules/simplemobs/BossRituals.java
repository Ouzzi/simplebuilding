package com.simplebuilding.modules.simplemobs;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Summoning the two End bosses (WP-19, concept: ritual, never natural spawns). Both only in the End, not on Peaceful.
 * <ul>
 *   <li><b>Void Runner:</b> a column of three Purpur Blocks or Purpur Pillars standing on End Stone. Throw an Eye of Ender
 *   and four Chorus Fruit onto the top, then right-click the top block with Dragon's Breath. The column, the offerings and
 *   the breath are used up (the bottle stays); the Void Runner appears where the column stood. Config
 *   {@code voidRunnerSummonEnabled}.</li>
 *   <li><b>Brimguard:</b> a 3 x 3 platform of Obsidian or Crying Obsidian. Throw three Shellker Shells onto the middle,
 *   then right-click the middle with a Nether Star. Star and shells are used up, the platform stays. Config
 *   {@code brimguardSummonEnabled}.</li>
 * </ul>
 * A wrong figure or missing offerings: smoke, the item stays in the hand.
 */
public final class BossRituals {
    public static final int CHORUS = 4, EYES = 1, SHELLS = 3;

    private BossRituals() {}

    static boolean purpur(BlockState s) { return s.is(Blocks.PURPUR_BLOCK) || s.is(Blocks.PURPUR_PILLAR); }
    static boolean obsidian(BlockState s) { return s.is(Blocks.OBSIDIAN) || s.is(Blocks.CRYING_OBSIDIAN); }

    private static boolean allowed(Level level) {
        return MobsRegistry.on() && level.getDifficulty() != Difficulty.PEACEFUL && (!MobsConfig.bossRitualsEndOnly || level.dimension() == Level.END);
    }

    /** Item entities lying on top of the block at {@code pos}. */
    public static List<ItemEntity> offerings(Level level, BlockPos pos) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos.above()).inflate(0.25, 0.5, 0.25), ItemEntity::isAlive);
    }

    static int count(List<ItemEntity> items, Predicate<ItemStack> what) {
        int n = 0;
        for (ItemEntity e : items) if (what.test(e.getItem())) n += e.getItem().getCount();
        return n;
    }

    /** Takes {@code amount} matching items from the lying stacks. */
    static void take(List<ItemEntity> items, Predicate<ItemStack> what, int amount) {
        for (ItemEntity e : items) {
            if (amount <= 0) return;
            ItemStack st = e.getItem();
            if (!what.test(st)) continue;
            int n = Math.min(amount, st.getCount());
            st.shrink(n);
            amount -= n;
            if (st.isEmpty()) e.discard();
            else e.setItem(st);
        }
    }

    // ---- Void Runner ------------------------------------------------------------------------

    /** The column (top block clicked) is complete: three purpur on End Stone, room above, and the offerings lie on top. */
    public static boolean runnerFigure(Level level, BlockPos top) {
        return purpur(level.getBlockState(top)) && purpur(level.getBlockState(top.below())) && purpur(level.getBlockState(top.below(2)))
                && level.getBlockState(top.below(3)).is(Blocks.END_STONE);
    }

    public static boolean runnerOfferings(Level level, BlockPos top) {
        List<ItemEntity> items = offerings(level, top);
        return count(items, s -> s.is(Items.ENDER_EYE)) >= EYES && count(items, s -> s.is(Items.CHORUS_FRUIT)) >= CHORUS;
    }

    public static boolean runnerMatches(Level level, BlockPos top) {
        return MobsConfig.voidRunnerSummonEnabled && allowed(level) && runnerFigure(level, top) && runnerOfferings(level, top);
    }

    /** Dragon's Breath on the top block: summons the Void Runner if everything is in place (server). */
    public static boolean trySummonRunner(Level level, BlockPos top) {
        if (!(level instanceof ServerLevel server) || !runnerMatches(level, top)) return false;
        VoidRunnerEntity runner = BossMobs.VOID_RUNNER.create(server, EntitySpawnReason.TRIGGERED);
        if (runner == null) return false;
        List<ItemEntity> items = offerings(level, top);
        take(items, s -> s.is(Items.ENDER_EYE), EYES);
        take(items, s -> s.is(Items.CHORUS_FRUIT), CHORUS);
        BlockPos foot = top.below(2);
        for (int i = 0; i < 3; i++) {
            level.destroyBlock(top.below(i), false);
        }
        spawn(server, runner, foot);
        server.sendParticles(ParticleTypes.PORTAL, foot.getX() + 0.5, foot.getY() + 1.0, foot.getZ() + 0.5, 80, 0.4, 1.0, 0.4, 0.6);
        server.playSound(null, foot, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.0F, 1.4F);
        server.playSound(null, foot, SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 1.5F, 1.2F);
        return true;
    }

    // ---- Brimguard --------------------------------------------------------------------------

    /** The 3 x 3 obsidian platform around {@code center} with two blocks of room above the middle. */
    public static boolean brimFigure(Level level, BlockPos center) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
            if (!obsidian(level.getBlockState(center.offset(dx, 0, dz)))) return false;
        return level.getBlockState(center.above()).getCollisionShape(level, center.above()).isEmpty()
                && level.getBlockState(center.above(2)).getCollisionShape(level, center.above(2)).isEmpty();
    }

    public static boolean brimOfferings(Level level, BlockPos center) {
        return count(offerings(level, center), s -> s.is(EndNetherMobs.SHELL)) >= SHELLS;
    }

    public static boolean brimMatches(Level level, BlockPos center) {
        return MobsConfig.brimguardSummonEnabled && allowed(level) && brimFigure(level, center) && brimOfferings(level, center);
    }

    /** Nether Star on the middle of the platform: summons the Brimguard (server). */
    public static boolean trySummonBrimguard(Level level, BlockPos center) {
        if (!(level instanceof ServerLevel server) || !brimMatches(level, center)) return false;
        BrimguardEntity brim = BossMobs.BRIMGUARD.create(server, EntitySpawnReason.TRIGGERED);
        if (brim == null) return false;
        take(offerings(level, center), s -> s.is(EndNetherMobs.SHELL), SHELLS);
        spawn(server, brim, center.above());
        server.sendParticles(ParticleTypes.REVERSE_PORTAL, center.getX() + 0.5, center.getY() + 1.5, center.getZ() + 0.5, 60, 1.0, 0.6, 1.0, 0.1);
        server.playSound(null, center, SoundEvents.SHULKER_CLOSE, SoundSource.HOSTILE, 2.0F, 0.4F);
        server.playSound(null, center, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5F, 0.5F);
        return true;
    }

    private static void spawn(ServerLevel server, Mob mob, BlockPos foot) {
        mob.snapTo(foot.getX() + 0.5, foot.getY() + 0.05, foot.getZ() + 0.5, server.getRandom().nextFloat() * 360.0F, 0.0F);
        mob.setPersistenceRequired();
        server.addFreshEntity(mob);
        for (ServerPlayer player : server.getEntitiesOfClass(ServerPlayer.class, mob.getBoundingBox().inflate(16.0))) {
            CriteriaTriggers.SUMMONED_ENTITY.trigger(player, mob);
        }
    }

    /** A key item used on a wrong figure: a puff of smoke, the item is kept. */
    public static void fizzle(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 8, 0.2, 0.1, 0.2, 0.01);
            server.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.2F);
        }
    }
}
