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
    public static final FoodProperties ROSE_GOLD_APPLE_FOOD = food(6);

    /** BLOCKS07 (06.11.): the carrots sit slightly above the golden carrot (6 hunger, 14.4 saturation); Rose Gold is the better one. No effects, not always edible (like the golden carrot). */
    public static final FoodProperties ROSE_BRONZE_CARROT_FOOD = new FoodProperties.Builder().nutrition(7).saturationModifier(SATURATION).build();
    public static final FoodProperties ROSE_GOLD_CARROT_FOOD = new FoodProperties.Builder().nutrition(8).saturationModifier(SATURATION).build();

    /** Like the golden apple (Regeneration II 0:05, Absorption I 2:00) plus Resistance I 0:30. No Fire Resistance on the plain apples (B02, 02.11.). */
    public static final Consumable ROSE_BRONZE_APPLE = effects(
            new MobEffectInstance(MobEffects.REGENERATION, 100, 1), new MobEffectInstance(MobEffects.ABSORPTION, 2400, 0),
            new MobEffectInstance(MobEffects.RESISTANCE, 600, 0));
    /** Like the golden apple plus Resistance I 0:50. */
    public static final Consumable ROSE_GOLD_APPLE = effects(
            new MobEffectInstance(MobEffects.REGENERATION, 100, 1), new MobEffectInstance(MobEffects.ABSORPTION, 2400, 0),
            new MobEffectInstance(MobEffects.RESISTANCE, 1000, 0));
    /** Like the enchanted golden apple: Regeneration II 0:30, Resistance I 5:00, Fire Resistance 5:00, Absorption IV 2:00. */
    public static final Consumable ENCHANTED_ROSE_BRONZE_APPLE = effects(
            new MobEffectInstance(MobEffects.REGENERATION, 600, 1), new MobEffectInstance(MobEffects.RESISTANCE, 6000, 0),
            new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0), new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3));
    /** Regeneration II 0:35, Resistance I 5:00, Fire Resistance 5:00, Absorption IV 2:00. */
    public static final Consumable ENCHANTED_ROSE_GOLD_APPLE = effects(
            new MobEffectInstance(MobEffects.REGENERATION, 700, 1), new MobEffectInstance(MobEffects.RESISTANCE, 6000, 0),
            new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0), new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3));

    /**
     * PAL35b (owner 12.11.: food of the dimensional metals, Vanilla-near effects; docs/ai/PLAN-A-PAL35-2026-11-12.md b4.4): one Vanilla effect
     * per dimension - Terranite (Overworld, mining) Haste I, Infernite (Nether) Fire Resistance, Zenithite (End, sky) Slow Falling. The apple
     * is the golden apple (Regeneration II 0:05, Absorption I 2:00) plus the effect for 2:00, the carrot the Rose Gold carrot's numbers plus
     * the effect for 1:00. No stronger fight bonus than the Rose Gold apple: prestige and utility, not a new tier.
     */
    public static final FoodProperties RIFT_APPLE_FOOD = food(6);
    public static final FoodProperties RIFT_CARROT_FOOD = ROSE_GOLD_CARROT_FOOD;
    public static final int RIFT_APPLE_TICKS = 2400;
    public static final int RIFT_CARROT_TICKS = 1200;

    /** The dimension's effect of a rift metal (terranite, infernite, zenithite). */
    public static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> riftEffect(String metal) {
        return switch (metal) {
            case "terranite" -> MobEffects.HASTE;
            case "infernite" -> MobEffects.FIRE_RESISTANCE;
            case "zenithite" -> MobEffects.SLOW_FALLING;
            default -> throw new IllegalArgumentException("no rift metal: " + metal);
        };
    }

    public static Consumable riftApple(String metal) {
        return effects(new MobEffectInstance(MobEffects.REGENERATION, 100, 1), new MobEffectInstance(MobEffects.ABSORPTION, 2400, 0),
                new MobEffectInstance(riftEffect(metal), RIFT_APPLE_TICKS, 0));
    }

    /** N77 (owner 16.11.): the enchanted rift apple - Regeneration II 0:20, Absorption IV 2:00, Resistance I 5:00 and the dimension's effect 5:00. */
    public static final int ENCHANTED_RIFT_APPLE_TICKS = 6000;

    public static Consumable enchantedRiftApple(String metal) {
        return effects(new MobEffectInstance(MobEffects.REGENERATION, 400, 1), new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3),
                new MobEffectInstance(MobEffects.RESISTANCE, ENCHANTED_RIFT_APPLE_TICKS, 0),
                new MobEffectInstance(riftEffect(metal), ENCHANTED_RIFT_APPLE_TICKS, 0));
    }

    public static Consumable riftCarrot(String metal) {
        return effects(new MobEffectInstance(riftEffect(metal), RIFT_CARROT_TICKS, 0));
    }

    private OresFood() {}

    private static FoodProperties food(int nutrition) {
        return new FoodProperties.Builder().nutrition(nutrition).saturationModifier(SATURATION).alwaysEdible().build();
    }

    private static Consumable effects(MobEffectInstance... effects) {
        return Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(List.of(effects))).build();
    }
}
