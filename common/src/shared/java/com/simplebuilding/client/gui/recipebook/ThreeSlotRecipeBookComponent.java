package com.simplebuilding.client.gui.recipebook;

import com.simplebuilding.mixin.client.GhostSlotsInvoker;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.recipebook.GhostSlots;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;

/**
 * Vanilla-Rezeptbuch fuer Tische mit drei Eingaben und einem Ergebnis (Befiederungstisch, Schmiedetisch). Alles
 * Uebrige - Seiten, Suche, Filter „nur herstellbare“, Klick legt ein, Geisterrezept - ist Vanillas
 * {@link RecipeBookComponent}. Rezepte erscheinen hier, wenn ihre Anzeige ein {@link SmithingRecipeDisplay} ist
 * (Vorlage/Spitze, Basis/Schaft, Material/Befiederung).
 */
public class ThreeSlotRecipeBookComponent<T extends RecipeBookMenu> extends RecipeBookComponent<T> {
    private static final WidgetSprites FILTER_BUTTON_SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("recipe_book/filter_enabled"),
            Identifier.withDefaultNamespace("recipe_book/filter_disabled"),
            Identifier.withDefaultNamespace("recipe_book/filter_enabled_highlighted"),
            Identifier.withDefaultNamespace("recipe_book/filter_disabled_highlighted"));
    private static final Component ONLY_CRAFTABLES_TOOLTIP = Component.translatable("gui.recipebook.toggleRecipes.craftable");

    /** Die drei Eingabe-Slots in Rezeptreihenfolge und der Ergebnis-Slot (aus dem echten Menue). */
    private final Supplier<List<Slot>> inputs;
    private final Supplier<Slot> result;

    public ThreeSlotRecipeBookComponent(T menu, List<TabInfo> tabs, Supplier<List<Slot>> inputs, Supplier<Slot> result) {
        super(menu, tabs);
        this.inputs = inputs;
        this.result = result;
    }

    @Override
    protected WidgetSprites getFilterButtonTextures() {
        return FILTER_BUTTON_SPRITES;
    }

    @Override
    protected boolean isCraftingSlot(Slot slot) {
        return slot == this.result.get() || this.inputs.get().contains(slot);
    }

    @Override
    protected void selectMatchingRecipes(RecipeCollection collection, StackedItemContents stackedContents) {
        collection.selectRecipes(stackedContents, display -> display instanceof SmithingRecipeDisplay);
    }

    @Override
    protected Component getRecipeFilterName() {
        return ONLY_CRAFTABLES_TOOLTIP;
    }

    @Override
    protected void fillGhostRecipe(GhostSlots ghostSlots, RecipeDisplay recipe, ContextMap context) {
        if (!(recipe instanceof SmithingRecipeDisplay display)) {
            return;
        }
        GhostSlotsInvoker ghosts = (GhostSlotsInvoker) ghostSlots;
        List<Slot> slots = this.inputs.get();
        ghosts.simplebuilding$setResult(this.result.get(), context, display.result());
        ghosts.simplebuilding$setInput(slots.get(0), context, display.template());
        ghosts.simplebuilding$setInput(slots.get(1), context, display.base());
        ghosts.simplebuilding$setInput(slots.get(2), context, display.addition());
    }
}
