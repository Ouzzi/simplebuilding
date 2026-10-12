package com.simplebuilding.modules.simpletools.vault;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;

/** Why an item entity leaves the world (server thread only): set around its own tick and around damage it takes. */
public final class RemovalContext {
    public static ItemEntity ticking;
    public static ItemEntity hurtTarget;
    public static DamageSource hurtSource;

    private RemovalContext() {}

    /** Called once when a tracked item entity is removed for good. */
    public static void removed(ItemEntity entity) {
        DeathLedger ledger = DeathLedger.of(entity);
        if (ledger == null || !ledger.tracked(entity.getUUID())) return;
        boolean despawn = ticking == entity && entity.getAge() >= DeathLedger.LIFETIME;
        DamageSource hurt = hurtTarget == entity ? hurtSource : null;
        boolean voidFall = entity.getY() < entity.level().getMinY() - 64;
        ledger.removed(entity, despawn && hurt == null, hurt, voidFall);
    }
}
