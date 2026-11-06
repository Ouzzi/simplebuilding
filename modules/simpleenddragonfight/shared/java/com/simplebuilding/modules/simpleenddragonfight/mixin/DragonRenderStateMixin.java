package com.simplebuilding.modules.simpleenddragonfight.mixin;

import com.simplebuilding.modules.simpleenddragonfight.DragonTinted;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import org.spongepowered.asm.mixin.Mixin;

/** Client only: carries the "chorus colour" flag from the dragon to the renderer. */
@Mixin(EnderDragonRenderState.class)
public abstract class DragonRenderStateMixin implements DragonTinted {
    private boolean simpleenddragonfight$tint;

    @Override
    public boolean simpleenddragonfight$tinted() { return simpleenddragonfight$tint; }

    @Override
    public void simpleenddragonfight$setTinted(boolean tinted) { simpleenddragonfight$tint = tinted; }
}
