package com.simplemobheads.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Server config of Simple Mobheads ({@code config/simplemobheads.json}). Every gameplay effect has its own switch
 * (owner 2026-09-29: small fun or a small use, nothing exploitable, each one switchable). Bounds are enforced by
 * {@link #normalize}. Plain fields, no Cloth annotations: the config screen reads them by reflection.
 */
public class MobHeadsConfig {
    /** Highest player-kill base chance a server may set (10 %). */
    public static final float MAX_PLAYER_KILL_CHANCE = 0.1F;
    /** Highest bonus per looting level (5 %). */
    public static final float MAX_LOOTING_BONUS = 0.05F;
    /** Ritual cooldown bounds: one minute to seven in-game days. */
    public static final int MIN_RITUAL_COOLDOWN = 1200;
    public static final int MAX_RITUAL_COOLDOWN = 168000;

    /** Master switch: drops, abilities and greetings (the heads always stay registered, worlds keep them). */
    public boolean enabled = true;
    /** A charged creeper's explosion drops the head of the mob it kills (Vanilla rule, one head per explosion). */
    public boolean creeperDrops = true;
    /** Boss: the wither's head (default off, owner 13.10.). */
    public boolean witherHead = false;
    /** Boss: the warden's head (default on, owner 13.10.). */
    public boolean wardenHead = true;
    /** Optional: a player kill may drop the head too (default off; base chance plus a bonus per looting level). */
    public boolean playerKillDrops = false;
    public float playerKillChance = 0.0F;
    public float playerKillLootingBonus = 0.01F;
    /** PvP: a player killed by another player drops their own head with their skin (moved in from Simple Fun). */
    public boolean playerHeadDrops = true;
    /** Sneaking with a head on: a quiet greeting in the mob's voice (moved in from Simple Fun, cosmetic). */
    public boolean greeting = true;
    /** Easter egg (KONZEPT-HEROBRINE-KOPF-2026-10-19, owner KONZ20): lighting the gold shrine at night gives the Herobrine head. */
    public boolean herobrineRitual = true;
    /** Ticks until the same shrine position or the same player may do the ritual again (one in-game day). */
    public int ritualCooldownTicks = 24000;
    /** After the ritual: five minutes of distant white eyes and footsteps, only for the player who lit it (no world change). */
    public boolean hauntPhase = true;
    /** A rare "Herobrine Notes" book in mineshaft chests hints at the ritual (read when loot tables load). */
    public boolean hintBook = true;
    /** One switch per head ability, keyed by ability id ({@code blaze}, {@code bee}, ...). */
    public Map<String, Boolean> abilities = new LinkedHashMap<>();

    public boolean ability(String id) {
        return abilities.getOrDefault(id, true);
    }

    public static float bounded(float value, float fallback, float min, float max) {
        return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }

    /** Fills missing ability switches (all on) and clamps the chances. */
    public void normalize(Iterable<String> abilityIds) {
        if (abilities == null) {
            abilities = new LinkedHashMap<>();
        }
        LinkedHashMap<String, Boolean> ordered = new LinkedHashMap<>();
        for (String id : abilityIds) {
            Boolean value = abilities.get(id);
            ordered.put(id, value == null || value);
        }
        abilities.forEach(ordered::putIfAbsent);
        abilities = ordered;
        playerKillChance = bounded(playerKillChance, 0.0F, 0.0F, MAX_PLAYER_KILL_CHANCE);
        playerKillLootingBonus = bounded(playerKillLootingBonus, 0.01F, 0.0F, MAX_LOOTING_BONUS);
        ritualCooldownTicks = Math.max(MIN_RITUAL_COOLDOWN, Math.min(MAX_RITUAL_COOLDOWN, ritualCooldownTicks));
    }
}
