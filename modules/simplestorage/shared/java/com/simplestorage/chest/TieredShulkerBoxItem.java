package com.simplestorage.chest;

import com.simplestorage.registry.StorageComponents;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;

/**
 * The item of a tier shulker box. A plain {@link BlockItem} - it never fits into another container
 * item, because its block is a {@code ShulkerBoxBlock} - except when the item entity is destroyed
 * (cactus, lava, explosion): vanilla spills {@code minecraft:container}, which holds at most 99 per
 * slot; this spills the real counts, split into normal stacks.
 */
public class TieredShulkerBoxItem extends BlockItem {

    public TieredShulkerBoxItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void onDestroyed(ItemEntity entity) {
        ItemStack stack = entity.getItem();
        List<ItemStack> contents = TieredShulkerBoxes.contentsOf(stack);
        stack.set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        stack.remove(StorageComponents.CONTAINER_COUNTS);
        ItemUtils.onContainerDestroyed(entity, TieredShulkerBoxes.splitToNormalStacks(contents).stream());
    }

    /** Slots, from Netherite on the stack factor, and the dye color. */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<net.minecraft.network.chat.Component> builder, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        TieredShulkerBoxes.tooltip(stack).forEach(builder);
    }
}
