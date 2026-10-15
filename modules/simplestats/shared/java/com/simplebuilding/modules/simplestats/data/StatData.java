package com.simplebuilding.modules.simplestats.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The statistics one item carries (value of the {@code simplestats:item_stats} component). Immutable; changes go through
 * {@link #apply} with a {@link Delta}. Pure Java.
 *
 * <p>Size rule (owner decision S4): lists are as complete as what was counted. The only protection is a byte budget for
 * the whole value ({@link Budget}): once it is reached, <em>new</em> keys are no longer listed and add to the list's
 * {@code other} amount instead, known keys keep counting. So for every list the entries plus {@code other} always equal
 * the total (invariant, tested). A value written by a newer schema ({@code version > VERSION}) is read but never changed.
 */
public final class StatData {
    public static final int VERSION = 1;
    /** Longest stored key (a registry id with a variant is far shorter). */
    public static final int MAX_KEY = 96;
    public static final StatData EMPTY = new StatData(VERSION, 0L, 0L, Map.of(), Map.of(), Map.of());

    /** Byte budget of one value, estimated like the network encoding. */
    public record Budget(int maxBytes, int maxEntriesPerList) {
        public static final Budget DEFAULT = new Budget(32768, 8192);
        public Budget {
            maxBytes = Math.max(2048, Math.min(maxBytes, 262144));
            maxEntriesPerList = Math.max(16, Math.min(maxEntriesPerList, 65536));
        }
    }

    /** One list: counted keys and the summed rest ("and others"). */
    public record Detail(Map<String, Long> entries, long other) {
        public static final Detail EMPTY = new Detail(Map.of(), 0L);
        public long sum() {
            long total = other;
            for (long value : entries.values()) total += value;
            return total;
        }
    }

    private final int version;
    private final long uid, since;
    private final Map<String, Long> totals, records;
    private final Map<String, Detail> details;
    private final int bytes;

    private StatData(int version, long uid, long since, Map<String, Long> totals, Map<String, Detail> details, Map<String, Long> records) {
        this.version = version;
        this.uid = uid;
        this.since = since;
        this.totals = Collections.unmodifiableMap(totals);
        this.details = Collections.unmodifiableMap(details);
        this.records = Collections.unmodifiableMap(records);
        this.bytes = estimate(this.totals, this.details, this.records);
    }

    public int version() { return version; }
    /** Random id assigned at the first count; lets a projectile's copy of the weapon find the real item again. */
    public long uid() { return uid; }
    /** Epoch day of the first count (0 = unknown). */
    public long since() { return since; }
    public Map<String, Long> totals() { return totals; }
    public Map<String, Detail> details() { return details; }
    public Map<String, Long> records() { return records; }
    public int bytes() { return bytes; }
    public boolean writable() { return version <= VERSION; }
    public long total(String key) { return totals.getOrDefault(key, 0L); }
    public long record(String key) { return records.getOrDefault(key, 0L); }
    public Detail detail(String list) { return details.getOrDefault(list, Detail.EMPTY); }
    /** True when nothing was counted (an item that only got an id). */
    public boolean isBlank() { return totals.isEmpty() && records.isEmpty(); }

    public StatData withUid(long id) {
        return writable() && uid == 0L ? new StatData(version, id, since, new LinkedHashMap<>(totals), new LinkedHashMap<>(details), new LinkedHashMap<>(records)) : this;
    }

    /** The value plus a delta; {@code today} is the epoch day stored when this is the first count. Unwritable values are returned unchanged. */
    public StatData apply(Delta delta, Budget budget, long today) {
        if (!writable() || delta.isEmpty()) return this;
        Map<String, Long> newTotals = new LinkedHashMap<>(totals);
        Map<String, Detail> newDetails = new LinkedHashMap<>(details);
        Map<String, Long> newRecords = new LinkedHashMap<>(records);
        for (var e : delta.totals().entrySet()) newTotals.merge(e.getKey(), e.getValue(), StatData::saturatedAdd);
        for (var e : delta.records().entrySet()) newRecords.merge(e.getKey(), e.getValue(), Math::max);
        int used = estimate(newTotals, newDetails, newRecords);
        for (var list : delta.details().entrySet()) {
            Detail old = newDetails.getOrDefault(list.getKey(), Detail.EMPTY);
            Map<String, Long> entries = new LinkedHashMap<>(old.entries());
            long other = old.other();
            if (old == Detail.EMPTY) used += list.getKey().length() + 8;
            for (var add : list.getValue().entrySet()) {
                String key = add.getKey();
                long amount = add.getValue();
                if (entries.containsKey(key)) {
                    entries.merge(key, amount, StatData::saturatedAdd);
                } else if (validKey(key) && used + entryBytes(key) <= budget.maxBytes() && entries.size() < budget.maxEntriesPerList()) {
                    entries.put(key, amount);
                    used += entryBytes(key);
                } else {
                    other = saturatedAdd(other, amount);
                }
            }
            newDetails.put(list.getKey(), new Detail(Collections.unmodifiableMap(entries), other));
        }
        return new StatData(version, uid, since == 0L ? today : since, newTotals, newDetails, newRecords);
    }

    /**
     * Builds a value from decoded data defensively: negative numbers become 0, bad keys and everything beyond the budget move
     * into the list's {@code other} amount (the sum stays exact).
     */
    public static StatData sanitize(int version, long uid, long since, Map<String, Long> totals, Map<String, Detail> details, Map<String, Long> records, Budget budget) {
        Map<String, Long> t = new LinkedHashMap<>();
        for (var e : totals.entrySet()) if (validKey(e.getKey())) t.put(e.getKey(), Math.max(0L, e.getValue()));
        Map<String, Long> r = new LinkedHashMap<>();
        for (var e : records.entrySet()) if (validKey(e.getKey())) r.put(e.getKey(), Math.max(0L, e.getValue()));
        Map<String, Detail> d = new LinkedHashMap<>();
        int used = estimate(t, Map.of(), r);
        for (var list : details.entrySet()) {
            if (!validKey(list.getKey())) continue;
            used += list.getKey().length() + 8;
            Map<String, Long> entries = new LinkedHashMap<>();
            long other = Math.max(0L, list.getValue().other());
            for (var e : list.getValue().entries().entrySet()) {
                long value = Math.max(0L, e.getValue());
                if (validKey(e.getKey()) && used + entryBytes(e.getKey()) <= budget.maxBytes() && entries.size() < budget.maxEntriesPerList()) {
                    entries.put(e.getKey(), value);
                    used += entryBytes(e.getKey());
                } else {
                    other = saturatedAdd(other, value);
                }
            }
            d.put(list.getKey(), new Detail(Collections.unmodifiableMap(entries), other));
        }
        return new StatData(version, uid, Math.max(0L, since), t, d, r);
    }

    public static boolean validKey(String key) {
        return key != null && !key.isBlank() && key.length() <= MAX_KEY;
    }

    private static int entryBytes(String key) { return key.length() + 4; }

    private static int estimate(Map<String, Long> totals, Map<String, Detail> details, Map<String, Long> records) {
        long size = 24;
        for (String key : totals.keySet()) size += entryBytes(key) + 4;
        for (String key : records.keySet()) size += entryBytes(key) + 4;
        for (var list : details.entrySet()) {
            size += list.getKey().length() + 8;
            for (String key : list.getValue().entries().keySet()) size += entryBytes(key);
        }
        return (int) Math.min(size, Integer.MAX_VALUE);
    }

    private static long saturatedAdd(long a, long b) {
        long sum = a + b;
        return ((a ^ sum) & (b ^ sum)) < 0 ? Long.MAX_VALUE : sum;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof StatData s && s.version == version && s.uid == uid && s.since == since
                && s.totals.equals(totals) && s.details.equals(details) && s.records.equals(records);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(version, uid, since, totals, details, records);
    }

    /** Collected changes (mutable); one delta holds everything that happened to one item within a tick. */
    public static final class Delta {
        private final Map<String, Long> totals = new LinkedHashMap<>(), records = new LinkedHashMap<>();
        private final Map<String, Map<String, Long>> details = new LinkedHashMap<>();

        public Map<String, Long> totals() { return totals; }
        public Map<String, Long> records() { return records; }
        public Map<String, Map<String, Long>> details() { return details; }
        public boolean isEmpty() { return totals.isEmpty() && records.isEmpty() && details.isEmpty(); }

        public Delta add(String total, long amount) {
            if (amount > 0) totals.merge(total, amount, StatData::saturatedAdd);
            return this;
        }

        /** Adds to a list entry only (use {@link #add(String, String, String, long)} to keep the total in step). */
        public Delta entry(String list, String key, long amount) {
            if (amount > 0) details.computeIfAbsent(list, k -> new LinkedHashMap<>()).merge(key, amount, StatData::saturatedAdd);
            return this;
        }

        /** Adds to the total and to one list entry. */
        public Delta add(String total, String list, String key, long amount) {
            return add(total, amount).entry(list, key, amount);
        }

        public Delta record(String key, long value) {
            if (value > 0) records.merge(key, value, Math::max);
            return this;
        }
    }
}
