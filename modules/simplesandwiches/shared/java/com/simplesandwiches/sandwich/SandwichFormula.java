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
 * <p>Owner decision F4 = C: nutrition and saturation are bread + the sum of all ingredients, without a
 * cap (FoodData caps at 20 when eating). Butter adds {@code butterBonus} (default 10 %) to both and to
 * effect durations.
 */
public final class SandwichFormula {
    public static final float BASE_SECONDS = 1.6F, SECONDS_PER_INGREDIENT = 0.2F;

    public record Result(FoodProperties food, Consumable consumable, Rarity rarity, boolean glint, float cooldownSeconds) {}

    public static Result compute(SandwichContents contents) {
        FoodProperties bread = Items.BREAD.components().get(DataComponents.FOOD);
        int nutrition = bread == null ? 5 : bread.nutrition();
        float saturation = bread == null ? 6.0F : bread.saturation();
        boolean alwaysEat = false;
        Rarity rarity = Rarity.COMMON;
        boolean glint = false;
        float cooldown = 0.0F;
        List<Consumable> consumables = new ArrayList<>();
        for (Holder<Item> ingredient : contents.ingredients()) {
            DataComponentMap c = ingredient.value().components();
            FoodProperties food = c.get(DataComponents.FOOD);
            if (food != null) {
                nutrition += food.nutrition();
                saturation += food.saturation();
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
        if (contents.buttered()) {
            nutrition = (int) Math.round(nutrition * factor);
            saturation = (float) (saturation * factor);
        }
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
