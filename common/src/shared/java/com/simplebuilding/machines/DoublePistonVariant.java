package com.simplebuilding.machines;

import java.util.List;

/**
 * Variant frame of the Double Piston (IG31-26): every variant is one base block with its own head and arm, the same
 * two-stage logic ({@link DoublePistons}) and its own textures ({@code <id>_side_overlay}, {@code <id>_plate_overlay}).
 * Only {@link #BASE} is registered; the End variants wait for the owner's approval of the base (owner 30.10.).
 */
public enum DoublePistonVariant {
    BASE("double_piston");

    /** Planned variants (ids {@code <name>_double_piston}), not registered yet: textures and push rules follow after the approval. */
    public static final List<String> PREPARED = List.of("astral", "nihil");

    private final String id;

    DoublePistonVariant(String id) {
        this.id = id;
    }

    /** Block and item id of the base. */
    public String id() {
        return id;
    }

    public String headId() {
        return id + "_head";
    }

    public String armId() {
        return id + "_arm";
    }
}
