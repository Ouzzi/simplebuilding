package com.simplesandwiches.sandwich;

import com.simplesandwiches.config.SandwichConfig;
import com.simplesandwiches.registry.ModComponents;
import com.simplesandwiches.registry.ModItems;
import com.simplesandwiches.registry.ModTags;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.UseCooldown;

/**
 * Computes a sandwich's Vanilla components once, when it is closed on the cutting board. Values come
 * from the ingredients' default components ({@link Item#components()}), never from the concrete
 * stacks, so renamed or edited ingredient stacks change nothing.
 *
 * <p>IG31-28 (replaces F4 = C): nutrition and saturation are the maximum over bread and all ingredients
 * plus a falling bonus for the others ({@link #BONUS_FIRST}, halved for each further one, values sorted
 * descending). Saturation is capped at {@link #SATURATION_CAP} x nutrition; nutrition rounds to whole points
 * (half a drumstick), saturation to {@link #ROUND_STEP}. Butter adds {@code butterBonus} (default 10 %) to both
 * and to effect durations. This is the one place for the formula.
 */
public final class SandwichFormula {
    public static final float BASE_SECONDS = 1.6F, SECONDS_PER_INGREDIENT = 0.2F;
    /** Bonus of the second-best value (share of that value); each further value gets half of the previous share. */
    public static final double BONUS_FIRST = 0.10, BONUS_DECAY = 0.5;
    /** Saturation never exceeds this multiple of nutrition. */
    public static final double SATURATION_CAP = 1.2;
    /** Saturation rounds to this step. */
    public static final double ROUND_STEP = 0.5;

    /** Largest value + falling share of the rest (sorted descending): 10 %, 5 %, 2.5 % ... */
    public static double combine(List<? extends Number> values) {
        List<Double> sorted = new ArrayList<>();
        for (Number n : values) sorted.add(n.doubleValue());
        sorted.sort(java.util.Comparator.reverseOrder());
        double total = sorted.isEmpty() ? 0.0 : sorted.get(0), share = BONUS_FIRST;
        for (int i = 1; i < sorted.size(); i++, share *= BONUS_DECAY) total += sorted.get(i) * share;
        return total;
    }

    public record Result(FoodProperties food, Consumable consumable, Rarity rarity, boolean glint, float cooldownSeconds) {}

    public static Result compute(SandwichContents contents) {
        FoodProperties bread = Items.BREAD.components().get(DataComponents.FOOD);
        List<Integer> nutritions = new ArrayList<>();
        List<Float> saturations = new ArrayList<>();
        nutritions.add(bread == null ? 5 : bread.nutrition());
        saturations.add(bread == null ? 6.0F : bread.saturation());
        boolean alwaysEat = false;
        Rarity rarity = Rarity.COMMON;
        boolean glint = false;
        float cooldown = 0.0F;
        List<Consumable> consumables = new ArrayList<>();
        for (Holder<Item> ingredient : contents.ingredients()) {
            DataComponentMap c = ingredient.value().components();
            FoodProperties food = c.get(DataComponents.FOOD);
            if (food != null) {
                nutritions.add(food.nutrition());
                saturations.add(food.saturation());
                alwaysEat |= food.canAlwaysEat();
            }
            consumables.add(c.get(DataComponents.CONSUMABLE));
            Rarity r = c.get(DataComponents.RARITY);
            if (r != null && r.ordinal() > rarity.ordinal()) rarity = r;
            glint |= Boolean.TRUE.equals(c.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
            UseCooldown uc = c.get(DataComponents.USE_COOLDOWN);
            if (uc != null) cooldown = Math.max(cooldown, uc.seconds());
        }
        double factor = contents.buttered() ? 1.0 + SandwichConfig.butterBonus : 1.0;
        double hunger = combine(nutritions) * factor;
        int nutrition = (int) Math.round(hunger);
        double sat = Math.round(combine(saturations) * factor / ROUND_STEP) * ROUND_STEP;
        float saturation = (float) Math.min(sat, Math.floor(nutrition * SATURATION_CAP / ROUND_STEP) * ROUND_STEP);
        Consumable.Builder builder = Consumable.builder()
                .consumeSeconds(BASE_SECONDS + SECONDS_PER_INGREDIENT * contents.ingredients().size())
                .animation(ItemUseAnimation.EAT).sound(SoundEvents.GENERIC_EAT).hasConsumeParticles(true);
        EffectMerger.merge(consumables, factor).forEach(builder::onConsume);
        return new Result(new FoodProperties(nutrition, saturation, alwaysEat), builder.build(), rarity, glint, cooldown);
    }

    /** Writes contents and every derived component onto {@code stack} (deterministic). */
    public static ItemStack apply(ItemStack stack, SandwichContents contents) {
        Result r = compute(contents);
        stack.set(ModComponents.SANDWICH_CONTENTS, contents);
        stack.set(DataComponents.FOOD, r.food());
        stack.set(DataComponents.CONSUMABLE, r.consumable());
        stack.set(DataComponents.RARITY, r.rarity());
        if (r.glint()) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        else stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        if (r.cooldownSeconds() > 0) stack.set(DataComponents.USE_COOLDOWN, new UseCooldown(r.cooldownSeconds()));
        else stack.remove(DataComponents.USE_COOLDOWN);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, modelData(contents));
        return stack;
    }

    /** strings = layer keys 0..4 ("" when absent) + ingredient count; flags = [buttered]. */
    public static CustomModelData modelData(SandwichContents contents) {
        List<String> strings = new ArrayList<>();
        for (int i = 0; i < SandwichContents.MAX; i++) {
            strings.add(i < contents.ingredients().size() ? SandwichVisuals.key(contents.ingredients().get(i)) : "");
        }
        strings.add(Integer.toString(contents.ingredients().size()));
        return new CustomModelData(List.of(), List.of(contents.buttered()), strings, List.of());
    }

    /** Sandwich stack for given contents. */
    public static ItemStack create(SandwichContents contents) {
        return apply(new ItemStack(ModItems.SANDWICH), contents);
    }

    /** True when the item may go between the bread: tag member, edible, no bowl/bottle remainder. */
    public static boolean isIngredient(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(ModTags.SANDWICH_INGREDIENTS)) return false;
        DataComponentMap c = stack.getItem().components();
        return c.has(DataComponents.FOOD) && !c.has(DataComponents.USE_REMAINDER)
                && !stack.is(ModItems.SANDWICH) && !stack.is(Items.BREAD)
                && !(stack.getItem() instanceof net.minecraft.world.item.BucketItem);
    }

    private SandwichFormula() {}
}
