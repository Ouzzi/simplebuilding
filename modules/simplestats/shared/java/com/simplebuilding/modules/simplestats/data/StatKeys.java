package com.simplebuilding.modules.simplestats.data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Every counter, list and record of an item statistic, with its unit and display order. Pure Java (no Minecraft types),
 * so the data model, the screen rows and the tests run without a game. Keys are the stored names: a schema change
 * must bump {@link StatData#VERSION}.
 */
public final class StatKeys {
    /** How a stored number is shown. Damage is stored in hundredths of a health point, distance in centimeters. */
    public enum Unit { COUNT, DAMAGE, TICKS, CM, BLOCKS }

    // totals
    public static final String BLOCKS_MINED = "blocks_mined", LOGS_STRIPPED = "logs_stripped", TILLED = "tilled", SHEARED = "sheared",
            KILLS = "kills", DAMAGE_DEALT = "damage_dealt", HITS = "hits", ARROWS_FIRED = "arrows_fired", THROWS = "throws",
            DAMAGE_ABSORBED = "damage_absorbed", HITS_TAKEN = "hits_taken", WORN_TIME = "worn_time",
            DAMAGE_BLOCKED = "damage_blocked", ATTACKS_BLOCKED = "attacks_blocked",
            DISTANCE_FLOWN = "distance_flown", FLIGHTS = "flights", ROCKETS_USED = "rockets_used",
            DISTANCE_WALKED = "distance_walked", DISTANCE_SPRINTED = "distance_sprinted", DISTANCE_SNEAKED = "distance_sneaked", DISTANCE_SWUM = "distance_swum",
            CASTS = "casts", CATCHES = "catches", BRUSHED = "brushed", IGNITED = "ignited";
    // detail lists
    public static final String L_BLOCKS = "blocks_mined", L_KILLS = "kills", L_KILL_VARIANTS = "kill_variants", L_ABSORBED = "damage_absorbed",
            L_BLOCKED_BY = "blocked_by", L_CATCHES = "catches", L_CATCH_KINDS = "catch_kinds", L_SHEARED = "sheared", L_BRUSHED = "brushed";
    // records (maximum values)
    public static final String R_BIGGEST_HIT = "biggest_hit", R_BIGGEST_SMASH = "biggest_smash", R_LONGEST_SHOT = "longest_shot",
            R_BIGGEST_ABSORBED = "biggest_absorbed", R_LONGEST_FLIGHT = "longest_flight";

    /** Display and tooltip priority of the totals (the first present one is the headline). */
    public static final List<String> TOTAL_ORDER = List.of(BLOCKS_MINED, KILLS, DAMAGE_ABSORBED, DAMAGE_BLOCKED, DISTANCE_FLOWN, CATCHES,
            ARROWS_FIRED, THROWS, SHEARED, BRUSHED, IGNITED, TILLED, LOGS_STRIPPED, DAMAGE_DEALT, HITS, ATTACKS_BLOCKED, HITS_TAKEN,
            FLIGHTS, ROCKETS_USED, CASTS, DISTANCE_WALKED, DISTANCE_SPRINTED, DISTANCE_SNEAKED, DISTANCE_SWUM, WORN_TIME);
    public static final List<String> LIST_ORDER = List.of(L_BLOCKS, L_KILLS, L_KILL_VARIANTS, L_ABSORBED, L_BLOCKED_BY, L_CATCHES, L_CATCH_KINDS, L_SHEARED, L_BRUSHED);
    public static final List<String> RECORD_ORDER = List.of(R_BIGGEST_HIT, R_BIGGEST_SMASH, R_LONGEST_SHOT, R_BIGGEST_ABSORBED, R_LONGEST_FLIGHT);

    /** The total a detail list adds up to (its entries plus "other" always equal that total). */
    public static final Map<String, String> LIST_TOTAL = Map.of(L_BLOCKS, BLOCKS_MINED, L_KILLS, KILLS, L_KILL_VARIANTS, KILLS,
            L_ABSORBED, DAMAGE_ABSORBED, L_BLOCKED_BY, DAMAGE_BLOCKED, L_CATCHES, CATCHES, L_CATCH_KINDS, CATCHES, L_SHEARED, SHEARED, L_BRUSHED, BRUSHED);

    private static final Map<String, Unit> UNITS = new LinkedHashMap<>();
    static {
        for (String key : TOTAL_ORDER) UNITS.put(key, Unit.COUNT);
        for (String key : List.of(DAMAGE_DEALT, DAMAGE_ABSORBED, DAMAGE_BLOCKED, R_BIGGEST_HIT, R_BIGGEST_SMASH, R_BIGGEST_ABSORBED)) UNITS.put(key, Unit.DAMAGE);
        for (String key : List.of(DISTANCE_FLOWN, DISTANCE_WALKED, DISTANCE_SPRINTED, DISTANCE_SNEAKED, DISTANCE_SWUM)) UNITS.put(key, Unit.CM);
        UNITS.put(WORN_TIME, Unit.TICKS);
        for (String key : List.of(R_LONGEST_SHOT, R_LONGEST_FLIGHT)) UNITS.put(key, Unit.BLOCKS);
    }
    private static final Set<String> TOTALS = Set.copyOf(TOTAL_ORDER);
    private static final Set<String> LISTS = Set.copyOf(LIST_ORDER);
    private static final Set<String> RECORDS = Set.copyOf(RECORD_ORDER);

    private StatKeys() {}

    public static Unit unit(String key) { return UNITS.getOrDefault(key, Unit.COUNT); }
    public static boolean isTotal(String key) { return TOTALS.contains(key); }
    public static boolean isList(String key) { return LISTS.contains(key); }
    public static boolean isRecord(String key) { return RECORDS.contains(key); }

    /** Value of the detail list in the same unit as its total (the lists of damage hold hundredths, too). */
    public static Unit listUnit(String list) { return unit(LIST_TOTAL.getOrDefault(list, "")); }
}
