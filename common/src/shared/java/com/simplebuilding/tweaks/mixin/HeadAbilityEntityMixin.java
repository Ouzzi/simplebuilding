package com.simplebuilding.tweaks.mixin;

import com.simplebuilding.tweaks.heads.HeadAbilities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ertrunkenenkopf ({@link HeadAbilities#resistsDownwardBubbles}): eine Magma-Blasensaeule zieht den Traeger nicht
 * nach unten - weder in der Saeule noch an ihrer Oberflaeche. Aufwaerts-Saeulen (Seelensand) wirken weiter.
 * Spieler rufen hier {@code super} (26.2 und 26.3).
 */
@Mixin(Entity.class)
public abstract class HeadAbilityEntityMixin {

    @Inject(method = "onInsideBubbleColumn", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$drownedHeadInside(boolean dragDown, CallbackInfo ci) {
        if (HeadAbilities.resistsDownwardBubbles((Entity) (Object) this, dragDown)) {
            ci.cancel();
        }
    }

    @Inject(method = "onAboveBubbleColumn", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$drownedHeadAbove(boolean dragDown, BlockPos pos, CallbackInfo ci) {
        if (HeadAbilities.resistsDownwardBubbles((Entity) (Object) this, dragDown)) {
            ci.cancel();
        }
    }
}
