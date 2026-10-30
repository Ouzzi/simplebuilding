package com.simplemobheads.mixin.client;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** IG31-10: the display context an item is being drawn in (package-private in Vanilla). */
@Mixin(ItemStackRenderState.class)
public interface ItemStackRenderStateAccessor {
    @Accessor("displayContext")
    ItemDisplayContext simplemobheads$displayContext();
}
