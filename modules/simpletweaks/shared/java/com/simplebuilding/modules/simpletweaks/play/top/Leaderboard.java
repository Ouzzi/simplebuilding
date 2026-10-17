package com.simplebuilding.modules.simpletweaks.play.top;

import com.mojang.brigadier.CommandDispatcher;
import com.simplebuilding.modules.simpletweaks.play.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.*;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/**
 * T22: /leaderboard &lt;playtime|deaths|kills|mined&gt; lists the top 10 of every known player (Vanilla statistics,
 * offline players from their files). The always-visible sidebar (objective {@code simpletweaks_top}, refreshed every
 * {@code refreshSeconds}) is off by default.
 */
public final class Leaderboard {
    public static final String OBJECTIVE = "simpletweaks_top";

    public static boolean active() { return Play.on() && Play.server() != null && Play.config().leaderboard().enabled(); }

    public static void register(CommandDispatcher<CommandSourceStack> d, PlayConfig c) {
        if (!c.leaderboard().enabled()) return;
        var root = Commands.literal("leaderboard");
        for (var kind : StatBoard.KINDS) root.then(Commands.literal(kind).executes(x -> show(x.getSource(), kind)));
        d.register(root);
    }
    /** Top 10 without vanished admins (T14) the viewer may not see; {@code viewer == null} = the public sidebar, which hides all of them. */
    public static java.util.List<StatBoard.Entry> top(MinecraftServer server, String kind, CommandSourceStack viewer) {
        return StatBoard.top(server, kind, 10, viewer == null ? com.simplebuilding.modules.simpletweaks.admin.vanish.Vanish::vanished : id -> Play.hidden(id, viewer));
    }
    static int show(CommandSourceStack source, String kind) {
        if (!active()) return 0;
        var server = source.getServer();
        var top = top(server, kind, source);
        var out = PlayText.t("top.title", PlayText.t("top." + kind)).withStyle(ChatFormatting.GOLD);
        int rank = 1;
        for (var e : top) {
            Component value = kind.equals("playtime") ? PlayText.duration(e.value() * 50) : Component.literal(Long.toString(e.value()));
            out.append(Component.literal("\n" + rank++ + ". ").withStyle(ChatFormatting.GRAY)).append(Component.literal(e.name()).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY)).append(value.copy().withStyle(ChatFormatting.GREEN));
        }
        source.sendSuccess(() -> out, false);
        return top.size();
    }

    public static void tick(MinecraftServer server) {
        if (!active() || !Play.config().leaderboard().sidebar()) return;
        if (server.getTickCount() % (Play.config().leaderboard().refreshSeconds() * 20) != 0) return;
        refresh(server);
    }
    /** Rebuilds the sidebar objective (top 10 of the configured kind; play time in hours). */
    public static void refresh(MinecraftServer server) {
        var c = Play.config().leaderboard();
        Scoreboard board = server.getScoreboard();
        var old = board.getObjective(OBJECTIVE);
        if (old != null) board.removeObjective(old);
        var obj = board.addObjective(OBJECTIVE, ObjectiveCriteria.DUMMY, PlayText.t("top." + c.sidebarStat()).withStyle(ChatFormatting.GOLD),
                ObjectiveCriteria.RenderType.INTEGER, true, null);
        for (var e : top(server, c.sidebarStat(), null)) {
            long v = c.sidebarStat().equals("playtime") ? e.value() / 72000 : e.value();
            board.getOrCreatePlayerScore(ScoreHolder.forNameOnly(e.name()), obj).set((int) Math.min(Integer.MAX_VALUE, v));
        }
        board.setDisplayObjective(DisplaySlot.SIDEBAR, obj);
    }
    /** Server stop: a sidebar left from an earlier setting is removed when the sidebar is off now. */
    public static void stop(MinecraftServer server) {
        if (Play.config().leaderboard().sidebar()) return;
        var obj = server.getScoreboard().getObjective(OBJECTIVE);
        if (obj != null) server.getScoreboard().removeObjective(obj);
    }
    private Leaderboard() {}
}
