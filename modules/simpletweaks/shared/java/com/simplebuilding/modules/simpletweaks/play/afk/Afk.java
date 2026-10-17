package com.simplebuilding.modules.simpletweaks.play.afk;

import com.mojang.brigadier.CommandDispatcher;
import com.simplebuilding.modules.simpletweaks.play.*;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * T24: a player without any action for {@code minutes} (Vanilla's last action time: moving, clicking, chatting) is AFK,
 * or by /afk. AFK players are grey in the tab list, do not count for sleeping ({@code SleepStatus}) and not for votes.
 * The kick after {@code kickMinutes} is off by default (Vanilla also has player-idle-timeout).
 */
public final class Afk {
    static final Set<UUID> AFK = new HashSet<>();
    static final Map<UUID, Long> MANUAL = new HashMap<>();

    public static boolean active() { return Play.on() && Play.server() != null && Play.config().afk().enabled(); }
    public static boolean afk(ServerPlayer p) { return active() && AFK.contains(p.getUUID()); }
    /** Excluded from the sleep count (SleepStatus mixin)? */
    public static boolean ignoredForSleep(ServerPlayer p) { return afk(p) && !p.isSleeping() && Play.config().afk().excludeFromSleep(); }

    public static void register(CommandDispatcher<CommandSourceStack> d, PlayConfig c) {
        if (!c.afk().enabled()) return;
        d.register(Commands.literal("afk").executes(x -> { var p = x.getSource().getPlayerOrException(); set(p, !AFK.contains(p.getUUID()), true); return 1; }));
    }

    /** Milliseconds since the player's last action. */
    public static long idle(ServerPlayer p) { return net.minecraft.util.Util.getMillis() - p.getLastActionTime(); }

    public static void set(ServerPlayer p, boolean on, boolean manual) {
        boolean changed = on ? AFK.add(p.getUUID()) : AFK.remove(p.getUUID());
        if (on && manual) MANUAL.put(p.getUUID(), p.getLastActionTime()); else if (!on) MANUAL.remove(p.getUUID());
        if (!changed) return;
        var server = p.level().getServer();
        server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, p));
        p.sendSystemMessage(PlayText.t(on ? "afk.now" : "afk.back").withStyle(ChatFormatting.GRAY));
        p.level().updateSleepingPlayerList();
    }
    /** The grey tab list name of an AFK player; null = keep the normal one. */
    public static Component tabName(ServerPlayer p, Component normal) {
        if (!afk(p)) return normal;
        return (normal == null ? p.getName().copy() : normal.copy()).withStyle(ChatFormatting.GRAY).append(PlayText.t("afk.tag").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        if (!active()) { if (!AFK.isEmpty()) for (var p : server.getPlayerList().getPlayers()) set(p, false, false); AFK.clear(); return; }
        var c = Play.config().afk();
        for (var p : List.copyOf(server.getPlayerList().getPlayers())) {
            long idle = idle(p);
            Long manualAt = MANUAL.get(p.getUUID());
            boolean acted = manualAt != null && p.getLastActionTime() != manualAt;
            if (AFK.contains(p.getUUID())) {
                if (manualAt != null ? acted : idle < c.minutes() * 60_000L) set(p, false, false);
                else if (c.kick() && idle >= c.kickMinutes() * 60_000L) p.connection.disconnect(PlayText.t("afk.kicked"));
            } else if (idle >= c.minutes() * 60_000L) set(p, true, false);
        }
    }
    public static void left(ServerPlayer p) { AFK.remove(p.getUUID()); MANUAL.remove(p.getUUID()); }
    private Afk() {}
}
