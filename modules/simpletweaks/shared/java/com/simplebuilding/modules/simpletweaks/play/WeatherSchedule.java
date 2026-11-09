package com.simplebuilding.modules.simpletweaks.play;

import java.time.*;
import java.util.List;
import java.util.Locale;

/**
 * One weather schedule line: {@code "SAT 20:00 thunder 30"} or {@code "DAILY 20:00 rain 30 blood_moon"} (day MON-SUN or
 * DAILY on the real clock, {@code D3} = every 3rd in-game day on the game clock; time HH:MM; kind rain, thunder or clear;
 * minutes; optional server event started together with it).
 */
public record WeatherSchedule(DayOfWeek day, int everyDays, LocalTime time, String kind, int minutes, String event) {
    public static final List<String> KINDS = List.of("rain", "thunder", "clear");

    public static WeatherSchedule parse(String line) {
        if (line == null) return null;
        var parts = line.trim().split("\\s+");
        if (parts.length != 4 && parts.length != 5) return null;
        try {
            DayOfWeek day = null;
            int every = 0;
            String d = parts[0].toUpperCase(Locale.ROOT);
            if (d.matches("D\\d{1,3}")) {
                every = Integer.parseInt(d.substring(1));
                if (every < 1 || every > 365) return null;
            } else if (!d.equals("DAILY")) {
                for (var v : DayOfWeek.values()) if (v.name().startsWith(d) && d.length() >= 3) day = v;
                if (day == null) return null;
            }
            var time = LocalTime.parse(parts[1].length() == 4 ? "0" + parts[1] : parts[1]);
            if (!KINDS.contains(parts[2])) return null;
            int minutes = Integer.parseInt(parts[3]);
            if (minutes < 1 || minutes > 1440) return null;
            String event = parts.length == 5 ? parts[4] : null;
            if (event != null && !ServerEventsSchedule.EVENTS.contains(event)) return null;
            return new WeatherSchedule(day, every, time, parts[2], minutes, event);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Real clock: weekday and DAILY lines only. */
    public boolean startsAt(LocalDateTime now) {
        return everyDays == 0 && (day == null || now.getDayOfWeek() == day) && now.getHour() == time.getHour() && now.getMinute() == time.getMinute();
    }

    /** Game tick of the day (0 = 06:00) at which this line starts. */
    public long startTick() {
        return ((time.getHour() - 6 + 24) % 24) * 1000L + time.getMinute() * 1000L / 60;
    }

    /** Game clock: DAILY and D-lines. {@code clockTicks} is the overworld clock; true in the 20-tick window of the start. */
    public boolean startsAtGame(long clockTicks) {
        if (day != null) return false;
        long inDay = Math.floorMod(clockTicks, 24000L), dayNo = Math.floorDiv(clockTicks, 24000L);
        if (everyDays > 0 && dayNo % everyDays != 0) return false;
        long start = startTick();
        return inDay >= start && inDay < start + 20;
    }
}
