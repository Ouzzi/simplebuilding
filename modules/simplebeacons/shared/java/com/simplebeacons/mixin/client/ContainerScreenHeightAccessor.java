package com.simplebeacons.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The Strong Beacon screen is taller than Vanilla's 219 px image (two more effect rows): it reports its real height so
 * the screen centres it and recipe viewers (JEI/REI) keep clear; Vanilla's drawing of its own image still sees 219.
 */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenHeightAccessor {
    @Mutable
    @Accessor("imageHeight")
    void simplebeacons$setImageHeight(int height);
}
