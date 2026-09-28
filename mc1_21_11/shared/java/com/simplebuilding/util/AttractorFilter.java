package com.simplebuilding.util;

import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * Which loose items the attractor (item id {@code magnet}) may pull towards a player. It used to
 * take every item entity in range and reset its pickup delay to zero - which also dragged other
 * mods' display items off their pedestals and emptied other players' death piles. Left alone now:
 *
 * <ul>
 *   <li>items that can never be picked up ({@code ItemEntity#setNeverPickUp}, pickup delay
 *       {@value #NEVER_PICK_UP}) - the usual mark of a display or marker item;</li>
 *   <li>items reserved for another player (vanilla's {@code Owner}/{@code target}, e.g. items a
 *       mod hands out to one player, or {@code /give} overflow);</li>
 *   <li>the death drops of another player - vanilla leaves no mark on them, so the mod tags them
 *       ({@value #DEATH_DROP_TAG_PREFIX}{@code <uuid>}) while the dying player's inventory is
 *       dropped; the owner's own attractor still collects them;</li>
 *   <li>items in the item tag {@code simplebuilding:attractor_ignore} (empty by default, for packs).</li>
 * </ul>
 */
public final class AttractorFilter {

    /** Vanilla's {@code ItemEntity.INFINITE_PICKUP_DELAY}. */
    public static final int NEVER_PICK_UP = 32767;
    /** Entity tag on a player's death drop, followed by the dead player's UUID. */
    public static final String DEATH_DROP_TAG_PREFIX = "simplebuilding.death_drop.";

    /** The player whose death loot is being dropped right now (server thread only), or null. */
    private static @Nullable LivingEntity dying;

    private AttractorFilter() {
    }

    /** Whether the attractor held by {@code player} may pull {@code item}. */
    public static boolean mayAttract(ItemEntity item, Player player) {
        if (item.getItem().is(ModTags.Items.ATTRACTOR_IGNORE)) {
            return false;
        }
        if (item instanceof ItemEntityPickupInfo info) {
            if (info.simplebuilding$pickupDelay() == NEVER_PICK_UP) {
                return false;
            }
            UUID owner = info.simplebuilding$pickupOwner();
            if (owner != null && !owner.equals(player.getUUID())) {
                return false;
            }
        }
        UUID deadOwner = deathDropOwner(item);
        return deadOwner == null || deadOwner.equals(player.getUUID());
    }

    /** The player whose death drop {@code item} is, or null. */
    public static @Nullable UUID deathDropOwner(ItemEntity item) {
        for (String tag : item.getTags()) {
            if (tag.startsWith(DEATH_DROP_TAG_PREFIX)) {
                try {
                    return UUID.fromString(tag.substring(DEATH_DROP_TAG_PREFIX.length()));
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    /** From {@code PlayerEntityMixin}: the player's death loot starts dropping. */
    public static void beginDeathDrops(LivingEntity player) {
        dying = player;
    }

    /** From {@code PlayerEntityMixin}: the player's death loot has been dropped. */
    public static void endDeathDrops() {
        dying = null;
    }

    /** From {@code LivingEntityMixin}: tags {@code dropped} when it is part of {@code dropper}'s death loot. */
    public static void markIfDeathDrop(LivingEntity dropper, @Nullable ItemEntity dropped) {
        if (dropped != null && dying == dropper) {
            dropped.addTag(DEATH_DROP_TAG_PREFIX + dropper.getUUID());
        }
    }
}
