package com.simplebeacons.upgrade;

import com.simplebeacons.BeaconsRegistry;
import com.simplebeacons.SimpleBeacons;
import com.simplebeacons.config.BeaconsConfig;
import com.simplelib.api.InWorldStrikes;
import com.simplelib.api.SimpleLibApi;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import org.jspecify.annotations.Nullable;

/**
 * The in-world Strong upgrade (concept §2 B): an upgrade tool in the main hand ({@code #simplelib:upgrade_tools}, i.e.
 * a hammer mod's sledgehammer, or any axe while SimpleLib's Vanilla axe ways are on - principle 5a), the Strong
 * Upgrade template in the off hand and a nether star anywhere in the inventory; use on a placed Vanilla beacon or
 * conduit. {@link BeaconsConfig#upgradeStrikes} strikes (default 8) with SimpleLib's cracks, sound and the growing
 * target preview; the last one swaps the block in place (choice, lock, name and levels stay) and uses up the template
 * and the star. A module helper instead of an entry in {@code simplelib:in_world_upgrades}: that table is loaded by
 * SimpleBuilding only and has no second (inventory) material.
 */
public final class StrongUpgrades {
    public static final String KIND = "simplebeacons:strong_upgrade";

    /** The Strong block a Vanilla block becomes, or null. */
    public static @Nullable Block targetFor(BlockState state) {
        if (state.is(Blocks.BEACON)) return BeaconsRegistry.STRONG_BEACON;
        if (state.is(Blocks.CONDUIT)) return BeaconsRegistry.STRONG_CONDUIT;
        return null;
    }

    /** Main hand tool + off hand template (the stance; the star is checked per strike). */
    public static boolean isStance(Player player) {
        return SimpleLibApi.isUpgradeTool(player.getMainHandItem()) && player.getOffhandItem().is(BeaconsRegistry.STRONG_UPGRADE_TEMPLATE);
    }

    /** Slot of a nether star in the inventory, or -1. */
    public static int starSlot(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(Items.NETHER_STAR)) return i;
        }
        return -1;
    }

    /**
     * One use on {@code pos}; null when this is not a Strong upgrade (Vanilla handles the click), else the result of
     * the strike (a strike that came too early still consumes the click, so the beacon menu does not open).
     */
    public static @Nullable InteractionResult strike(ServerLevel level, BlockPos pos, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !SimpleBeacons.on() || !isStance(player)) return null;
        BlockState state = level.getBlockState(pos);
        Block target = targetFor(state);
        if (target == null) return null;
        if (!player.mayBuild() || !level.mayInteract(player, pos)) return InteractionResult.FAIL;
        if (!player.hasInfiniteMaterials() && starSlot(player) < 0) {
            level.playSound(null, pos, SoundEvents.ITEM_BREAK.value(), SoundSource.BLOCKS, 0.5F, 1.4F);
            return InteractionResult.FAIL;
        }
        int total = BeaconsConfig.upgradeStrikes;
        int done = InWorldStrikes.count(level, pos, KIND, state, total, level.getGameTime(), InWorldStrikes.interval(player.getMainHandItem()));
        if (done == 0) return InteractionResult.SUCCESS;
        InWorldStrikes.feedback(level, pos, state, SoundEvents.ANVIL_USE, 0.6F, done, total);
        InWorldStrikes.preview(level, pos, target.defaultBlockState(), done, total);
        if (!player.hasInfiniteMaterials()) player.getMainHandItem().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        if (done >= total) {
            finish(level, pos, player, target);
        }
        return InteractionResult.SUCCESS;
    }

    private static void finish(ServerLevel level, BlockPos pos, Player player, Block target) {
        InWorldStrikes.clearPreview(level, pos);
        if (!player.hasInfiniteMaterials()) {
            int star = starSlot(player);
            if (star < 0) {
                // The star was put away between the strikes: same answer as a first strike without it (REV2-A).
                level.playSound(null, pos, SoundEvents.ITEM_BREAK.value(), SoundSource.BLOCKS, 0.5F, 1.4F);
                return;
            }
            player.getInventory().getItem(star).shrink(1);
            player.getOffhandItem().shrink(1);
        }
        upgrade(level, pos, target);
        level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0F, 1.2F);
    }

    /** Swaps the block at {@code pos} for {@code to}; properties and block entity data (same keys) stay. */
    public static void upgrade(ServerLevel level, BlockPos pos, Block to) {
        BlockState old = level.getBlockState(pos);
        BlockEntity before = level.getBlockEntity(pos);
        CompoundTag data = before != null ? before.saveCustomOnly(level.registryAccess()) : new CompoundTag();
        BlockState next = to.withPropertiesOf(old);
        level.setBlock(pos, next, Block.UPDATE_ALL);
        BlockEntity after = level.getBlockEntity(pos);
        if (after != null) {
            after.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data));
            after.setChanged();
            level.sendBlockUpdated(pos, next, next, Block.UPDATE_ALL);
        }
    }

    private StrongUpgrades() {}
}
