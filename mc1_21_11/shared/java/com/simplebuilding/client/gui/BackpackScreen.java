package com.simplebuilding.client.gui;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.client.ClientState;
import com.simplebuilding.items.custom.BackpackTier;
import com.simplebuilding.screen.BackpackLayout;
import com.simplebuilding.screen.BackpackMenu;
import com.simplebuilding.screen.BackpackSlot;
import com.simplebuilding.util.DyedStorage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.display.RecipeDisplay;

/**
 * Der Rucksack-Bildschirm: exakt das Vanilla-Inventar (2x2-Crafting samt Rezeptbuch, Ruestung,
 * Nebenhand, Hauptinventar, Hotbar, Spielermodell, Effekte und der Besatz-Knopf der Mod) plus die
 * Rucksack-Slots. Rucksack-Reihen sind leicht braun getoent (ein gefaerbter Rucksack: leicht in
 * seiner Farbe), Zusatzspalten violett.
 *
 * <p>Warum nicht von {@code InventoryScreen}/{@code AbstractRecipeBookScreen} geerbt: auf 26.2
 * sind {@code imageWidth}/{@code imageHeight} final und nur ueber den 5-Argument-Konstruktor von
 * {@code AbstractContainerScreen} zu setzen, den {@code AbstractRecipeBookScreen} nicht anbietet.
 * Die 1.21.11-Fassung folgt demselben Aufbau, damit beide Linien gleich bleiben (hier werden die
 * Felder im Konstruktor gesetzt, und die Zeichenmethoden heissen render, renderSlots und renderBg statt extract...).
 * Die Rezeptbuch-Anbindung ist deshalb hier nachgebaut - mit einer Abweichung: bei offenem Buch
 * wird der Bildschirm an Vanillas Position fuer ein 176 Pixel breites Inventar ausgerichtet
 * (nicht zentriert auf die volle Breite), damit Zusatzspalten nicht unter dem Buch landen.
 *
 * <p>Der Hintergrund ist ein Fenster aus einem Guss je Stufe ({@code textures/gui/container/backpack/},
 * erzeugt von {@code tools/textures/generate_textures.py}): Korpus und Laschen der Zusatzspalten
 * sind eine Flaeche mit Vanillas Rand und abgerundeten Ecken, ohne Naht dazwischen. Nur der obere
 * Bereich (Ruestung, Spielerbild, 2x2-Raster, Nebenhand) kommt weiter aus Vanillas
 * {@code inventory.png}, damit Ressourcenpakete dort greifen. Rucksack-Reihen und Zusatzspalten
 * tragen nur eine leichte Toenung ueber dem Slot.
 */
public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> implements RecipeUpdateListener {
    /** Kaum sichtbare braune Toenung der Rucksack-Reihen. */
    public static final int TINT_BACKPACK_ROW = 0x1CA0602A;
    /** Leichte violette Toenung der Zusatzspalten (werden spaeter Spezial-Slots). */
    public static final int TINT_EXTRA_COLUMN = 0x306A3FC8;
    /** Die Fenster der vier Stufen, in der Reihenfolge von {@link BackpackTier}. */
    private static final Identifier[] BACKGROUNDS = {background("basic"), background("reinforced"),
            background("netherite"), background("enderite")};
    /** Innenbereich des oberen Vanilla-Teils in {@code inventory.png}, ohne dessen Rand. */
    private static final int TOP_INNER_X = 4;
    private static final int TOP_INNER_Y = 4;
    private static final int TOP_INNER_WIDTH = 168;
    private static final int TOP_INNER_HEIGHT = 79;
    /** Vanillas Breitenschwelle fuer das Rezeptbuch neben dem Inventar. */
    private static final int VANILLA_RECIPE_BOOK_MIN_WIDTH = 379;

    private final CraftingRecipeBookComponent recipeBookComponent;
    private final EffectsInInventory effects;
    private final BackpackLayout layout;
    private final TrimStatsPanel trimStats = new TrimStatsPanel();
    private boolean widthTooNarrow;
    private boolean buttonClicked;
    private float xMouse;
    private float yMouse;

    public BackpackScreen(BackpackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.translatable("container.crafting"));
        this.layout = menu.layout();
        // MC 1.21.11: imageWidth/imageHeight sind noch nicht final und werden hier gesetzt.
        this.imageWidth = this.layout.imageWidth();
        this.imageHeight = this.layout.imageHeight();
        this.recipeBookComponent = new CraftingRecipeBookComponent(menu);
        this.titleLabelX = this.layout.vanillaX() + 97;
        this.effects = new EffectsInInventory(this);
    }

    @Override
    protected void init() {
        super.init();
        // Wie Vanilla (379) - oder breiter, wenn das Bild samt Zusatzspalten rechts vom Buch
        // sonst nicht mehr auf den Schirm passt.
        this.widthTooNarrow = this.width < Math.max(VANILLA_RECIPE_BOOK_MIN_WIDTH, 2 * this.imageWidth - 14);
        this.recipeBookComponent.init(this.width, this.height, this.minecraft, this.widthTooNarrow);
        this.leftPos = computeLeftPos();
        this.addRenderableWidget(new ImageButton(recipeButtonX(), recipeButtonY(), 20, 18, RecipeBookComponent.RECIPE_BUTTON_SPRITES, button -> {
            this.recipeBookComponent.toggleVisibility();
            this.leftPos = computeLeftPos();
            button.setPosition(recipeButtonX(), recipeButtonY());
            this.buttonClicked = true;
        }));
        this.addWidget(this.recipeBookComponent);
        this.addRenderableWidget(this.trimStats.createButton(this.leftPos, this.topPos));
    }

    private int computeLeftPos() {
        if (this.recipeBookComponent.isVisible() && !this.widthTooNarrow) {
            // Vanillas Platz fuer ein 176er-Inventar rechts vom Buch; nie ueber den rechten Rand.
            int anchored = this.recipeBookComponent.updateScreenPosition(this.width, BackpackLayout.VANILLA_WIDTH);
            return Math.min(anchored, this.width - this.imageWidth);
        }
        return (this.width - this.imageWidth) / 2;
    }

    private int recipeButtonX() {
        return this.leftPos + this.layout.vanillaX() + 104;
    }

    private int recipeButtonY() {
        // Vanilla: height / 2 - 22 bei einem 166 hohen Bild, also 61 unter der Oberkante.
        return this.topPos + 61;
    }

    // =================================================================================
    // Zeichnen
    // =================================================================================

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        this.effects.render(graphics, mouseX, mouseY);
        if (this.recipeBookComponent.isVisible() && this.widthTooNarrow) {
            this.renderBackground(graphics, mouseX, mouseY, a);
        } else {
            super.renderContents(graphics, mouseX, mouseY, a);
        }
        graphics.nextStratum();
        this.recipeBookComponent.render(graphics, mouseX, mouseY, a);
        graphics.nextStratum();
        this.renderCarriedItem(graphics, mouseX, mouseY);
        this.renderSnapbackItem(graphics);
        this.renderTooltip(graphics, mouseX, mouseY);
        this.recipeBookComponent.renderTooltip(graphics, mouseX, mouseY, this.hoveredSlot);
        this.trimStats.render(graphics, this.font, this.minecraft, this.leftPos, this.topPos, mouseX, mouseY);
        this.xMouse = mouseX;
        this.yMouse = mouseY;
    }

    @Override
    protected void renderSlots(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderSlots(graphics, mouseX, mouseY);
        this.recipeBookComponent.renderGhostRecipe(graphics, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int xm, int ym) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float a, int mouseX, int mouseY) {
        BackpackTier tier = this.menu.tier();
        int x = this.leftPos + this.layout.vanillaX();
        int y = this.topPos;

        // Das ganze Fenster in einem Stueck, dann der obere Vanilla-Bereich ohne dessen Rand.
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUNDS[tier.ordinal()], this.leftPos, this.topPos, 0, 0,
                this.imageWidth, this.imageHeight, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, INVENTORY_LOCATION, x + TOP_INNER_X, y + TOP_INNER_Y, TOP_INNER_X, TOP_INNER_Y,
                TOP_INNER_WIDTH, TOP_INNER_HEIGHT, 256, 256);

        // Toenung unter den Items (dieser Durchgang liegt vor den Slots). Ein gefaerbter Rucksack
        // toent seine Reihen leicht in seiner Farbe statt braun; die Zusatzspalten bleiben violett.
        int rowTint = rowTint(this.menu.openData().dyeColor());
        for (Slot slot : this.menu.slots) {
            if (slot instanceof BackpackSlot backpackSlot) {
                int color = backpackSlot.isExtraColumn() ? TINT_EXTRA_COLUMN : rowTint;
                int sx = this.leftPos + slot.x;
                int sy = this.topPos + slot.y;
                graphics.fill(sx, sy, sx + 16, sy + 16, color);
            }
        }

        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, x + 26, y + 8, x + 75, y + 78, 30, 0.0625F,
                this.xMouse, this.yMouse, this.minecraft.player);
    }

    /** Toenung der Rucksack-Reihen: braun ohne Farbstoff, sonst die Farbe mit geringer Deckkraft. */
    public static int rowTint(int dyeColor) {
        return DyedStorage.slotTint(dyeColor, TINT_BACKPACK_ROW);
    }

    private static Identifier background(String tier) {
        return Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "textures/gui/container/backpack/" + tier + ".png");
    }

    @Override
    public boolean showsActiveEffects() {
        return this.effects.canSeeEffects();
    }

    // =================================================================================
    // Eingaben (Rezeptbuch zuerst, wie AbstractRecipeBookScreen)
    // =================================================================================

    @Override
    public boolean charTyped(CharacterEvent event) {
        return this.recipeBookComponent.charTyped(event) || super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.recipeBookComponent.keyPressed(event)) {
            return true;
        }
        // Die Rucksack-Taste schliesst den Bildschirm wieder, so wie E das Inventar schliesst.
        if (ClientState.backpackKey != null && ClientState.backpackKey.matches(event)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.recipeBookComponent.mouseClicked(event, doubleClick)) {
            this.setFocused(this.recipeBookComponent);
            return true;
        }
        return this.widthTooNarrow && this.recipeBookComponent.isVisible() || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.buttonClicked) {
            this.buttonClicked = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return this.recipeBookComponent.mouseDragged(event, dx, dy) || super.mouseDragged(event, dx, dy);
    }

    @Override
    protected boolean isHovering(int left, int top, int w, int h, double xm, double ym) {
        return (!this.widthTooNarrow || !this.recipeBookComponent.isVisible()) && super.isHovering(left, top, w, h, xm, ym);
    }

    /**
     * Ausserhalb geklickt (wirft den Cursor-Stapel), wenn weder der Vanilla-Teil noch eine Lasche
     * getroffen wurde - die leeren Ecken ueber und unter den Laschen zaehlen als draussen.
     */
    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        boolean outside = !isInsideImage(mx, my, xo, yo);
        return this.recipeBookComponent.hasClickedOutside(mx, my, this.leftPos, this.topPos, this.imageWidth, this.imageHeight) && outside;
    }

    private boolean isInsideImage(double mx, double my, int xo, int yo) {
        double rx = mx - xo;
        double ry = my - yo;
        if (ry < 0 || ry >= this.imageHeight || rx < 0 || rx >= this.imageWidth) {
            return false;
        }
        int vx = this.layout.vanillaX();
        if (rx >= vx && rx < vx + BackpackLayout.VANILLA_WIDTH) {
            return true;
        }
        int tabTop = BackpackLayout.FIRST_ROW_Y - 8;
        int tabBottom = BackpackLayout.FIRST_ROW_Y + BackpackLayout.SLOT * this.menu.tier().columnHeight() + 6;
        return ry >= tabTop && ry < tabBottom;
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int buttonNum, ClickType containerInput) {
        super.slotClicked(slot, slotId, buttonNum, containerInput);
        this.recipeBookComponent.slotClicked(slot);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.recipeBookComponent.tick();
    }

    @Override
    public void recipesUpdated() {
        this.recipeBookComponent.recipesUpdated();
    }

    @Override
    public void fillGhostRecipe(RecipeDisplay display) {
        this.recipeBookComponent.fillGhostRecipe(display);
    }
}
