package com.simplebuilding.modules.simpletools.vault;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Why an item entity leaves the world (server thread only): set around its own tick (despawn), around damage it takes
 * (fire, lava, cactus, explosion) and around a player touching it (pickup).
 */
public final class RemovalContext {
    public static ItemEntity ticking;
    public static ItemEntity hurtTarget;
    public static DamageSource hurtSource;
    public static ItemEntity touching;
    public static Player toucher;
    private static int touchCount;

    private RemovalContext() {}

    public static void touchStart(ItemEntity entity, Player player) {
        touching = entity;
        toucher = player;
        touchCount = entity.getItem().getCount();
    }

    /** After {@code playerTouch}: a part taken from a stack that stays in the world is a pickup too. */
    public static void touchEnd(ItemEntity entity) {
        if (touching != entity) return;
        Player player = toucher;
        touching = null;
        toucher = null;
        if (player == null || entity.isRemoved() || entity.getItem().getCount() >= touchCount) return;
        DeathLedger ledger = DeathLedger.of(entity);
        if (ledger != null) ledger.pickedUp(entity.getUUID(), player);
    }

    /** Called once when a tracked item entity is removed for good. */
    public static void removed(ItemEntity entity) {
        DeathLedger ledger = DeathLedger.of(entity);
        if (ledger == null || !ledger.tracked(entity.getUUID())) return;
        if (touching == entity && toucher != null) {
            ledger.pickedUp(entity.getUUID(), toucher);
            return;
        }
        boolean despawn = ticking == entity && entity.getAge() >= DeathLedger.LIFETIME;
        DamageSource hurt = hurtTarget == entity ? hurtSource : null;
        boolean voidFall = entity.getY() < entity.level().getMinY() - 64;
        ledger.removed(entity, despawn && hurt == null, hurt, voidFall);
    }

    /** Called when a tracked item entity unloads with its chunk (still alive, the timer pauses). */
    public static void unloaded(ItemEntity entity) {
        DeathLedger ledger = DeathLedger.of(entity);
        if (ledger != null && ledger.tracked(entity.getUUID())) ledger.unloaded(entity);
    }
}
