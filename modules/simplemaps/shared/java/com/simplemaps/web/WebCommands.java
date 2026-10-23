package com.simplemaps.web;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.simplemaps.MapsComponents;
import com.simplemaps.MapsConfig;
import com.simplemaps.MapsItems;
import com.simplemaps.WayfinderData;
import com.simplemaps.Waypoints;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * {@code /simplemaps web export [full]|status} (game masters, permission level 2), {@code /simplemaps web show|hide} (every
 * player: show or hide the own position on the web map; default hidden) and {@code /simplemaps web waypoints share|unshare}
 * (every player: share a snapshot of the held map's waypoints, or take it back; default nothing shared). Texts have English fallbacks for servers whose
 * clients lack the mod.
 */
public final class WebCommands {
    private WebCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("simplemaps").then(Commands.literal("web")
                .then(Commands.literal("export").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> export(c.getSource(), false))
                        .then(Commands.literal("full").executes(c -> export(c.getSource(), true))))
                .then(Commands.literal("status").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> status(c.getSource())))
                .then(Commands.literal("show").executes(c -> show(c.getSource(), true)))
                .then(Commands.literal("hide").executes(c -> show(c.getSource(), false)))
                .then(Commands.literal("waypoints")
                        .then(Commands.literal("share").executes(c -> share(c.getSource())))
                        .then(Commands.literal("unshare").executes(c -> unshare(c.getSource()))))));
    }

    private static Component text(String key, String fallback, Object... args) {
        return Component.translatableWithFallback("commands.simplemaps.web." + key, fallback, args);
    }

    static int export(CommandSourceStack source, boolean full) {
        var server = source.getServer();
        switch (WebMap.start(server, full)) {
            case DISABLED -> source.sendFailure(text("disabled", "The web map export is off (webEnabled in %s).", MapsConfig.FILE));
            case AUTO_OFF -> source.sendFailure(text("auto_off", "The web map export is off because %s is loaded.", WebMap.autoOff()));
            case RUNNING -> source.sendFailure(text("running", "A web map export is already running."));
            case STARTED -> {
                WebExport job = WebMap.running(server);
                if (job.incremental()) {
                    source.sendSuccess(() -> text("started_changed", "Web map export started: changed tiles of %s in %s dimensions.", job.tilesTotal(), job.dimensions()), true);
                } else {
                    source.sendSuccess(() -> text("started", "Web map export started: %s tiles in %s dimensions.", job.tilesTotal(), job.dimensions()), true);
                }
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

    /** The wayfinder map in the main hand, else in the off hand, or empty. */
    private static ItemStack heldMap(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (MapsItems.isWayfinder(stack) && stack.get(MapsComponents.MAP_ID) != null) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** Shares a snapshot of the held map's waypoints (stage 3); later changes need another share. */
    static int share(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = heldMap(player);
        int id = stack.isEmpty() ? -1 : stack.get(MapsComponents.MAP_ID).id();
        var dimension = id < 0 ? java.util.Optional.<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>>empty()
                : WayfinderData.get(source.getServer(), id).dimension();
        if (dimension.isEmpty()) {
            source.sendFailure(text("waypoints.no_map", "Hold a wayfinder map to share its waypoints."));
            return 0;
        }
        int n = WebMap.share(source.getServer(), player.getUUID(), id, dimension.get(), stack.getOrDefault(MapsComponents.WAYPOINTS, Waypoints.EMPTY));
        source.sendSuccess(() -> MapsConfig.webWaypoints
                ? text("waypoints.shared", "%s waypoints of this map are shared on the web map (from the next export; share again after changes).", n)
                : text("waypoints.shared_off", "%s waypoints of this map are shared; the server does not show waypoints on the web map (webWaypoints).", n), false);
        return 1;
    }

    /** Takes back the held map's share, or every own share when no map is held. */
    static int unshare(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = heldMap(player);
        int maps = WebMap.unshare(source.getServer(), player.getUUID(), stack.isEmpty() ? -1 : stack.get(MapsComponents.MAP_ID).id());
        source.sendSuccess(() -> text("waypoints.unshared", "Waypoints of %s maps are no longer shared (from the next export).", maps), false);
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
