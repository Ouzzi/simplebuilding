package com.simplebuilding.modules.simpletweaks.admin;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.*;

/** Vanilla operator levels (0–4) for every admin feature; no permission mod is used in this repository. */
public final class AdminPerms {
    public static Permission level(int level) { return new Permission.HasCommandLevel(PermissionLevel.byId(AdminConfig.level(level))); }
    public static boolean has(CommandSourceStack source, int level) { return source.permissions().hasPermission(level(level)); }
    public static boolean has(ServerPlayer player, int level) { return player.permissions().hasPermission(level(level)); }
    public static boolean has(MinecraftServer server, net.minecraft.server.players.NameAndId profile, int level) {
        return server.getProfilePermissions(profile).hasPermission(level(level));
    }
    /** The highest operator level of a player (0–4). */
    public static int levelOf(ServerPlayer player) {
        for (int l = 4; l > 0; l--) if (has(player, l)) return l;
        return 0;
    }
    private AdminPerms() {}
}
