package com.simpleenchanting;

/**
 * The two enchanters (ENCH01, owner 03.11.; docs/ai/KONZEPT-B-ENCH01-2026-11-02.md): what the 5x5 floor under the table adds once
 * the shelves give 30, the highest tier and whether the endgame levels open.
 * <ul>
 *   <li>{@link #DARK}: Blazing or Nihil Obsidian floor (mixed is fine) +10 -> tier 50, every slider up to Vanilla's maximum.</li>
 *   <li>{@link #ASTRAL}: a whole Nihil Obsidian floor +20 -> tier 60 with the endgame levels; Blazing Obsidian (or mixed)
 *       works half, +10 -> 50.</li>
 * </ul>
 */
public enum EnchanterStage {
    DARK("dark_enchanter", 10, 10, 50, false),
    ASTRAL("astral_enchanter", 20, 10, 60, true);

    /** Block id path (namespace simpleenchanting). */
    public final String path;
    /** What a whole Nihil Obsidian floor adds. */
    public final int nihilFloor;
    /** What a Blazing Obsidian (or mixed) floor adds. */
    public final int blazingFloor;
    /** Highest tier and budget of this table. */
    public final int maxTier;
    /** Whether the endgame levels open at {@link #maxTier}. */
    public final boolean endgame;

    EnchanterStage(String path, int nihilFloor, int blazingFloor, int maxTier, boolean endgame) {
        this.path = path;
        this.nihilFloor = nihilFloor;
        this.blazingFloor = blazingFloor;
        this.maxTier = maxTier;
        this.endgame = endgame;
    }

    /** Floor bonus for a 5x5 that is {@code floor} ({@link EnchantingRules#FLOOR_NONE}, _BLAZING (any obsidian floor), _NIHIL (all Nihil)). */
    public int floorBonus(int floor) {
        return floor == EnchantingRules.FLOOR_NIHIL ? nihilFloor : floor == EnchantingRules.FLOOR_BLAZING ? blazingFloor : 0;
    }

    public static EnchanterStage byId(int id) {
        return id == 1 ? ASTRAL : DARK;
    }
}
