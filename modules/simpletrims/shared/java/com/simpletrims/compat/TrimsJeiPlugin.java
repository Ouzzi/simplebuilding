package com.simpletrims.compat;

import com.simpletrims.SimpleTrims;
import com.simplelib.compat.jei.InWorldUpgrade;
import com.simplelib.compat.jei.InWorldUpgradeCategory;
import com.simpletrims.TemplateTools;
import com.simpletrims.TrimUpgrades;
import com.simpletrims.placed.PlacedTemplates;
import com.simpletrims.recipe.TrimUpgradeRecipe;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.smithing.ISmithingCategoryExtension;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Loaded only by JEI: the trim upgrades (template + armor + material) show the armor WITH the upgrade as the result,
 * not the plain armor the recipe display carries, and follow the armor the player focuses on.
 */
@JeiPlugin
public final class TrimsJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return SimpleTrims.id("jei");
    }

    /**
     * The in-world forging of templates (placed template + tool + ink in the off hand, strikes) in SimpleLib's shared
     * tab {@link InWorldUpgradeCategory}. SimpleBuilding's own tab of the same ways is empty with Simple Trims (its
     * table is switched off there), so this is what shows the Glowing, Emitting and Inverting templates' origin.
     */
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(InWorldUpgradeCategory.TYPE, inWorldUpgrades());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (Item tool : tools()) registration.addCraftingStation(InWorldUpgradeCategory.TYPE, tool);
    }

    /** One entry per upgrade of {@link PlacedTemplates#upgrades()}: any Vanilla template + catalyst (+ extras) -> the upgraded template. */
    static List<InWorldUpgrade> inWorldUpgrades() {
        List<Item> templates = new ArrayList<>();
        BuiltInRegistries.ITEM.forEach(item -> {
            if (PlacedTemplates.isTrimTemplate(new ItemStack(item))) templates.add(item);
        });
        List<Item> tools = tools();
        List<InWorldUpgrade> out = new ArrayList<>();
        if (templates.isEmpty() || tools.isEmpty()) return out;
        List<Component> notes = List.of(Component.translatable("jei.simpletrims.note.in_world", PlacedTemplates.PLACED_HITS),
                Component.translatable("jei.simpletrims.note.durability", PlacedTemplates.TOOL_DAMAGE));
        PlacedTemplates.upgrades().forEach((catalyst, result) -> {
            List<InWorldUpgrade.Input> inputs = new ArrayList<>();
            inputs.add(new InWorldUpgrade.Input(templates, 1));
            inputs.add(InWorldUpgrade.Input.of(catalyst, PlacedTemplates.CATALYST_COST));
            PlacedTemplates.extraMaterials(catalyst).forEach((item, count) -> inputs.add(InWorldUpgrade.Input.of(item, count)));
            out.add(new InWorldUpgrade("simpletrims/" + BuiltInRegistries.ITEM.getKey(result).getPath(), inputs, tools, new ItemStack(result), notes));
        });
        return out;
    }

    /** The tools of the current mode: axes, or the hammers once a hammer partner switched the axe ways off. */
    private static List<Item> tools() {
        List<Item> tools = new ArrayList<>();
        BuiltInRegistries.ITEM.getTagOrEmpty(TemplateTools.currentTag()).forEach(holder -> tools.add(holder.value()));
        return tools;
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getSmithingCategory().addExtension(TrimUpgradeRecipe.class, new Extension());
    }

    private static final class Extension implements ISmithingCategoryExtension<TrimUpgradeRecipe> {
        private static ItemStack first(Optional<Ingredient> ingredient) {
            return ingredient.flatMap(i -> i.items().findFirst()).map(ItemStack::new).orElse(ItemStack.EMPTY);
        }

        /** The armor of the base list with this upgrade: same order and length as the base slot. */
        private static List<ItemStack> outputs(TrimUpgradeRecipe recipe) {
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
        public <T extends IIngredientAcceptor<T>> void setTemplate(TrimUpgradeRecipe recipe, T ingredients) {
            recipe.templateIngredient().ifPresent(ingredients::add);
        }

        @Override
        public <T extends IIngredientAcceptor<T>> void setBase(TrimUpgradeRecipe recipe, T ingredients) {
            ingredients.add(recipe.baseIngredient());
        }

        @Override
        public <T extends IIngredientAcceptor<T>> void setAddition(TrimUpgradeRecipe recipe, T ingredients) {
            recipe.additionIngredient().ifPresent(ingredients::add);
        }

        @Override
        public <T extends IIngredientAcceptor<T>> void setOutput(TrimUpgradeRecipe recipe, T ingredients) {
            ingredients.addItemStacks(outputs(recipe));
        }

        @Override
        public void onDisplayedIngredientsUpdate(TrimUpgradeRecipe recipe, IRecipeSlotDrawable templateSlot,
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
}
