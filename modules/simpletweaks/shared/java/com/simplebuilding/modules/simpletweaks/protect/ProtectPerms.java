package com.simplebuilding.modules.simpletweaks.protect;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.*;

/** Vanilla operator levels (0–4); no permission mod is used in this repository. */
public final class ProtectPerms {
    public static Permission level(int level) { return new Permission.HasCommandLevel(PermissionLevel.byId(Math.clamp(level, 0, 4))); }
    public static boolean has(CommandSourceStack source, int level) { return level <= 0 || source.permissions().hasPermission(level(level)); }
    public static boolean has(ServerPlayer player, int level) { return level <= 0 || player.permissions().hasPermission(level(level)); }
    private ProtectPerms() {}
}
