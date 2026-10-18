package com.simplebuilding.modules.simpletweaks.admin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Hub of the admin and protection features (T10 T11 T13–T18 T34, plan docs/ai/PLAN-TWEAKS-ADMIN-2026-10-17.md):
 * settings, world state and the server lifecycle. Loader entry points call {@link #start}, {@link #stop} and the
 * command registration; ticks and joins come from shared mixins.
 */
public final class Admin {
    private static volatile AdminConfig config;
    private static MinecraftServer server;
    private static AdminState state = new AdminState();

    public static AdminConfig config() {
        var c = config;
        if (c == null) config = c = AdminConfig.load(AdminConfig.PATH).withAutoOff(com.simplebuilding.modules.simpletweaks.play.Mods::loaded, org.slf4j.LoggerFactory.getLogger("simpletweaks-admin"));
        return c;
    }
    /** Tests: other settings until the next call (null = reload from the file). */
    public static void configure(AdminConfig value) { config = value; }
    /** Super-mod switch "Enable Simple Tweaks": off = every admin feature does nothing. */
    public static boolean on() { return com.simplebuilding.framework.api.SubMods.enabled("simpletweaks"); }
    public static MinecraftServer server() { return server; }
    public static AdminState state() { return state; }

    public static void start(MinecraftServer s) {
        server = s;
        state = AdminState.load(s);
        com.simplebuilding.modules.simpletweaks.admin.blocklog.BlockLog.start(s);
        com.simplebuilding.modules.simpletweaks.admin.restart.Restart.start(s);
        com.simplebuilding.modules.simpletweaks.admin.backup.Backups.start(s);
    }
    public static void stop(MinecraftServer s) {
        com.simplebuilding.modules.simpletweaks.admin.invsee.Invsee.closeAll(s);
        com.simplebuilding.modules.simpletweaks.admin.blocklog.BlockLog.stop();
        com.simplebuilding.modules.simpletweaks.admin.restart.Restart.stop(s);
        com.simplebuilding.modules.simpletweaks.admin.backup.Backups.stop();
        state.save();
        server = null;
        state = new AdminState();
        config = null;
    }
    /** End of every server tick (mixin on {@code MinecraftServer.tickServer}). */
    public static void tick(MinecraftServer s) {
        if (s != server || !on()) return;
        com.simplebuilding.modules.simpletweaks.admin.blocklog.BlockLog.tick(s);
        com.simplebuilding.modules.simpletweaks.admin.freeze.Freeze.tick(s);
        com.simplebuilding.modules.simpletweaks.admin.restart.Restart.tick(s);
        com.simplebuilding.modules.simpletweaks.admin.backup.Backups.tick(s);
        com.simplebuilding.modules.simpletweaks.admin.lag.LagFinder.tick(s);
    }
    /** After a player joined (mixin on {@code PlayerList.placeNewPlayer}). */
    public static void joined(ServerPlayer player) {
        if (!on()) return;
        com.simplebuilding.modules.simpletweaks.admin.vanish.Vanish.joined(player);
        com.simplebuilding.modules.simpletweaks.admin.freeze.Freeze.joined(player);
    }
    private Admin() {}
}
