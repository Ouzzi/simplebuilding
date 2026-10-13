package com.simplemobheads;

import com.simplemobheads.data.HeadDef;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;

/**
 * A mob head item: standing on the floor, hanging on a wall, worn on the head. A variant head names its variant after the
 * head ("Cat Head (Siamese)", owner question 10); the creative tab shows every variant.
 */
public final class MobHeadItem extends StandingAndWallBlockItem {
    private final HeadDef head;

    public MobHeadItem(HeadDef head, Block standing, Block wall, Properties properties) {
        super(standing, wall, Direction.DOWN, properties);
        this.head = head;
    }

    public HeadDef head() {
        return head;
    }

    @Override
    public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        String variant = stack.get(MobHeads.VARIANT);
        if (variant == null || variant.isEmpty() || !head.variantIds().contains(variant)) {
            return base;
        }
        return Component.translatable("simplemobheads.head_with_variant", base,
                Component.translatable("variant.simplemobheads." + head.id() + "." + variant));
    }
}
