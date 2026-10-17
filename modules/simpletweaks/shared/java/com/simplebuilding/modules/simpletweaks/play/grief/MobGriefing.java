package com.simplebuilding.modules.simpletweaks.play.grief;

import com.simplebuilding.modules.simpletweaks.play.Play;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;

/**
 * T8 (off by default): mob griefing per mob. While an entity ticks ({@code ServerLevel.tickNonPassenger}) the Vanilla
 * rule {@code mob_griefing} reads false for listed mobs (projectiles count as their owner, e.g. ghast fireballs).
 * It can only forbid: with the Vanilla rule off nothing changes. Vanilla 26.3 has no per-mob rule (re-check on every
 * port, principle A5).
 */
public final class MobGriefing {
    private static final ThreadLocal<Entity> TICKING = new ThreadLocal<>();
    private static List<String> cachedList;
    private static Set<EntityType<?>> cachedTypes = Set.of();

    public static void enter(Entity e) { TICKING.set(e); }
    public static void exit() { TICKING.remove(); }

    public static boolean active() { return Play.on() && Play.server() != null && Play.config().mobGriefing().enabled(); }

    /** May the entity that is ticking right now grief, given that the Vanilla rule says yes? */
    public static boolean allowed() {
        var e = TICKING.get();
        if (e == null || !active()) return true;
        return allowed(e);
    }
    public static boolean allowed(Entity e) {
        for (int i = 0; i < 4 && e instanceof Projectile p && p.getOwner() != null; i++) e = p.getOwner();
        return !blocked().contains(e.getType());
    }
    static Set<EntityType<?>> blocked() {
        var list = Play.config().mobGriefing().blocked();
        if (list != cachedList) {
            var out = new HashSet<EntityType<?>>();
            for (var id : list) {
                var key = net.minecraft.resources.Identifier.tryParse(id);
                if (key != null && BuiltInRegistries.ENTITY_TYPE.containsKey(key)) out.add(BuiltInRegistries.ENTITY_TYPE.getValue(key));
            }
            cachedTypes = out;
            cachedList = list;
        }
        return cachedTypes;
    }
    private MobGriefing() {}
}
