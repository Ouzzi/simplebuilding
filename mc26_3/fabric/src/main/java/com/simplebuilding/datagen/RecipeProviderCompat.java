package com.simplebuilding.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.RecipeUnlockedTrigger;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;

import java.util.concurrent.CompletableFuture;

/** Recipe datagen plumbing, MC 26.3 side (see the 26.2 twin in src/mc26_2/java). */
public abstract class RecipeProviderCompat extends FabricRecipeProvider {

    protected RecipeProviderCompat(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected final RecipeProvider createRecipeProvider(HolderLookup.Provider registries,
                                                        BootstrapContext<Recipe<?>> recipes,
                                                        BootstrapContext<Advancement> advancements) {
        return createGenerator(recipes, advancements);
    }

    protected abstract RecipeProvider createGenerator(Object first, Object second);

    public abstract static class Generator extends RecipeProvider {

        @SuppressWarnings("unchecked")
        protected Generator(Object first, Object second) {
            super((BootstrapContext<Recipe<?>>) first, (BootstrapContext<Advancement>) second);
        }

        protected HolderGetter<Item> items() {
            return this.output.lookup(Registries.ITEM);
        }

        protected Criterion<RecipeUnlockedTrigger.TriggerInstance> unlockedRecipe(ResourceKey<Recipe<?>> recipe) {
            return RecipeUnlockedTrigger.unlocked(this.output.lookup(Registries.RECIPE).getOrThrow(recipe));
        }
    }
}
