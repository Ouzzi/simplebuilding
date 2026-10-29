package com.simplemobheads.client;

import net.minecraft.world.item.ItemStack;

/** BS28b-1: the helmet of a placed head and its size, carried on {@code SkullBlockRenderState} from extraction to drawing. */
public interface HelmetState {
    ItemStack simplemobheads$helmet();

    float simplemobheads$helmetSize();

    void simplemobheads$setHelmet(ItemStack helmet, float size);
}
