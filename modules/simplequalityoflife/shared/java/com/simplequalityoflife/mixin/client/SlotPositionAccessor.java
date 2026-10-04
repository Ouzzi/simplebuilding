package com.simplequalityoflife.mixin.client;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Client: the linked panel moves its slots (layout and scrolling). */
@Mixin(Slot.class)
public interface SlotPositionAccessor {
    @Mutable
    @Accessor("x")
    void qol$setX(int x);

    @Mutable
    @Accessor("y")
    void qol$setY(int y);
}
