package com.simplebuilding.modules.simpletweaks.play.seen;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.simplebuilding.modules.simpletweaks.play.*;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * T7: /playtime [player] from the Vanilla statistic {@code play_time} (offline players from their statistics file),
 * /seen &lt;player&gt;: online now, or how long ago the player left (own leave time in the world state).
 */
public final class Playtime {
    public static boolean active() { return Play.on() && Play.config().playtime().enabled(); }

    public static void register(CommandDispatcher<CommandSourceStack> d, PlayConfig c) {
        if (!c.playtime().enabled()) return;
        d.register(Commands.literal("playtime")
                .executes(x -> playtime(x.getSource(), x.getSource().getPlayerOrException().nameAndId().name()))
                .then(Commands.argument("player", StringArgumentType.word()).suggests((x, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(x.getSource().getOnlinePlayerNames(), b))
                        .executes(x -> playtime(x.getSource(), StringArgumentType.getString(x, "player")))));
        d.register(Commands.literal("seen")
                .then(Commands.argument("player", StringArgumentType.word()).suggests((x, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(Play.state().names.values(), b))
                        .executes(x -> seen(x.getSource(), StringArgumentType.getString(x, "player")))));
    }

    /** UUID of a name: online, then names this feature saw, then the server's name cache. */
    public static UUID resolve(MinecraftServer server, String name) {
        var online = server.getPlayerList().getPlayerByName(name);
        if (online != null) return online.getUUID();
        for (var e : Play.state().names.entrySet()) if (e.getValue().equalsIgnoreCase(name)) return UUID.fromString(e.getKey());
        return server.services().nameToIdCache().get(name).map(net.minecraft.server.players.NameAndId::id).orElse(null);
    }

    static int playtime(CommandSourceStack source, String name) {
        if (!active()) return 0;
        var server = source.getServer();
        var id = resolve(server, name);
        if (id == null) { source.sendFailure(PlayText.t("unknown_player", name)); return 0; }
        long ticks = StatBoard.value(StatBoard.counter(server, id), "playtime");
        source.sendSuccess(() -> PlayText.t("playtime.line", StatBoard.name(server, id), PlayText.duration(ticks * 50).withStyle(ChatFormatting.GREEN)), false);
        return (int) Math.min(Integer.MAX_VALUE, ticks / 20);
    }
    static int seen(CommandSourceStack source, String name) {
        if (!active()) return 0;
        var server = source.getServer();
        var id = resolve(server, name);
        if (id == null) { source.sendFailure(PlayText.t("unknown_player", name)); return 0; }
        if (server.getPlayerList().getPlayer(id) != null) { source.sendSuccess(() -> PlayText.t("seen.online", StatBoard.name(server, id)), false); return 1; }
        Long at = Play.state().lastSeen.get(id.toString());
        if (at == null) { source.sendFailure(PlayText.t("seen.never", name)); return 0; }
        source.sendSuccess(() -> PlayText.t("seen.ago", StatBoard.name(server, id), PlayText.duration(System.currentTimeMillis() - at).withStyle(ChatFormatting.GREEN)), false);
        return 1;
    }

    public static void joined(ServerPlayer p) { Play.state().names.put(p.getUUID().toString(), p.nameAndId().name()); }
    public static void left(ServerPlayer p) {
        var s = Play.state();
        s.names.put(p.getUUID().toString(), p.nameAndId().name());
        s.lastSeen.put(p.getUUID().toString(), System.currentTimeMillis());
        s.save();
    }
    private Playtime() {}
}
