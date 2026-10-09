package com.simplebuilding.datagen;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.effect.ModEffects;
import com.simplebuilding.items.ModItems;
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
        if (ModEffects.CRAFTY_SHULKER_POTION != null && ModEffects.LONG_CRAFTY_SHULKER_POTION != null) {
            // Seltsamer Trank + Shulkerkopf -> Listiger Shulker; + Redstone -> verlaengert.
            mix(Potions.AWKWARD, TweaksItems.SHULKER_HEAD, ModEffects.CRAFTY_SHULKER_POTION);
            mix(ModEffects.CRAFTY_SHULKER_POTION, Items.REDSTONE, ModEffects.LONG_CRAFTY_SHULKER_POTION);
            containers(ModEffects.CRAFTY_SHULKER_POTION, ModEffects.LONG_CRAFTY_SHULKER_POTION);
        }
        if (ModEffects.SHIVERING_POTION != null) {
            // Queue N20/N24, Zutaten mit Begruendung: docs/ai/PLAN-BRAUEN-WERKBANK-2026-10-09.md.
            mix(Potions.AWKWARD, ModItems.WARDEN_TENDRIL, ModEffects.DARKNESS_POTION);
            mix(ModEffects.DARKNESS_POTION, Items.REDSTONE, ModEffects.LONG_DARKNESS_POTION);
            mix(Potions.AWKWARD, Items.RED_MUSHROOM, ModEffects.NAUSEA_POTION);
            mix(ModEffects.NAUSEA_POTION, Items.REDSTONE, ModEffects.LONG_NAUSEA_POTION);
            mix(Potions.AWKWARD, Items.SNOWBALL, ModEffects.SHIVERING_POTION);
            mix(ModEffects.SHIVERING_POTION, Items.REDSTONE, ModEffects.LONG_SHIVERING_POTION);
            mix(ModEffects.SHIVERING_POTION, Items.GLOWSTONE_DUST, ModEffects.STRONG_SHIVERING_POTION);
            mix(Potions.AWKWARD, Items.AMETHYST_SHARD, ModEffects.MIRAGE_POTION);
            mix(ModEffects.MIRAGE_POTION, Items.REDSTONE, ModEffects.LONG_MIRAGE_POTION);
            // Wie Vanilla: das fermentierte Spinnenauge kehrt um (auch die verlaengerte Fassung).
            mix(ModEffects.MIRAGE_POTION, Items.FERMENTED_SPIDER_EYE, ModEffects.REVERSE_MIRAGE_POTION);
            mix(ModEffects.LONG_MIRAGE_POTION, Items.FERMENTED_SPIDER_EYE, ModEffects.LONG_REVERSE_MIRAGE_POTION);
            mix(ModEffects.REVERSE_MIRAGE_POTION, Items.REDSTONE, ModEffects.LONG_REVERSE_MIRAGE_POTION);
            mix(Potions.AWKWARD, Items.INK_SAC, ModEffects.FADED_POTION);
            mix(ModEffects.FADED_POTION, Items.REDSTONE, ModEffects.LONG_FADED_POTION);
            containers(ModEffects.DARKNESS_POTION, ModEffects.LONG_DARKNESS_POTION, ModEffects.NAUSEA_POTION,
                    ModEffects.LONG_NAUSEA_POTION, ModEffects.SHIVERING_POTION, ModEffects.LONG_SHIVERING_POTION,
                    ModEffects.STRONG_SHIVERING_POTION, ModEffects.MIRAGE_POTION, ModEffects.LONG_MIRAGE_POTION,
                    ModEffects.REVERSE_MIRAGE_POTION, ModEffects.LONG_REVERSE_MIRAGE_POTION, ModEffects.FADED_POTION,
                    ModEffects.LONG_FADED_POTION);
        }
    }

    /** Schwarzpulver macht den Wurftrank, Drachenatem aus dem Wurftrank den Verweiltrank. */
    @SafeVarargs
    private void containers(Holder<Potion>... potions) {
        for (Holder<Potion> potion : potions) {
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
