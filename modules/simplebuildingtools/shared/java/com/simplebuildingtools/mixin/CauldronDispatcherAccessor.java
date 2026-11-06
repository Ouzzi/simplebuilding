package com.simplebuildingtools.mixin;

import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The water cauldron's item table is package-private: the octant wash ({@code OctantCauldronWash}) enters itself through this. */
@Mixin(CauldronInteraction.Dispatcher.class)
public interface CauldronDispatcherAccessor {
    @Invoker("put")
    void simplebuildingtools$put(Item item, CauldronInteraction interaction);
}
