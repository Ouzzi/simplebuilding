package com.simplebuilding.modules.simplestats.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.simplebuilding.modules.simplestats.data.StatData.Budget;
import com.simplebuilding.modules.simplestats.data.StatData.Delta;
import com.simplebuilding.modules.simplestats.data.StatRows.Kind;
import com.simplebuilding.modules.simplestats.data.StatRows.Row;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Rows of the statistics screen. <b>What breaks this test:</b> a counted entry missing from the screen, lists not sorted by
 * amount, sections without rows, totals in the wrong order, or an empty item producing rows.
 */
class StatRowsTest {
    @Test
    void emptyItemHasNoRows() {
        assertTrue(StatRows.build(StatData.EMPTY).isEmpty());
        assertTrue(StatRows.build(StatData.EMPTY.withUid(3)).isEmpty());
    }

    @Test
    void everyCountedEntryAppearsSortedByAmount() {
        Delta d = new Delta();
        for (int i = 0; i < 700; i++) d.add(StatKeys.BLOCKS_MINED, StatKeys.L_BLOCKS, "mod:block_" + i, 1 + i % 13);
        d.add(StatKeys.KILLS, StatKeys.L_KILLS, "minecraft:zombie", 4).entry(StatKeys.L_KILL_VARIANTS, "minecraft:zombie#baby", 4);
        d.record(StatKeys.R_BIGGEST_HIT, 700);
        StatData data = StatData.EMPTY.apply(d, Budget.DEFAULT, 20000);
        List<Row> rows = StatRows.build(data);
        assertEquals(Kind.SECTION, rows.get(0).kind());
        assertEquals("overview", rows.get(0).stat());
        long blockEntries = rows.stream().filter(r -> r.kind() == Kind.ENTRY && r.stat().equals(StatKeys.L_BLOCKS)).count();
        assertEquals(700, blockEntries, "all counted blocks are shown");
        long previous = Long.MAX_VALUE;
        for (Row r : rows) {
            if (r.kind() == Kind.ENTRY && r.stat().equals(StatKeys.L_BLOCKS)) {
                assertTrue(r.value() <= previous, "sorted by amount");
                previous = r.value();
                assertEquals(data.detail(StatKeys.L_BLOCKS).sum(), r.whole());
            }
        }
        assertTrue(rows.stream().anyMatch(r -> r.kind() == Kind.RECORD && r.stat().equals(StatKeys.R_BIGGEST_HIT)));
        assertTrue(rows.stream().anyMatch(r -> r.kind() == Kind.SINCE && r.value() == 20000));
    }

    @Test
    void totalsFollowTheFixedOrderAndOtherComesLast() {
        Delta d = new Delta().add(StatKeys.DISTANCE_WALKED, 500).add(StatKeys.KILLS, StatKeys.L_KILLS, "a:b", 1).add(StatKeys.BLOCKS_MINED, 3);
        StatData small = StatData.EMPTY.apply(d, Budget.DEFAULT, 1);
        List<String> order = StatRows.build(small).stream().filter(r -> r.kind() == Kind.TOTAL).map(Row::stat).toList();
        assertEquals(List.of(StatKeys.BLOCKS_MINED, StatKeys.KILLS, StatKeys.DISTANCE_WALKED), order);
        StatData over = StatData.sanitize(1, 1, 1, java.util.Map.of(StatKeys.KILLS, 9L),
                java.util.Map.of(StatKeys.L_KILLS, new StatData.Detail(java.util.Map.of("a:b", 4L), 5L)), java.util.Map.of(), Budget.DEFAULT);
        List<Row> rows = StatRows.build(over);
        Row last = rows.get(rows.size() - 1);
        assertEquals(Kind.OTHER, last.kind());
        assertEquals(5, last.value());
    }

    @Test
    void unknownKeysOfNewerVersionsStillShow() {
        StatData data = StatData.sanitize(StatData.VERSION + 1, 1, 1, java.util.Map.of("future_stat", 3L, StatKeys.KILLS, 1L), java.util.Map.of(), java.util.Map.of(), Budget.DEFAULT);
        List<String> totals = StatRows.build(data).stream().filter(r -> r.kind() == Kind.TOTAL).map(Row::stat).toList();
        assertEquals(List.of(StatKeys.KILLS, "future_stat"), totals);
    }
}
