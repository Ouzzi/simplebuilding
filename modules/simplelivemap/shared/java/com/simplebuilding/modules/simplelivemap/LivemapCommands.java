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
 * {@code /simplelivemap status|link|visibility private|public} (game masters, level 2) and {@code /simplelivemap hide|show}
 * (every player: the own position on the web map; shown only when the server has {@code web.showPlayers}). The link with
 * the token goes only to the asking source (never to other operators or the log). Options: {@code /simplelivemap config}
 * (SimpleLib). Texts have English fallbacks for clients without the mod (the mod is server side only).
 */
public final class LivemapCommands {
    private LivemapCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal(SimpleLivemap.MOD_ID)
                .then(Commands.literal("status").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> status(c.getSource())))
                .then(Commands.literal("link").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> link(c.getSource())))
                .then(Commands.literal("visibility").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal(LivemapConfig.PRIVATE).executes(c -> visibility(c.getSource(), LivemapConfig.PRIVATE)))
                        .then(Commands.literal(LivemapConfig.PUBLIC).executes(c -> visibility(c.getSource(), LivemapConfig.PUBLIC))))
                .then(Commands.literal("hide").executes(c -> hide(c.getSource(), true)))
                .then(Commands.literal("show").executes(c -> hide(c.getSource(), false))));
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
        String base = "http://" + web.settings().bind() + ":" + web.port() + "/";
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
