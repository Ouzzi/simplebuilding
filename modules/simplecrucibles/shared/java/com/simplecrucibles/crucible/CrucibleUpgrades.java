package com.simplecrucibles.crucible;

import com.simplecrucibles.registry.CrucibleBlocks;
import com.simplecrucibles.registry.CrucibleTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Upgrading a crucible in place without SimpleBuilding (principle 5a, owner 50): an axe in the main
 * hand, the material in the off hand. Crucibles (owner N51): {@value #CRUCIBLE_STRIKES} strikes, every
 * {@value #STRIKES_PER_UNIT}rd strikes one unit in ({@link CrucibleBlock#UPGRADE} shows it as a band), four units:
 * Iron -> Reinforced with cracked diamonds (or diamonds), Reinforced -> Netherite with netherite nuggets (or netherite
 * scrap). Barrels keep ten strikes (owner F9) with the material on the last strike: copper -> reinforced (2 diamonds)
 * -> netherite (1 netherite ingot). Contents, progress and experience stay. SimpleBuilding replaces this with its
 * sledgehammer way and calls {@link #upgradeInPlace} / {@link #strikeInUnit} itself.
 */
public final class CrucibleUpgrades {
    public static final int STRIKES = 10;
    /** Crucible upgrades (owner N51): 12 strikes, one unit every 3 strikes = 4 units. */
    public static final int CRUCIBLE_STRIKES = 12;
    public static final int STRIKES_PER_UNIT = 3;
    public static final int CRUCIBLE_UNITS = CRUCIBLE_STRIKES / STRIKES_PER_UNIT;
    /** The cauldron has no block entity: its strikes are counted here per position (cleared after 30 s). */
    public static final int CAULDRON_STRIKES = 5;
    public static final int CAULDRON_COST = 8;
    private static final java.util.Map<String, long[]> CAULDRON_PROGRESS = new java.util.HashMap<>();

    public record Step(Block from, Block to, TagKey<Item> material, int count) {}

    public static @Nullable Step stepFor(BlockState state, ItemStack material) {
        if (material.isEmpty()) return null;
        if (state.is(CrucibleBlocks.IRON_CRUCIBLE) && material.is(CrucibleTags.CRUCIBLE_UPGRADE_REINFORCED))
            return new Step(CrucibleBlocks.IRON_CRUCIBLE, CrucibleBlocks.REINFORCED_CRUCIBLE, CrucibleTags.CRUCIBLE_UPGRADE_REINFORCED, 1);
        if (state.is(CrucibleBlocks.REINFORCED_CRUCIBLE) && material.is(CrucibleTags.CRUCIBLE_UPGRADE_NETHERITE))
            return new Step(CrucibleBlocks.REINFORCED_CRUCIBLE, CrucibleBlocks.NETHERITE_CRUCIBLE, CrucibleTags.CRUCIBLE_UPGRADE_NETHERITE, 1);
        if (state.is(CrucibleBlocks.COPPER_BARREL) && material.is(CrucibleTags.UPGRADE_REINFORCED))
            return new Step(CrucibleBlocks.COPPER_BARREL, CrucibleBlocks.REINFORCED_BARREL, CrucibleTags.UPGRADE_REINFORCED, 2);
        if (state.is(CrucibleBlocks.REINFORCED_BARREL) && material.is(CrucibleTags.UPGRADE_NETHERITE))
            return new Step(CrucibleBlocks.REINFORCED_BARREL, CrucibleBlocks.NETHERITE_BARREL, CrucibleTags.UPGRADE_NETHERITE, 1);
        // Owner 56 B: without SimpleBuilding the cauldron becomes reinforced with an axe and 8 diamonds (owner 2026-10-06: doubled from 4).
        if (state.is(net.minecraft.world.level.block.Blocks.CAULDRON) && material.is(CrucibleTags.UPGRADE_REINFORCED))
            return new Step(net.minecraft.world.level.block.Blocks.CAULDRON, CrucibleBlocks.REINFORCED_CAULDRON, CrucibleTags.UPGRADE_REINFORCED, CAULDRON_COST);
        return null;
    }

    /** One axe strike on a crucible; true when it counted. */
    public static boolean strike(Level level, BlockPos pos, Player player, ItemStack tool) {
        BlockState state = level.getBlockState(pos);
        ItemStack material = player.getOffhandItem();
        Step step = stepFor(state, material);
        if (step == null || material.getCount() < step.count() || player.getCooldowns().isOnCooldown(tool)) return false;
        if (!(level instanceof ServerLevel server)) return true;
        int done;
        int needed = STRIKES;
        if (level.getBlockEntity(pos) instanceof CrucibleBlockEntity be) {
            done = be.addUpgradeStrike(unitsIn(state) * STRIKES_PER_UNIT);
            strikeInUnit(server, pos, state, step.to(), done, player.getAbilities().instabuild ? ItemStack.EMPTY : material);
            if (!player.getAbilities().instabuild) tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            player.getCooldowns().addCooldown(tool, CrucibleBlankBlock.STRIKE_COOLDOWN);
            return true;
        }
        else if (level.getBlockEntity(pos) instanceof CrucibleBarrelBlockEntity barrel) done = barrel.addUpgradeStrike();
        else if (state.is(net.minecraft.world.level.block.Blocks.CAULDRON)) {
            done = cauldronStrike(server, pos);
            needed = CAULDRON_STRIKES;
        } else return true;
        // Same progress feedback as every in-world conversion (owner 2026-10-06/10): cracks, particles and a rising hit
        // sound per strike, plus the growing preview of the upgraded block (cracks gone at the end).
        com.simplelib.api.InWorldStrikes.feedback(server, pos, state, SoundEvents.ANVIL_PLACE, 0.4F, done, needed);
        com.simplelib.api.InWorldStrikes.preview(server, pos, previewState(state, step.to()), done, needed);
        if (done >= needed) {
            if (!player.getAbilities().instabuild) material.shrink(step.count());
            if (step.to() == CrucibleBlocks.REINFORCED_CAULDRON) {
                CAULDRON_PROGRESS.remove(server.dimension().identifier() + "@" + pos.asLong());
                server.setBlock(pos, CrucibleBlocks.REINFORCED_CAULDRON.defaultBlockState(), Block.UPDATE_ALL);
            } else upgradeInPlace(server, pos, step.to());
            server.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        if (!player.getAbilities().instabuild) tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        player.getCooldowns().addCooldown(tool, CrucibleBlankBlock.STRIKE_COOLDOWN);
        return true;
    }

    /** Upgrade units already struck into a crucible (0 on anything else). */
    public static int unitsIn(BlockState state) {
        return state.hasProperty(CrucibleBlock.UPGRADE) ? state.getValue(CrucibleBlock.UPGRADE) : 0;
    }

    /**
     * Crucible upgrade strike number {@code done} (1..{@value #CRUCIBLE_STRIKES}) - shared by the axe way and partner ways
     * (Simple Hammers' sledgehammer): cracks, sound and the growing preview of {@code to}; every
     * {@value #STRIKES_PER_UNIT}rd strike takes one unit from {@code material} (empty = creative, nothing taken) and
     * shows it as a band ({@link CrucibleBlock#UPGRADE}); the last one upgrades in place. Returns the units struck in.
     */
    public static int strikeInUnit(ServerLevel server, BlockPos pos, BlockState state, Block to, int done, ItemStack material) {
        done = Math.max(1, Math.min(done, CRUCIBLE_STRIKES));
        com.simplelib.api.InWorldStrikes.feedback(server, pos, state, SoundEvents.ANVIL_PLACE, 0.4F, done, CRUCIBLE_STRIKES);
        com.simplelib.api.InWorldStrikes.preview(server, pos, previewState(state, to), done, CRUCIBLE_STRIKES);
        if (done % STRIKES_PER_UNIT != 0) return unitsIn(state);
        ItemStack unit = material.isEmpty() ? ItemStack.EMPTY : material.split(1);
        if (done >= CRUCIBLE_STRIKES) {
            upgradeInPlace(server, pos, to);
            server.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            return CRUCIBLE_UNITS;
        }
        int units = Math.min(CRUCIBLE_UNITS - 1, done / STRIKES_PER_UNIT);
        if (server.getBlockEntity(pos) instanceof CrucibleBlockEntity be) be.addUpgradeUnit(unit);
        if (state.hasProperty(CrucibleBlock.UPGRADE) && state.getValue(CrucibleBlock.UPGRADE) != units) {
            server.setBlock(pos, state.setValue(CrucibleBlock.UPGRADE, units), Block.UPDATE_ALL);
        }
        server.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5F, 1.2F + 0.1F * units);
        return units;
    }

    /** The upgraded block as the preview shows it: the target's default state with the old facing kept. */
    public static BlockState previewState(BlockState from, Block to) {
        BlockState next = to.defaultBlockState();
        if (from.hasProperty(CrucibleBarrelBlock.FACING) && next.hasProperty(CrucibleBarrelBlock.FACING)) {
            next = next.setValue(CrucibleBarrelBlock.FACING, from.getValue(CrucibleBarrelBlock.FACING));
        }
        return next;
    }

    private static int cauldronStrike(ServerLevel level, BlockPos pos) {
        String key = level.dimension().identifier() + "@" + pos.asLong();
        long now = level.getGameTime();
        long[] entry = CAULDRON_PROGRESS.get(key);
        if (entry == null || now - entry[1] > 600) entry = new long[] {0, now};
        entry[0]++;
        entry[1] = now;
        CAULDRON_PROGRESS.put(key, entry);
        if (CAULDRON_PROGRESS.size() > 256) CAULDRON_PROGRESS.entrySet().removeIf(e -> now - e.getValue()[1] > 600);
        return (int) entry[0];
    }

    /** Replaces the crucible by {@code to}, keeping facing, contents, progress and experience (owner 11). */
    public static void upgradeInPlace(ServerLevel level, BlockPos pos, Block to) {
        if (level.getBlockEntity(pos) instanceof CrucibleBarrelBlockEntity barrel) {
            java.util.List<ItemStack> kept = new java.util.ArrayList<>();
            for (int i = 0; i < barrel.getContainerSize(); i++) kept.add(barrel.removeItemNoUpdate(i));
            BlockState from = level.getBlockState(pos);
            BlockState next = to.defaultBlockState();
            if (from.hasProperty(CrucibleBarrelBlock.FACING)) {
                next = next.setValue(CrucibleBarrelBlock.FACING, from.getValue(CrucibleBarrelBlock.FACING))
                        .setValue(CrucibleBarrelBlock.ATTACHED, from.getValue(CrucibleBarrelBlock.ATTACHED));
            }
            level.setBlock(pos, next, Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof CrucibleBarrelBlockEntity fresh) {
                for (int i = 0; i < kept.size() && i < fresh.getContainerSize(); i++) fresh.setItem(i, kept.get(i));
            }
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof CrucibleBlockEntity old)) return;
        CrucibleBlockEntity.Snapshot snapshot = old.snapshot();
        old.clearForUpgrade();
        BlockState state = level.getBlockState(pos);
        BlockState next = to.defaultBlockState();
        if (state.hasProperty(CrucibleBlock.FACING) && next.hasProperty(CrucibleBlock.FACING)) {
            next = next.setValue(CrucibleBlock.FACING, state.getValue(CrucibleBlock.FACING));
        }
        level.setBlock(pos, next, Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof CrucibleBlockEntity fresh) fresh.restore(snapshot);
    }

    private CrucibleUpgrades() {}
}
