package com.simplelib.mixin;

import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The item of a floating partial result (used by {@code InWorldStrikes}). */
@Mixin(Display.ItemDisplay.class)
public interface ItemDisplayStrikeAccessor {
    @Invoker("setItemStack")
    void simplelib$setItemStack(ItemStack value);
}
