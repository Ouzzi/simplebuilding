package com.simplebuilding.client.gui;

import com.simplebuilding.client.gui.recipebook.ThreeSlotRecipeBookComponent;
import com.simplebuilding.fletching.FletchingMenu;
import com.simplebuilding.fletching.FletchingRecipes;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Bildschirm des Befiederungstischs wie die Werkbank ({@code CraftingScreen}): Vanilla-Hintergrund der Werkbank ohne die
 * sechs Felder, die der Pfeil nicht braucht; Spitze, Schaft und Befiederung in einer Reihe, das Ergebnis rechts. Das
 * Rezeptbuch-Symbol sitzt wie am Schmiedetisch ueber dem dritten Teil (x 42, y 27 zwischen Titel und Teile-Reihe) und
 * oeffnet links Vanillas Rezeptbuch mit drei Reitern (Spitze, Schaft, Befiederung); ein Klick legt das Teil ein.
 */
public class FletchingScreen extends AbstractRecipeBookScreen<FletchingMenu> {
    private static final Identifier CRAFTING_TABLE_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/crafting_table.png");
    /** Hintergrundfarbe der Vanilla-Container; uebermalt die ungenutzten Gitterfelder. */
    private static final int PANEL = 0xFFC6C6C6;

    public FletchingScreen(FletchingMenu menu, Inventory inventory, Component title) {
        super(menu, component(menu), inventory, title);
    }

    private static RecipeBookComponent<FletchingMenu> component(FletchingMenu menu) {
        // N16 (Besitzer): drei Kategorien - Spitze, Stab, Feder; je Reiter nur die Teile zum Zusammenstellen.
        List<RecipeBookComponent.TabInfo> tabs = List.of(
                new RecipeBookComponent.TabInfo(new ItemStack(Items.FLINT), Optional.empty(), FletchingRecipes.TIP_CATEGORY),
                new RecipeBookComponent.TabInfo(new ItemStack(Items.STICK), Optional.empty(), FletchingRecipes.SHAFT_CATEGORY),
                new RecipeBookComponent.TabInfo(new ItemStack(Items.FEATHER), Optional.empty(), FletchingRecipes.FLETCHING_CATEGORY));
        return new ThreeSlotRecipeBookComponent<>(menu, tabs,
                () -> List.of(menu.getSlot(FletchingMenu.TIP_SLOT), menu.getSlot(FletchingMenu.SHAFT_SLOT), menu.getSlot(FletchingMenu.FLETCHING_SLOT)),
                () -> menu.getSlot(FletchingMenu.RESULT_SLOT));
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = ModScreenStyle.ACTIVE ? 8 : 29;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!ModScreenStyle.fletchingLabels(graphics, this.font, this.title, this.titleLabelX, this.titleLabelY)) {
            super.extractLabels(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected ScreenPosition getRecipeBookButtonPosition() {
        return new ScreenPosition(this.leftPos + ModScreenLayout.FLETCHING_BOOK_X, this.topPos + ModScreenLayout.FLETCHING_BOOK_Y);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = this.leftPos;
        int yo = (this.height - this.imageHeight) / 2;
        if (ModScreenStyle.fletching(graphics, this.menu, this.font, this.title, xo, yo, this.imageWidth)) {
            return;
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, CRAFTING_TABLE_LOCATION, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        // Gitter 3x3 ab (29,16), Felder je 18 px: nur die mittlere Reihe bleibt.
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (row != 1) {
                    int x = xo + 29 + column * 18;
                    int y = yo + 16 + row * 18;
                    graphics.fill(x, y, x + 18, y + 18, PANEL);
                }
            }
        }
    }
}
