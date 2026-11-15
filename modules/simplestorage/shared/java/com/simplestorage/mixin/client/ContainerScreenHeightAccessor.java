package com.simplestorage.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * N62: the Storage Crafting Table screen grows by the container margin (UiMargin) but extends {@code AbstractRecipeBookScreen},
 * which has no constructor with an image height - it sets its height here, in its constructor, before {@code init} centres it
 * (pattern of simplebeacons' {@code ContainerScreenHeightAccessor}).
 */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenHeightAccessor {
    @Mutable
    @Accessor("imageHeight")
    void simplestorage$setImageHeight(int height);
}
