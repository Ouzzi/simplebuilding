package com.simplebuilding.modules.simpletweaks.play;

import com.simplebuilding.modules.simpletweaks.play.afk.Afk;
import com.simplebuilding.modules.simpletweaks.play.border.Borders;
import com.simplebuilding.modules.simpletweaks.play.cleanup.GroundCleanup;
import com.simplebuilding.modules.simpletweaks.play.coords.Coords;
import com.simplebuilding.modules.simpletweaks.play.crowd.EntityLimit;
import com.simplebuilding.modules.simpletweaks.play.events.ServerEvents;
import com.simplebuilding.modules.simpletweaks.play.pregen.Pregen;
import com.simplebuilding.modules.simpletweaks.play.reward.LoginReward;
import com.simplebuilding.modules.simpletweaks.play.roof.NetherRoof;
import com.simplebuilding.modules.simpletweaks.play.seen.Playtime;
import com.simplebuilding.modules.simpletweaks.play.shield.NewPlayerShield;
import com.simplebuilding.modules.simpletweaks.play.top.Leaderboard;
import com.simplebuilding.modules.simpletweaks.play.vote.Votes;
import com.simplebuilding.modules.simpletweaks.play.welcome.Welcome;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Hub of the player/world/event features (T4–T9, T12, T19–T28, T30–T33; plan docs/ai/PLAN-TWEAKS-PLAY-2026-10-17.md):
 * settings, world state and the server lifecycle. Loader entry points call {@link #start}, {@link #stop} and the command
 * registration; ticks, joins, leaves and chat come from shared mixins.
 */
public final class Play {
    private static volatile PlayConfig config;
    private static MinecraftServer server;
    private static PlayState state = new PlayState();

    public static PlayConfig config() {
        var c = config;
        if (c == null) config = c = PlayConfig.load(PlayConfig.PATH).withAutoOff(Mods::loaded, org.slf4j.LoggerFactory.getLogger("simpletweaks-play"));
        return c;
    }
    /** Tests: other settings until the next call (null = reload from the file). */
    public static void configure(PlayConfig value) { config = value; }
    /** Super-mod switch "Enable Simple Tweaks": off = every feature here does nothing. */
    public static boolean on() { return com.simplebuilding.framework.api.SubMods.enabled("simpletweaks"); }
    public static MinecraftServer server() { return server; }
    public static PlayState state() { return state; }
    /** Is a partner mod loaded? Read from the registries/namespace, never from its classes (principle 4). */
    public static boolean modLoaded(String id) { return Mods.loaded(id); }

    public static void start(MinecraftServer s) {
        server = s;
        state = PlayState.load(s);
        Borders.apply(s);
        Pregen.start(s);
    }
    public static void stop(MinecraftServer s) {
        for (var p : s.getPlayerList().getPlayers()) Playtime.left(p);
        Pregen.stop(s);
        ServerEvents.shutdown();
        Leaderboard.stop(s);
        NewPlayerShield.reset();
        NetherRoof.forgetAll();
        state.save();
        server = null;
        state = new PlayState();
        config = null;
    }
    /** End of every server tick (mixin on {@code MinecraftServer.tickServer}). */
    public static void tick(MinecraftServer s) {
        if (s != server || !on()) return;
        Coords.tick(s);
        Afk.tick(s);
        Votes.tick(s);
        ServerEvents.tick(s);
        Welcome.tick(s);
        Leaderboard.tick(s);
        NetherRoof.tick(s);
        EntityLimit.tick(s);
        GroundCleanup.tick(s);
        Pregen.tick(s);
        NewPlayerShield.tick(s);
    }
    /** After a player joined (mixin on {@code PlayerList.placeNewPlayer}). */
    public static void joined(ServerPlayer player) {
        if (!on() || server == null) return;
        Playtime.joined(player);
        Welcome.joined(player);
        LoginReward.joined(player);
        NewPlayerShield.joined(player);
    }
    /** Before a player leaves (mixin on {@code ServerGamePacketListenerImpl.removePlayerFromWorld}). */
    public static void left(ServerPlayer player) {
        if (!on() || server == null) return;
        Playtime.left(player);
        Afk.left(player);
        Votes.left(player);
        NetherRoof.forget(player.getUUID());
    }
    /** T14 integration: is {@code id} a vanished admin that {@code viewer} (null or non-player = console/block, sees all) may not see? */
    public static boolean hidden(java.util.UUID id, net.minecraft.commands.CommandSourceStack viewer) {
        return com.simplebuilding.modules.simpletweaks.admin.vanish.Vanish.vanished(id)
                && viewer != null && viewer.getEntity() instanceof ServerPlayer v && !v.getUUID().equals(id)
                && !com.simplebuilding.modules.simpletweaks.admin.vanish.Vanish.sees(v);
    }
    private Play() {}
}
