package com.simplebeacons.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The Strong Beacon screen reports its full width (Vanilla image + extra panel) so recipe viewers (JEI/REI) keep their
 * item lists off the panel; Vanilla's code paths that draw the 230 px image still see 230.
 */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenWidthAccessor {
    @Mutable
    @Accessor("imageWidth")
    void simplebeacons$setImageWidth(int width);
}
