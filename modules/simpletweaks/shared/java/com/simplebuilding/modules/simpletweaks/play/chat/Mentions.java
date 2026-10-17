package com.simplebuilding.modules.simpletweaks.play.chat;

import com.simplebuilding.modules.simpletweaks.play.*;
import java.util.*;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

/**
 * T6: "@Name" in chat is highlighted for everybody and the mentioned players hear a soft bell. The highlight is the
 * unsigned display content of the chat message (the Vanilla way of decorating chat); the signed text is unchanged.
 * "@everyone" / "@all" ping everybody, only from operators of {@code everyonePermission}.
 */
public final class Mentions {
    static final Pattern MENTION = Pattern.compile("@([A-Za-z0-9_]{1,16})");

    public static boolean active() { return Play.on() && Play.server() != null && Play.config().mentions().enabled(); }

    public record Result(Component text, Set<ServerPlayer> mentioned) {}

    /** Highlight mentions in {@code plain} among {@code online} (names by {@code nameOf}); null when nobody online is mentioned. */
    public static Result decorate(Collection<ServerPlayer> online, java.util.function.Function<ServerPlayer, String> nameOf, ServerPlayer sender, boolean everyoneAllowed, String plain) {
        // T14: a vanished player is neither highlighted nor pinged (the message is public)
        online = online.stream().filter(p -> p == sender || !com.simplebuilding.modules.simpletweaks.admin.vanish.Vanish.vanished(p)).toList();
        var m = MENTION.matcher(plain);
        var out = Component.empty();
        var mentioned = new LinkedHashSet<ServerPlayer>();
        int at = 0;
        while (m.find()) {
            String name = m.group(1);
            List<ServerPlayer> hits = new ArrayList<>();
            if ((name.equalsIgnoreCase("everyone") || name.equalsIgnoreCase("all")) && everyoneAllowed) hits.addAll(online);
            else for (var p : online) if (nameOf.apply(p).equalsIgnoreCase(name)) hits.add(p);
            if (hits.isEmpty()) continue;
            out.append(Component.literal(plain.substring(at, m.start())));
            out.append(Component.literal(m.group()).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
            at = m.end();
            mentioned.addAll(hits);
        }
        if (mentioned.isEmpty()) return null;
        out.append(Component.literal(plain.substring(at)));
        if (sender != null) mentioned.remove(sender);
        return new Result(out, mentioned);
    }
    public static Result decorate(MinecraftServer server, ServerPlayer sender, String plain) {
        boolean everyone = sender == null || PlayPerms.has(sender, Play.config().mentions().everyonePermission());
        return decorate(server.getPlayerList().getPlayers(), p -> p.nameAndId().name(), sender, everyone, plain);
    }

    /** Called for every chat message before it is broadcast. */
    public static PlayerChatMessage chat(PlayerChatMessage message, ServerPlayer sender) {
        if (!active() || message.unsignedContent() != null) return message;
        var r = decorate(Play.server(), sender, message.signedContent());
        if (r == null) return message;
        var c = Play.config().mentions();
        if (c.sound()) for (var p : r.mentioned()) PlaySounds.to(p, SoundEvents.NOTE_BLOCK_BELL, c.volume(), 1.6f);
        return message.withUnsignedContent(r.text());
    }
    private Mentions() {}
}
