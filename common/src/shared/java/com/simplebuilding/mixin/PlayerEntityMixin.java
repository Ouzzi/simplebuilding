package com.simplebuilding.mixin;

import com.simplebuilding.util.ISpaceKeyTracker;
import com.simplebuilding.util.TrimBenefitUser;
import com.simplebuilding.util.TrimEffectUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity implements TrimBenefitUser, ISpaceKeyTracker {

    // Konstruktor ist notwendig, da wir von LivingEntity erben
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Unique private boolean simplebuilding$trimBenefitsEnabled = true;
    @Unique private boolean simplebuilding$spacePressed = false;
    @Shadow public abstract Inventory getInventory();

    @Override public boolean simplebuilding$areTrimBenefitsEnabled() { return this.simplebuilding$trimBenefitsEnabled; }
    @Override public void simplebuilding$setTrimBenefitsEnabled(boolean enabled) { this.simplebuilding$trimBenefitsEnabled = enabled; }
    @Override public boolean simplebuilding$isSpacePressed() { return this.simplebuilding$spacePressed; }
    @Override public void simplebuilding$setSpacePressed(boolean pressed) { this.simplebuilding$spacePressed = pressed; }

    /** The death loot drops between these two ({@link com.simplebuilding.util.AttractorFilter}). */
    @Inject(method = "dropEquipment", at = @At("HEAD"))
    private void simplebuilding$beginDeathDrops(net.minecraft.server.level.ServerLevel level, CallbackInfo ci) {
        com.simplebuilding.util.AttractorFilter.beginDeathDrops(this);
    }

    @Inject(method = "dropEquipment", at = @At("RETURN"))
    private void simplebuilding$endDeathDrops(net.minecraft.server.level.ServerLevel level, CallbackInfo ci) {
        com.simplebuilding.util.AttractorFilter.endDeathDrops();
    }

    // --- TICK LOGIK ---
    @Inject(method = "tick", at = @At("TAIL"))
    private void simplebuilding$tickLogic(CallbackInfo ci) {
        Player player = (Player) (Object) this;

        // 1. Trim Effekte (Stasis, Astralit Jump Boost)
        TrimEffectUtil.tick(player);

        // 2. Nihilith Gravity (Client & Server für prediction)
        TrimEffectUtil.handleNihilithGravity(player);

        // 2b. Luftsprung: die Landung setzt die Abklingzeit NICHT mehr zurueck (Besitzer 2026-09-29),
        //     deshalb kein Aufruf mehr hier - AirJumpGuard prueft die volle Abklingzeit beim Paket.

        // 3. Enderit-Sinkdaempfer: EnderiteSinkDamperMixin (Schwerkraft und Fallweg beim schleichenden Fall).
    }

    // --- HUNGER / EXHAUSTION ---
    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float simplebuilding$reduceExhaustion(float exhaustion) {
        Player player = (Player) (Object) this;
        if (player.isSprinting()) {
            // Nutzt jetzt den zentralen Rechner mit Multiplikator
            float reductionPct = TrimEffectUtil.getExhaustionReduction(player);
            if (reductionPct > 0) {
                return exhaustion * (1.0f - reductionPct);
            }
        }
        return exhaustion;
    }

    // --- XP BOOST ---
    @ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true)
    private int simplebuilding$modifyXpGain(int experience) {
        Player player = (Player) (Object) this;
        if (experience <= 0) return experience;

        float multiplier = TrimEffectUtil.getXPMultiplier(player);
        if (multiplier > 1.0f) {
            return Math.round(experience * multiplier);
        }
        return experience;
    }

    // Glueck und Laufgeschwindigkeit (Host/Smaragd, Bolt/Redstone) sind seit 2026-09 echte
    // Vanilla-Attribut-Modifikatoren, siehe TrimAttributeHandler - keine getLuck/getSpeed-Eingriffe mehr.
}
