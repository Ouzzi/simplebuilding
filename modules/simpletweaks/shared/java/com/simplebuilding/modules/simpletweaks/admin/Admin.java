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

    /** Tests: the answer of {@link #dedicated} (null = the real one). */
    private static volatile Boolean dedicatedForTest;
    public static void dedicatedForTest(Boolean value) { dedicatedForTest = value; }
    /**
     * Dedicated server (E1ANS F1)? False on the integrated server (single player, LAN). The gametest server counts as dedicated,
     * so the server tools are testable; it is told apart by its class name (no hard reference to a test class).
     */
    public static boolean dedicated(MinecraftServer s) {
        var test = dedicatedForTest;
        if (test != null) return test;
        return s != null && (s.isDedicatedServer() || s.getClass().getName().equals("net.minecraft.gametest.framework.GameTestServer"));
    }
    /**
     * A server tool (block log, auto backup, anti-x-ray) runs when its {@code enabled} switch is on and, with its
     * {@code onlyDedicated} option (default), only on a dedicated server (owner 12.11.: off in single player and on LAN).
     */
    public static boolean runs(MinecraftServer s, boolean enabled, boolean onlyDedicated) { return enabled && (!onlyDedicated || dedicated(s)); }

    public static void start(MinecraftServer s) {
        server = s;
        state = AdminState.load(s);
        com.simplebuilding.modules.simpletweaks.admin.blocklog.BlockLog.start(s);
        // Blocks a tool set without a block item (SimpleBuilding blueprint builds) reach the log through SimpleLib.
        com.simplelib.api.ToolPlacements.listen("simpletweaks:blocklog", (level, player, pos, placed) ->
                com.simplebuilding.modules.simpletweaks.admin.blocklog.BlockRecorder.placed(level, player, pos));
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
