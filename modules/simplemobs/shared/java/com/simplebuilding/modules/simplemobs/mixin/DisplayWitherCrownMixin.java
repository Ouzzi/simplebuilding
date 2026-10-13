package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.BlightedPattern;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A helmet sitting on a wither skull (Blighted Wither summoning) drops when its skull is gone. */
@Mixin(Display.class)
public abstract class DisplayWitherCrownMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void simplemobs$crownTick(CallbackInfo ci) {
        if ((Object) this instanceof Display.ItemDisplay d && d.tickCount % 10 == 0 && d.isAlive()
                && d.level() instanceof ServerLevel level && d.entityTags().contains(BlightedPattern.CROWN_TAG)) {
            BlightedPattern.tickCrown(level, d);
        }
    }
}
