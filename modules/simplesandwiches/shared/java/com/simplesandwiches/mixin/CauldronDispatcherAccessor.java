package com.simplesandwiches.mixin;

import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** {@code Dispatcher#put(Item, ...)} is package-private; used once to add milk to the empty cauldron. */
@Mixin(CauldronInteraction.Dispatcher.class)
public interface CauldronDispatcherAccessor {
    @Invoker("put")
    void simplesandwiches$put(Item item, CauldronInteraction interaction);
}
