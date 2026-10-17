package com.simplebuilding.modules.simpletweaks.admin.freeze;

import com.mojang.brigadier.CommandDispatcher;
import com.simplebuilding.modules.simpletweaks.admin.*;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * T15: a frozen player is put back to where they were frozen every tick and cannot break, use, place, attack or drop
 * (mixins ask {@link #frozen}). A title when frozen, the action bar every 2 s. Survives relogs (AdminState); the
 * position is taken anew at every login.
 */
public final class Freeze {
    private static final Map<UUID, Vec3> AT = new HashMap<>();

    public static boolean active() { return Admin.on() && Admin.server() != null && Admin.config().freeze().enabled(); }
    public static boolean frozen(net.minecraft.world.entity.Entity e) {
        return e instanceof ServerPlayer p && !Admin.state().frozen.isEmpty() && active() && Admin.state().frozen.contains(p.getUUID());
    }

    public static void set(ServerPlayer p, boolean on) {
        var state = Admin.state();
        if (on ? !state.frozen.add(p.getUUID()) : !state.frozen.remove(p.getUUID())) return;
        state.save();
        if (on) {
            AT.put(p.getUUID(), p.position());
            p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 60, 10));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("simpletweaks.admin.freeze.title").withStyle(ChatFormatting.AQUA)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("simpletweaks.admin.freeze.subtitle")));
        } else {
            AT.remove(p.getUUID());
            p.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("simpletweaks.admin.freeze.released").withStyle(ChatFormatting.GREEN)));
        }
    }
    public static void joined(ServerPlayer p) { if (frozen(p)) AT.put(p.getUUID(), p.position()); }

    public static void tick(MinecraftServer server) {
        if (Admin.state().frozen.isEmpty() || !active()) return;
        long time = server.getTickCount();
        for (var p : server.getPlayerList().getPlayers()) {
            if (!Admin.state().frozen.contains(p.getUUID())) continue;
            var at = AT.computeIfAbsent(p.getUUID(), k -> p.position());
            if (p.position().distanceToSqr(at) > 0.0025) {
                if (p.isPassenger()) p.stopRiding();
                p.connection.teleport(at.x, at.y, at.z, p.getYRot(), p.getXRot());
            }
            p.setDeltaMovement(Vec3.ZERO);
            if (time % 40 == 0) p.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("simpletweaks.admin.freeze.notice").withStyle(ChatFormatting.AQUA)));
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> d, AdminConfig config) {
        if (!config.freeze().enabled()) return;
        d.register(Commands.literal("freeze").requires(s -> Admin.on() && AdminPerms.has(s, Admin.config().freeze().permission()))
                .then(Commands.argument("player", EntityArgument.player()).executes(c -> freeze(c.getSource(), EntityArgument.getPlayer(c, "player"), true))));
        d.register(Commands.literal("unfreeze").requires(s -> Admin.on() && AdminPerms.has(s, Admin.config().freeze().permission()))
                .then(Commands.argument("player", EntityArgument.player()).executes(c -> freeze(c.getSource(), EntityArgument.getPlayer(c, "player"), false))));
    }
    static int freeze(CommandSourceStack s, ServerPlayer p, boolean on) {
        // nobody freezes someone with a higher operator level (an owner cannot be locked out by a moderator)
        if (on && s.getPlayer() != null && AdminPerms.levelOf(p) > AdminPerms.levelOf(s.getPlayer())) {
            s.sendFailure(Component.translatable("simpletweaks.admin.freeze.higher", p.getDisplayName())); return 0;
        }
        if (on && p == s.getPlayer()) { s.sendFailure(Component.translatable("simpletweaks.admin.freeze.self")); return 0; }
        set(p, on);
        s.sendSuccess(() -> Component.translatable(on ? "simpletweaks.admin.freeze.on" : "simpletweaks.admin.freeze.off", p.getDisplayName()), true);
        return 1;
    }
    private Freeze() {}
}
