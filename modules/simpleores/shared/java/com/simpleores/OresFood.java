package com.simpleores;

import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

/**
 * The rose gold apples (IG31-41, 01.11.2026): a step between the golden apple and the Netherite apple of SimpleBuilding, one plain and one
 * enchanted apple per alloy. Numbers and reasons: docs/ai/PLAN-B-APPLE41-2026-11-01.md and docs/ai/PLAN-B-APPLE02-2026-11-02.md (final values). Always edible like the golden apple, no cooldown.
 */
public final class OresFood {
    /** Saturation factor of the Vanilla golden apple (nutrition * factor * 2). */
    private static final float SATURATION = 1.2F;

    public static final FoodProperties ROSE_BRONZE_APPLE_FOOD = food(5);
    public static final FoodProperties BLOSSOM_GOLD_APPLE_FOOD = food(6);

    /** Like the golden apple (Regeneration II 0:05, Absorption I 2:00) plus Resistance I 0:30. No Fire Resistance on the plain apples (B02, 02.11.). */
    public static final Consumable ROSE_BRONZE_APPLE = effects(
            new MobEffectInstance(MobEffects.REGENERATION, 100, 1), new MobEffectInstance(MobEffects.ABSORPTION, 2400, 0),
            new MobEffectInstance(MobEffects.RESISTANCE, 600, 0));
    /** Like the golden apple plus Resistance I 0:50. */
    public static final Consumable BLOSSOM_GOLD_APPLE = effects(
            new MobEffectInstance(MobEffects.REGENERATION, 100, 1), new MobEffectInstance(MobEffects.ABSORPTION, 2400, 0),
            new MobEffectInstance(MobEffects.RESISTANCE, 1000, 0));
    /** Like the enchanted golden apple: Regeneration II 0:30, Resistance I 5:00, Fire Resistance 5:00, Absorption IV 2:00. */
    public static final Consumable ENCHANTED_ROSE_BRONZE_APPLE = effects(
            new MobEffectInstance(MobEffects.REGENERATION, 600, 1), new MobEffectInstance(MobEffects.RESISTANCE, 6000, 0),
            new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0), new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3));
    /** Regeneration II 0:35, Resistance I 5:00, Fire Resistance 5:00, Absorption IV 2:00. */
    public static final Consumable ENCHANTED_BLOSSOM_GOLD_APPLE = effects(
            new MobEffectInstance(MobEffects.REGENERATION, 700, 1), new MobEffectInstance(MobEffects.RESISTANCE, 6000, 0),
            new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0), new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3));

    private OresFood() {}

    private static FoodProperties food(int nutrition) {
        return new FoodProperties.Builder().nutrition(nutrition).saturationModifier(SATURATION).alwaysEdible().build();
    }

    private static Consumable effects(MobEffectInstance... effects) {
        return Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(List.of(effects))).build();
    }
}
