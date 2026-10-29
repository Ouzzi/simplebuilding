package com.simplebuilding.modules.simpletweaks.admin.blocklog;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.simplebuilding.modules.simpletweaks.admin.*;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * /inspect [page n], /tweaks lookup &lt;filter&gt; | page &lt;n&gt;, /tweaks rollback &lt;filter&gt; [--preview] (also the old
 * &lt;player&gt; &lt;time&gt; [radius]), /tweaks restore, /tweaks purge &lt;player&gt;; each with its own operator level.
 */
public final class BlockLogCommands {
    /** Rollback never touches hopper moves. */
    static final String ROLLBACK_ACTIONS = "BPTDSXF";
    private static final Map<String, List<BlockLog.Entry>> PAGES = new HashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> d, AdminConfig config) {
        if (!config.blockLog().enabled()) return;
        d.register(Commands.literal("inspect").requires(s -> Admin.on() && AdminPerms.has(s, Admin.config().blockLog().permission()))
                .executes(c -> {
                    boolean on = Inspect.toggle(c.getSource().getPlayerOrException());
                    c.getSource().sendSuccess(() -> Component.translatable(on ? "simpletweaks.admin.blocklog.inspect.on" : "simpletweaks.admin.blocklog.inspect.off"), false);
                    return 1;
                })
                .then(Commands.literal("page").then(Commands.argument("page", IntegerArgumentType.integer(1)).executes(c -> {
                    if (Inspect.page(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "page"))) return 1;
                    c.getSource().sendFailure(Component.translatable("simpletweaks.admin.blocklog.nothing"));
                    return 0;
                }))));
        d.register(Commands.literal("tweaks")
                .then(Commands.literal("lookup").requires(s -> Admin.on() && AdminPerms.has(s, Admin.config().blockLog().lookupPermission()))
                        .executes(c -> { c.getSource().sendFailure(Component.translatable("simpletweaks.admin.blocklog.filter.usage")); return 0; })
                        .then(Commands.literal("page").then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(c -> page(c.getSource(), IntegerArgumentType.getInteger(c, "page")) ? 1 : 0)))
                        .then(Commands.argument("filter", StringArgumentType.greedyString()).suggests(suggest(false))
                                .executes(c -> lookup(c.getSource(), StringArgumentType.getString(c, "filter")))))
                .then(Commands.literal("rollback").requires(s -> Admin.on() && AdminPerms.has(s, Admin.config().blockLog().rollbackPermission()))
                        .then(Commands.argument("filter", StringArgumentType.greedyString()).suggests(suggest(true))
                                .executes(c -> rollback(c.getSource(), StringArgumentType.getString(c, "filter")))))
                .then(Commands.literal("restore").requires(s -> Admin.on() && AdminPerms.has(s, Admin.config().blockLog().restorePermission()))
                        .executes(c -> restore(c.getSource())))
                .then(Commands.literal("purge").requires(s -> Admin.on() && AdminPerms.has(s, Admin.config().blockLog().purgePermission()))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(c -> purge(c.getSource(), StringArgumentType.getString(c, "player"))))));
    }

    private static SuggestionProvider<CommandSourceStack> suggest(boolean rollback) {
        return (c, b) -> {
            String all = b.getRemaining();
            int cut = all.lastIndexOf(' ') + 1;
            String last = all.substring(cut).toLowerCase(Locale.ROOT);
            var out = b.createOffset(b.getStart() + cut);
            var options = new ArrayList<>(LogFilter.KEYS);
            if (rollback) options.add("--preview");
            for (String a : LogFilter.ACTIONS.keySet()) options.add("action:" + a);
            for (String n : c.getSource().getOnlinePlayerNames()) options.add("player:" + n);
            for (String o : options) if (o.toLowerCase(Locale.ROOT).startsWith(last)) out.suggest(o);
            return out.buildFuture();
        };
    }

    private static LogFilter.Parsed parse(CommandSourceStack s, String text) {
        var p = LogFilter.parse(s.getServer(), text, BlockPos.containing(s.getPosition()), s.getLevel().dimension().identifier().toString(), BlockLog.clock.millis());
        if (p.error() != null) s.sendFailure(Component.translatable(p.error(), p.errorArg()));
        return p.error() == null ? p : null;
    }
    private static String key(CommandSourceStack s) { return s.getEntity() == null ? "console" : s.getEntity().getStringUUID(); }

    public static void forget(net.minecraft.server.level.ServerPlayer p) { PAGES.remove(p.getStringUUID()); }

    static int lookup(CommandSourceStack s, String text) {
        var p = parse(s, text);
        if (p == null) return 0;
        var server = s.getServer();
        String key = key(s);
        BlockLog.query(server, p.filter(), Admin.config().blockLog().lookupMaxResults()).thenAccept(list -> server.execute(() -> {
            PAGES.put(key, list);
            page(s, 1);
        }));
        return 1;
    }
    /** One page of the source's last lookup; false if there is none. */
    static boolean page(CommandSourceStack s, int page) {
        var list = PAGES.get(key(s));
        if (list == null) { s.sendFailure(Component.translatable("simpletweaks.admin.blocklog.lookup.nothing")); return false; }
        int pages = Math.max(1, (list.size() + Inspect.PAGE - 1) / Inspect.PAGE);
        int n = Math.clamp(page, 1, pages);
        s.sendSuccess(() -> Component.translatable("simpletweaks.admin.blocklog.lookup.header", list.size(), n, pages).withStyle(ChatFormatting.GOLD), false);
        if (list.isEmpty()) { s.sendSuccess(() -> Component.translatable("simpletweaks.admin.blocklog.none").withStyle(ChatFormatting.GRAY), false); return true; }
        long now = BlockLog.clock.millis();
        for (var e : list.subList((n - 1) * Inspect.PAGE, Math.min(list.size(), n * Inspect.PAGE)))
            s.sendSuccess(() -> Component.translatable("simpletweaks.admin.blocklog.lookup.line", Inspect.ago(now - e.time()), Inspect.action(e), e.x(), e.y(), e.z(), e.dimension()), false);
        if (pages > 1) s.sendSuccess(() -> Component.translatable("simpletweaks.admin.blocklog.lookup.more", n, pages).withStyle(ChatFormatting.GRAY), false);
        return true;
    }

    static int rollback(CommandSourceStack s, String text) {
        var p = parse(s, text);
        if (p == null) return 0;
        if (!p.hasTime() || !p.filter().narrowed()) { s.sendFailure(Component.translatable("simpletweaks.admin.blocklog.rollback.narrow")); return 0; }
        var server = s.getServer();
        String shown = text.replace("--preview", "").trim();
        BlockLog.query(server, p.filter(), Admin.config().blockLog().rollbackMaxChanges()).thenAccept(found -> server.execute(() -> {
            var list = new ArrayList<>(found);
            list.removeIf(e -> ROLLBACK_ACTIONS.indexOf(e.action()) < 0);
            var r = Rollback.apply(server, list, p.preview());
            if (p.preview()) s.sendSuccess(() -> Component.translatable("simpletweaks.admin.blocklog.rollback.preview", r.undone(), r.skipped(), shown).withStyle(ChatFormatting.YELLOW), false);
            else s.sendSuccess(() -> Component.translatable("simpletweaks.admin.blocklog.rolledback", r.undone(), r.skipped(), shown).withStyle(ChatFormatting.GREEN), true);
        }));
        s.sendSuccess(() -> Component.translatable("simpletweaks.admin.blocklog.rollback.started", shown), false);
        return 1;
    }
    static int restore(CommandSourceStack s) {
        if (Rollback.restorable() == 0) { s.sendFailure(Component.translatable("simpletweaks.admin.blocklog.restore.nothing")); return 0; }
        var r = Rollback.restore(s.getServer());
        s.sendSuccess(() -> Component.translatable("simpletweaks.admin.blocklog.restored", r.undone(), r.skipped()).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }
    static int purge(CommandSourceStack s, String who) {
        var profile = AdminPlayers.resolve(s.getServer(), who);
        if (profile == null) { s.sendFailure(Component.translatable("simpletweaks.admin.unknown_player", who)); return 0; }
        var server = s.getServer();
        BlockLog.purgePlayer(profile.id()).thenAccept(n -> server.execute(() ->
                s.sendSuccess(() -> Component.translatable("simpletweaks.admin.blocklog.purged", n, profile.name()).withStyle(ChatFormatting.GREEN), true)));
        return 1;
    }
    private BlockLogCommands() {}
}
