package com.simplearrows.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * N64: the fletching table screen grows by the container margin ({@code FletchingMargins}) but extends
 * {@code AbstractRecipeBookScreen}, which has no constructor with an image height (pattern of simplestorage's
 * {@code ContainerScreenHeightAccessor}).
 */
@Mixin(AbstractContainerScreen.class)
public interface ScreenHeightAccessor {
    @Mutable
    @Accessor("imageHeight")
    void simplearrows$setImageHeight(int height);
}
