package com.simplebuilding.tweaks.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simplebuilding.tweaks.heads.HeadAbilities;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.FarmlandBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Breezekopf ({@link HeadAbilities#tramplesFarmland}), Vanilla- und Fabric-Weg: Vanilla zertrampelt Ackerland nur,
 * wenn Breite x Breite x Hoehe des Fallenden ueber 0,512 liegt (kleine Tiere tun es nicht). Fuer den Traeger zaehlt
 * die Hoehe hier als 0 - dieselbe Stelle auf 26.2 ({@code turnToDirt}) und 26.3 ({@code turnToBaseBlock}); der
 * Fallschaden danach bleibt unberuehrt. NeoForge (und Forge) verlegen die Pruefung nach {@code Entity#canTrample}
 * und feuern ein Ereignis - dort haengt {@code TweaksNeoForge#onFarmlandTrample}; darum {@code require = 0}.
 */
@Mixin(FarmlandBlock.class)
public abstract class HeadAbilityFarmlandMixin {

    @ModifyExpressionValue(method = "fallOn", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getBbHeight()F"))
    private float simplebuilding$breezeHeadDoesNotTrample(float height, @Local(argsOnly = true) Entity entity) {
        return HeadAbilities.tramplesFarmland(entity) ? height : 0.0F;
    }
}
