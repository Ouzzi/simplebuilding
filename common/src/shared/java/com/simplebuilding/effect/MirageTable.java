package com.simplebuilding.effect;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import org.jspecify.annotations.Nullable;

/**
 * Which mob another mob looks like under Mirage / Reverse Mirage (docs/ai/KONZEPT-DECEIVER-EFFEKTE-2026-10-07.md):
 * Mirage shows peaceful mobs as hostile ones, Reverse Mirage hostile mobs as peaceful ones, always within the same
 * size class (small: chicken, rabbit, silverfish; medium: pig, sheep, zombie, skeleton; large: cow, horse, iron golem,
 * warden, wither skeleton). The choice is random per mob but stable: it comes from the mob's UUID, so a pig stays
 * the same zombie for the whole effect. Only the look changes (client); bosses, players and armor stands never.
 * Plain data, no client classes: the server tests check the table.
 */
public final class MirageTable {
    public enum Size { SMALL, MEDIUM, LARGE }

    /** What peaceful mobs turn into under Mirage. */
    public static final Map<Size, List<EntityType<?>>> HOSTILE_LOOKS = new EnumMap<>(Map.of(
            Size.SMALL, List.of(EntityTypes.SILVERFISH, EntityTypes.ENDERMITE, EntityTypes.CAVE_SPIDER),
            Size.MEDIUM, List.of(EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.CREEPER, EntityTypes.HUSK, EntityTypes.STRAY),
            Size.LARGE, List.of(EntityTypes.WARDEN, EntityTypes.WITHER_SKELETON, EntityTypes.RAVAGER)));
    /** What hostile mobs turn into under Reverse Mirage. */
    public static final Map<Size, List<EntityType<?>>> PEACEFUL_LOOKS = new EnumMap<>(Map.of(
            Size.SMALL, List.of(EntityTypes.CHICKEN, EntityTypes.RABBIT),
            Size.MEDIUM, List.of(EntityTypes.PIG, EntityTypes.SHEEP, EntityTypes.GOAT),
            Size.LARGE, List.of(EntityTypes.COW, EntityTypes.HORSE, EntityTypes.MOOSHROOM)));

    /** Mobs whose box says otherwise than the concept (cows count as large, spiders and pigs as medium). */
    private static final Set<EntityType<?>> LARGE = Set.of(EntityTypes.COW, EntityTypes.MOOSHROOM);
    private static final Set<EntityType<?>> MEDIUM = Set.of(EntityTypes.SPIDER, EntityTypes.PIG, EntityTypes.SHEEP);

    private MirageTable() {
    }

    /** Size class from the hit box: small below 0.4 (width x height), large from 2.2 tall or 1.3 wide. */
    public static Size sizeOf(EntityType<?> type) {
        if (LARGE.contains(type)) return Size.LARGE;
        if (MEDIUM.contains(type)) return Size.MEDIUM;
        float width = type.getWidth(), height = type.getHeight();
        if (height >= 2.2F || width >= 1.3F) return Size.LARGE;
        return width * height < 0.4F ? Size.SMALL : Size.MEDIUM;
    }

    /** Whether the effects touch this entity at all: mobs only, no bosses. */
    public static boolean affects(Entity entity) {
        return entity instanceof Mob && !(entity instanceof EnderDragon) && !(entity instanceof WitherBoss);
    }

    /**
     * The type {@code entity} is drawn as, or null when it keeps its look. {@code mirage}/{@code reverseMirage} = the
     * viewer has that effect.
     */
    public static @Nullable EntityType<?> disguise(Entity entity, boolean mirage, boolean reverseMirage) {
        if (!affects(entity)) return null;
        boolean hostile = entity instanceof Enemy;
        if (hostile ? !reverseMirage : !mirage) return null;
        return pick(entity.getType(), hostile, entity.getUUID().getLeastSignificantBits() ^ entity.getUUID().getMostSignificantBits());
    }

    /** The disguise of a mob of {@code type}, {@code hostile} or not, with the stable seed {@code seed}. */
    public static EntityType<?> pick(EntityType<?> type, boolean hostile, long seed) {
        List<EntityType<?>> looks = (hostile ? PEACEFUL_LOOKS : HOSTILE_LOOKS).get(sizeOf(type));
        EntityType<?> look = looks.get((int) Math.floorMod(seed, (long) looks.size()));
        // Never its own type (a peaceful-looking table could contain the mob itself, e.g. a modded one).
        return look == type ? looks.get((looks.indexOf(look) + 1) % looks.size()) : look;
    }
}
