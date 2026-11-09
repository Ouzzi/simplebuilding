package com.simplegadgets.attractor;

import com.simplegadgets.GadgetProtection;
import com.simplegadgets.mixin.ItemEntityAccessor;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * Which loose items the Attractor (item id {@code magnet}) may pull towards a player (OW24-2 G3: moved from SimpleBuilding's
 * {@code util/AttractorFilter}, same rules, same entity tags so saved worlds keep their marks). Left alone:
 *
 * <ul>
 *   <li>items that can never be picked up ({@code ItemEntity#setNeverPickUp}, pickup delay {@value #NEVER_PICK_UP}) - the
 *       usual mark of a display or marker item;</li>
 *   <li>items reserved for another player (vanilla's {@code target});</li>
 *   <li>the death drops of another player - tagged ({@value #DEATH_DROP_TAG_PREFIX}{@code <uuid>}) while the dying player's
 *       inventory is dropped ({@code mixin/PlayerDeathDropsMixin}, {@code mixin/DeathDropMarkMixin}); the owner's own
 *       Attractor still collects them;</li>
 *   <li>items in the item tag {@code simplebuilding:attractor_ignore} (the id stays SimpleBuilding's, so packs keep their
 *       entries; a data pack may fill it without SimpleBuilding too);</li>
 *   <li>items on land the player may not touch (claims, {@link GadgetProtection#mayAffectEntity}).</li>
 * </ul>
 */
public final class AttractorFilter {
    /** Vanilla's {@code ItemEntity.INFINITE_PICKUP_DELAY}. */
    public static final int NEVER_PICK_UP = 32767;
    /** Entity tag on a player's death drop, followed by the dead player's UUID (SimpleBuilding's name, kept for old worlds). */
    public static final String DEATH_DROP_TAG_PREFIX = "simplebuilding.death_drop.";
    /** Items in this tag no Attractor pulls or filters on (modpacks, data packs). */
    public static final TagKey<Item> IGNORE = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("simplebuilding", "attractor_ignore"));

    /** The player whose death loot is being dropped right now (server thread only), or null. */
    private static @Nullable LivingEntity dying;

    private AttractorFilter() {}

    /** Whether the Attractor held by {@code player} may pull {@code item}. */
    public static boolean mayAttract(ItemEntity item, Player player) {
        if (item.getItem().is(IGNORE)) return false;
        ItemEntityAccessor info = (ItemEntityAccessor) item;
        if (info.simplegadgets$pickupDelay() == NEVER_PICK_UP) return false;
        UUID owner = info.simplegadgets$target();
        if (owner != null && !owner.equals(player.getUUID())) return false;
        UUID deadOwner = deathDropOwner(item);
        if (deadOwner != null && !deadOwner.equals(player.getUUID())) return false;
        // Claims: items lying on land the player may not touch stay there (else the magnet would pull them into the wilderness).
        return GadgetProtection.mayAffectEntity(player, item);
    }

    /** The player whose death drop {@code item} is, or null. */
    public static @Nullable UUID deathDropOwner(ItemEntity item) {
        for (String tag : item.entityTags()) {
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

    /** From {@code PlayerDeathDropsMixin}: the player's death loot starts dropping. */
    public static void beginDeathDrops(LivingEntity player) {
        dying = player;
    }

    /** From {@code PlayerDeathDropsMixin}: the player's death loot has been dropped. */
    public static void endDeathDrops() {
        dying = null;
    }

    /** From {@code DeathDropMarkMixin}: tags {@code dropped} when it is part of {@code dropper}'s death loot. */
    public static void markIfDeathDrop(LivingEntity dropper, @Nullable ItemEntity dropped) {
        if (dropped != null && dying == dropper) {
            dropped.addTag(DEATH_DROP_TAG_PREFIX + dropper.getUUID());
        }
    }
}
