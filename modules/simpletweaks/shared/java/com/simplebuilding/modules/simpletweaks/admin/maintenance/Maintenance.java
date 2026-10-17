package com.simplebuilding.modules.simpletweaks.admin.maintenance;

import com.mojang.brigadier.CommandDispatcher;
import com.simplebuilding.modules.simpletweaks.admin.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;

/**
 * T18 maintenance mode. The feature switch is on by default, the mode itself starts off: /maintenance on lets only
 * operators of {@code joinPermission} and the names/UUIDs in {@code allowed} join, shows {@code motd} in the server list
 * and (with {@code kickOnline}) disconnects everybody else. State in AdminState, so it survives a restart.
 */
public final class Maintenance {
    public static boolean on() { return Admin.on() && Admin.server() != null && Admin.config().maintenance().enabled() && Admin.state().maintenance; }

    public static boolean allowed(MinecraftServer server, NameAndId who) {
        var cfg = Admin.config().maintenance();
        if (AdminPerms.has(server, who, cfg.joinPermission())) return true;
        for (String s : cfg.allowed()) if (s.equalsIgnoreCase(who.name()) || s.equalsIgnoreCase(who.id().toString())) return true;
        return false;
    }
    /** The login refusal while active (mixin on {@code PlayerList.canPlayerLogin}), or null. */
    public static Component refuse(MinecraftServer server, NameAndId who) {
        return on() && !allowed(server, who) ? Component.literal(Admin.config().maintenance().kickMessage()) : null;
    }
    public static String motd(String vanilla) { return on() ? Admin.config().maintenance().motd() : vanilla; }

    public static void set(MinecraftServer server, boolean active) {
        Admin.state().maintenance = active;
        Admin.state().save();
        server.invalidateStatus();
        if (active && Admin.config().maintenance().kickOnline())
            for (var p : java.util.List.copyOf(server.getPlayerList().getPlayers()))
                if (!allowed(server, p.nameAndId())) p.connection.disconnect(Component.literal(Admin.config().maintenance().kickMessage()));
    }

    public static void register(CommandDispatcher<CommandSourceStack> d, AdminConfig config) {
        if (!config.maintenance().enabled()) return;
        d.register(Commands.literal("maintenance").requires(s -> Admin.on() && AdminPerms.has(s, Admin.config().maintenance().permission()))
                .executes(c -> status(c.getSource()))
                .then(Commands.literal("status").executes(c -> status(c.getSource())))
                .then(Commands.literal("on").executes(c -> { set(c.getSource().getServer(), true);
                    c.getSource().sendSuccess(() -> Component.translatable("simpletweaks.admin.maintenance.on").withStyle(ChatFormatting.GOLD), true); return 1; }))
                .then(Commands.literal("off").executes(c -> { set(c.getSource().getServer(), false);
                    c.getSource().sendSuccess(() -> Component.translatable("simpletweaks.admin.maintenance.off").withStyle(ChatFormatting.GREEN), true); return 1; })));
    }
    static int status(CommandSourceStack s) {
        s.sendSuccess(() -> Component.translatable(on() ? "simpletweaks.admin.maintenance.status.on" : "simpletweaks.admin.maintenance.status.off"), false);
        return 1;
    }
    private Maintenance() {}
}
