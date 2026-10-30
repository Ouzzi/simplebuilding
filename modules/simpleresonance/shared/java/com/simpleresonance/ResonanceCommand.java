package com.simpleresonance;

import com.mojang.brigadier.CommandDispatcher;
import com.simplebuilding.framework.api.ResonanceContracts;
import com.simpleresonance.config.ResonanceConfig;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /resonance}: everybody sees his own value, the zone level where he stands and his grace period. {@code /resonance debug}
 * (game masters) lists the possible owners of a spawn at his place with their chances and the level such a spawn would get.
 */
public final class ResonanceCommand {
    private ResonanceCommand() {}

    private static String n(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("resonance")
                .executes(ctx -> own(ctx.getSource()))
                .then(Commands.literal("debug").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> debug(ctx.getSource()))));
    }

    private static int own(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        ResonanceService s = ResonanceService.INSTANCE;
        if (!s.enabled()) {
            source.sendFailure(Component.translatable("command.simpleresonance.off"));
            return 0;
        }
        double w = s.valueOf(p), f = s.fractionOf(p);
        source.sendSuccess(() -> Component.translatable("command.simpleresonance.value", n(w), n(ResonanceMath.level(w))), false);
        source.sendSuccess(() -> Component.translatable("command.simpleresonance.parts", n(f), n(ResonanceMath.longTerm(s.data().record(p.getUUID()).points))), false);
        double zone = s.zoneLevel(ResonanceService.dimensionOf(p), p.getX(), p.getZ());
        source.sendSuccess(() -> Component.translatable("command.simpleresonance.zone", n(zone)), false);
        double hours = ResonanceService.playHours(p);
        if (hours < ResonanceConfig.graceHours) {
            source.sendSuccess(() -> Component.translatable("command.simpleresonance.grace", n(ResonanceConfig.graceHours - hours)), false);
        } else {
            source.sendSuccess(() -> Component.translatable("command.simpleresonance.grace_over"), false);
        }
        return 1;
    }

    private static int debug(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        ResonanceService s = ResonanceService.INSTANCE;
        if (!s.enabled()) {
            source.sendFailure(Component.translatable("command.simpleresonance.off"));
            return 0;
        }
        String dim = ResonanceService.dimensionOf(p);
        List<ResonanceService.Candidate> c = s.candidates(dim, p.getX(), p.getY(), p.getZ());
        source.sendSuccess(() -> Component.translatable("command.simpleresonance.debug_head", c.size()), false);
        for (ResonanceService.Candidate cand : c) {
            source.sendSuccess(() -> Component.translatable("command.simpleresonance.debug_owner", cand.name(), n(cand.distance()), n(cand.probability() * 100.0)), false);
        }
        ResonanceContracts.Spawn sp = s.sample(dim, p.getX(), p.getY(), p.getZ(), 0.0, 0.5);
        source.sendSuccess(() -> Component.translatable("command.simpleresonance.debug_level", n(sp.level()), n(sp.zone())), false);
        return c.size();
    }
}
