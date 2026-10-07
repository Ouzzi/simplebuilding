package com.simplebuilding.tweaks;

import com.simplebuilding.Simplebuilding;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * The balancing rules of the potion pads, per effect (owner 2026-09-29, docs/TRANK-PADS.md) - kept in
 * this one place so the balancing server can read and later override them.
 *
 * <p>A pad keeps its potion until it is broken and hands it out again and again; nothing is used up.
 * Balance therefore comes from four levers only:
 * <ol>
 *   <li><b>Who gets it</b> ({@link Target}): three mob effect tags, tunable by any datapack -
 *       {@code #simplebuilding:potion_pad/blocked} (never given), {@code #simplebuilding:potion_pad/public}
 *       (anyone standing on the pad) and {@code #simplebuilding:potion_pad/owner_only} (only the player who
 *       placed the pad). An effect in none of them follows its category: beneficial = everyone, harmful
 *       or neutral = owner only. Mobs never get anything - the pad only looks for players.</li>
 *   <li><b>How strong</b> ({@link Rule#maxAmplifier}): at most the highest level a vanilla potion brews
 *       for that effect, so a command-made potion cannot hand out Resistance V.</li>
 *   <li><b>How long</b>: the tier duration (45/90/180 s), but never longer than the potion itself lasts
 *       when drunk ({@link Rule#potionDurationShare}) and never longer than {@link Rule#maxSeconds}.</li>
 *   <li><b>How often</b>: the pad's cooldown is the config factor times the longest granted duration
 *       times the effect's {@link Rule#cooldownMultiplier} (instant effects count as
 *       {@link #INSTANT_COOLDOWN_BASIS_TICKS}); on top, {@link Rule#lockoutTicks} keeps one player from
 *       getting the same effect from any pad again for a while, so a row of pads cannot chain healing.</li>
 * </ol>
 */
public final class PotionPadRules {

    /** Who may receive an effect from a pad. */
    public enum Target {
        /** Every player standing on the pad. */
        EVERYONE,
        /** Only the player who placed the pad (and anyone on a pad nobody owns, e.g. set by a command). */
        OWNER_ONLY,
        /** Nobody. */
        BLOCKED
    }

    /**
     * Numbers for one effect.
     *
     * @param maxAmplifier        highest amplifier handed out (0 = level I); higher levels in the potion are clamped
     * @param maxSeconds          absolute cap on the duration in seconds, 0 = none
     * @param potionDurationShare cap as a share of the effect's duration in the stored potion (1.0 = never longer
     *                            than drinking it), 0 = none
     * @param cooldownMultiplier  factor on the pad cooldown this effect causes
     * @param lockoutTicks        after a full grant, the same player gets this effect from no pad for so many
     *                            ticks, 0 = no lockout
     */
    public record Rule(int maxAmplifier, int maxSeconds, double potionDurationShare, double cooldownMultiplier, int lockoutTicks) {
    }

    public static final TagKey<MobEffect> BLOCKED = tag("potion_pad/blocked");
    public static final TagKey<MobEffect> PUBLIC = tag("potion_pad/public");
    public static final TagKey<MobEffect> OWNER_ONLY = tag("potion_pad/owner_only");

    /** What an instant effect (no duration) counts as for the cooldown: tier I's 45 s. */
    public static final int INSTANT_COOLDOWN_BASIS_TICKS = 45 * 20;

    /** Every effect not in {@link #TABLE}: level I, capped at the potion's own duration, plain cooldown. */
    public static final Rule DEFAULT = new Rule(0, 0, 1.0, 1.0, 0);

    /**
     * The per-effect table (effect id -> rule). Amplifier caps are the highest vanilla brew of that effect
     * (Strong Swiftness II, Strong Turtle Master: Slowness VI and Resistance IV, ...).
     */
    public static final Map<String, Rule> TABLE = Map.ofEntries(
            // Instant effects: once per full charge, two times the plain cooldown, one minute lockout per player.
            Map.entry("minecraft:instant_health", new Rule(1, 0, 1.0, 2.0, 1200)),
            Map.entry("minecraft:instant_damage", new Rule(1, 0, 1.0, 2.0, 1200)),
            // Healing over time: never longer than the potion, 1.5x cooldown, one minute lockout per player.
            Map.entry("minecraft:regeneration", new Rule(1, 0, 1.0, 1.5, 1200)),
            // Turtle Master's parts: vanilla levels, never longer than the potion (20 s / 40 s).
            Map.entry("minecraft:resistance", new Rule(3, 0, 1.0, 1.5, 0)),
            Map.entry("minecraft:slowness", new Rule(5, 0, 1.0, 1.0, 0)),
            // Strong variants of vanilla potions.
            Map.entry("minecraft:speed", new Rule(1, 0, 1.0, 1.0, 0)),
            Map.entry("minecraft:jump_boost", new Rule(1, 0, 1.0, 1.0, 0)),
            Map.entry("minecraft:strength", new Rule(1, 0, 1.0, 1.0, 0)),
            Map.entry("minecraft:poison", new Rule(1, 0, 1.0, 1.0, 0)),
            // PvP-relevant: hiding costs more pad time.
            Map.entry("minecraft:invisibility", new Rule(0, 0, 1.0, 1.5, 0)),
            // Harmless comfort: half the cooldown, so standing on it again keeps it up.
            Map.entry("minecraft:night_vision", new Rule(0, 0, 1.0, 0.5, 0)),
            // An owner's elevator, not a flight: 10 s like a shulker bullet.
            Map.entry("minecraft:levitation", new Rule(0, 10, 1.0, 1.0, 0)));

    /** Replacement rules set at runtime (balancing server); consulted before {@link #TABLE}. */
    private static volatile Map<String, Rule> overrides = Map.of();

    /** Game time until which a player is locked out of an effect: "uuid|effect" -> end tick. */
    private static final Map<String, Long> LOCKOUTS = new ConcurrentHashMap<>();

    private PotionPadRules() {
    }

    private static TagKey<MobEffect> tag(String path) {
        return TagKey.create(Registries.MOB_EFFECT, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, path));
    }

    /** Replaces the runtime overrides (effect id -> rule); an empty map goes back to {@link #TABLE}. */
    public static void setOverrides(Map<String, Rule> rules) {
        overrides = Map.copyOf(new HashMap<>(rules));
    }

    public static String id(Holder<MobEffect> effect) {
        return effect.unwrapKey().map(key -> key.identifier().toString()).orElse("");
    }

    public static Rule rule(Holder<MobEffect> effect) {
        String id = id(effect);
        Rule override = overrides.get(id);
        if (override != null) {
            return override;
        }
        return TABLE.getOrDefault(id, DEFAULT);
    }

    /** Who may receive the effect: tags first (blocked, public, owner_only), then the effect's category. */
    public static Target target(Holder<MobEffect> effect) {
        if (effect.is(BLOCKED)) {
            return Target.BLOCKED;
        }
        if (effect.is(PUBLIC)) {
            return Target.EVERYONE;
        }
        if (effect.is(OWNER_ONLY)) {
            return Target.OWNER_ONLY;
        }
        return effect.value().getCategory() == MobEffectCategory.BENEFICIAL ? Target.EVERYONE : Target.OWNER_ONLY;
    }

    /** Whether a player may receive the effect ({@code owner}: the player placed the pad, or nobody did). */
    public static boolean allows(Holder<MobEffect> effect, boolean owner) {
        Target target = target(effect);
        return target == Target.EVERYONE || (target == Target.OWNER_ONLY && owner);
    }

    public static int amplifier(Holder<MobEffect> effect, int amplifier) {
        return Math.max(0, Math.min(rule(effect).maxAmplifier(), amplifier));
    }

    /**
     * Full (100 %) duration a pad hands out for {@code effect}: the tier duration, capped by the potion's
     * own duration times {@link Rule#potionDurationShare} and by {@link Rule#maxSeconds}. An infinite
     * effect in the potion only has the tier cap. At least one tick.
     */
    public static int fullDuration(MobEffectInstance effect, int tierTicks) {
        Rule rule = rule(effect.getEffect());
        long cap = tierTicks;
        if (rule.potionDurationShare() > 0 && !effect.isInfiniteDuration()) {
            cap = Math.min(cap, Math.round(effect.getDuration() * rule.potionDurationShare()));
        }
        if (rule.maxSeconds() > 0) {
            cap = Math.min(cap, rule.maxSeconds() * 20L);
        }
        return (int) Math.max(1, cap);
    }

    /** The pad cooldown one granted effect asks for ({@code full}: its full duration, ignored for instant effects). */
    public static int cooldownFor(Holder<MobEffect> effect, boolean instant, int full, double configFactor) {
        int basis = instant ? INSTANT_COOLDOWN_BASIS_TICKS : full;
        return (int) Math.round(configFactor * basis * rule(effect).cooldownMultiplier());
    }

    public static boolean isLockedOut(UUID player, Holder<MobEffect> effect, long gameTime) {
        Long until = LOCKOUTS.get(player + "|" + id(effect));
        if (until == null) {
            return false;
        }
        int length = rule(effect).lockoutTicks();
        // A stale entry (from another world, whose clock ran further) never locks longer than one lockout.
        if (gameTime >= until || until - gameTime > length) {
            LOCKOUTS.remove(player + "|" + id(effect));
            return false;
        }
        return true;
    }

    /** Starts the player's lockout for the effect after a full grant (no-op without {@link Rule#lockoutTicks}). */
    public static void lockOut(UUID player, Holder<MobEffect> effect, long gameTime) {
        int length = rule(effect).lockoutTicks();
        if (length > 0) {
            LOCKOUTS.put(player + "|" + id(effect), gameTime + length);
        }
    }

    /** Remaining lockout of a player for an effect, in ticks (0 = none); for tests and info displays. */
    public static int lockoutLeft(UUID player, Holder<MobEffect> effect, long gameTime) {
        Long until = LOCKOUTS.get(player + "|" + id(effect));
        return until == null || !isLockedOut(player, effect, gameTime) ? 0 : (int) (until - gameTime);
    }
}
