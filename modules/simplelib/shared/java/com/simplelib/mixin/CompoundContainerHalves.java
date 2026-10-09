package com.simplelib.mixin;

import com.simplelib.api.StackLimits;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Lets {@link StackLimits#limit} ask a double chest's first half for its raised stack limit. */
@Mixin(CompoundContainer.class)
public abstract class CompoundContainerHalves implements StackLimits.Halves {
    @Shadow @Final private Container container1;

    @Override
    public Container simplelib$first() {
        return container1;
    }
}
