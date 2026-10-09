package com.simplebuilding.client.gui;

import com.simplebuilding.client.gui.recipebook.ThreeSlotRecipeBookComponent;
import com.simplebuilding.fletching.FletchingMenu;
import com.simplebuilding.fletching.FletchingRecipes;
import com.simplebuilding.items.ModItems;
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
 * Rezeptbuch-Symbol sitzt exakt wie bei der Werkbank und oeffnet links Vanillas Rezeptbuch mit allen Pfeilen; ein Klick
 * legt die Teile ein.
 */
public class FletchingScreen extends AbstractRecipeBookScreen<FletchingMenu> {
    private static final Identifier CRAFTING_TABLE_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/crafting_table.png");
    /** Hintergrundfarbe der Vanilla-Container; uebermalt die ungenutzten Gitterfelder. */
    private static final int PANEL = 0xFFC6C6C6;

    public FletchingScreen(FletchingMenu menu, Inventory inventory, Component title) {
        super(menu, component(menu), inventory, title);
    }

    private static RecipeBookComponent<FletchingMenu> component(FletchingMenu menu) {
        ItemStack icon = ModItems.CRAFTED_ARROW != null ? new ItemStack(ModItems.CRAFTED_ARROW) : new ItemStack(Items.ARROW);
        List<RecipeBookComponent.TabInfo> tabs = List.of(new RecipeBookComponent.TabInfo(icon, Optional.empty(), FletchingRecipes.CATEGORY));
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
        return new ScreenPosition(this.leftPos + 5, this.height / 2 - 49);
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
