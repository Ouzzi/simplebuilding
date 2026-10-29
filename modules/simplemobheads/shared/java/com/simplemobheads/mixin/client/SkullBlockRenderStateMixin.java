package com.simplemobheads.mixin.client;

import com.simplemobheads.client.HelmetState;
import net.minecraft.client.renderer.blockentity.state.SkullBlockRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** BS28b-1: the render state of a placed head also carries the helmet it wears. */
@Mixin(SkullBlockRenderState.class)
public abstract class SkullBlockRenderStateMixin implements HelmetState {
    @Unique
    private ItemStack simplemobheads$helmet = ItemStack.EMPTY;
    @Unique
    private float simplemobheads$helmetSize;

    @Override
    public ItemStack simplemobheads$helmet() {
        return this.simplemobheads$helmet;
    }

    @Override
    public float simplemobheads$helmetSize() {
        return this.simplemobheads$helmetSize;
    }

    @Override
    public void simplemobheads$setHelmet(ItemStack helmet, float size) {
        this.simplemobheads$helmet = helmet;
        this.simplemobheads$helmetSize = size;
    }
}
