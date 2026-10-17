package com.simplebuilding.modules.simpletweaks.protect;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;

/**
 * {@code /tweaks protect status | reload | bench [radius] | checks [player]} (operator level {@code permission},
 * default 3). Registered as a {@code /tweaks} branch (the root carries no requirement), so the {@code /simpletweaks}
 * root of branch claude-tweaks-cmd picks it up once merged.
 */
public final class ProtectCommands {
    public static void register(CommandDispatcher<CommandSourceStack> d) { register(d, Protect.config()); }
    public static void register(CommandDispatcher<CommandSourceStack> d, ProtectConfig c) {
        int perm = c.permission();
        d.register(Commands.literal("tweaks").then(Commands.literal("protect").requires(s -> ProtectPerms.has(s, perm))
                .then(Commands.literal("status").executes(x -> status(x.getSource())))
                .then(Commands.literal("reload").executes(x -> reload(x.getSource())))
                .then(Commands.literal("bench").executes(x -> bench(x.getSource(), 2))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(0, 8)).executes(x -> bench(x.getSource(), IntegerArgumentType.getInteger(x, "radius")))))
                .then(Commands.literal("checks").executes(x -> checks(x.getSource(), null))
                        .then(Commands.argument("player", EntityArgument.player()).executes(x -> checks(x.getSource(), EntityArgument.getPlayer(x, "player")))))));
    }

    static Component onOff(boolean on) { return Protect.t(on ? "on" : "off"); }

    static int status(CommandSourceStack s) {
        var c = Protect.config();
        var a = c.antiXray();
        s.sendSuccess(() -> Protect.t("status.antixray", onOff(a.enabled()), a.engineMode(), String.join(", ", a.dimensions().keySet())), false);
        s.sendSuccess(() -> Protect.t("status.freecam", onOff(c.freecam().enabled()), onOff(c.freecam().requireClient())), false);
        s.sendSuccess(() -> Protect.t("status.others", onOff(c.seedHash().enabled()), onOff(c.hideHealth().enabled()), onOff(c.actionChecks().enabled())), false);
        return 1;
    }
    static int reload(CommandSourceStack s) {
        Protect.configure(ProtectConfig.load(ProtectConfig.PATH));
        FreecamRule.broadcast(s.getServer());
        s.sendSuccess(() -> Protect.t("reloaded"), true);
        return 1;
    }
    /** Cost of the obfuscation on the loaded chunks around the source, against Vanilla's own serialization. */
    static int bench(CommandSourceStack s, int radius) {
        var level = s.getLevel();
        var center = net.minecraft.core.BlockPos.containing(s.getPosition());
        int cx = center.getX() >> 4, cz = center.getZ() >> 4, n = 0;
        long v = 0, o = 0;
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            var chunk = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
            if (chunk == null) continue;
            var t = AntiXray.bench(chunk, 5);
            v += t[0]; o += t[1]; n += 5;
        }
        if (n == 0) { s.sendFailure(Protect.t("bench.none")); return 0; }
        double vu = v / 1000.0 / n, ou = o / 1000.0 / n;
        int chunks = n / 5;
        s.sendSuccess(() -> Protect.t("bench.result", chunks, String.format(Locale.ROOT, "%.0f", vu), String.format(Locale.ROOT, "%.0f", ou)), false);
        return chunks;
    }
    static int checks(CommandSourceStack s, net.minecraft.server.level.ServerPlayer player) {
        if (player != null) {
            var m = ActionChecks.counts(player.getUUID());
            s.sendSuccess(() -> Protect.t("checks.player", player.getGameProfile().name(), m.getOrDefault(ActionChecks.Kind.REACH_BLOCK, 0),
                    m.getOrDefault(ActionChecks.Kind.REACH_ENTITY, 0), m.getOrDefault(ActionChecks.Kind.FAST_BREAK, 0)), false);
            return m.values().stream().mapToInt(Integer::intValue).sum();
        }
        var all = ActionChecks.all();
        if (all.isEmpty()) { s.sendSuccess(() -> Protect.t("checks.none"), false); return 0; }
        for (var e : all.entrySet()) {
            var name = s.getServer().services().nameToIdCache().get(e.getKey()).map(p -> p.name()).orElse(e.getKey().toString());
            var m = e.getValue();
            s.sendSuccess(() -> Protect.t("checks.player", name, m.getOrDefault(ActionChecks.Kind.REACH_BLOCK, 0),
                    m.getOrDefault(ActionChecks.Kind.REACH_ENTITY, 0), m.getOrDefault(ActionChecks.Kind.FAST_BREAK, 0)), false);
        }
        return all.size();
    }
    private ProtectCommands() {}
}
