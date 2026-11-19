package com.simpleslabs.item;

import com.simpleslabs.block.SideHalfSlabBlock;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Side half slab item (N86S, owner 19.11.): places {@link SideHalfSlabBlock} against the clicked block face, and the same item
 * on its open face adds a layer (thickness 1 to 4). Waxed copper shows the same grey line as the half slab.
 */
public class SideHalfSlabItem extends BlockItem {
    public SideHalfSlabItem(SideHalfSlabBlock block, Item.Properties properties) {
        super(block, properties);
    }

    public SideHalfSlabBlock sideHalfSlab() {
        return (SideHalfSlabBlock) getBlock();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        HalfSlabItem.waxedLine(getBlock(), lines);
    }
}
