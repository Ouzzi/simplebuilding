package com.simplebuilding.modules.simplestats.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** The rows of the statistics screen for one item: sections, totals, records and the complete detail lists. Pure Java. */
public final class StatRows {
    public enum Kind { SECTION, TOTAL, RECORD, ENTRY, OTHER, SINCE }

    /**
     * @param stat  total, record or list name the row belongs to
     * @param key   entry key (blocks, mobs ...), empty otherwise
     * @param value counted amount in the unit of the stat
     * @param whole sum of the whole list (for the share bar), 0 when not a list row
     */
    public record Row(Kind kind, String stat, String key, long value, long whole) {}

    private StatRows() {}

    public static List<Row> build(StatData data) {
        List<Row> rows = new ArrayList<>();
        boolean any = !data.totals().isEmpty() || !data.records().isEmpty();
        if (any || data.since() > 0) rows.add(new Row(Kind.SECTION, "overview", "", 0, 0));
        for (String key : orderedKeys(StatKeys.TOTAL_ORDER, data.totals())) rows.add(new Row(Kind.TOTAL, key, "", data.total(key), 0));
        for (String key : orderedKeys(StatKeys.RECORD_ORDER, data.records())) rows.add(new Row(Kind.RECORD, key, "", data.record(key), 0));
        if (data.since() > 0) rows.add(new Row(Kind.SINCE, "since", "", data.since(), 0));
        for (String list : orderedKeys(StatKeys.LIST_ORDER, data.details())) {
            StatData.Detail detail = data.detail(list);
            if (detail.entries().isEmpty() && detail.other() == 0) continue;
            long whole = detail.sum();
            rows.add(new Row(Kind.SECTION, list, "", 0, 0));
            List<Map.Entry<String, Long>> entries = new ArrayList<>(detail.entries().entrySet());
            entries.sort(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()));
            for (var e : entries) rows.add(new Row(Kind.ENTRY, list, e.getKey(), e.getValue(), whole));
            if (detail.other() > 0) rows.add(new Row(Kind.OTHER, list, "", detail.other(), whole));
        }
        return rows;
    }

    /** Known keys in their fixed order, then unknown (newer-version) keys alphabetically; only keys present in {@code present}. */
    private static List<String> orderedKeys(List<String> order, Map<String, ?> present) {
        List<String> out = new ArrayList<>();
        for (String key : order) if (present.containsKey(key)) out.add(key);
        TreeSet<String> extra = new TreeSet<>(Comparator.naturalOrder());
        for (String key : present.keySet()) if (!order.contains(key)) extra.add(key);
        out.addAll(extra);
        return out;
    }
}
