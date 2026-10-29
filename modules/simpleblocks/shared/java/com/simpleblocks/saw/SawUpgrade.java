package com.simpleblocks.saw;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * In-world upgrade of a placed Vanilla stonecutter into the Shapecutter (owner 17.10.): use the tool on it with
 * {@link #NUGGETS} iron nuggets anywhere in the inventory (used up). The tool follows principle 5a: without a hammer mod
 * (Simple Hammers) an axe, with Simple Hammers a hammer ({@code #simplelib:hammers}, filled by the hammer mod; split stage 7). The
 * facing stays. One use, no strikes; SimpleLib's strike-based {@code in_world_upgrades} is not used because Simple
 * Blocks runs without SimpleLib and the owner wants the nuggets taken from the inventory.
 */
public final class SawUpgrade {
    public static final int NUGGETS = 4;
    /** Heavy weighted pressure plates in the off hand for the metal finish (BS25-3). */
    public static final int PLATES = 2;
    public static final TagKey<Item> HAMMERS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("simplelib", "hammers"));
    /** True while Simple Hammers (the hammer mod, split stage 7) is loaded; set by each loader at start. */
    public static volatile boolean hammerMode;

    private SawUpgrade() {}

    public static boolean toolFits(ItemStack tool, boolean hammer) {
        return hammer ? tool.is(HAMMERS) : tool.is(ItemTags.AXES);
    }

    /** Use event of every loader: true when the stonecutter was (or, on the client, will be) upgraded. */
    public static boolean onUse(Level level, BlockPos pos, Player player, InteractionHand hand) {
        java.util.function.Predicate<ItemStack> tool = t -> toolFits(t, hammerMode);
        return tryUpgrade(level, pos, player, hand, hammerMode, tool) || tryMetal(level, pos, player, hand, hammerMode, tool);
    }

    /**
     * BS25-3 metal finish: the same tool in {@code hand} on a wood-finish Shapecutter while {@link #PLATES} heavy weighted
     * pressure plates (iron) are in the other hand turns the finish to metal (plates on top and bottom). The plates are used
     * up (the broken block gives them back), the tool takes one damage; creative needs neither. No sound of its own: the
     * smithing/anvil sound of the upgrade is reused.
     */
    public static boolean tryMetal(Level level, BlockPos pos, Player player, InteractionHand hand, boolean hammer,
                                   java.util.function.Predicate<ItemStack> tool) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(Sawing.SAW) || state.getValue(ShapecutterBlock.METAL) || !tool.test(player.getItemInHand(hand))) return false;
        if (player.isSpectator() || !player.mayBuild()) return false;
        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack plates = player.getItemInHand(other);
        boolean free = player.hasInfiniteMaterials();
        if (!free && !(plates.is(Items.HEAVY_WEIGHTED_PRESSURE_PLATE) && plates.getCount() >= PLATES)) return false;
        if (level.isClientSide()) return true;
        if (!free) {
            plates.shrink(PLATES);
            player.getItemInHand(hand).hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        level.setBlock(pos, state.setValue(ShapecutterBlock.METAL, true), 3);
        level.playSound(null, pos, hammer ? SoundEvents.ANVIL_USE : SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.3F);
        return true;
    }

    /** The upgrade with an explicit tool rule (GameTests check both paths). */
    public static boolean tryUpgrade(Level level, BlockPos pos, Player player, InteractionHand hand, boolean hammer,
                                     java.util.function.Predicate<ItemStack> tool) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.STONECUTTER) || !tool.test(player.getItemInHand(hand))) return false;
        // Swapping a block is building: not for spectators or players who may not build (Adventure mode, REV2-A).
        if (player.isSpectator() || !player.mayBuild()) return false;
        boolean free = player.hasInfiniteMaterials();
        if (!free && player.getInventory().countItem(Items.IRON_NUGGET) < NUGGETS) return false;
        if (level.isClientSide()) return true;
        if (!free) {
            ContainerHelper.clearOrCountMatchingItems(player.getInventory(), s -> s.is(Items.IRON_NUGGET), NUGGETS, false);
            player.getItemInHand(hand).hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        level.setBlock(pos, Sawing.SAW.defaultBlockState().setValue(StonecutterBlock.FACING, state.getValue(StonecutterBlock.FACING)), 3);
        level.playSound(null, pos, hammer ? SoundEvents.ANVIL_USE : SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.1F);
        return true;
    }
}
