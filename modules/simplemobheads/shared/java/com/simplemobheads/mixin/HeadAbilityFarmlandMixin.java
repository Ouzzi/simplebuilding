package com.simplemobheads.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simplemobheads.ability.HeadAbilities;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.FarmlandBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Breeze head, Vanilla/Fabric way: Vanilla tramples farmland only when width x width x height of the faller exceeds 0.512;
 * for the wearer the height counts as 0. NeoForge and Forge move the check into {@code Entity#canTrample} and fire an event
 * (their loader entries cancel it), hence {@code require = 0}.
 */
@Mixin(FarmlandBlock.class)
public abstract class HeadAbilityFarmlandMixin {
    @ModifyExpressionValue(method = "fallOn", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getBbHeight()F"))
    private float simplemobheads$breezeHeadDoesNotTrample(float height, @Local(argsOnly = true) Entity entity) {
        return HeadAbilities.tramplesFarmland(entity) ? height : 0.0F;
    }
}
