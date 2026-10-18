package com.simplebuilding.modules.simpletweaks.admin;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

/**
 * Registers the commands of every enabled admin feature. {@code /tweaks} itself carries no requirement (Brigadier merges
 * equal roots and keeps the first requirement), each branch checks its own operator level.
 */
public final class AdminCommands {
    public static void register(CommandDispatcher<CommandSourceStack> d) { register(d, Admin.config()); }
    public static void register(CommandDispatcher<CommandSourceStack> d, AdminConfig c) {
        com.simplebuilding.modules.simpletweaks.admin.blocklog.BlockLogCommands.register(d, c);
        com.simplebuilding.modules.simpletweaks.admin.invsee.Invsee.register(d, c);
        com.simplebuilding.modules.simpletweaks.admin.vanish.Vanish.register(d, c);
        com.simplebuilding.modules.simpletweaks.admin.freeze.Freeze.register(d, c);
        com.simplebuilding.modules.simpletweaks.admin.restart.Restart.register(d, c);
        com.simplebuilding.modules.simpletweaks.admin.backup.Backups.register(d, c);
        com.simplebuilding.modules.simpletweaks.admin.maintenance.Maintenance.register(d, c);
        com.simplebuilding.modules.simpletweaks.admin.lag.LagFinder.register(d, c);
        com.simplebuilding.modules.simpletweaks.TweakCommands.alias(d, "inspect", "invsee", "endersee", "vanish", "freeze", "unfreeze", "backup", "maintenance", "lag");
    }
    private AdminCommands() {}
}
