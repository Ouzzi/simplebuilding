package com.simplemobheads.mixin;

import com.simplemobheads.easter.Technoblade;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.pig.Pig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Once a second (EH21): a pig named Technoblade gets its crown or loses it with the name (TO-1); a cow whose lucky hay bale
 * came in a low barn grows into a giant cow as soon as it has room (CO-2).
 */
@Mixin(Animal.class)
public abstract class AnimalAiMixin {
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void simplemobheads$technobladeCrown(ServerLevel level, CallbackInfo ci) {
        if ((Object) this instanceof Pig pig) {
            Technoblade.tick(level, pig);
        } else if ((Object) this instanceof net.minecraft.world.entity.animal.cow.Cow cow) {
            com.simplemobheads.easter.Giants.tick(level, cow);
        }
    }
}
