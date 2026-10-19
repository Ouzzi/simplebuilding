package com.simplestorage.mixin;

import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** {@code Dispatcher#put(Item, ...)} is package-private; used once to wash dyed tier shulker boxes in a water cauldron. */
@Mixin(CauldronInteraction.Dispatcher.class)
public interface CauldronDispatcherAccessor {
    @Invoker("put")
    void simplestorage$put(Item item, CauldronInteraction interaction);
}
