package com.simplebuilding.mixin;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.TrimEffectUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float simplebuilding$modifyDamageAmount(float amount, ServerWorld world, DamageSource source) {
        LivingEntity entity = (LivingEntity) (Object) this;
        // Ruft die große Logic-Methode in TrimEffectUtil auf
        return TrimEffectUtil.modifyDamage(entity, amount, source);
    }

    // --- SWIM SPEED (Tide) ---
    @Inject(method = "getMovementSpeed", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$modifySwimSpeed(CallbackInfoReturnable<Float> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // Nur wenn wir im Wasser sind und schwimmen
        if (entity.isSwimming()) {
             float mult = TrimEffectUtil.getSwimSpeedMultiplier(entity);
             if (mult > 1.0f) {
                 cir.setReturnValue(cir.getReturnValue() * mult);
             }
        }
    }



    @Inject(method = "getNextAirUnderwater", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$modifyAir(int air, CallbackInfoReturnable<Integer> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;

        float chance = TrimEffectUtil.getAirSaveChance(entity);

        if (chance > 0) {
            if (entity.getRandom().nextFloat() < chance) {
                cir.setReturnValue(air); // Luftstand behalten (kein Verbrauch)
            }
        }
    }

    // --- RIB TRIM ---
    @Inject(method = "tick", at = @At("TAIL"))
    private void simplebuilding$tickEffects(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!entity.getEntityWorld().isClient() && entity.age % 20 == 0) {
            if (entity.hasStatusEffect(StatusEffects.WITHER)) {
                int reduction = TrimEffectUtil.getWitherReductionAmount(entity);
                if (reduction > 0) {
                    StatusEffectInstance effect = entity.getStatusEffect(StatusEffects.WITHER);
                    // Wenn Restzeit klein ist, entfernen
                    if (effect != null && effect.getDuration() <= reduction) {
                        entity.removeStatusEffect(StatusEffects.WITHER);
                    } else if (effect != null) {
                        // Leider kann man Duration nicht einfach setzen ohne Accessor.
                        // Workaround: Wir heilen den Wither-Schaden einfach gegen.
                        // Da Wither alle 2 Sek Schaden macht (bei Level 1), heilen wir leicht.
                        entity.heal(0.5f); // Halbes Herz gegenheilen
                    }
                }
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void simplebuilding$materialTickEffects(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (!entity.getEntityWorld().isClient() && entity.age % 200 == 0) { // Alle 10 Sekunden

            float chance = TrimEffectUtil.getAmethystHealChance(entity);

            if (chance > 0 && entity.getHealth() < entity.getMaxHealth()) {
                if (entity.getRandom().nextFloat() < chance) {
                    entity.heal(1.0f);
                }
            }
        }
    }

    // 6. SILENCE TRIM (Stealth / Sichtbarkeit) - KORRIGIERT
    // Wir nutzen "getAttackDistanceScalingFactor", da "getVisibilityTo" nicht existiert.
    // Diese Methode berechnet Faktoren wie Sneaking (0.8) oder MobHeads (0.5).
    // Wir multiplizieren unseren Stealth-Faktor dazu.
    @Inject(method = "getAttackDistanceScalingFactor", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$modifyVisibility(Entity observer, CallbackInfoReturnable<Double> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        float mult = TrimEffectUtil.getStealthMultiplier(entity);

        // Wenn Stealth aktiv ist (Multiplikator < 1.0), verringern wir den Faktor weiter
        if (mult < 1.0f) {
            cir.setReturnValue(cir.getReturnValue() * mult);
        }
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void modifyVoidDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.isOf(DamageTypes.OUT_OF_WORLD) && (Object) this instanceof PlayerEntity player) {

            int enderitePieces = 0;
            // FIX: Nutze getInventory().armor statt getArmorItems() (da getArmorItems Iterable ist, inventory Liste)
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
                ItemStack stack = player.getEquippedStack(slot);
                if (isEnderiteArmor(stack.getItem())) {
                    enderitePieces++;
                }
            }

            if (enderitePieces > 0) {
                int damageInterval = 10;
                if (enderitePieces == 1) damageInterval = 20;
                if (enderitePieces == 2) damageInterval = 40;
                if (enderitePieces == 3) damageInterval = 60;
                if (enderitePieces == 4) damageInterval = 100;

                if (player.age % damageInterval != 0) {
                    cir.setReturnValue(false);
                }
            }
        }
    }

    @Unique
    private boolean isEnderiteArmor(Item item) {
        return item == ModItems.ENDERITE_BOOTS || item == ModItems.ENDERITE_LEGGINGS
                || item == ModItems.ENDERITE_CHESTPLATE || item == ModItems.ENDERITE_HELMET;
    }
}