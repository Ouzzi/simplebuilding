package com.simplebuilding.modules.simplelivemap;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.simplebuilding.modules.simplelivemap.web.LivemapWeb;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /simplelivemap status|link|hint on|off} (game masters, level 2, and the single-player owner, also without cheats),
 * {@code /simplelivemap visibility private|public} (game masters) and {@code /simplelivemap hide|show}
 * (every player: the own position on the web map; shown only when the server has {@code web.showPlayers}). The link with
 * the token goes only to the asking source (never to other operators or the log). Options: {@code /simplelivemap config}
 * (SimpleLib). Texts have English fallbacks for clients without the mod (the mod is server side only).
 */
public final class LivemapCommands {
    private LivemapCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal(SimpleLivemap.MOD_ID)
                .then(Commands.literal("status").requires(LivemapCommands::masterOrOwner).executes(c -> status(c.getSource())))
                .then(Commands.literal("link").requires(LivemapCommands::masterOrOwner).executes(c -> link(c.getSource())))
                .then(Commands.literal("hint").requires(LivemapCommands::masterOrOwner)
                        .then(Commands.literal("on").executes(c -> hint(c.getSource(), true)))
                        .then(Commands.literal("off").executes(c -> hint(c.getSource(), false))))
                .then(Commands.literal("visibility").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal(LivemapConfig.PRIVATE).executes(c -> visibility(c.getSource(), LivemapConfig.PRIVATE)))
                        .then(Commands.literal(LivemapConfig.PUBLIC).executes(c -> visibility(c.getSource(), LivemapConfig.PUBLIC))))
                .then(Commands.literal("hide").executes(c -> hide(c.getSource(), true)))
                .then(Commands.literal("show").executes(c -> hide(c.getSource(), false))));
    }

    /** Game master (level 2) or the owner of a single-player/LAN world (who may lack cheats). */
    static boolean masterOrOwner(CommandSourceStack source) {
        if (Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source)) return true;
        ServerPlayer player = source.getPlayer();
        return player != null && !source.getServer().isDedicatedServer() && source.getServer().isSingleplayerOwner(player.nameAndId());
    }

    static Component text(String key, String fallback, Object... args) {
        return Component.translatableWithFallback("commands.simplelivemap." + key, fallback, args);
    }

    static int status(CommandSourceStack source) {
        VisitedStore store = LivemapRuntime.store();
        long chunks = 0;
        for (String dim : store.dimensions()) chunks += store.count(dim);
        long total = chunks;
        source.sendSuccess(() -> text("status", "Simple Livemap: %s explored chunks in %s dimensions, %s waiting to be drawn.",
                total, store.dimensions().size(), LivemapRuntime.queued()), false);
        LivemapWeb web = LivemapRuntime.web();
        if (web != null) {
            source.sendSuccess(() -> text("status.web", "Web map: http://%s:%s/ (%s, players %s).", web.settings().bind(), web.port(),
                    LivemapConfig.visibility, LivemapConfig.showPlayers ? "on" : "off"), false);
        } else {
            source.sendSuccess(() -> text("status.web_off", "Web map server: off (web.enabled in %s).", LivemapConfig.FILE), false);
        }
        return 1;
    }

    static String url(LivemapWeb web) {
        String bind = web.settings().bind();
        // A wildcard bind is no address a browser can open: the own machine is.
        String host = bind.equals("0.0.0.0") || bind.equals("::") || bind.equals("[::]") ? "127.0.0.1" : bind.contains(":") && !bind.startsWith("[") ? "[" + bind + "]" : bind;
        String base = "http://" + host + ":" + web.port() + "/";
        return LivemapConfig.isPublic() ? base : base + "#t=" + LivemapConfig.token;
    }

    static int link(CommandSourceStack source) {
        LivemapWeb web = LivemapRuntime.web();
        if (web == null) {
            source.sendFailure(text("status.web_off", "Web map server: off (web.enabled in %s).", LivemapConfig.FILE));
            return 0;
        }
        String url = url(web);
        // Only to this source: sendSystemMessage does not echo to other operators or the server log like sendSuccess(.., true).
        source.sendSystemMessage(text(LivemapConfig.isPublic() ? "link.public" : "link.private",
                LivemapConfig.isPublic() ? "Public web map: %s" : "Private web map (keep this link secret): %s", url).copy().withStyle(ChatFormatting.AQUA));
        return 1;
    }

    /**
     * The one chat line for the single-player owner (N29): text, "[Open Livemap]" (opens {@code url}, the private key after
     * #) and "[Don't show again]" ({@code /simplelivemap hint off}).
     */
    public static Component hintMessage(String url) {
        net.minecraft.network.chat.MutableComponent open = text("hint.open", "[Open Livemap]").copy().withStyle(style -> style
                .withColor(ChatFormatting.AQUA).withUnderlined(true)
                .withClickEvent(new net.minecraft.network.chat.ClickEvent.OpenUrl(java.net.URI.create(url)))
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(text("hint.open.hover", "Opens the map of this world in your browser."))));
        net.minecraft.network.chat.MutableComponent off = text("hint.off", "[Don't show again]").copy().withStyle(style -> style
                .withColor(ChatFormatting.GRAY)
                .withClickEvent(new net.minecraft.network.chat.ClickEvent.RunCommand("/" + SimpleLivemap.MOD_ID + " hint off"))
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(text("hint.off.hover", "Turns this message off (web.chatHint). /simplelivemap link still shows the link."))));
        return text("hint", "Simple Livemap: your world as a live map in the browser. ").copy()
                .append(open).append(" ").append(off);
    }

    static int hint(CommandSourceStack source, boolean on) {
        LivemapConfig.chatHint = on;
        LivemapConfig.save(SimpleLivemap.configDir);
        source.sendSuccess(() -> on ? text("hint.on_done", "The live map chat hint shows again (once per session).")
                : text("hint.off_done", "The live map chat hint is off. /simplelivemap link shows the link."), false);
        return 1;
    }

    static int visibility(CommandSourceStack source, String value) {
        LivemapConfig.visibility = value;
        if (LivemapConfig.token.isEmpty()) LivemapConfig.token = LivemapConfig.newToken();
        LivemapConfig.save(SimpleLivemap.configDir);
        LivemapRuntime.syncWeb(source.getServer());
        source.sendSuccess(() -> text("visibility." + value, LivemapConfig.PUBLIC.equals(value)
                ? "The web map is now public: everyone who reaches the port sees it." : "The web map is now private: it needs the link with the key."), true);
        return 1;
    }

    static int hide(CommandSourceStack source, boolean hide) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LivemapRuntime.store().setHidden(player.getUUID(), hide);
        source.sendSuccess(() -> hide ? text("hide", "Your position no longer shows on the web map.")
                : text("show", "Your position shows on the web map when the server shows players."), false);
        return 1;
    }
}
