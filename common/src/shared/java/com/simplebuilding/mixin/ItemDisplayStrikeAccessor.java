package com.simplebuilding.mixin;

import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Schrittweise Ergebnisse (2026-10-06, {@code InWorldStrikes}): das Item der schwebenden Teil-Anzeige. */
@Mixin(Display.ItemDisplay.class)
public interface ItemDisplayStrikeAccessor {
    @Invoker("setItemStack")
    void simplebuilding$setItemStack(ItemStack stack);
}
