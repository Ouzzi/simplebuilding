package com.simplebuilding.modules.simplestats.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import org.junit.jupiter.api.Test;

/** Number, distance and time text. <b>What breaks this test:</b> wrong unit conversion (cm, hundredths of health, ticks) or a locale mix-up. */
class StatFormatTest {
    @Test
    void numbers() {
        assertEquals("5,472", StatFormat.full(5472, Locale.US));
        assertEquals("5.472", StatFormat.full(5472, Locale.GERMANY));
        assertEquals("9,999", StatFormat.compact(9999, Locale.US));
        assertEquals("12.4k", StatFormat.compact(12_400, Locale.US));
        assertEquals("12,4k", StatFormat.compact(12_400, Locale.GERMANY));
        assertEquals("12k", StatFormat.compact(12_000, Locale.US));
        assertEquals("1.2M", StatFormat.compact(1_234_567, Locale.US));
    }

    @Test
    void hearts() {
        assertEquals("1 \u2665", StatFormat.hearts(200, Locale.US));
        assertEquals("1.5 \u2665", StatFormat.hearts(300, Locale.US));
        assertEquals("0 \u2665", StatFormat.hearts(0, Locale.US));
    }

    @Test
    void distances() {
        assertEquals("123 m", StatFormat.distance(12_345, Locale.US));
        assertEquals("1.2 km", StatFormat.distance(123_456, Locale.US));
        assertEquals("0 m", StatFormat.distance(99, Locale.US));
    }

    @Test
    void durations() {
        assertEquals("42 s", StatFormat.duration(42 * 20));
        assertEquals("12 min", StatFormat.duration(12 * 60 * 20));
        assertEquals("5 h 12 min", StatFormat.duration((5 * 60 + 12) * 60 * 20));
        assertEquals("3 d 4 h", StatFormat.duration((3 * 24 + 4) * 3600L * 20));
        assertEquals("0 s", StatFormat.duration(-5));
    }

    @Test
    void valuesByUnitAndDates() {
        assertEquals("37 m", StatFormat.value(StatKeys.Unit.BLOCKS, 37, Locale.US, false));
        assertEquals("12.4k", StatFormat.value(StatKeys.Unit.COUNT, 12_400, Locale.US, true));
        assertEquals("12,400", StatFormat.value(StatKeys.Unit.COUNT, 12_400, Locale.US, false));
        assertEquals("2026-10-15", StatFormat.date(20_741));
        assertEquals("", StatFormat.date(0));
        assertEquals(Locale.US, StatFormat.locale(""));
        assertEquals("de", StatFormat.locale("de_de").getLanguage());
    }
}
