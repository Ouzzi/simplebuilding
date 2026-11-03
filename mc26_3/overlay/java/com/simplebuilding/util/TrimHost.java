package com.simplebuilding.util;

import com.simplebuilding.framework.api.TrimContracts;
import net.minecraft.world.entity.player.Player;

/**
 * Bridge to framework {@link TrimContracts} (MOB27-3; ZO9 A, docs/ai/PLAN-B-TRIMMOVE01-2026-11-03.md). On 26.3 Simple Mobs applies the
 * armour trim abilities; the core has no trim-ability code any more and only feeds them as their {@link TrimContracts.Host}: the
 * resonance from {@link TrimMultiplierLogic} (players; everyone else gets Simple Mobs' fixed value, the same 0.2 as
 * {@link TrimRates#NON_PLAYER_MULTIPLIER}), the strengths from {@code server.trimStrengths} and the player's own switch. The 26.2
 * twin does nothing (no framework there; the core still applies the abilities on that line until the port run).
 */
public final class TrimHost {
    private TrimHost() {
    }

    /** Registers the core as host; called once from every loader's entry point. */
    public static void install() {
        TrimContracts.setHost(new TrimContracts.Host() {
            @Override
            public double multiplier(Object livingEntity) {
                return livingEntity instanceof Player player ? TrimMultiplierLogic.getMultiplier(player) : Double.NaN;
            }

            @Override
            public float strength(String bonusKey) {
                return com.simplebuilding.config.ServerTuning.trimStrength(bonusKey);
            }

            @Override
            public boolean enabled(Object livingEntity) {
                return !(livingEntity instanceof TrimBenefitUser user) || user.simplebuilding$areTrimBenefitsEnabled();
            }
        });
    }
}
