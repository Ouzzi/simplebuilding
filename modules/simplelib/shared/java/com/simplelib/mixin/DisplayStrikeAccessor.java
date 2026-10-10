package com.simplelib.mixin;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Size and placement of the floating display entities (used by {@code InWorldStrikes}). */
@Mixin(Display.class)
public interface DisplayStrikeAccessor {
    @Invoker("setTransformation")
    void simplelib$setTransformation(Transformation value);
}
