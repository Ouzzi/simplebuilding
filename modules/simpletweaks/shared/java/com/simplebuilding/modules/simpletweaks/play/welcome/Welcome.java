package com.simplebuilding.modules.simpletweaks.play.welcome;

import com.simplebuilding.modules.simpletweaks.play.Play;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * T19 (off by default): welcome line for the joining player ({@code {player}} is replaced), rotating tips every
 * {@code tipMinutes} for everybody, own join/leave lines instead of the Vanilla ones (empty = Vanilla line).
 */
public final class Welcome {
    static int nextTip;

    public static boolean active() { return Play.on() && Play.server() != null && Play.config().welcome().enabled(); }

    public static String fill(String text, ServerPlayer p) { return text.replace("{player}", p.nameAndId().name()); }

    public static void joined(ServerPlayer p) {
        if (!active()) return;
        var msg = Play.config().welcome().message();
        if (!msg.isBlank()) p.sendSystemMessage(Component.literal(fill(msg, p)).withStyle(ChatFormatting.GOLD));
    }
    /** The tip shown next (and advances the rotation); null without tips. */
    public static String nextTip() {
        var tips = Play.config().welcome().tips();
        if (tips.isEmpty()) return null;
        var tip = tips.get(Math.floorMod(nextTip, tips.size()));
        nextTip = Math.floorMod(nextTip + 1, tips.size());
        return tip;
    }
    public static void tick(MinecraftServer server) {
        if (!active()) return;
        int every = Play.config().welcome().tipMinutes() * 1200;
        if (server.getTickCount() % every != 0 || server.getPlayerList().getPlayerCount() == 0) return;
        var tip = nextTip();
        if (tip != null) server.getPlayerList().broadcastSystemMessage(Component.literal(tip).withStyle(ChatFormatting.AQUA), false);
    }
    /** Replaces the Vanilla join/leave line? Then broadcasts the own one and returns false. */
    public static boolean vanillaLine(ServerPlayer p, boolean join) {
        if (!active()) return true;
        var c = Play.config().welcome();
        var text = join ? c.joinMessage() : c.leaveMessage();
        if (text.isBlank()) return true;
        p.level().getServer().getPlayerList().broadcastSystemMessage(Component.literal(fill(text, p)).withStyle(ChatFormatting.YELLOW), false);
        return false;
    }
    private Welcome() {}
}
