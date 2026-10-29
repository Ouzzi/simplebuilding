package com.simplebuilding.modules.simplechallenges.price;

import java.util.List;

/** What the probe found around one cell: hand-reachable items, the value of the whole column (block unlock) and the surface height. */
public record CellInfo(List<WeightedItem> reachable, double columnValue, int surfaceY) {
    public static final CellInfo EMPTY = new CellInfo(List.of(), 0, 0);
}
