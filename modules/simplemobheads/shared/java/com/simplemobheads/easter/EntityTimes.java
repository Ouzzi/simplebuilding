package com.simplemobheads.easter;

import net.minecraft.world.entity.Entity;

/**
 * A number kept on an entity as one scoreboard tag {@code <key>=<value>} (EH21: the crown's and the tribute's ready time, the
 * hay bales a cow ate). Vanilla saves the tags with the entity on every loader and drops them with it, so no world data and no
 * cleanup is needed; {@code /tag} shows them to an admin.
 */
public final class EntityTimes {
    private EntityTimes() {
    }

    /** The value stored under {@code key}, or {@code fallback}. */
    public static long get(Entity entity, String key, long fallback) {
        String prefix = key + "=";
        for (String tag : entity.entityTags()) {
            if (tag.startsWith(prefix)) {
                try {
                    return Long.parseLong(tag.substring(prefix.length()));
                } catch (NumberFormatException e) {
                    return fallback;
                }
            }
        }
        return fallback;
    }

    /** Stores {@code value} under {@code key} (replacing an older value). */
    public static void set(Entity entity, String key, long value) {
        clear(entity, key);
        entity.addTag(key + "=" + value);
    }

    public static void clear(Entity entity, String key) {
        String prefix = key + "=";
        entity.entityTags().removeIf(tag -> tag.startsWith(prefix));
    }
}
