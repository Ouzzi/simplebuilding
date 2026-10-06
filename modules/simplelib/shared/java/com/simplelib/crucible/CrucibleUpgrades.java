package com.simplelib.crucible;

import com.simplelib.registry.LibBlocks;
import com.simplelib.registry.LibTags;
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
 * hand, the Vanilla material in the off hand, ten strikes (owner F9: twice the furnace upgrade):
 * Iron -> Reinforced with 2 diamonds, Reinforced -> Netherite with 1 netherite ingot (barrels the same:
 * copper -> reinforced -> netherite). The material
 * is used on the last strike; contents, progress and experience stay. SimpleBuilding replaces this
 * with its sledgehammer way and calls {@link #upgradeInPlace} itself.
 */
public final class CrucibleUpgrades {
    public static final int STRIKES = 10;
    /** The cauldron has no block entity: its strikes are counted here per position (cleared after 30 s). */
    public static final int CAULDRON_STRIKES = 5;
    public static final int CAULDRON_COST = 8;
    private static final java.util.Map<String, long[]> CAULDRON_PROGRESS = new java.util.HashMap<>();

    public record Step(Block from, Block to, TagKey<Item> material, int count) {}

    public static @Nullable Step stepFor(BlockState state, ItemStack material) {
        if (material.isEmpty()) return null;
        if (state.is(LibBlocks.IRON_CRUCIBLE) && material.is(LibTags.UPGRADE_REINFORCED))
            return new Step(LibBlocks.IRON_CRUCIBLE, LibBlocks.REINFORCED_CRUCIBLE, LibTags.UPGRADE_REINFORCED, 2);
        if (state.is(LibBlocks.REINFORCED_CRUCIBLE) && material.is(LibTags.UPGRADE_NETHERITE))
            return new Step(LibBlocks.REINFORCED_CRUCIBLE, LibBlocks.NETHERITE_CRUCIBLE, LibTags.UPGRADE_NETHERITE, 1);
        if (state.is(LibBlocks.COPPER_BARREL) && material.is(LibTags.UPGRADE_REINFORCED))
            return new Step(LibBlocks.COPPER_BARREL, LibBlocks.REINFORCED_BARREL, LibTags.UPGRADE_REINFORCED, 2);
        if (state.is(LibBlocks.REINFORCED_BARREL) && material.is(LibTags.UPGRADE_NETHERITE))
            return new Step(LibBlocks.REINFORCED_BARREL, LibBlocks.NETHERITE_BARREL, LibTags.UPGRADE_NETHERITE, 1);
        // Owner 56 B: without SimpleBuilding the cauldron becomes reinforced with an axe and 8 diamonds (owner 2026-10-06: doubled from 4).
        if (state.is(net.minecraft.world.level.block.Blocks.CAULDRON) && material.is(LibTags.UPGRADE_REINFORCED))
            return new Step(net.minecraft.world.level.block.Blocks.CAULDRON, LibBlocks.REINFORCED_CAULDRON, LibTags.UPGRADE_REINFORCED, CAULDRON_COST);
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
        if (level.getBlockEntity(pos) instanceof CrucibleBlockEntity be) done = be.addUpgradeStrike();
        else if (level.getBlockEntity(pos) instanceof CrucibleBarrelBlockEntity barrel) done = barrel.addAttachStrike();
        else if (state.is(net.minecraft.world.level.block.Blocks.CAULDRON)) {
            done = cauldronStrike(server, pos);
            needed = CAULDRON_STRIKES;
        } else return true;
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                6, 0.25, 0.1, 0.25, 0.05);
        if (done >= needed) {
            if (!player.getAbilities().instabuild) material.shrink(step.count());
            if (step.to() == LibBlocks.REINFORCED_CAULDRON) {
                CAULDRON_PROGRESS.remove(server.dimension().identifier() + "@" + pos.asLong());
                server.setBlock(pos, LibBlocks.REINFORCED_CAULDRON.defaultBlockState(), Block.UPDATE_ALL);
            } else upgradeInPlace(server, pos, step.to());
            server.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else {
            server.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.4F, 1.2F + 0.05F * done);
        }
        if (!player.getAbilities().instabuild) tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        player.getCooldowns().addCooldown(tool, CrucibleBlankBlock.STRIKE_COOLDOWN);
        return true;
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
