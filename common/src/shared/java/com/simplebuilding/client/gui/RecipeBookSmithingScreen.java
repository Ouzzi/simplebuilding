package com.simplebuilding.client.gui;

import com.simplebuilding.client.gui.recipebook.SmithingBookMenu;
import com.simplebuilding.client.gui.recipebook.ThreeSlotRecipeBookComponent;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.display.RecipeDisplay;

/**
 * Vanillas Schmiedebildschirm mit Vanillas Rezeptbuch, wie an der Werkbank: das Buch-Symbol sitzt ueber dem dritten Slot
 * (Material), oeffnet links die bekannten Schmiede-Rezepte (Kategorie {@code smithing}), ein Klick legt Vorlage, Basis und
 * Material ein. Die Logik ist {@code AbstractRecipeBookScreen} nachgebaut, weil der Schmiedebildschirm nicht davon erbt;
 * eingesetzt wird der Bildschirm von {@code GuiSetScreenMixin}.
 */
public class RecipeBookSmithingScreen extends com.simplebuilding.version.RecipeBookSmithingScreenBase implements RecipeUpdateListener {
    /** Ueber dem Material-Slot (x 44): zwischen Titel (y 15) und Slot-Reihe (y 48). */
    private static final int BUTTON_X = 42;
    private static final int BUTTON_Y = 27;

    private final RecipeBookComponent<SmithingBookMenu> recipeBook;
    private boolean widthTooNarrow;

    public RecipeBookSmithingScreen(SmithingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.recipeBook = new ThreeSlotRecipeBookComponent<>(new SmithingBookMenu(menu, inventory),
                List.of(new RecipeBookComponent.TabInfo(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, RecipeBookCategories.SMITHING)),
                () -> List.of(menu.getSlot(0), menu.getSlot(1), menu.getSlot(2)),
                () -> menu.getSlot(menu.getResultSlot()));
    }

    @Override
    protected void init() {
        super.init();
        this.widthTooNarrow = this.width < 379;
        this.recipeBook.init(this.width, this.height, this.minecraft, this.widthTooNarrow);
        this.leftPos = this.recipeBook.updateScreenPosition(this.width, this.imageWidth);
        this.addRenderableWidget(new ImageButton(this.leftPos + BUTTON_X, this.topPos + BUTTON_Y, 20, 18,
                RecipeBookComponent.RECIPE_BUTTON_SPRITES, button -> {
                    this.recipeBook.toggleVisibility();
                    this.leftPos = this.recipeBook.updateScreenPosition(this.width, this.imageWidth);
                    button.setPosition(this.leftPos + BUTTON_X, this.topPos + BUTTON_Y);
                }));
        this.addWidget(this.recipeBook);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (this.recipeBook.isVisible() && this.widthTooNarrow) {
            this.extractBackground(graphics, mouseX, mouseY, a);
        } else {
            super.extractRenderState(graphics, mouseX, mouseY, a);
        }
        graphics.nextStratum();
        this.recipeBook.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.nextStratum();
        this.recipeBook.extractTooltip(graphics, mouseX, mouseY, this.hoveredSlot);
    }

    @Override
    protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractSlots(graphics, mouseX, mouseY);
        this.recipeBook.extractGhostRecipe(graphics, false);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return this.recipeBook.charTyped(event) || super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return this.recipeBook.keyPressed(event) || super.keyPressed(event);
    }

    @Override
    protected RecipeBookComponent<?> recipeBook() {
        return this.recipeBook;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.recipeBook.mouseClicked(event, doubleClick)) {
            this.setFocused(this.recipeBook);
            return true;
        }
        return this.widthTooNarrow && this.recipeBook.isVisible() || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return this.recipeBook.mouseDragged(event, dx, dy) || super.mouseDragged(event, dx, dy);
    }

    @Override
    protected boolean isHovering(int left, int top, int w, int h, double xm, double ym) {
        return (!this.widthTooNarrow || !this.recipeBook.isVisible()) && super.isHovering(left, top, w, h, xm, ym);
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        boolean clickedOutside = mx < xo || my < yo || mx >= xo + this.imageWidth || my >= yo + this.imageHeight;
        return this.recipeBook.hasClickedOutside(mx, my, this.leftPos, this.topPos, this.imageWidth, this.imageHeight) && clickedOutside;
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput) {
        super.slotClicked(slot, slotId, buttonNum, containerInput);
        this.recipeBook.slotClicked(slot);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.recipeBook.tick();
    }

    @Override
    public void recipesUpdated() {
        this.recipeBook.recipesUpdated();
    }

    @Override
    public void fillGhostRecipe(RecipeDisplay display) {
        this.recipeBook.fillGhostRecipe(display);
    }
}
