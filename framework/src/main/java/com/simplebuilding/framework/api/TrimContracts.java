package com.simplebuilding.framework.api;

import java.util.function.BooleanSupplier;

/**
 * Armour trim abilities (MOB27-3, docs/ai/PLAN-B-MOB32-2026-10-31.md), since 0.1.16. Simple Mobs applies the abilities of armour
 * trims (pattern and material bonuses) and {@link #claim claims} them; the SimpleBuilding core, which applied them before, then keeps
 * its own hooks quiet ({@link #claimed}) and only stays the fallback. While the core still measures the old trim resonance it
 * registers itself as the {@link Host}: its tracker, its server tuning and its player switch then feed the abilities, so the numbers
 * stay exactly what they were. Pure Java, no Minecraft class: entities travel as {@code Object}. Nobody imports the other side.
 */
public final class TrimContracts {
    private static volatile Host host;
    private static volatile BooleanSupplier engine;

    private TrimContracts() {
    }

    /** What the host of the old measurement tells the abilities. */
    public interface Host {
        /** The trim resonance of a wearer (0.2..2.0 with the default base); {@code NaN} when the host does not know the wearer. */
        double multiplier(Object livingEntity);

        /** The strength of one bonus (catalogue key such as {@code "fire_protection"}), 1.0 = as designed. */
        float strength(String bonusKey);

        /** Whether the wearer has trim benefits switched on (player switch); true for everyone the host does not know. */
        boolean enabled(Object livingEntity);
    }

    /** Called by the host (SimpleBuilding core); {@code null} removes it. */
    public static void setHost(Host impl) {
        host = impl;
    }

    /** The registered host, {@code null} without one. */
    public static Host host() {
        return host;
    }

    /** Called once by the mod that applies the abilities (Simple Mobs) with its own on-switch; {@code null} withdraws the claim. */
    public static void claim(BooleanSupplier active) {
        engine = active;
    }

    /** Whether a mod applies the abilities right now; the core's own hooks then do nothing. */
    public static boolean claimed() {
        BooleanSupplier e = engine;
        return e != null && e.getAsBoolean();
    }
}
