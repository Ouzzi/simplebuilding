package com.simplelib.crucible;

/**
 * Copper barrel tiers (owner wish round 2, answers 58/59): on its own a barrel has the size of the
 * matching chest tier (copper 27, reinforced 36, enderite 54 with SimpleBuilding), attached to a
 * crucible only its first 9 slots are used and shown. Enderite holds double stacks like the Enderite
 * crucible.
 */
public enum BarrelTier {
    COPPER("copper", 3, 1),
    REINFORCED("reinforced", 4, 1),
    ENDERITE("enderite", 6, 2);

    /** Slots a crucible uses in an attached barrel. */
    public static final int CRUCIBLE_SLOTS = 9;

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
