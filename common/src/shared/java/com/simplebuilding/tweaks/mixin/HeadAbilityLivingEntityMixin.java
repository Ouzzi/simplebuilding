package com.simplebuilding.tweaks.mixin;

import com.simplebuilding.tweaks.heads.HeadAbilities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mob-Koepfe, die vor einer Schadensart schuetzen ({@link HeadAbilities#ignoresDamage}): Lohenkopf gegen Magmabloecke
 * und Lagerfeuer, Endermankopf gegen den Enderperlen-Teleport. Spieler rufen {@code super.isInvulnerableTo}
 * zuerst, landen also auch hier.
 */
@Mixin(LivingEntity.class)
public abstract class HeadAbilityLivingEntityMixin {

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$headIgnoresDamage(ServerLevel level, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (HeadAbilities.ignoresDamage((LivingEntity) (Object) this, source)) {
            cir.setReturnValue(true);
        }
    }
}
