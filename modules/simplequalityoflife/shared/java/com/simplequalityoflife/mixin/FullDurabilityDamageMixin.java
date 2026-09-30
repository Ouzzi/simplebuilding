package com.simplequalityoflife.mixin;

import com.simplequalityoflife.Simplequalityoflife;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class FullDurabilityDamageMixin {

    @Inject(method = "getAttributeValue", at = @At("RETURN"), cancellable = true)
    private void modifyAttackDamage(Holder<Attribute> attribute, CallbackInfoReturnable<Double> info) {
        if (attribute != Attributes.ATTACK_DAMAGE) return;

        if (!((Object) this instanceof Player player)) return;

        if (!Simplequalityoflife.configFor(player.level()).qOL.enableFullDurabilityBonus) return;

        ItemStack stack = player.getMainHandItem();

        if (stack.isEmpty() || !stack.isDamageableItem()) return;

        float maxDamage = stack.getMaxDamage();
        float currentDamage = stack.getDamageValue();
        float durabilityPercent = (maxDamage - currentDamage) / maxDamage;

        if (durabilityPercent >= Simplequalityoflife.configFor(player.level()).qOL.fullDurabilityThreshold) {
            double originalDamage = info.getReturnValue();
            info.setReturnValue(originalDamage * Simplequalityoflife.configFor(player.level()).qOL.fullDurabilityBonusMultiplier);
        }
    }
}
