package com.simplesandwiches.sandwich;

import com.simplesandwiches.config.SandwichConfig;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ClearAllStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.consume_effects.RemoveStatusEffectsConsumeEffect;

/**
 * Owner decision F3 = B: the same effect from several ingredients becomes one effect. Strength is
 * the maximum, durations add up (at most {@code effectDurationMultiplierCap} times the longest
 * single duration and at most {@code effectDurationCapTicks}), and the chance is combined
 * {@code 1 - prod(1 - p)}. Butter multiplies the summed duration before the caps. Removal effects
 * are kept once; teleport and sound effects are dropped.
 */
public final class EffectMerger {
    private static final class Acc {
        int amplifier = -1;
        long sum;
        int longest;
        double miss = 1.0;
        boolean ambient = true, visible = false, icon = false;
    }

    public static List<ConsumeEffect> merge(List<Consumable> consumables, double durationFactor) {
        Map<Holder<MobEffect>, Acc> merged = new LinkedHashMap<>();
        boolean clearAll = false;
        LinkedHashSet<Holder<MobEffect>> removals = new LinkedHashSet<>();
        for (Consumable consumable : consumables) {
            if (consumable == null) continue;
            for (ConsumeEffect effect : consumable.onConsumeEffects()) {
                if (effect instanceof ApplyStatusEffectsConsumeEffect apply) {
                    for (MobEffectInstance inst : apply.effects()) {
                        Acc acc = merged.computeIfAbsent(inst.getEffect(), k -> new Acc());
                        int duration = inst.isInfiniteDuration() ? SandwichConfig.effectDurationCapTicks : Math.max(1, inst.getDuration());
                        acc.amplifier = Math.max(acc.amplifier, inst.getAmplifier());
                        acc.sum += duration;
                        acc.longest = Math.max(acc.longest, duration);
                        acc.miss *= 1.0 - Math.max(0.0, Math.min(1.0, apply.probability()));
                        acc.ambient &= inst.isAmbient();
                        acc.visible |= inst.isVisible();
                        acc.icon |= inst.showIcon();
                    }
                } else if (effect instanceof ClearAllStatusEffectsConsumeEffect) {
                    clearAll = true;
                } else if (effect instanceof RemoveStatusEffectsConsumeEffect remove) {
                    for (Holder<MobEffect> h : remove.effects()) removals.add(h);
                }
            }
        }
        List<ConsumeEffect> out = new ArrayList<>();
        if (clearAll) out.add(ClearAllStatusEffectsConsumeEffect.INSTANCE);
        if (!removals.isEmpty()) out.add(new RemoveStatusEffectsConsumeEffect(HolderSet.direct(List.copyOf(removals))));
        for (var e : merged.entrySet()) {
            Acc acc = e.getValue();
            int duration = cappedDuration(acc.sum, acc.longest, durationFactor, e.getKey().value().isInstantaneous());
            float probability = (float) Math.max(0.0, Math.min(1.0, 1.0 - acc.miss));
            if (probability <= 0.0F) continue;
            out.add(new ApplyStatusEffectsConsumeEffect(
                    new MobEffectInstance(e.getKey(), duration, acc.amplifier, acc.ambient, acc.visible, acc.icon), probability));
        }
        return List.copyOf(out);
    }

    /** Sum times the butter factor, capped at multiplier x longest and at the absolute tick cap. */
    public static int cappedDuration(long sum, int longest, double factor, boolean instant) {
        if (instant) return 1;
        double scaled = sum * Math.max(1.0, factor);
        double cap = Math.min(longest * SandwichConfig.effectDurationMultiplierCap, SandwichConfig.effectDurationCapTicks);
        return (int) Math.max(1, Math.min(scaled, Math.max(cap, 1)));
    }

    private EffectMerger() {}
}
