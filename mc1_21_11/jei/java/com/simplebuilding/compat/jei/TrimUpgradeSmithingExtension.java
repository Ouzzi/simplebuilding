package com.simplebuilding.compat.jei;

import com.simplebuilding.recipe.UpgradeSmithingRecipe;
import com.simplebuilding.util.TrimUpgrades;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.smithing.ISmithingCategoryExtension;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * JEI-Anzeige der drei Besatz-Aufwertungen (Glowing, Emitting, Pulsating): Vorlage + Ruestung +
 * Material -> <em>dieselbe Ruestung mit der Wirkung</em>. Die Datapack-Rezepte dafuer
 * ({@code *_armor_upgrade_dummy}) sind Platzhalter, die nur die Slots des Schmiedetischs freischalten,
 * und nennen die Vorlage als Ergebnis - JEI zeigte deshalb "Vorlage + Ruestung + Leuchttinte ->
 * Vorlage" (Besitzer 2026-09-28). Das Plugin blendet die Platzhalter aus und zeigt je Aufwertung ein
 * {@link UpgradeSmithingRecipe} als Traeger, dessen Ergebnis {@link TrimUpgrades} berechnet - genau
 * das, was der Schmiedetisch ausgibt.
 */
final class TrimUpgradeSmithingExtension implements ISmithingCategoryExtension<UpgradeSmithingRecipe> {

    /** Die Platzhalter-Rezepte, die das Plugin in JEI ausblendet. */
    static final List<Identifier> DUMMIES = List.of(
            Identifier.fromNamespaceAndPath("simplebuilding", "glowing_armor_upgrade_dummy"),
            Identifier.fromNamespaceAndPath("simplebuilding", "emitting_armor_upgrade_dummy"),
            Identifier.fromNamespaceAndPath("simplebuilding", "pulsating_armor_upgrade_dummy"));

    /** Je Aufwertung ein Anzeige-Rezept: Vorlage, alle Besatz-faehigen Ruestungen, Material. */
    static List<UpgradeSmithingRecipe> displayRecipes() {
        List<UpgradeSmithingRecipe> recipes = new ArrayList<>();
        Item[] pieces = examples().stream().map(ItemStack::getItem).distinct().toArray(Item[]::new);
        if (pieces.length == 0) {
            return recipes;
        }
        Ingredient armor = Ingredient.of(pieces);
        for (TrimUpgrades.Upgrade upgrade : TrimUpgrades.ALL) {
            recipes.add(new UpgradeSmithingRecipe(Optional.of(Ingredient.of(upgrade.template())), armor,
                    Optional.of(Ingredient.of(upgrade.material()))));
        }
        return recipes;
    }

    /** Beispiel-Ruestungen: alles im Tag {@code #minecraft:trimmable_armor}, in Registry-Reihenfolge. */
    private static List<ItemStack> examples() {
        List<ItemStack> stacks = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (stack.is(ItemTags.TRIMMABLE_ARMOR)) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    private static ItemStack first(Optional<Ingredient> ingredient) {
        return ingredient.flatMap(i -> i.items().findFirst()).map(holder -> new ItemStack(holder)).orElse(ItemStack.EMPTY);
    }

    /** Die Ruestungen der Basis-Liste, jeweils mit der Wirkung dieser Aufwertung - gleiche Reihenfolge, gleiche Laenge. */
    private static List<ItemStack> outputs(UpgradeSmithingRecipe recipe) {
        ItemStack template = first(recipe.templateIngredient());
        ItemStack material = first(recipe.additionIngredient());
        List<ItemStack> out = new ArrayList<>();
        recipe.baseIngredient().items().forEach(holder -> {
            ItemStack base = new ItemStack(holder);
            ItemStack result = TrimUpgrades.result(template, base, material);
            out.add(result == null || result.isEmpty() ? base : result);
        });
        return out;
    }

    @Override
    public <T extends IIngredientAcceptor<T>> void setTemplate(UpgradeSmithingRecipe recipe, T ingredients) {
        recipe.templateIngredient().ifPresent(ingredients::add);
    }

    @Override
    public <T extends IIngredientAcceptor<T>> void setBase(UpgradeSmithingRecipe recipe, T ingredients) {
        ingredients.add(recipe.baseIngredient());
    }

    @Override
    public <T extends IIngredientAcceptor<T>> void setAddition(UpgradeSmithingRecipe recipe, T ingredients) {
        recipe.additionIngredient().ifPresent(ingredients::add);
    }

    @Override
    public <T extends IIngredientAcceptor<T>> void setOutput(UpgradeSmithingRecipe recipe, T ingredients) {
        ingredients.addItemStacks(outputs(recipe));
    }

    /** Zeigt der Basis-Slot gerade ein bestimmtes Teil (etwa mit Fokus), passt das Ergebnis dazu. */
    @Override
    public void onDisplayedIngredientsUpdate(UpgradeSmithingRecipe recipe, IRecipeSlotDrawable templateSlot,
                                             IRecipeSlotDrawable baseSlot, IRecipeSlotDrawable additionSlot,
                                             IRecipeSlotDrawable outputSlot, IFocusGroup focuses) {
        ItemStack base = baseSlot.getDisplayedItemStack().orElse(ItemStack.EMPTY);
        if (base.isEmpty()) {
            return;
        }
        ItemStack template = templateSlot.getDisplayedItemStack().orElse(first(recipe.templateIngredient()));
        ItemStack material = additionSlot.getDisplayedItemStack().orElse(first(recipe.additionIngredient()));
        ItemStack result = TrimUpgrades.result(template, base, material);
        if (result != null && !result.isEmpty()) {
            outputSlot.createDisplayOverrides().add(result);
        }
    }
}
