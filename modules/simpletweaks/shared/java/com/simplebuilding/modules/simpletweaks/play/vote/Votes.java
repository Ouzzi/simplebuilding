package com.simplebuilding.modules.simpletweaks.play.vote;

import com.mojang.brigadier.CommandDispatcher;
import com.simplebuilding.modules.simpletweaks.play.*;
import com.simplebuilding.modules.simpletweaks.play.afk.Afk;
import com.simplebuilding.modules.simpletweaks.play.events.ServerEvents;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * T23 votes: /vote weather clear, /vote event &lt;name&gt; (with server events on), /vote day|night (option
 * {@code dayNight}, off by default), /vote yes|no. One vote at a time with clickable [Yes] [No]; it passes as soon as
 * {@code percent} % of the active players (not AFK, not spectating) said yes, otherwise it fails after {@code seconds}.
 * Each topic then waits {@code cooldownSeconds}. The weather vote is not offered while Simple Weather switches weather off.
 */
public final class Votes {
    public record Vote(String topic, UUID starter, Set<UUID> yes, Set<UUID> no, long until) {}
    static Vote running;
    static final Map<String, Long> COOLDOWN = new HashMap<>();
    /** Tests: the action of a passed vote is replaced. */
    public static java.util.function.BiConsumer<MinecraftServer, String> action = Votes::apply;

    public static boolean active() { return Play.on() && Play.server() != null && Play.config().vote().enabled(); }
    public static Vote running() { return running; }

    public static void register(CommandDispatcher<CommandSourceStack> d, PlayConfig c) {
        if (!c.vote().enabled()) return;
        var root = Commands.literal("vote")
                .then(Commands.literal("yes").executes(x -> cast(x.getSource().getPlayerOrException(), true)))
                .then(Commands.literal("no").executes(x -> cast(x.getSource().getPlayerOrException(), false)));
        if (c.vote().weather()) root.then(Commands.literal("weather").then(Commands.literal("clear").executes(x -> start(x.getSource(), "weather_clear"))));
        if (c.vote().events() && c.events().enabled()) {
            var ev = Commands.literal("event");
            for (var e : ServerEvents.EVENTS) ev.then(Commands.literal(e).executes(x -> start(x.getSource(), "event_" + e)));
            root.then(ev);
        }
        if (c.vote().dayNight()) {
            root.then(Commands.literal("day").executes(x -> start(x.getSource(), "day")));
            root.then(Commands.literal("night").executes(x -> start(x.getSource(), "night")));
        }
        d.register(root);
    }

    /** Simple Weather's own switch: with weather switched off there is nothing to vote about. */
    public static boolean weatherOff() {
        if (!Play.modLoaded("simpleweather")) return false;
        try {
            var file = java.nio.file.Path.of("config/simpleweather.json");
            if (!java.nio.file.Files.isRegularFile(file)) return false;
            var json = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(file)).getAsJsonObject();
            return json.has("disableWeather") && json.get("disableWeather").getAsBoolean();
        } catch (Exception e) { return false; }
    }

    public static Component topic(String topic) {
        if (topic.startsWith("event_")) return PlayText.t("vote.topic.event", ServerEvents.name(topic.substring(6)));
        return PlayText.t("vote.topic." + topic);
    }

    static int start(CommandSourceStack source, String topic) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var p = source.getPlayerOrException();
        if (!active()) return 0;
        if (topic.equals("weather_clear") && weatherOff()) { source.sendFailure(PlayText.t("vote.weather_off")); return 0; }
        var r = start(source.getServer(), p, topic);
        if (r != null) source.sendFailure(r);
        return r == null ? 1 : 0;
    }
    /** Starts a vote; null if it started, otherwise the reason it did not. */
    public static Component start(MinecraftServer server, ServerPlayer starter, String topic) {
        if (running != null) return PlayText.t("vote.busy");
        long now = System.currentTimeMillis();
        Long cd = COOLDOWN.get(topic);
        if (cd != null && cd > now) return PlayText.t("vote.cooldown", (cd - now) / 1000 + 1);
        running = new Vote(topic, starter.getUUID(), new LinkedHashSet<>(), new LinkedHashSet<>(), now + Play.config().vote().seconds() * 1000L);
        running.yes().add(starter.getUUID());
        var line = PlayText.t("vote.started", starter.getDisplayName(), topic(topic), Play.config().vote().seconds()).withStyle(ChatFormatting.GOLD)
                .append(" ").append(PlayText.button(PlayText.t("vote.yes"), "/vote yes", ChatFormatting.GREEN))
                .append(" ").append(PlayText.button(PlayText.t("vote.no"), "/vote no", ChatFormatting.RED));
        server.getPlayerList().broadcastSystemMessage(line, false);
        check(server);
        return null;
    }
    static int cast(ServerPlayer p, boolean yes) {
        if (!active()) return 0;
        if (running == null) { p.sendSystemMessage(PlayText.t("vote.none").withStyle(ChatFormatting.RED)); return 0; }
        running.yes().remove(p.getUUID()); running.no().remove(p.getUUID());
        (yes ? running.yes() : running.no()).add(p.getUUID());
        p.sendSystemMessage(PlayText.t(yes ? "vote.counted_yes" : "vote.counted_no").withStyle(ChatFormatting.GRAY));
        check(p.level().getServer());
        return 1;
    }
    /** Players whose vote counts: online, not spectating, not AFK. */
    public static int eligible(MinecraftServer server) {
        int n = 0;
        for (var p : server.getPlayerList().getPlayers()) if (!p.isSpectator() && !Afk.afk(p)) n++;
        return Math.max(1, n);
    }
    public static boolean passes(int yes, int eligible, int percent) { return yes * 100 >= percent * eligible; }

    static void check(MinecraftServer server) {
        if (running == null) return;
        int yes = 0;
        for (var id : running.yes()) { var p = server.getPlayerList().getPlayer(id); if (p != null && !p.isSpectator()) yes++; }
        if (passes(yes, eligible(server), Play.config().vote().percent())) finish(server, true);
        else if (System.currentTimeMillis() >= running.until()) finish(server, false);
    }
    static void finish(MinecraftServer server, boolean passed) {
        var v = running;
        running = null;
        COOLDOWN.put(v.topic(), System.currentTimeMillis() + Play.config().vote().cooldownSeconds() * 1000L);
        server.getPlayerList().broadcastSystemMessage(PlayText.t(passed ? "vote.passed" : "vote.failed", topic(v.topic()))
                .withStyle(passed ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        if (passed) action.accept(server, v.topic());
    }
    static void apply(MinecraftServer server, String topic) {
        var source = server.createCommandSourceStack().withSuppressedOutput();
        switch (topic) {
            case "weather_clear" -> server.getCommands().performPrefixedCommand(source, "weather clear");
            case "day" -> server.getCommands().performPrefixedCommand(source, "time set day");
            case "night" -> server.getCommands().performPrefixedCommand(source, "time set night");
            default -> { if (topic.startsWith("event_")) ServerEvents.start(server, topic.substring(6), Play.config().events().defaultMinutes()); }
        }
    }
    public static void tick(MinecraftServer server) {
        if (running == null) return;
        if (!active()) { running = null; return; }
        if (server.getTickCount() % 20 == 0) check(server);
    }
    public static void left(ServerPlayer p) { if (running != null) { running.yes().remove(p.getUUID()); running.no().remove(p.getUUID()); } }
    /** Tests: no running vote, no cooldowns, the real action. */
    public static void reset() { running = null; COOLDOWN.clear(); action = Votes::apply; }
    private Votes() {}
}
