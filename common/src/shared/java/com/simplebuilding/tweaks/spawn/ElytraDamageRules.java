package com.simplebuilding.tweaks.spawn;

import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.component.TweaksComponents;
import com.simplebuilding.tweaks.item.TweaksItems;
import net.minecraft.core.BlockPos;
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
                && inSpawnCircle(player);
    }

    /** Fliegen gegen die Wand mit sicherer Spawn-Elytra. */
    public static boolean preventsDamage(LivingEntity entity, DamageSource source) {
        return source.is(DamageTypes.FLY_INTO_WALL) && wearsSafeElytra(entity);
    }

    /** Kreis (2D) um die Spawnmitte, wie im Schadens-Mixin von Simple Tweaks. */
    public static boolean inSpawnCircle(ServerPlayer player) {
        BlockPos center = SpawnElytra.center(player);
        BlockPos pos = player.blockPosition();
        double dx = pos.getX() - center.getX();
        double dz = pos.getZ() - center.getZ();
        int radius = SimpleTweaks.config().spawn.spawnElytraRadius;
        return dx * dx + dz * dz <= (double) radius * radius;
    }
}
