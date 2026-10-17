package com.simplebuilding.modules.simpletweaks.admin.blocklog;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.simplebuilding.modules.simpletweaks.admin.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;

/** /inspect [page n] and /tweaks rollback &lt;player&gt; &lt;time&gt; [radius]. */
public final class BlockLogCommands {
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
        d.register(Commands.literal("tweaks").then(Commands.literal("rollback")
                .requires(s -> Admin.on() && AdminPerms.has(s, Admin.config().blockLog().rollbackPermission()))
                .then(Commands.argument("player", StringArgumentType.word()).then(Commands.argument("time", StringArgumentType.word())
                        .executes(c -> rollback(c.getSource(), StringArgumentType.getString(c, "player"), StringArgumentType.getString(c, "time"), -1))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 4096))
                                .executes(c -> rollback(c.getSource(), StringArgumentType.getString(c, "player"), StringArgumentType.getString(c, "time"), IntegerArgumentType.getInteger(c, "radius"))))))));
    }

    static int rollback(CommandSourceStack s, String who, String time, int radius) {
        long span = Inspect.parseDuration(time);
        if (span <= 0) { s.sendFailure(Component.translatable("simpletweaks.admin.blocklog.badtime", time)); return 0; }
        var profile = AdminPlayers.resolve(s.getServer(), who);
        if (profile == null) { s.sendFailure(Component.translatable("simpletweaks.admin.unknown_player", who)); return 0; }
        var server = s.getServer();
        var limit = Admin.config().blockLog().rollbackMaxChanges();
        BlockLog.byPlayer(server, profile.id(), BlockLog.clock.millis() - span, radius > 0 ? s.getLevel() : null,
                radius > 0 ? net.minecraft.core.BlockPos.containing(s.getPosition()) : null, radius, limit).thenAccept(list -> server.execute(() -> {
            var r = Rollback.apply(server, list);
            s.sendSuccess(() -> Component.translatable("simpletweaks.admin.blocklog.rolledback", r.undone(), r.skipped(), profile.name(), time).withStyle(ChatFormatting.GREEN), true);
        }));
        s.sendSuccess(() -> Component.translatable("simpletweaks.admin.blocklog.rollback.started", profile.name(), time), false);
        return 1;
    }
    private BlockLogCommands() {}
}
