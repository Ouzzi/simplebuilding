package com.simplebuilding.modules.simpletweaks.play.events;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.simplebuilding.modules.simpletweaks.play.*;
import java.time.LocalDateTime;
import net.minecraft.commands.*;
import net.minecraft.server.MinecraftServer;

/**
 * Weather schedule (server, off by default): lines of {@link WeatherSchedule} set Vanilla weather
 * ({@code MinecraftServer.setWeatherParameters}, no Simple Weather class needed) and can start a server event with it.
 * {@code /tweaks weather now <kind> [minutes]} does the same on demand, {@code /tweaks weather schedule} lists the lines.
 */
public final class WeatherPlan {
    private static int lastMinute = -1;

    private WeatherPlan() {}

    public static boolean enabled() { return ServerEvents.enabled() && Play.config().events().weatherScheduleEnabled(); }

    public static void register(CommandDispatcher<CommandSourceStack> d, PlayConfig c) {
        if (!c.events().enabled()) return;
        var now = Commands.literal("now");
        for (var kind : WeatherSchedule.KINDS) now.then(Commands.literal(kind)
                .executes(x -> apply(x.getSource().getServer(), kind, 10, null) ? 1 : 0)
                .then(Commands.argument("minutes", IntegerArgumentType.integer(1, 1440))
                        .executes(x -> apply(x.getSource().getServer(), kind, IntegerArgumentType.getInteger(x, "minutes"), null) ? 1 : 0)));
        d.register(Commands.literal("tweaks").then(Commands.literal("weather").requires(s -> PlayPerms.has(s, c.events().permission()))
                .then(now)
                .then(Commands.literal("schedule").executes(x -> {
                    var lines = Play.config().events().weatherSchedule();
                    x.getSource().sendSuccess(() -> lines.isEmpty() ? PlayText.t("weather.none") : PlayText.t("weather.list", Play.config().events().weatherClock(), String.join("; ", lines)), false);
                    return lines.size();
                }))));
    }

    /** Sets the weather for {@code minutes} (clear: that long without rain) and starts {@code event} when given. */
    public static boolean apply(MinecraftServer server, String kind, int minutes, String event) {
        if (!WeatherSchedule.KINDS.contains(kind)) return false;
        int ticks = minutes * 1200;
        switch (kind) {
            case "rain" -> server.setWeatherParameters(0, ticks, true, false);
            case "thunder" -> server.setWeatherParameters(0, ticks, true, true);
            default -> server.setWeatherParameters(ticks, 0, false, false);
        }
        if (event != null) ServerEvents.start(server, event, minutes);
        return true;
    }

    /** Lines due now on the chosen clock (pure, for tests). */
    public static java.util.List<WeatherSchedule> due(java.util.List<String> lines, String clockKind, LocalDateTime real, long gameClockTicks) {
        var out = new java.util.ArrayList<WeatherSchedule>();
        for (var line : lines) {
            var s = WeatherSchedule.parse(line);
            if (s != null && (clockKind.equals("game") ? s.startsAtGame(gameClockTicks) : s.startsAt(real))) out.add(s);
        }
        return out;
    }

    public static void tick(MinecraftServer server) {
        if (!enabled()) return;
        var c = Play.config().events();
        if (server.getTickCount() % 20 != 0) return;
        boolean game = c.weatherClock().equals("game");
        if (!game) {
            var now = LocalDateTime.now(ServerEvents.clock);
            int minute = now.getHour() * 60 + now.getMinute();
            if (minute == lastMinute) return;
            lastMinute = minute;
        }
        var due = due(c.weatherSchedule(), c.weatherClock(), LocalDateTime.now(ServerEvents.clock), server.overworld().getOverworldClockTime());
        if (due.isEmpty()) return;
        var s = due.get(0);
        // Weather switched off by Simple Weather: no weather change, a combined event still starts.
        if (!com.simplebuilding.modules.simpletweaks.play.vote.Votes.weatherOff()) apply(server, s.kind(), s.minutes(), s.event());
        else if (s.event() != null) ServerEvents.start(server, s.event(), s.minutes());
    }

    public static void shutdown() { lastMinute = -1; }
}
