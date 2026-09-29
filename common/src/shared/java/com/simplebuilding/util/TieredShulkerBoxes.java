package com.simplebuilding.util;

import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.blocks.custom.TieredShulkerBoxBlock;
import com.simplebuilding.blocks.entity.custom.TieredShulkerBoxBlockEntity;
import com.simplebuilding.component.ContainerCounts;
import com.simplebuilding.component.ModDataComponentTypes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The tier shulker boxes: vanilla shulker box (27, any color) -&gt; Reinforced (36) -&gt; Netherite
 * (45, stacks x2) -&gt; Enderite (54, stacks x4) - the slot counts and stack factors of the tier
 * chests ({@link ChestTier}). Climbed in the world with the sledgehammer like the chests
 * ({@link SledgehammerUpgrades}), but dearer: the constants below, read by the Balancing-Zentrale.
 *
 * <p>The upgrade keeps the contents slot for slot, the name, the lock, the facing and the color
 * (a vanilla box's color comes from its block, a tier box's from its block entity).
 */
public final class TieredShulkerBoxes {

    /**
     * A shulker upgrade takes this many times the blows of a machine or chest upgrade
     * ({@code server.tools.sledgehammerUpgradeSeconds}, default 5): 10 blows, 10 seconds.
     */
    public static final int SHULKER_UPGRADE_DURATION_FACTOR = 2;
    /** Material per shulker upgrade (cracked diamonds, netherite or enderite nuggets); a chest takes one. */
    public static final int SHULKER_UPGRADE_MATERIAL_COST = 2;
    /** Cracked diamonds in the crafting recipe vanilla shulker box -&gt; Reinforced Shulker Box. */
    public static final int REINFORCED_RECIPE_CRACKED_DIAMOND_COST = 4;

    private TieredShulkerBoxes() {
    }

    /** The tier of {@code block}, or null when it is no tier shulker box. */
    public static @Nullable ChestTier tierOf(Block block) {
        return block instanceof TieredShulkerBoxBlock box ? box.tier() : null;
    }

    /** Whether {@code block} is a vanilla shulker box of any color (the first step of the chain). */
    public static boolean isVanillaShulkerBox(Block block) {
        return block instanceof ShulkerBoxBlock && !(block instanceof TieredShulkerBoxBlock);
    }

    /** The dye color of the box at {@code pos}: from the block (vanilla) or the block entity (tier box). */
    public static @Nullable DyeColor colorAt(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof TieredShulkerBoxBlockEntity box) {
            return box.getColor();
        }
        return state.getBlock() instanceof ShulkerBoxBlock vanilla ? vanilla.getColor() : null;
    }

    // =====================================================================================
    // ITEMS
    // =====================================================================================

    /**
     * The contents an item of a tier shulker box carries, with the real counts (oversized slots
     * from {@code simplebuilding:container_counts}); as many slots as the tier has.
     */
    public static NonNullList<ItemStack> contentsOf(ItemStack stack) {
        ChestTier tier = stack.getItem() instanceof BlockItem item ? tierOf(item.getBlock()) : null;
        ItemContainerContents container = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        int size = tier == null ? 27 : tier.slots();
        NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
        container.copyInto(items);
        ContainerCounts counts = stack.get(ModDataComponentTypes.CONTAINER_COUNTS);
        if (counts != null) {
            counts.applyTo(items);
        }
        return items;
    }

    /** {@code items} split into stacks no larger than a normal stack (for spilling an oversized slot). */
    public static List<ItemStack> splitToNormalStacks(List<ItemStack> items) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : items) {
            ItemStack rest = stack.copy();
            while (!rest.isEmpty()) {
                out.add(rest.split(Math.max(1, rest.getMaxStackSize())));
            }
        }
        return out;
    }

    /** The tooltip lines of a tier shulker box (slots, from Netherite on the stack factor), otherwise empty. */
    public static List<Component> tooltip(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item) || tierOf(item.getBlock()) == null) {
            return List.of();
        }
        ChestTier tier = tierOf(item.getBlock());
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("tooltip.simplebuilding.tiered_shulker_box.slots", tier.slots()).withStyle(ChatFormatting.GRAY));
        if (tier.stackMultiplier() > 1) {
            lines.add(Component.translatable("tooltip.simplebuilding.tiered_chest.stacks", tier.stackMultiplier())
                    .withStyle(ChatFormatting.GRAY));
        }
        DyeColor color = stack.get(DataComponents.BASE_COLOR);
        if (color != null) {
            lines.add(Component.translatable("tooltip.simplebuilding.tiered_shulker_box.color",
                    Component.translatable("color.minecraft." + color.getName())).withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    // =====================================================================================
    // DISPENSER AND CAULDRON (registered by every loader)
    // =====================================================================================

    /** Water levels one wash costs, like vanilla's shulker box wash. */
    public static final int WASH_WATER_LEVELS = 1;

    /**
     * Washing a dyed tier shulker box in a water cauldron: like vanilla's, the box loses its color
     * (and nothing else) and the cauldron one level of water.
     */
    public static final net.minecraft.core.cauldron.CauldronInteraction WASH = (state, level, pos, player, hand, stack) -> {
        if (!(stack.getItem() instanceof BlockItem item) || tierOf(item.getBlock()) == null || !stack.has(DataComponents.BASE_COLOR)) {
            return net.minecraft.world.InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            ItemStack washed = stack.copyWithCount(1);
            washed.remove(DataComponents.BASE_COLOR);
            player.setItemInHand(hand, net.minecraft.world.item.ItemUtils.createFilledResult(stack, player, washed, false));
            player.awardStat(net.minecraft.stats.Stats.CLEAN_SHULKER_BOX);
            net.minecraft.world.level.block.LayeredCauldronBlock.lowerFillLevel(state, level, pos);
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    };

    /** The three tier shulker box items. */
    public static List<net.minecraft.world.item.Item> items() {
        return List.of(com.simplebuilding.items.ModItems.REINFORCED_SHULKER_BOX, com.simplebuilding.items.ModItems.NETHERITE_SHULKER_BOX,
                com.simplebuilding.items.ModItems.ENDERITE_SHULKER_BOX);
    }

    /** Dispensers place the boxes like vanilla's (facing away from the dispenser). */
    public static void registerDispenserBehavior() {
        for (net.minecraft.world.item.Item item : items()) {
            net.minecraft.world.level.block.DispenserBlock.registerBehavior(item, new net.minecraft.core.dispenser.ShulkerBoxDispenseBehavior());
        }
    }

    // =====================================================================================
    // UPGRADE IN THE WORLD
    // =====================================================================================

    /**
     * Turns the shulker box at {@code pos} into {@code to} and keeps its contents (slot for slot, the
     * new tier has more slots and larger stacks), name, lock, facing and color. The old block entity
     * is emptied first so that nothing spills when the block changes.
     */
    public static void upgradeInPlace(ServerLevel level, BlockPos pos, Block to) {
        BlockState old = level.getBlockState(pos);
        DyeColor color = colorAt(level, pos, old);
        List<ItemStack> items = new ArrayList<>();
        DataComponentMap.Builder kept = DataComponentMap.builder();
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof BaseContainerBlockEntity container) {
            DataComponentMap components = container.collectComponents();
            kept.set(DataComponents.CUSTOM_NAME, components.get(DataComponents.CUSTOM_NAME));
            if (components.has(DataComponents.LOCK)) {
                kept.set(DataComponents.LOCK, components.get(DataComponents.LOCK));
            }
            for (int i = 0; i < container.getContainerSize(); i++) {
                items.add(container.getItem(i));
            }
            container.clearContent();
        }
        if (color != null) {
            kept.set(DataComponents.BASE_COLOR, color);
        }
        level.setBlock(pos, to.withPropertiesOf(old), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof TieredShulkerBoxBlockEntity box) {
            box.applyComponents(kept.build(), DataComponentPatch.EMPTY);
            box.receiveUpgradedContents(items);
            box.setColor(color);
        } else {
            for (ItemStack stack : items) {
                Block.popResource(level, pos, stack);
            }
        }
        level.updateNeighbourForOutputSignal(pos, to);
        SledgehammerProgress.clear(level, pos);
    }
}
