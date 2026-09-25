package com.simplebuilding.mixin;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.TrimEffectUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.simplebuilding.util.DynamicLightHandler;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.core.BlockPos;
import com.simplebuilding.util.TrimAttributeHandler;
import com.simplebuilding.util.OwnedLightHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements OwnedLightHolder {

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float simplebuilding$modifyDamageAmount(float amount, ServerLevel world, DamageSource source) {
        LivingEntity entity = (LivingEntity) (Object) this;
        // Ruft die große Logic-Methode in TrimEffectUtil auf
        return TrimEffectUtil.modifyDamage(entity, amount, source);
    }

    // Schwimmen (Tide) ist seit 2026-09 ein Modifikator auf water_movement_efficiency, siehe
    // TrimAttributeHandler - der getSpeed-Eingriff wirkte ohne Wassertritt ohnehin kaum.

    @Inject(method = "decreaseAirSupply", at = @At("HEAD"), cancellable = true)
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
        if (!entity.level().isClientSide() && entity.tickCount % 20 == 0) {
            if (entity.hasEffect(MobEffects.WITHER)) {
                int reduction = TrimEffectUtil.getWitherReductionAmount(entity);
                if (reduction > 0) {
                    MobEffectInstance effect = entity.getEffect(MobEffects.WITHER);
                    // Wenn Restzeit klein ist, entfernen
                    if (effect != null && effect.getDuration() <= reduction) {
                        entity.removeEffect(MobEffects.WITHER);
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

        if (!entity.level().isClientSide() && entity.tickCount % 200 == 0) { // Alle 10 Sekunden

            float chance = TrimEffectUtil.getAmethystHealChance(entity);

            if (chance > 0 && entity.getHealth() < entity.getMaxHealth()) {
                if (entity.getRandom().nextFloat() < chance) {
                    entity.heal(1.0f);
                }
            }
        }
    }

    // --- RADIANCE (emittierende Ruestung) und Besatz-Attribute ---
    // Licht: jeder Traeger ausser Spielern (die haben ihren eigenen Pfad im DynamicLightHandler),
    // also Ruestungsstaender und Mobs; Schimmer clientseitig an jedem Traeger.
    @Inject(method = "tick", at = @At("TAIL"))
    private void simplebuilding$radianceTick(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide()) {
            DynamicLightHandler.tickGlowMotes(entity, () -> DynamicLightHandler.wornEmission(entity));
        } else {
            DynamicLightHandler.tickWearer(entity);
            TrimAttributeHandler.tick(entity);
        }
    }

    // Vom Traeger gesetzter Lichtblock (Ruestungsstaender, Mobs), mit der Entity gespeichert.
    @Unique
    private @Nullable BlockPos simplebuilding$ownedLight;

    @Override
    public @Nullable BlockPos simplebuilding$getOwnedLight() {
        return this.simplebuilding$ownedLight;
    }

    @Override
    public void simplebuilding$setOwnedLight(@Nullable BlockPos pos) {
        this.simplebuilding$ownedLight = pos;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void simplebuilding$writeOwnedLight(ValueOutput output, CallbackInfo ci) {
        output.storeNullable("SimpleBuildingOwnedLight", BlockPos.CODEC, this.simplebuilding$ownedLight);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void simplebuilding$readOwnedLight(ValueInput input, CallbackInfo ci) {
        this.simplebuilding$ownedLight = input.read("SimpleBuildingOwnedLight", BlockPos.CODEC).orElse(null);
    }

    // 6. SILENCE TRIM (Stealth / Sichtbarkeit) - KORRIGIERT
    // Wir nutzen "getAttackDistanceScalingFactor", da "getVisibilityTo" nicht existiert.
    // Diese Methode berechnet Faktoren wie Sneaking (0.8) oder MobHeads (0.5).
    // Wir multiplizieren unseren Stealth-Faktor dazu.
    @Inject(method = "getVisibilityPercent", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$modifyVisibility(Entity observer, CallbackInfoReturnable<Double> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        float mult = TrimEffectUtil.getStealthMultiplier(entity);

        // Wenn Stealth aktiv ist (Multiplikator < 1.0), verringern wir den Faktor weiter
        if (mult < 1.0f) {
            cir.setReturnValue(cir.getReturnValue() * mult);
        }
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void modifyVoidDamage(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD) && (Object) this instanceof Player player) {

            int enderitePieces = 0;
            // FIX: Nutze getInventory().armor statt getArmorItems() (da getArmorItems Iterable ist, inventory Liste)
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
                ItemStack stack = player.getItemBySlot(slot);
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

                if (player.tickCount % damageInterval != 0) {
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