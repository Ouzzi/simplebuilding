package com.simplebuilding.tweaks.spawn;

import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.component.TweaksComponents;
import com.simplebuilding.tweaks.item.TweaksItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Wann Fall- bzw. Kinetikschaden entfaellt (aus dem Schadens-Mixin herausgezogen, damit Tests ihn fragen koennen). */
public final class ElytraDamageRules {
    private ElytraDamageRules() {
    }

    public static boolean wearsSafeElytra(LivingEntity entity) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        return chest.is(TweaksItems.SPAWN_ELYTRA) && Boolean.TRUE.equals(chest.get(TweaksComponents.IS_SAFE_ELYTRA));
    }

    public static boolean preventsFallDamage(LivingEntity entity) {
        if (wearsSafeElytra(entity)) {
            return true;
        }
        if (LaunchSafety.consumeOnFall(entity)) {
            return true;
        }
        return entity instanceof ServerPlayer player
                && SimpleTweaks.config().spawn.disableFallDamageInSpawn
                && SimpleTweaks.config().spawn.giveElytraOnSpawn
                && inSpawnArea(player);
    }

    /** Fliegen gegen die Wand mit sicherer Spawn-Elytra. */
    public static boolean preventsDamage(LivingEntity entity, DamageSource source) {
        return source.is(DamageTypes.FLY_INTO_WALL) && wearsSafeElytra(entity);
    }

    /**
     * Fallschutz-Bereich = Elytra-Bereich ({@link SpawnElytra#insideSpawn}): dasselbe Quadrat, nur
     * in der Weltspawn-Dimension. Simple Tweaks nahm hier einen Kreis in jeder Dimension - die Ecken
     * gaben Elytren ohne Fallschutz, und Nether/End bei 0,0 schuetzten (Audit 2026-09-26 #4).
     */
    public static boolean inSpawnArea(ServerPlayer player) {
        return SpawnElytra.insideSpawn(player);
    }
}
