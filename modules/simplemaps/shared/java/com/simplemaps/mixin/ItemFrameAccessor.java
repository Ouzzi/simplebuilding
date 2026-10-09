package com.simplemaps.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Updates the framed stack without {@code setItem}'s add sound (scrolling the framed view, Feature 4). */
@Mixin(ItemFrame.class)
public interface ItemFrameAccessor {
    @Accessor("DATA_ITEM")
    static EntityDataAccessor<ItemStack> simplemaps$dataItem() {
        throw new AssertionError();
    }
}
