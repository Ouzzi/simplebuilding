package com.simplebuilding.mixin;

import com.simplebuilding.dummy.TrainingDummy;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Trainingspuppe (2026-10-02): Vanilla setzt die Schlagstaerke vor dem Treffer zurueck; die Puppe braucht sie, um einen
 * kritischen Treffer zu erkennen. Nur lesen, nichts aendern.
 */
@Mixin(Player.class)
public abstract class PlayerAttackDummyMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void simplebuilding$noteDummyAttack(Entity target, CallbackInfo ci) {
        if (target instanceof TrainingDummy dummy && !target.level().isClientSide()) {
            Player self = (Player) (Object) this;
            dummy.noteAttack(self, self.getAttackStrengthScale(0.5F));
        }
    }
}
