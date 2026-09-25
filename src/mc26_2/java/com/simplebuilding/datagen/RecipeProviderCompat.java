package com.simplebuilding.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.RecipeUnlockedTrigger;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;

import java.util.concurrent.CompletableFuture;

/**
 * Recipe datagen plumbing, MC 26.2 side (Fabric only; twin in mc26_3/fabric/src/main/java).
 *
 * <p>26.2 builds a RecipeProvider from a registry lookup and a RecipeOutput; 26.3 from two
 * BootstrapContexts (recipes and advancements are reloadable registries there). ModRecipeProvider
 * stays one shared file by only ever talking to this class: it gets the constructor arguments as
 * opaque objects and asks {@link Generator} for the item lookup and the "recipe unlocked" criterion.
 */
public abstract class RecipeProviderCompat extends FabricRecipeProvider {

    protected RecipeProviderCompat(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected final RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        return createGenerator(registries, output);
    }

    /** Builds the generator; pass both arguments on to the {@link Generator} constructor unchanged. */
    protected abstract RecipeProvider createGenerator(Object first, Object second);

    public abstract static class Generator extends RecipeProvider {

        protected Generator(Object first, Object second) {
            super((HolderLookup.Provider) first, (RecipeOutput) second);
        }

        protected HolderGetter<Item> items() {
            return this.registries.lookupOrThrow(Registries.ITEM);
        }

        protected Criterion<RecipeUnlockedTrigger.TriggerInstance> unlockedRecipe(ResourceKey<Recipe<?>> recipe) {
            return RecipeUnlockedTrigger.unlocked(recipe);
        }

        /**
         * Cooking time for a blasting/smoking recipe, given the ticks the blast furnace or smoker
         * should really need. 26.2 stores exactly that; 26.3 stores the furnace time and lets those
         * machines cook twice as fast (minecraft:block/fast_cooking).
         */
        protected int fastMachineTicks(int machineTicks) {
            return machineTicks;
        }
    }
}
