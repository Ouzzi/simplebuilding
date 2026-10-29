package com.simplemusic.upload;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Moderation of uploaded tracks (concept §6, owner decision U2 b): {@code /music list [pending]}, {@code approve},
 * {@code reject}, {@code delete} (the discs become silent "scratched" discs), {@code info} with a hash or its first
 * 6+ characters, {@code ban}/{@code unban <player>} (no more uploads). Operators only (permission level 2).
 */
public final class MusicCommands {
    private MusicCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("music").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("list").executes(c -> list(c, false))
                        .then(Commands.literal("pending").executes(c -> list(c, true))))
                .then(status("approve", TrackStore.Status.APPROVED))
                .then(status("reject", TrackStore.Status.REJECTED))
                .then(status("delete", TrackStore.Status.DELETED))
                .then(Commands.literal("info").then(Commands.argument("hash", StringArgumentType.word()).executes(MusicCommands::info)))
                .then(Commands.literal("ban").then(Commands.argument("player", StringArgumentType.word()).executes(c -> ban(c, true))))
                .then(Commands.literal("unban").then(Commands.argument("player", StringArgumentType.word()).executes(c -> ban(c, false)))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> status(String name, TrackStore.Status status) {
        return Commands.literal(name).then(Commands.argument("hash", StringArgumentType.word()).executes(c -> {
            TrackStore store = MusicServer.store(c.getSource().getServer());
            TrackStore.Track track = find(store, StringArgumentType.getString(c, "hash"));
            if (track == null) {
                c.getSource().sendFailure(Component.translatable("simplemusic.command.unknown"));
                return 0;
            }
            store.setStatus(track.hash(), status);
            c.getSource().sendSuccess(() -> Component.translatable("simplemusic.command." + name, track.title(), track.hash().substring(0, 8)), true);
            return 1;
        }));
    }

    /** A track by full hash or a unique prefix of at least 6 characters. */
    public static TrackStore.Track find(TrackStore store, String text) {
        String prefix = text.toLowerCase(Locale.ROOT);
        if (prefix.length() < 6) return null;
        List<TrackStore.Track> hits = store.all().stream().filter(t -> t.hash().startsWith(prefix)).toList();
        return hits.size() == 1 ? hits.get(0) : null;
    }

    private static int list(CommandContext<CommandSourceStack> c, boolean pendingOnly) {
        TrackStore store = MusicServer.store(c.getSource().getServer());
        List<TrackStore.Track> tracks = pendingOnly ? store.pending() : store.all();
        c.getSource().sendSuccess(() -> Component.translatable("simplemusic.command.list", tracks.size()), false);
        for (TrackStore.Track t : tracks) {
            c.getSource().sendSuccess(() -> Component.literal(t.hash().substring(0, 8) + "  " + t.status().name().toLowerCase(Locale.ROOT)
                    + "  " + t.seconds() / 60 + ":" + String.format(Locale.ROOT, "%02d", t.seconds() % 60) + "  " + t.title() + "  (" + t.uploaderName() + ")"), false);
        }
        return tracks.size();
    }

    private static int info(CommandContext<CommandSourceStack> c) {
        TrackStore.Track t = find(MusicServer.store(c.getSource().getServer()), StringArgumentType.getString(c, "hash"));
        if (t == null) {
            c.getSource().sendFailure(Component.translatable("simplemusic.command.unknown"));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.translatable("simplemusic.command.info", t.title(), t.uploaderName(),
                t.seconds(), t.bytes() / 1024, t.status().name().toLowerCase(Locale.ROOT), t.hash()), false);
        return 1;
    }

    private static int ban(CommandContext<CommandSourceStack> c, boolean on) {
        String name = StringArgumentType.getString(c, "player");
        var server = c.getSource().getServer();
        ServerPlayer player = server.getPlayerList().getPlayerByName(name);
        // Offline players too (REV2-B-6): the server's name cache knows everyone who joined once.
        java.util.UUID id = player != null ? player.getUUID() : server.services().nameToIdCache().get(name).map(net.minecraft.server.players.NameAndId::id).orElse(null);
        if (id == null) {
            c.getSource().sendFailure(Component.translatable("simplemusic.command.no_player", name));
            return 0;
        }
        MusicServer.store(server).ban(id, on);
        c.getSource().sendSuccess(() -> Component.translatable(on ? "simplemusic.command.ban" : "simplemusic.command.unban", name), true);
        return 1;
    }
}
