package com.simplebuilding.mixin;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Schrittweise Ergebnisse (2026-10-06, {@code InWorldStrikes}): Groesse der schwebenden Teil-Anzeige. */
@Mixin(Display.class)
public interface DisplayStrikeAccessor {
    @Invoker("setTransformation")
    void simplebuilding$setTransformation(Transformation transformation);
}
