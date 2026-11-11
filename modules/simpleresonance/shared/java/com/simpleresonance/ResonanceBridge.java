package com.simpleresonance;

import com.simpleresonance.config.ResonanceConfig;
import com.simpleresonance.net.ResonanceStatsPayload;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The trim resonance for other mods that must not import Simple Resonance (MOB27-3 R2, docs/ai/PLAN-B-TRIMSTATS27-2026-11-11.md):
 * SimpleBuilding's core finds this class by name and calls the static methods reflectively (its {@code TrimMultiplierLogic} and
 * {@code TrimStatsPanel} on 26.3). Parameters are {@code Object} so the caller needs no Minecraft class in the signature lookup.
 * Server players are measured by the service, client players read the last {@link ResonanceStatsPayload} the server sent.
 */
public final class ResonanceBridge {
    private ResonanceBridge() {}

    /** What {@code player} gathered since his last death; fresh for anything that is no player. */
    public static ResonanceStatsPayload stats(Object player) {
        if (player instanceof ServerPlayer sp) return ResonanceService.INSTANCE.stats(sp);
        if (player instanceof Player p && p.level().isClientSide()) return com.simpleresonance.client.ResonanceClient.stats();
        return new ResonanceStatsPayload(0, 0, 0, 0, 0, 0, ResonanceConfig.trimBenefitBaseMultiplier);
    }

    /** The trim resonance of a player (configured base x mean of the three factors, 0.2..2.0 with the default base). */
    public static double trimMultiplier(Object player) {
        return stats(player).multiplier();
    }

    /** {xp, survival, combat} of a player, each 0.1..1.0. */
    public static double[] factors(Object player) {
        return stats(player).factors();
    }

    /** {xp points, metres, active ticks, hostile kills, passive kills, damage} since the last death. */
    public static int[] gathered(Object player) {
        ResonanceStatsPayload s = stats(player);
        return new int[] {Math.max(0, s.xp()), Math.max(0, s.metres()), Math.max(0, s.activeTicks()), Math.max(0, s.hostile()),
                Math.max(0, s.passive()), Math.max(0, s.damage())};
    }

    /** The configured base: the server's own config, on the client the one the server sent last (else the local file). */
    public static double base(boolean clientSide) {
        return clientSide ? com.simpleresonance.client.ResonanceClient.stats().base() : ResonanceConfig.trimBenefitBaseMultiplier;
    }

    /** Client only: the resonance button's tooltip as shown ({@code shift}: every line). */
    public static List<?> shown(Object player, boolean shift) {
        return com.simpleresonance.client.ResonancePanel.shown((Player) player, shift);
    }

    /** Client only: every line of the resonance button's tooltip. */
    public static List<?> tooltip(Object player) {
        return com.simpleresonance.client.ResonancePanel.tooltip((Player) player);
    }
}
