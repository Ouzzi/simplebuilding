package com.simplebuilding.recipe;

import com.simplebuilding.util.ModRegistries;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.core.HolderLookup;

/**
 * Ein geformtes Rezept, das einen Behaelter aufwertet und dabei alles mitnimmt, was an ihm haengt.
 *
 * <p>Benutzt von {@code recipe/reinforced_bundle.json} (Vanilla-Buendel -> verstaerktes Buendel)
 * und {@code recipe/reinforced_quiver.json} (Koecher -> verstaerkter Koecher). Ein gewoehnliches
 * {@code crafting_shaped} gibt eine Kopie seines festen Ergebnisses zurueck und wirft damit Inhalt,
 * Verzauberungen und Namen des eingelegten Behaelters weg - und das Rezeptbuch legt ohne Rueckfrage
 * auch einen gefuellten oder verzauberten Koecher ein, weil {@code Ingredient.of} keine Komponenten
 * vergleicht. Hier uebernimmt das Ergebnis deshalb den kompletten Komponenten-Patch des Behaelters,
 * genau wie der Schmiedetisch bei der Netherit-Aufwertung ({@code TransmuteResult#apply}).
 *
 * <p>Der Behaelter ist die erste Zutat im Raster, die ein {@link BundleItem} ist - der
 * {@code QuiverItem} ist ueber {@code ReinforcedBundleItem} auch eines; Faden, Lederplatte,
 * Diamantkiesel und Kupfer-Nugget sind es nicht.
 */
public class ReinforcedBundleRecipe extends ShapedRecipe {
    private final ItemStack resultStack;
    private final ShapedRecipePattern rawPattern;

    public ReinforcedBundleRecipe(String group, CraftingBookCategory category, ShapedRecipePattern raw, ItemStack result) {
        // MC 1.21.11: ShapedRecipe nimmt group/category/pattern/result noch einzeln entgegen
        // (Recipe.CommonInfo und CraftingRecipe.CraftingBookInfo gibt es erst ab 26.2); der
        // 4-Argument-Konstruktor setzt showNotification=true -- identisch zu CommonInfo(true).
        super(
                group,
                category,
                raw,
                result
        );
        this.resultStack = result;
        this.rawPattern = raw;
    }

    public ItemStack getResultStack() {
        return this.resultStack;
    }

    public ShapedRecipePattern getRaw() {
        return this.rawPattern;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);

            if (stack.getItem() instanceof BundleItem) {
                // MC 1.21.11: dasselbe, was TransmuteResult#apply beim Schmieden tut (26.2:
                // TransmuteRecipe#createWithOriginalComponents) - Patch des Behaelters auf das
                // Ergebnis-Item, darueber die Komponenten des Rezept-Ergebnisses.
                ItemStack upgraded = stack.transmuteCopy(this.resultStack.getItem(), this.resultStack.getCount());
                upgraded.applyComponents(this.resultStack.getComponentsPatch());
                return upgraded;
            }
        }
        // Ohne Behaelter im Raster kann das Muster nicht gepasst haben; nur zur Sicherheit.
        return super.assemble(input, registries);
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return (RecipeSerializer<ShapedRecipe>) (RecipeSerializer<?>) ModRegistries.REINFORCED_BUNDLE_SERIALIZER;
    }
}
