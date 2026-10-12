package com.simplebuilding.modules.simplemobs;

import java.util.Map;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Server-side disguise state every player carries (added by {@code PlayerDisguiseMixin}); clients read it from the
 * {@link DisguiseMarker}. Use {@link Disguise} instead of calling these directly.
 */
public interface DisguiseHolder {
    /** "" = own shape, an entity type id, or {@link CloakLogic#playerSpec}. */
    String simplemobs$spec();

    void simplemobs$setSpec(String spec);

    /** Shown gear of a player disguise, index as in {@link Disguise#GEAR_SLOTS}. */
    ItemStack[] simplemobs$gear();

    @Nullable DisguiseMarker simplemobs$marker();

    void simplemobs$setMarker(@Nullable DisguiseMarker marker);

    long simplemobs$revealedUntil();

    void simplemobs$setRevealedUntil(long gameTime);

    /** Mob UUID -> game time until which that mob knows the player. */
    Map<UUID, Long> simplemobs$spotted();
}
