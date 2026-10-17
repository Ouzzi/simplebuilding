package com.simplebuilding.modules.simpletweaks.protect;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Hub of the protection features (plan docs/ai/PLAN-TWEAKS-PROTECT-2026-10-17.md): anti-x-ray, the Freecam rule, seed
 * hash, hidden health and action checks. Loader entry points call {@link #start}, {@link #stop} and the command
 * registration; packets, block changes and joins come from the shared mixins in {@code protect.mixin}.
 */
public final class Protect {
    private static volatile ProtectConfig config;
    private static MinecraftServer server;

    public static ProtectConfig config() {
        var c = config;
        if (c == null) config = c = ProtectConfig.load(ProtectConfig.PATH);
        return c;
    }
    /** Tests: other settings until the next call (null = reload from the file). */
    public static void configure(ProtectConfig value) { config = value; AntiXray.invalidate(); }
    /** Super-mod switch "Enable Simple Tweaks": off = every feature here does nothing. */
    public static boolean on() { return com.simplebuilding.framework.api.SubMods.enabled("simpletweaks"); }
    public static MinecraftServer server() { return server; }

    public static void start(MinecraftServer s) {
        server = s;
        config = null;
        AntiXray.invalidate();
        SeedHash.reset();
        HideHealth.healthId = com.simplebuilding.modules.simpletweaks.protect.mixin.ProtectLivingAccessor.simpletweaks$healthId();
    }
    public static void stop(MinecraftServer s) {
        ActionChecks.reset();
        server = null;
        config = null;
        AntiXray.invalidate();
    }
    /** After a player joined (mixin on {@code PlayerList.placeNewPlayer}). */
    public static void joined(ServerPlayer player) {
        if (!on()) return;
        FreecamRule.joined(player);
    }
    public static MutableComponent t(String key, Object... args) { return Component.translatable("simpletweaks.protect." + key, args); }
    private Protect() {}
}
