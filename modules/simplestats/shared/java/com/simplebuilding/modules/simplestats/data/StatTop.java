package com.simplebuilding.modules.simplestats.data;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** N78: the largest entries of a list for the tooltip (amount descending, ties by key, so the order never flickers). */
public final class StatTop {
    private static final Comparator<Map.Entry<String, Long>> ORDER =
            Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey());

    private StatTop() {}

    /** Up to {@code n} entries with an amount above 0, largest first; empty for a missing list. */
    public static List<Map.Entry<String, Long>> of(StatData.Detail detail, int n) {
        if (detail == null || n <= 0) return List.of();
        return detail.entries().entrySet().stream().filter(e -> e.getValue() > 0).sorted(ORDER).limit(n).toList();
    }
}
