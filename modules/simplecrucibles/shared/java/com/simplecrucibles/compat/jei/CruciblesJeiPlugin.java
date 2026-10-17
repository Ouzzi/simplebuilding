package com.simplecrucibles.compat.jei;

import com.simplecrucibles.SimpleCrucibles;
import com.simplecrucibles.compat.CrucibleInfo;
import com.simplecrucibles.compat.CrucibleRecipeCatalog;
import com.simplecrucibles.registry.CrucibleItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.level.ItemLike;

/**
 * Optional JEI plugin of Simple Crucibles (split stage 5b, was SimpleBuilding's): the crucible category (every Vanilla
 * cooking recipe as a crucible job per heat, plus warming), the four crucibles as its stations and the info pages of
 * the buckets and the Enderite tier ({@link CrucibleInfo#infoPages}). Only JEI loads it (Fabric: entrypoint
 * {@code jei_mod_plugin}; NeoForge: the annotation); Forge builds without it.
 */
@JeiPlugin
public final class CruciblesJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return SimpleCrucibles.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new CrucibleCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (Map.Entry<String, List<ItemLike>> page : CrucibleInfo.infoPages().entrySet()) {
            List<ItemStack> stacks = page.getValue().stream().filter(item -> item != null).map(ItemStack::new).toList();
            if (!stacks.isEmpty()) registration.addItemStackInfo(stacks, Component.translatable(CrucibleInfo.INFO_PREFIX + page.getKey()));
        }
    }

    /** The cooking recipes are only complete once the client has them. */
    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        IRecipeManager recipes = runtime.getRecipeManager();
        List<AbstractCookingRecipe> cooking = new ArrayList<>();
        recipes.createRecipeLookup(RecipeTypes.CAMPFIRE_COOKING).get().forEach(holder -> cooking.add(holder.value()));
        recipes.createRecipeLookup(RecipeTypes.SMOKING).get().forEach(holder -> cooking.add(holder.value()));
        recipes.createRecipeLookup(RecipeTypes.SMELTING).get().forEach(holder -> cooking.add(holder.value()));
        recipes.createRecipeLookup(RecipeTypes.BLASTING).get().forEach(holder -> cooking.add(holder.value()));
        recipes.addRecipes(CrucibleCategory.TYPE, CrucibleRecipeCatalog.build(cooking));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (Item item : List.of(CrucibleItems.IRON_CRUCIBLE, CrucibleItems.REINFORCED_CRUCIBLE, CrucibleItems.NETHERITE_CRUCIBLE,
                CrucibleItems.ENDERITE_CRUCIBLE)) {
            registration.addCraftingStation(CrucibleCategory.TYPE, item);
        }
    }
}
