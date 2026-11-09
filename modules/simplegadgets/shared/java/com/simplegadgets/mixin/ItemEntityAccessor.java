package com.simplegadgets.mixin;

import java.util.UUID;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The Attractor's filter (G3) reads Vanilla's pickup delay and reserved owner of a loose item. */
@Mixin(ItemEntity.class)
public interface ItemEntityAccessor {
    @Accessor("pickupDelay")
    int simplegadgets$pickupDelay();

    @Accessor("target")
    UUID simplegadgets$target();
}
