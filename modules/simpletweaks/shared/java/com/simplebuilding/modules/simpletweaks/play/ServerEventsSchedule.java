package com.simplebuilding.modules.simpletweaks.play;

import java.time.*;
import java.util.Locale;

/** One schedule line of T21: {@code "SAT 20:00 double_xp 60"} (day MON–SUN or DAILY, server time, event, minutes). */
public record ServerEventsSchedule(DayOfWeek day, LocalTime time, String event, int minutes) {
    public static final java.util.List<String> EVENTS = java.util.List.of("double_xp", "more_loot", "blood_moon");

    public static ServerEventsSchedule parse(String line) {
        var parts = line.trim().split("\\s+");
        if (parts.length != 4) return null;
        try {
            DayOfWeek day = null;
            String d = parts[0].toUpperCase(Locale.ROOT);
            if (!d.equals("DAILY")) {
                day = null;
                for (var v : DayOfWeek.values()) if (v.name().startsWith(d) && d.length() >= 3) day = v;
                if (day == null) return null;
            }
            var time = LocalTime.parse(parts[1].length() == 4 ? "0" + parts[1] : parts[1]);
            if (!EVENTS.contains(parts[2])) return null;
            int minutes = Integer.parseInt(parts[3]);
            if (minutes < 1 || minutes > 1440) return null;
            return new ServerEventsSchedule(day, time, parts[2], minutes);
        } catch (RuntimeException e) {
            return null;
        }
    }
    /** Does this entry start in the minute of {@code now}? */
    public boolean startsAt(LocalDateTime now) {
        return (day == null || now.getDayOfWeek() == day) && now.getHour() == time.getHour() && now.getMinute() == time.getMinute();
    }
}
