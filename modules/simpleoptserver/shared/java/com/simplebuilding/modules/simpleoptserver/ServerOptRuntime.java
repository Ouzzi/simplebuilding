package com.simplebuilding.modules.simpleoptserver;

import com.mojang.brigadier.CommandDispatcher;
import com.simplebuilding.framework.api.OptCompat;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;

/** Loader-free runtime of S5a (driven by the loader's server events) and the {@code /simpleopt status} command. */
public final class ServerOptRuntime {
    private static DynamicDistance.Controller controller;

    private ServerOptRuntime() {}

    /** Dedicated servers only: the bounds are the server.properties distances at start. */
    public static void started(MinecraftServer server) {
        controller = null;
        if (!server.isDedicatedServer() || !ServerOpt.active(ServerOpt.DYNAMIC_DISTANCE)) return;
        PlayerList list = server.getPlayerList();
        controller = new DynamicDistance.Controller(list.getViewDistance(), list.getSimulationDistance());
        ServerOpt.LOG.info("S5a dynamic distance: view {}..{}, simulation {} ({}), target {} MSPT", ServerOpt.params().minView(),
                list.getViewDistance(), list.getSimulationDistance(), ServerOpt.params().adjustSimulation() ? "adjusted" : "fixed",
                ServerOpt.params().targetMspt());
    }

    public static void stopped() {
        controller = null;
    }

    public static void tick(MinecraftServer server) {
        DynamicDistance.Controller c = controller;
        if (c == null || !ServerOpt.active(ServerOpt.DYNAMIC_DISTANCE)) return;
        PlayerList list = server.getPlayerList();
        var now = new DynamicDistance.Distances(list.getViewDistance(), list.getSimulationDistance());
        double mspt = server.getAverageTickTimeNanos() / 1_000_000.0;
        var next = c.onTick(list.getPlayerCount(), mspt, now, ServerOpt.params());
        if (next == null) return;
        if (next.view() != now.view()) list.setViewDistance(next.view());
        if (next.simulation() != now.simulation()) list.setSimulationDistance(next.simulation());
        ServerOpt.LOG.info(String.format(Locale.ROOT, "S5a: MSPT %.1f -> view %d, simulation %d", mspt, next.view(), next.simulation()));
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("simpleopt").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("status").executes(ctx -> {
                    var src = ctx.getSource();
                    for (OptCompat.Decision dec : OptCompat.all()) {
                        String reason = dec.reason().name().toLowerCase(Locale.ROOT) + (dec.rival().isEmpty() ? "" : ": " + dec.rival());
                        src.sendSuccess(() -> Component.translatableWithFallback("simpleoptserver.status.area", "%s %s: %s (%s)",
                                dec.area().code(), dec.area().id(), dec.active() ? "on" : "off", reason), false);
                    }
                    DynamicDistance.Controller c = controller;
                    if (c != null) {
                        PlayerList list = src.getServer().getPlayerList();
                        src.sendSuccess(() -> Component.translatableWithFallback("simpleoptserver.status.distance",
                                "View distance %s of %s, simulation %s of %s, MSPT %s", list.getViewDistance(), c.maxView(),
                                list.getSimulationDistance(), c.maxSimulation(),
                                String.format(Locale.ROOT, "%.1f", src.getServer().getAverageTickTimeNanos() / 1_000_000.0)), false);
                    }
                    return 1;
                })));
    }
}
