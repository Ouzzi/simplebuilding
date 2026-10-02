package com.simplebuilding.mixin.client;

import net.minecraft.client.gui.screens.recipebook.GhostSlots;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Geisterslots fuer die eigenen Rezeptbuecher (Befiederungs- und Schmiedetisch): Vanilla haelt die Setter paketintern. */
@Mixin(GhostSlots.class)
public interface GhostSlotsInvoker {
    @Invoker("setInput")
    void simplebuilding$setInput(Slot slot, ContextMap context, SlotDisplay contents);

    @Invoker("setResult")
    void simplebuilding$setResult(Slot slot, ContextMap context, SlotDisplay contents);
}
