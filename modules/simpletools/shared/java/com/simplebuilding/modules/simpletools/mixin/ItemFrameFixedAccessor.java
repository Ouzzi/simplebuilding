package com.simplebuilding.modules.simpletools.mixin;

import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Vanilla's {@code Fixed} item frame flag (saved as {@code Fixed}, otherwise only settable by command). */
@Mixin(ItemFrame.class)
public interface ItemFrameFixedAccessor {
    @Accessor("fixed")
    boolean simpletools$fixed();

    @Accessor("fixed")
    void simpletools$setFixed(boolean fixed);
}
