package com.simplebuilding.mixin;

import com.simplebuilding.config.ServerTuning;
import com.simplebuilding.dummy.SmallArmorStand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ruestungsstaender mit Armen (Besitzer 2026-10-08, docs/ai/PLAN-STAENDER-2026-10-09.md): ein vom Spieler aufgestellter
 * Staender (Item, Grund {@code SPAWN_ITEM_USE}) oder einer aus dem Spender ({@code DISPENSER}) hat Arme, Schalter
 * {@code server.features.armorStandArms}. Gesetzt direkt nach dem Erzeugen, also vor den Entity-Daten des Items, die
 * {@code ShowArms} noch ausdruecklich setzen koennen. Gerufene, geladene und per Code erzeugte Staender bleiben Vanilla.
 * Gilt auch fuer Stroh-Staender und Trainingspuppe, nicht fuer die armlosen mittleren/kleinen Staender.
 */
@Mixin(EntityType.class)
public abstract class EntityTypeArmorStandArmsMixin {
    @Inject(method = "create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnReason;)Lnet/minecraft/world/entity/Entity;",
            at = @At("RETURN"))
    private void simplebuilding$armsForPlacedStands(Level level, EntitySpawnReason reason, CallbackInfoReturnable<Entity> cir) {
        if (cir.getReturnValue() instanceof ArmorStand stand && !(stand instanceof SmallArmorStand) && !level.isClientSide()
                && (reason == EntitySpawnReason.SPAWN_ITEM_USE || reason == EntitySpawnReason.DISPENSER)
                && ServerTuning.get().features.armorStandArms) {
            stand.setShowArms(true);
        }
    }
}
