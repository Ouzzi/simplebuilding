package com.simplemaps.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Updates the framed stack without {@code setItem}'s add sound (saving the framed view, Feature 4) and reads Vanilla's
 * "Fixed" flag (a fixed frame takes no interaction, so its view cannot be saved either).
 */
@Mixin(ItemFrame.class)
public interface ItemFrameAccessor {
    @Accessor("DATA_ITEM")
    static EntityDataAccessor<ItemStack> simplemaps$dataItem() {
        throw new AssertionError();
    }

    @Accessor("fixed")
    boolean simplemaps$fixed();

    @Accessor("fixed")
    void simplemaps$setFixed(boolean fixed);
}
