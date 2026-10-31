package com.simplebuilding.util;

import com.simplebuilding.framework.api.TrimContracts;
import net.minecraft.world.entity.player.Player;

/**
 * Bridge to framework {@link TrimContracts} (MOB27-3, docs/ai/PLAN-B-MOB32-2026-10-31.md; 26.3: the framework is on the classpath,
 * the 26.2 twin keeps every trim ability in the core). Simple Mobs applies the armour trim abilities and claims them; the core then
 * keeps its own hooks quiet ({@link #handledElsewhere}) and only feeds the abilities as their {@link TrimContracts.Host}: the
 * resonance from {@link TrimMultiplierLogic} (players; everyone else gets Simple Mobs' fixed value, the same 0.2 as
 * {@link TrimEffectUtil#NON_PLAYER_MULTIPLIER}), the strengths from {@code server.trimStrengths} and the player's own switch.
 * Without Simple Mobs (or with it switched off) the core applies everything itself, as before.
 */
public final class TrimHost {
    private TrimHost() {
    }

    /** Registers the core as host; called once from the Fabric entry point. */
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

    /** Whether another mod (Simple Mobs) applies the trim abilities right now; the core's trim hooks then do nothing. */
    public static boolean handledElsewhere() {
        return TrimContracts.claimed();
    }
}
