package com.simplebuilding.datagen;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.effect.ModEffects;
import com.simplebuilding.tweaks.item.TweaksItems;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.BrewingRecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;

/**
 * Brau-Rezepte der Mod-Traenke (26.3 braut datengetrieben, {@code minecraft:brewing}), nach dem Schema von Vanillas
 * {@code VanillaBrewingProvider}: jede Mischung fuer Trank, Wurftrank und Verweiltrank; Schwarzpulver macht aus dem
 * Trank den Wurftrank, Drachenatem aus dem Wurftrank den Verweiltrank. Getraenkte Pfeile entstehen wie bei Vanilla am
 * Werktisch. Anders als {@code BrewingProvider} nur fuer die eigenen Traenke (keine Kopien der Vanilla-Rezepte) und im
 * Namensraum der Mod ({@code simplebuilding:brewing/...}).
 */
public final class ModBrewingProvider {
    private static final List<Item> CONTAINERS = List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION);

    private final RecipeOutput output;

    public ModBrewingProvider(RecipeOutput output) {
        this.output = output;
    }

    public void buildRecipes() {
        if (ModEffects.CRAFTY_SHULKER_POTION == null || ModEffects.LONG_CRAFTY_SHULKER_POTION == null) {
            return;
        }
        // Seltsamer Trank + Shulkerkopf -> Listiger Shulker; + Redstone -> verlaengert.
        mix(Potions.AWKWARD, TweaksItems.SHULKER_HEAD, ModEffects.CRAFTY_SHULKER_POTION);
        mix(ModEffects.CRAFTY_SHULKER_POTION, Items.REDSTONE, ModEffects.LONG_CRAFTY_SHULKER_POTION);
        for (Holder<Potion> potion : List.of(ModEffects.CRAFTY_SHULKER_POTION, ModEffects.LONG_CRAFTY_SHULKER_POTION)) {
            save(BrewingRecipeBuilder.brewingContainerTransform(Items.POTION, potion, Items.GUNPOWDER, Items.SPLASH_POTION));
            save(BrewingRecipeBuilder.brewingContainerTransform(Items.SPLASH_POTION, potion, Items.DRAGON_BREATH, Items.LINGERING_POTION));
        }
    }

    private void mix(Holder<Potion> input, Item reagent, Holder<Potion> result) {
        for (Item container : CONTAINERS) {
            save(BrewingRecipeBuilder.brewingMix(container, input, reagent, result));
        }
    }

    private void save(BrewingRecipeBuilder builder) {
        Identifier id = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, builder.defaultId().identifier().getPath());
        builder.save(this.output, ResourceKey.create(Registries.RECIPE, id));
    }
}
