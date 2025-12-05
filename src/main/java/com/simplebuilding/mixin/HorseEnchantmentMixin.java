package com.simplebuilding.mixin;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class HorseEnchantmentMixin {

    @Shadow public abstract ItemStack getEquippedStack(EquipmentSlot slot);

    // 1. SCHUTZ (Protection)
    // LivingEntity.modifyAppliedDamage ist protected und wird von damage() aufgerufen.
    // Wir können hier injecten.
    @Inject(method = "modifyAppliedDamage", at = @At("RETURN"), cancellable = true)
    private void applyHorseProtection(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        // Nur für Pferde!
        if (!(self instanceof AbstractHorseEntity)) return;

        float currentDamage = cir.getReturnValue();
        if (currentDamage <= 0 || source.isSourceCreativePlayer()) return;

        // Rüstung holen (BODY Slot ist Standard für Pferderüstung)
        ItemStack armor = self.getEquippedStack(EquipmentSlot.BODY);
        if (armor.isEmpty()) return;

        var registry = self.getEntityWorld().getRegistryManager();
        var lookup = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var protectionKey = lookup.getOptional(Enchantments.PROTECTION);

        if (protectionKey.isPresent()) {
            int level = EnchantmentHelper.getLevel(protectionKey.get(), armor);
            if (level > 0) {
                // 4% Reduktion pro Level
                float reduction = level * 0.04f;
                float newDamage = currentDamage * (1.0f - reduction);
                cir.setReturnValue(newDamage);
            }
        }
    }

    // 2. FEDERFALL (Feather Falling)
    @Inject(method = "computeFallDamage", at = @At("RETURN"), cancellable = true)
    private void applyHorseFeatherFalling(double fallDistance, float damagePerDistance, CallbackInfoReturnable<Integer> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        // Nur für Pferde!
        if (!(self instanceof AbstractHorseEntity)) return;

        int originalDamage = cir.getReturnValue();
        if (originalDamage <= 0) return;

        ItemStack armor = self.getEquippedStack(EquipmentSlot.BODY);
        if (armor.isEmpty()) return;

        var registry = self.getEntityWorld().getRegistryManager();
        var lookup = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var featherKey = lookup.getOptional(Enchantments.FEATHER_FALLING);

        if (featherKey.isPresent()) {
            int level = EnchantmentHelper.getLevel(featherKey.get(), armor);
            if (level > 0) {
                // 12% Reduktion pro Level (Vanilla Logic approx.)
                float reduction = level * 0.12f;
                int newDamage = (int) (originalDamage * (1.0f - reduction));
                cir.setReturnValue(Math.max(0, newDamage));
            }
        }
    }
}