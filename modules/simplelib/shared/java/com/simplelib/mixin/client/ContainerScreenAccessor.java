package com.simplelib.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Panel position of a container screen, for the dev variants panel beside the inventory. */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
    @Accessor("leftPos")
    int simplelib$leftPos();

    @Accessor("topPos")
    int simplelib$topPos();

    @Accessor("imageWidth")
    int simplelib$imageWidth();
}
