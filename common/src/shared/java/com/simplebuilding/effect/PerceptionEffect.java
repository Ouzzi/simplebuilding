package com.simplebuilding.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Effects that only change what the affected player sees (queue N20, docs/ai/KONZEPT-DECEIVER-EFFEKTE-2026-10-07.md):
 * Shivering (the crosshair trembles), Mirage and Reverse Mirage (mobs look like others of their size) and Faded
 * (everything black and white). The server only carries the effect; the client draws it for its own player
 * (26.3 client mixins {@code ShiveringCrosshairMixin}, {@code MirageEntityMixin}, {@code FadedGameRendererMixin}).
 * Milk removes them like any effect.
 */
public class PerceptionEffect extends MobEffect {
    public PerceptionEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }
}
