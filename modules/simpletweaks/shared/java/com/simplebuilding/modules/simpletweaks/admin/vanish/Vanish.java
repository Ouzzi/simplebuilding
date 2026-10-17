package com.simplebuilding.modules.simpletweaks.admin.vanish;

import com.mojang.brigadier.CommandDispatcher;
import com.simplebuilding.modules.simpletweaks.admin.*;
import java.util.*;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * T14: vanished players are not sent to players below {@code seePermission} (entity tracking, tab list), are left out
 * of the server list count and sample, cause no join/leave message, are ignored by mobs, cause no game events
 * (sculk) and pick nothing up unless /vanish pickup is on. The state survives relogs (AdminState).
 */
public final class Vanish {
    public static boolean active() { return Admin.on() && Admin.server() != null && Admin.config().vanish().enabled(); }
    public static boolean vanished(UUID id) { return active() && Admin.state().vanished.contains(id); }
    public static boolean vanished(Entity e) { return e instanceof ServerPlayer p && !Admin.state().vanished.isEmpty() && vanished(p.getUUID()); }
    /** May {@code viewer} see vanished players? */
    public static boolean sees(ServerPlayer viewer) { return AdminPerms.has(viewer, Admin.config().vanish().seePermission()); }
    /** Is {@code target} hidden from {@code viewer}? */
    public static boolean hidden(ServerPlayer target, ServerPlayer viewer) { return target != viewer && vanished(target) && !sees(viewer); }
    public static boolean pickup(ServerPlayer p) { return !vanished(p) || Admin.state().vanishPickup.contains(p.getUUID()); }

    public static void set(ServerPlayer player, boolean on) {
        var state = Admin.state();
        boolean changed = on ? state.vanished.add(player.getUUID()) : state.vanished.remove(player.getUUID());
        if (!changed) return;
        state.save();
        var server = player.level().getServer();
        for (var viewer : server.getPlayerList().getPlayers()) {
            if (viewer == player || sees(viewer)) continue;
            if (on) viewer.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID())));
            else viewer.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player)));
        }
        Tracking.refresh(player);
        server.invalidateStatus();
    }
    /** After a join: hide vanished players from the newcomer (its first tab list came with everybody). */
    public static void joined(ServerPlayer player) {
        if (!active() || Admin.state().vanished.isEmpty()) return;
        if (!sees(player)) {
            var hide = new ArrayList<UUID>();
            for (var other : player.level().getServer().getPlayerList().getPlayers()) if (other != player && vanished(other)) hide.add(other.getUUID());
            if (!hide.isEmpty()) player.connection.send(new ClientboundPlayerInfoRemovePacket(hide));
        }
        if (vanished(player)) {
            player.sendSystemMessage(Component.translatable("simpletweaks.admin.vanish.still").withStyle(net.minecraft.ChatFormatting.GRAY));
            Tracking.refresh(player);
        }
    }
    /** Server list: online count and sample without vanished players. */
    public static net.minecraft.network.protocol.status.ServerStatus.Players status(MinecraftServer server, net.minecraft.network.protocol.status.ServerStatus.Players players) {
        if (!active() || Admin.state().vanished.isEmpty()) return players;
        int hidden = 0;
        for (var p : server.getPlayerList().getPlayers()) if (vanished(p)) hidden++;
        var sample = players.sample().stream().filter(n -> !vanished(n.id())).toList();
        return new net.minecraft.network.protocol.status.ServerStatus.Players(players.max(), Math.max(0, players.online() - hidden), sample);
    }

    public static void register(CommandDispatcher<CommandSourceStack> d, AdminConfig config) {
        if (!config.vanish().enabled()) return;
        d.register(Commands.literal("vanish").requires(s -> Admin.on() && AdminPerms.has(s, Admin.config().vanish().permission()))
                .executes(c -> toggle(c.getSource(), c.getSource().getPlayerOrException()))
                .then(Commands.literal("pickup").executes(c -> {
                    var p = c.getSource().getPlayerOrException();
                    var set = Admin.state().vanishPickup;
                    boolean on = set.add(p.getUUID()) || !set.remove(p.getUUID());
                    Admin.state().save();
                    c.getSource().sendSuccess(() -> Component.translatable(on ? "simpletweaks.admin.vanish.pickup.on" : "simpletweaks.admin.vanish.pickup.off"), false);
                    return 1;
                }))
                .then(Commands.argument("player", EntityArgument.player()).executes(c -> toggle(c.getSource(), EntityArgument.getPlayer(c, "player")))));
    }
    static int toggle(CommandSourceStack s, ServerPlayer p) {
        boolean on = !Admin.state().vanished.contains(p.getUUID());
        set(p, on);
        s.sendSuccess(() -> Component.translatable(on ? "simpletweaks.admin.vanish.on" : "simpletweaks.admin.vanish.off", p.getDisplayName()), true);
        return 1;
    }
    private Vanish() {}
}
