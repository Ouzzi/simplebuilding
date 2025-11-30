package com.simplebuilding.mixin.client;

import com.simplebuilding.util.IEnchantableRenderState;
import net.minecraft.client.render.block.entity.state.ShulkerBoxBlockEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ShulkerBoxBlockEntityRenderState.class)
public class ShulkerBoxBlockEntityRenderStateMixin implements IEnchantableRenderState {
    @Unique boolean isEnchanted = false;

    @Override
    public void simplebuilding$setEnchanted(boolean enchanted) {
        this.isEnchanted = enchanted;
    }

    @Override
    public boolean simplebuilding$isEnchanted() {
        return this.isEnchanted;
    }
}