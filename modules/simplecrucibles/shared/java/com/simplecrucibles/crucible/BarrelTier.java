package com.simplecrucibles.crucible;

/**
 * Copper barrel tiers (owner wish round 2, answers 58/59; netherite owner addition 11): on its own a
 * barrel has the size of the matching chest tier (copper 27, reinforced 36, netherite 45, enderite 54
 * with SimpleBuilding), attached to a crucible it offers as many slots as the crucible has (owner N12c: iron 6,
 * reinforced 12 (N30), netherite 18, enderite 30 since N37); the slots beyond stay stored but hidden. Enderite holds double stacks like
 * the Enderite crucible.
 */
public enum BarrelTier {
    COPPER("copper", 3, 1),
    REINFORCED("reinforced", 4, 1),
    NETHERITE("netherite", 5, 1),
    ENDERITE("enderite", 6, 2);

    /** Most slots a crucible uses in an attached barrel (the Enderite crucible's 30 since N37; a smaller barrel, e.g. copper with 27, stores that many while attached). */
    public static final int MAX_CRUCIBLE_SLOTS = 30;

    private final String id;
    private final int rows;
    private final int stackMultiplier;

    BarrelTier(String id, int rows, int stackMultiplier) {
        this.id = id;
        this.rows = rows;
        this.stackMultiplier = stackMultiplier;
    }

    public String id() { return id; }
    public int rows() { return rows; }
    public int slots() { return rows * 9; }
    public int stackMultiplier() { return stackMultiplier; }
}
