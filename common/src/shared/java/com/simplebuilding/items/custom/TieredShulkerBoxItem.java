package com.simplebuilding.items.custom;

import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.util.TieredShulkerBoxes;
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
        stack.remove(ModDataComponentTypes.CONTAINER_COUNTS);
        ItemUtils.onContainerDestroyed(entity, TieredShulkerBoxes.splitToNormalStacks(contents).stream());
    }
}
