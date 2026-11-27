package com.simplebuilding.modules.simplelivemap;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.simplebuilding.modules.simplelivemap.hosted.HostedClient;
import com.simplebuilding.modules.simplelivemap.hosted.HostedLink;
import com.simplebuilding.modules.simplelivemap.hosted.HostedUploader;
import com.simplebuilding.modules.simplelivemap.web.LivemapWeb;
import com.simplebuilding.modules.simplelivemap.web.SelfHostLock;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;
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
 * N34: {@code /simplelivemap connect <code>} and {@code disconnect} (game masters and the single-player owner) bind THIS world to
 * livemap.simplemods.de with a one-time code from the website; the network part runs on its own thread, the answer comes back
 * on the server thread. The upload key is stored in the world folder and never shown.
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
                .then(Commands.literal("connect").requires(LivemapCommands::masterOrOwner)
                        .then(Commands.argument("code", StringArgumentType.word()).executes(c -> connect(c.getSource(), StringArgumentType.getString(c, "code")))))
                .then(Commands.literal("disconnect").requires(LivemapCommands::masterOrOwner).executes(c -> disconnect(c.getSource())))
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
        } else if (LivemapRuntime.blocked().equals("lock")) {
            SelfHostLock.Holder h = SelfHostLock.holder(SimpleLivemap.configDir);
            source.sendSuccess(() -> text("status.web_lock", "Web map server: off - another live map of this installation runs (world %s, port %s). Self-hosting serves exactly one map.",
                    h == null ? "?" : h.world(), h == null ? "?" : h.port()), false);
        } else if (LivemapRuntime.blocked().startsWith("port:")) {
            source.sendSuccess(() -> text("status.web_port", "Web map server: off - another Simple Livemap serves port %s. Self-hosting serves exactly one map.",
                    LivemapRuntime.blocked().substring(5)), false);
        } else {
            source.sendSuccess(() -> text("status.web_off", "Web map server: off (web.enabled in %s).", LivemapConfig.FILE), false);
        }
        HostedUploader up = LivemapRuntime.hosted();
        if (up != null) {
            source.sendSuccess(() -> text("status.hosted", "Hosted map: %s (%s, %s %%).", up.link().map(), up.state(), up.progress()), false);
        } else {
            source.sendSuccess(() -> text("status.hosted_off", "Hosted map: not connected (/simplelivemap connect <code> with a code from livemap.simplemods.de)."), false);
        }
        return 1;
    }

    /** {@code /simplelivemap connect <code>}: one-time code from the website -> the key of this world's map (stored in the world). */
    static int connect(CommandSourceStack source, String raw) {
        String code = HostedClient.normalizeCode(raw);
        if (code == null) {
            source.sendFailure(text("connect.bad_code", "That is no connect code (8 letters and digits from livemap.simplemods.de)."));
            return 0;
        }
        MinecraftServer server = source.getServer();
        String world = LivemapRuntime.worldId();
        if (world == null || !SimpleLivemap.on()) {
            source.sendFailure(text("connect.off", "Simple Livemap is off on this server."));
            return 0;
        }
        if (LivemapRuntime.hosted() != null) {
            source.sendFailure(text("connect.already", "This world is already connected (map %s). /simplelivemap disconnect first.", LivemapRuntime.hosted().link().map()));
            return 0;
        }
        String endpoint = LivemapConfig.hostedEndpoint;
        Path root = LivemapRuntime.root(server);
        source.sendSuccess(() -> text("connect.started", "Connecting this world to %s ...", endpoint), false);
        Thread t = new Thread(() -> {
            try {
                HostedClient.Answer a = new HostedClient(endpoint, "").connect(code, world, SimpleLivemap.version);
                if (a.ok() && a.json().has("key")) {
                    HostedLink link = new HostedLink(endpoint, string(a, "map"), string(a, "viewer"), a.json().get("key").getAsString());
                    link.save(root);
                    server.execute(() -> {
                        LivemapRuntime.startHosted(link);
                        source.sendSuccess(() -> connectedMessage(link.viewer()), false);
                    });
                    return;
                }
                String why = string(a, "message");
                String reason = why.isEmpty() ? "HTTP " + a.status() : why.length() > 200 ? why.substring(0, 200) : why;
                server.execute(() -> source.sendFailure(text("connect.failed", "Connecting failed: %s", reason)));
            } catch (IOException | RuntimeException e) {
                server.execute(() -> source.sendFailure(text("connect.offline", "Could not reach %s (%s). Does the server allow outgoing HTTPS?", endpoint, e.getClass().getSimpleName())));
            }
        }, "simplelivemap-connect");
        t.setDaemon(true);
        t.start();
        return 1;
    }

    static String string(HostedClient.Answer a, String field) {
        try {
            return a.json().has(field) ? a.json().get(field).getAsString() : "";
        } catch (RuntimeException e) {
            return "";
        }
    }

    /** "Connected: the map is being prepared." + the viewer address as a link (only https/http addresses). */
    static Component connectedMessage(String viewer) {
        net.minecraft.network.chat.MutableComponent msg = text("connect.done", "Connected! The map is being prepared - progress on livemap.simplemods.de. ").copy();
        if (viewer != null && (viewer.startsWith("https://") || viewer.startsWith("http://127.0.0.1") || viewer.startsWith("http://localhost"))) {
            try {
                java.net.URI uri = java.net.URI.create(viewer);
                msg.append(Component.literal(viewer).withStyle(style -> style.withColor(ChatFormatting.AQUA).withUnderlined(true)
                        .withClickEvent(new net.minecraft.network.chat.ClickEvent.OpenUrl(uri))));
            } catch (IllegalArgumentException ignored) {
                // no link then
            }
        }
        return msg;
    }

    /** {@code /simplelivemap disconnect}: tells the service (best effort) and forgets the link of this world. */
    static int disconnect(CommandSourceStack source) {
        HostedUploader up = LivemapRuntime.hosted();
        MinecraftServer server = source.getServer();
        HostedLink link = up != null ? up.link() : HostedLink.load(LivemapRuntime.root(server));
        if (link == null) {
            source.sendFailure(text("disconnect.none", "This world is not connected to livemap.simplemods.de."));
            return 0;
        }
        LivemapRuntime.stopHosted(true);
        // a link to another service (world from elsewhere, O8 audit 4) is only forgotten, nothing is sent there
        if (HostedLink.trusted(link, LivemapConfig.hostedEndpoint) == null) {
            source.sendSuccess(() -> text("disconnect.done", "Disconnected: the map %s moved to the archive on livemap.simplemods.de.", link.map()), false);
            return 1;
        }
        Thread t = new Thread(() -> {
            try {
                new HostedClient(link.endpoint(), link.key()).disconnect();
            } catch (IOException | RuntimeException ignored) {
                // the website can disconnect it as well; the key is gone here either way
            }
        }, "simplelivemap-disconnect");
        t.setDaemon(true);
        t.start();
        source.sendSuccess(() -> text("disconnect.done", "Disconnected: the map %s moved to the archive on livemap.simplemods.de.", link.map()), false);
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
        // Only to this source: sendSystemMessage does not echo to other operators or the server log like sendSuccess(.., true).
        source.sendSystemMessage(linkMessage(url(web), LivemapConfig.isPublic()));
        return 1;
    }

    /**
     * The answer of {@code /simplelivemap link} (N125): the address as one clickable line like the chat hint's "[Open Livemap]"
     * (Vanilla does not turn addresses in system messages into links; before N125 the line was text only).
     */
    public static Component linkMessage(String url, boolean isPublic) {
        net.minecraft.network.chat.MutableComponent line = text(isPublic ? "link.public" : "link.private",
                isPublic ? "Public web map: %s" : "Private web map (keep this link secret): %s", url).copy().withStyle(ChatFormatting.AQUA);
        try {
            java.net.URI uri = java.net.URI.create(url);
            line.withStyle(style -> style.withUnderlined(true).withClickEvent(new net.minecraft.network.chat.ClickEvent.OpenUrl(uri))
                    .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(text("hint.open.hover", "Opens the map of this world in your browser."))));
        } catch (IllegalArgumentException ignored) {
            // an address the URI parser refuses stays plain text
        }
        return line;
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
