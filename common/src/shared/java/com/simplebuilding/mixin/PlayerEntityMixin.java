package com.simplebuilding.mixin;

import com.simplebuilding.util.ISpaceKeyTracker;
import com.simplebuilding.util.TrimBenefitUser;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    // Die Besatz-Faehigkeiten der Spieler (Stase/Astralit, Nihilith, Gleiten, Sprint-Hunger, Erfahrung) wirken auf 26.3 in
    // Simple Mobs (ZO9 A / MOB27-3, docs/ai/PLAN-B-TRIMMOVE01-2026-11-03.md), auf 26.2 in PlayerTrimMixin. Der Spieler-Schalter
    // (TrimBenefitUser) bleibt hier: der Kern gibt ihn als Gastgeber (TrimHost) an Simple Mobs weiter.
}
