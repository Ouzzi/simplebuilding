package com.simplebeacons.mixin.client;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The Strong Beacon screen moves the payment and inventory slots down by the two extra effect rows (client only). */
@Mixin(Slot.class)
public interface SlotYAccessor {
    @Mutable
    @Accessor("y")
    void simplebeacons$setY(int y);
}
