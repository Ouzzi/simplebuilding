package com.simplebuilding.modules.simplemobs;

import java.util.Map;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;

/** Disguise state every player carries (added by {@code PlayerDisguiseMixin}); use {@link Disguise} instead. */
public interface DisguiseHolder {
    /** "" = own shape, an entity type id, or {@link CloakLogic#playerSpec}. Synced to all clients. */
    String simplemobs$spec();

    void simplemobs$setSpec(String spec);

    /** Shown gear of a player disguise, slot index as in {@link Disguise#GEAR_SLOTS}. Synced. */
    ItemStack simplemobs$gear(int slot);

    void simplemobs$setGear(int slot, ItemStack stack);

    long simplemobs$revealedUntil();

    void simplemobs$setRevealedUntil(long gameTime);

    /** Mob UUID -> game time until which that mob knows the player (server only). */
    Map<UUID, Long> simplemobs$spotted();

    boolean simplemobs$isSpecKey(Object accessor);
}
