package com.simplebuilding.modules.simpleenddragonfight.mixin;

import com.simplebuilding.modules.simpleenddragonfight.DragonTinted;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import org.spongepowered.asm.mixin.Mixin;

/** Client only: carries the "chorus colour" flag from the dragon to the renderer. */
@Mixin(EnderDragonRenderState.class)
public abstract class DragonRenderStateMixin implements DragonTinted {
    private int simpleenddragonfight$tint;

    @Override
    public int simpleenddragonfight$tintLevel() { return simpleenddragonfight$tint; }

    @Override
    public void simpleenddragonfight$setTintLevel(int level) { simpleenddragonfight$tint = level; }
}
