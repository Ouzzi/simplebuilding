package com.simplebuilding.modules.simpletweaks.play.coords;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.simplebuilding.modules.simpletweaks.play.*;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * T4: /coords [message] shares the own position with everybody, /coords to &lt;player&gt; [message] only with one player.
 * The coordinates are green and copy on click; an optional ping marker (end rod particle column) shows the spot for a
 * few seconds. A short cooldown keeps chat calm.
 */
public final class Coords {
    record Ping(ResourceKey<Level> dimension, Vec3 pos, long until) {}
    static final List<Ping> PINGS = new ArrayList<>();
    static final Map<UUID, Long> LAST = new HashMap<>();

    public static boolean active() { return Play.on() && Play.config().coords().enabled(); }

    public static void register(CommandDispatcher<CommandSourceStack> d, PlayConfig c) {
        if (!c.coords().enabled()) return;
        d.register(Commands.literal("coords")
                .executes(x -> share(x.getSource(), null, ""))
                .then(Commands.literal("to").then(Commands.argument("player", EntityArgument.player())
                        .executes(x -> share(x.getSource(), EntityArgument.getPlayer(x, "player"), ""))
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(x -> share(x.getSource(), EntityArgument.getPlayer(x, "player"), StringArgumentType.getString(x, "message"))))))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(x -> share(x.getSource(), null, StringArgumentType.getString(x, "message")))));
    }

    /** The shared line: "&lt;name&gt; is at x y z (dimension) message". */
    public static Component line(ServerPlayer p, String message) {
        var pos = p.blockPosition();
        var out = PlayText.t("coords.line", p.getDisplayName(), PlayText.coords(pos.getX(), pos.getY(), pos.getZ()), PlayText.dimension(p.level().dimension()));
        if (message != null && !message.isBlank()) out.append(Component.literal(" " + message.strip()).withStyle(ChatFormatting.GRAY));
        return out;
    }

    static int share(CommandSourceStack source, ServerPlayer to, String message) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var p = source.getPlayerOrException();
        if (!active()) return 0;
        var c = Play.config().coords();
        long now = System.currentTimeMillis();
        Long last = LAST.get(p.getUUID());
        if (last != null && now - last < c.cooldownSeconds() * 1000L) {
            source.sendFailure(PlayText.t("coords.cooldown", (c.cooldownSeconds() * 1000L - (now - last)) / 1000 + 1));
            return 0;
        }
        LAST.put(p.getUUID(), now);
        var line = line(p, message);
        if (to != null) { to.sendSystemMessage(line); if (to != p) p.sendSystemMessage(line); }
        else source.getServer().getPlayerList().broadcastSystemMessage(line, false);
        if (c.ping()) ping(p.level().dimension(), p.position(), c.pingSeconds());
        return 1;
    }

    public static void ping(ResourceKey<Level> dimension, Vec3 pos, int seconds) {
        PINGS.add(new Ping(dimension, pos, System.currentTimeMillis() + seconds * 1000L));
    }
    public static int pings() { return PINGS.size(); }

    public static void tick(MinecraftServer server) {
        if (PINGS.isEmpty() || server.getTickCount() % 10 != 0) return;
        long now = System.currentTimeMillis();
        PINGS.removeIf(p -> p.until() < now);
        for (var ping : PINGS) {
            ServerLevel level = server.getLevel(ping.dimension());
            if (level == null) continue;
            for (int i = 0; i < 6; i++) level.sendParticles(ParticleTypes.END_ROD, ping.pos().x, ping.pos().y + 0.5 + i * 0.8, ping.pos().z, 2, 0.05, 0.3, 0.05, 0.0);
        }
    }
    /** Tests: forget cooldowns and markers. */
    /** Leaving: drop the share time once its cooldown is over. */
    public static void forget(ServerPlayer p) {
        Long last = LAST.get(p.getUUID());
        if (last != null && System.currentTimeMillis() - last >= Play.config().coords().cooldownSeconds() * 1000L) LAST.remove(p.getUUID());
    }
    public static void reset() { PINGS.clear(); LAST.clear(); }
    private Coords() {}
}
