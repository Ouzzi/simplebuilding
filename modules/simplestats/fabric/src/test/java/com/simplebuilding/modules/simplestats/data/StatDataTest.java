package com.simplebuilding.modules.simplestats.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.simplebuilding.modules.simplestats.data.StatData.Budget;
import com.simplebuilding.modules.simplestats.data.StatData.Delta;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The statistics value: exact sums, byte budget, hostile input. <b>What breaks this test:</b> a list whose entries plus
 * "other" differ from its total, a budget that lets a value grow without limit or silently drops counts, a newer-version
 * value that gets modified, negative or oversized decoded data that is accepted as is.
 */
class StatDataTest {
    private static final Budget SMALL = new Budget(2048, 64);

    private static void adds(StatData data, String list) {
        String total = StatKeys.LIST_TOTAL.get(list);
        assertEquals(data.total(total), data.detail(list).sum(), list + " must add up to " + total);
    }

    @Test
    void sumsStayExactWithThousandsOfKeys() {
        StatData data = StatData.EMPTY;
        long expected = 0;
        for (int round = 0; round < 5; round++) {
            Delta d = new Delta();
            for (int i = 0; i < 1000; i++) {
                long amount = 1 + (i * 7 + round) % 9;
                d.add(StatKeys.BLOCKS_MINED, StatKeys.L_BLOCKS, "mod:block_" + i, amount);
                expected += amount;
            }
            data = data.apply(d, Budget.DEFAULT, 1);
        }
        assertEquals(expected, data.total(StatKeys.BLOCKS_MINED));
        adds(data, StatKeys.L_BLOCKS);
        assertEquals(1000, data.detail(StatKeys.L_BLOCKS).entries().size(), "a list is as complete as the counting (1,000 kinds all listed)");
        assertEquals(0, data.detail(StatKeys.L_BLOCKS).other());
    }

    @Test
    void budgetMovesNewKeysIntoOtherAndKeepsTheSum() {
        StatData data = StatData.EMPTY;
        long expected = 0;
        for (int i = 0; i < 2000; i++) {
            data = data.apply(new Delta().add(StatKeys.KILLS, StatKeys.L_KILLS, "mod:mob_with_a_long_name_" + i, 3), SMALL, 1);
            expected += 3;
        }
        adds(data, StatKeys.L_KILLS);
        assertEquals(expected, data.total(StatKeys.KILLS));
        assertTrue(data.detail(StatKeys.L_KILLS).entries().size() <= SMALL.maxEntriesPerList(), "entry cap holds");
        assertTrue(data.detail(StatKeys.L_KILLS).other() > 0, "the rest is counted as other");
        assertTrue(data.bytes() <= SMALL.maxBytes(), "budget holds: " + data.bytes());
        // a known key keeps counting after the budget is used up
        String known = data.detail(StatKeys.L_KILLS).entries().keySet().iterator().next();
        long before = data.detail(StatKeys.L_KILLS).entries().get(known);
        data = data.apply(new Delta().add(StatKeys.KILLS, StatKeys.L_KILLS, known, 5), SMALL, 1);
        assertEquals(before + 5, data.detail(StatKeys.L_KILLS).entries().get(known));
        adds(data, StatKeys.L_KILLS);
    }

    @Test
    void entriesBudgetAppliesToTheWholeValueNotPerList() {
        StatData data = StatData.EMPTY;
        for (int i = 0; i < 300; i++) {
            data = data.apply(new Delta().add(StatKeys.BLOCKS_MINED, StatKeys.L_BLOCKS, "mod:b" + i, 1).add(StatKeys.KILLS, StatKeys.L_KILLS, "mod:m" + i, 1), SMALL, 1);
        }
        assertTrue(data.bytes() <= SMALL.maxBytes(), "two lists share one budget: " + data.bytes());
        adds(data, StatKeys.L_BLOCKS);
        adds(data, StatKeys.L_KILLS);
    }

    @Test
    void recordsKeepTheMaximumAndFirstCountSetsTheDate() {
        StatData data = StatData.EMPTY.apply(new Delta().record(StatKeys.R_BIGGEST_HIT, 900).add(StatKeys.KILLS, 1), Budget.DEFAULT, 20000);
        data = data.apply(new Delta().record(StatKeys.R_BIGGEST_HIT, 400), Budget.DEFAULT, 20005);
        assertEquals(900, data.record(StatKeys.R_BIGGEST_HIT));
        assertEquals(20000, data.since(), "the day of the first count stays");
        data = data.apply(new Delta().record(StatKeys.R_BIGGEST_HIT, 1200), Budget.DEFAULT, 20005);
        assertEquals(1200, data.record(StatKeys.R_BIGGEST_HIT));
    }

    @Test
    void emptyDeltaChangesNothingAndAnIdIsOnlyGivenOnce() {
        assertSame(StatData.EMPTY, StatData.EMPTY.apply(new Delta().add(StatKeys.KILLS, 0), Budget.DEFAULT, 1));
        StatData withId = StatData.EMPTY.withUid(7);
        assertEquals(7, withId.uid());
        assertEquals(7, withId.withUid(9).uid());
        assertTrue(StatData.EMPTY.isBlank());
        assertTrue(withId.isBlank(), "an item that only got an id shows nothing");
    }

    @Test
    void valueOfANewerVersionIsNeverChanged() {
        StatData future = StatData.sanitize(StatData.VERSION + 1, 5, 1, Map.of(StatKeys.KILLS, 4L), Map.of(), Map.of(), Budget.DEFAULT);
        assertFalse(future.writable());
        assertSame(future, future.apply(new Delta().add(StatKeys.KILLS, 1), Budget.DEFAULT, 1));
        assertSame(future, future.withUid(3));
        assertEquals(4, future.total(StatKeys.KILLS), "but it is still readable");
    }

    @Test
    void decodedDataIsMadeSafe() {
        Map<String, Long> totals = new LinkedHashMap<>();
        totals.put(StatKeys.KILLS, -5L);
        totals.put("", 3L);
        totals.put("x".repeat(StatData.MAX_KEY + 1), 3L);
        Map<String, Long> entries = new LinkedHashMap<>();
        entries.put("mod:good", 4L);
        entries.put("mod:negative", -9L);
        entries.put("x".repeat(500), 6L);
        Map<String, StatData.Detail> details = Map.of(StatKeys.L_KILLS, new StatData.Detail(entries, 2L));
        StatData data = StatData.sanitize(StatData.VERSION, 1, -4, totals, details, Map.of(), Budget.DEFAULT);
        assertEquals(0, data.total(StatKeys.KILLS), "negative total becomes 0");
        assertEquals(1, data.totals().size(), "blank and oversized keys dropped");
        assertEquals(0, data.since());
        StatData.Detail kills = data.detail(StatKeys.L_KILLS);
        assertEquals(Map.of("mod:good", 4L, "mod:negative", 0L), kills.entries());
        assertEquals(2 + 6, kills.other(), "an invalid key moves into other, nothing is lost");
    }

    @Test
    void decodedOverflowMovesIntoOther() {
        Map<String, Long> entries = new LinkedHashMap<>();
        long sum = 0;
        for (int i = 0; i < 5000; i++) {
            entries.put("mod:entry_" + i, 2L);
            sum += 2;
        }
        StatData data = StatData.sanitize(StatData.VERSION, 1, 1, Map.of(StatKeys.BLOCKS_MINED, sum), Map.of(StatKeys.L_BLOCKS, new StatData.Detail(entries, 0)), Map.of(), SMALL);
        assertTrue(data.bytes() <= SMALL.maxBytes());
        assertEquals(sum, data.detail(StatKeys.L_BLOCKS).sum(), "the sum survives truncation");
    }

    @Test
    void additionDoesNotOverflow() {
        StatData data = StatData.EMPTY.apply(new Delta().add(StatKeys.KILLS, Long.MAX_VALUE - 1), Budget.DEFAULT, 1);
        data = data.apply(new Delta().add(StatKeys.KILLS, 10), Budget.DEFAULT, 1);
        assertEquals(Long.MAX_VALUE, data.total(StatKeys.KILLS));
    }

    @Test
    void budgetIsClamped() {
        assertEquals(2048, new Budget(1, 1).maxBytes());
        assertEquals(262144, new Budget(Integer.MAX_VALUE, 5).maxBytes());
        assertEquals(16, new Budget(5000, 0).maxEntriesPerList());
    }

    @Test
    void equalValuesAreEqualSoStacksOfTheSameStatsMerge() {
        StatData a = StatData.EMPTY.apply(new Delta().add(StatKeys.KILLS, StatKeys.L_KILLS, "a:b", 1), Budget.DEFAULT, 1).withUid(5);
        StatData b = StatData.EMPTY.apply(new Delta().add(StatKeys.KILLS, StatKeys.L_KILLS, "a:b", 1), Budget.DEFAULT, 1).withUid(5);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
