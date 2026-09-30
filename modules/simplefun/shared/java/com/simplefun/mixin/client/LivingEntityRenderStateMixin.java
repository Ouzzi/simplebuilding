package com.simplefun.mixin.client;

import com.simplefun.client.PiggyStateExtension;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements PiggyStateExtension {

    @Unique
    private boolean simplefun$piggy = false;

    @Override
    public void simplefun$setPiggy(boolean isPiggy) {
        this.simplefun$piggy = isPiggy;
    }

    @Override
    public boolean simplefun$isPiggy() {
        return this.simplefun$piggy;
    }
}
