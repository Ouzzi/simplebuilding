package com.simplemobheads.mixin;

import com.simplemobheads.easter.Technoblade;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.pig.Pig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Technoblade (EH21 TO-1): once a second a pig named Technoblade gets its crown (or loses it with the name). */
@Mixin(Animal.class)
public abstract class AnimalAiMixin {
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void simplemobheads$technobladeCrown(ServerLevel level, CallbackInfo ci) {
        if ((Object) this instanceof Pig pig) {
            Technoblade.tick(level, pig);
        }
    }
}
