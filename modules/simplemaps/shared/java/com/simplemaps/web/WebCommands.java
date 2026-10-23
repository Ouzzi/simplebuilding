package com.simplemaps.web;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.simplemaps.MapsConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /simplemaps web export|status} (game masters, permission level 2) and {@code /simplemaps web show|hide} (every
 * player: show or hide the own position on the web map; default hidden). Texts have English fallbacks for servers whose
 * clients lack the mod.
 */
public final class WebCommands {
    private WebCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("simplemaps").then(Commands.literal("web")
                .then(Commands.literal("export").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> export(c.getSource())))
                .then(Commands.literal("status").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> status(c.getSource())))
                .then(Commands.literal("show").executes(c -> show(c.getSource(), true)))
                .then(Commands.literal("hide").executes(c -> show(c.getSource(), false)))));
    }

    private static Component text(String key, String fallback, Object... args) {
        return Component.translatableWithFallback("commands.simplemaps.web." + key, fallback, args);
    }

    static int export(CommandSourceStack source) {
        var server = source.getServer();
        switch (WebMap.start(server)) {
            case DISABLED -> source.sendFailure(text("disabled", "The web map export is off (webEnabled in %s).", MapsConfig.FILE));
            case AUTO_OFF -> source.sendFailure(text("auto_off", "The web map export is off because %s is loaded.", WebMap.autoOff()));
            case RUNNING -> source.sendFailure(text("running", "A web map export is already running."));
            case STARTED -> {
                WebExport job = WebMap.running(server);
                source.sendSuccess(() -> text("started", "Web map export started: %s tiles in %s dimensions.", job.tilesTotal(), job.dimensions()), true);
                return 1;
            }
        }
        return 0;
    }

    static int status(CommandSourceStack source) {
        var server = source.getServer();
        if (!WebMap.active()) {
            String mod = WebMap.autoOff();
            source.sendSuccess(() -> mod != null && MapsConfig.webEnabled ? text("auto_off", "The web map export is off because %s is loaded.", mod)
                    : text("disabled", "The web map export is off (webEnabled in %s).", MapsConfig.FILE), false);
        }
        WebExport job = WebMap.running(server);
        WebMap.Last last = WebMap.last(server);
        if (job != null) {
            source.sendSuccess(() -> text("status.running", "Web map export running: %s of %s tiles, %s chunks read.",
                    job.tilesDone(), job.tilesTotal(), job.chunksDone()), false);
        } else if (last != null) {
            long minutes = (System.currentTimeMillis() - last.finishedMillis()) / 60000;
            source.sendSuccess(() -> text("status.last", "Last web map export %s min ago: %s tiles in %s dimensions, folder %s.",
                    minutes, last.tiles(), last.dimensions(), last.dir().toString()), false);
        } else {
            source.sendSuccess(() -> text("status.never", "No web map export since the server started; folder %s.",
                    WebMap.exportDir(server).toString()), false);
        }
        WebHttp http = WebMap.http(server);
        if (http != null) {
            String url = "http://" + http.settings().bind() + ":" + http.port() + "/";
            source.sendSuccess(() -> text("status.http", "Web map server: %s (read only).", url), false);
        } else if (MapsConfig.webHttpEnabled) {
            source.sendSuccess(() -> text("status.http_off", "Web map server: not running (needs webEnabled; see the server log)."), false);
        }
        return 1;
    }

    static int show(CommandSourceStack source, boolean show) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        WebMap.setVisible(source.getServer(), player.getUUID(), show);
        source.sendSuccess(() -> show ? text("show", "Your position now shows on the web map (from the next export).")
                : text("hide", "Your position no longer shows on the web map (from the next export)."), false);
        return 1;
    }
}
