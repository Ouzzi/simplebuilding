package com.simplestorage.client;

import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiMotifs;
import com.simplelib.api.client.ui.UiPalette;
import com.simplelib.api.client.ui.UiStyleToggle;
import com.simplelib.api.client.ui.UiSymbols;
import com.simplestorage.SimpleStorage;
import com.simplestorage.backpack.BackpackTier;
import com.simplestorage.backpack.BackpackLayout;
import com.simplestorage.backpack.BackpackMenu;
import com.simplestorage.backpack.BackpackSlot;
import com.simplestorage.bundle.DyedStorage;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.crafting.display.RecipeDisplay;

/**
 * Der Rucksack-Bildschirm (split stage 8c-2, from SimpleBuilding's {@code BackpackScreen}; the box style of
 * SimpleBuilding's {@code ModScreenStyle#backpack} is drawn here with SimpleLib's UI parts, SimpleBuilding's extra head slot
 * frame and resonance button come through SimpleLib {@code InventoryExtrasClient}): exakt das Vanilla-Inventar (2x2-Crafting samt Rezeptbuch, Ruestung,
 * Nebenhand, Hauptinventar, Hotbar, Spielermodell, Effekte und der Besatz-Knopf der Mod) plus die
 * Rucksack-Slots. Rucksack-Reihen sind leicht braun getoent (ein gefaerbter Rucksack: leicht in
 * seiner Farbe), Zusatzspalten violett.
 *
 * <p>Warum nicht von {@code InventoryScreen}/{@code AbstractRecipeBookScreen} geerbt: auf 26.2
 * sind {@code imageWidth}/{@code imageHeight} final und nur ueber den 5-Argument-Konstruktor von
 * {@code AbstractContainerScreen} zu setzen, den {@code AbstractRecipeBookScreen} nicht anbietet.
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
    private boolean widthTooNarrow;
    private boolean buttonClicked;
    private float xMouse;
    private float yMouse;

    public BackpackScreen(BackpackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.translatable("container.crafting"),
                menu.layout().imageWidth(), menu.layout().imageHeight());
        this.layout = menu.layout();
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.effects.extractRenderState(graphics, mouseX, mouseY);
        if (this.recipeBookComponent.isVisible() && this.widthTooNarrow) {
            this.extractBackground(graphics, mouseX, mouseY, a);
        } else {
            super.extractContents(graphics, mouseX, mouseY, a);
        }
        graphics.nextStratum();
        this.recipeBookComponent.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.nextStratum();
        this.extractCarriedItem(graphics, mouseX, mouseY);
        this.extractTooltip(graphics, mouseX, mouseY);
        this.recipeBookComponent.extractTooltip(graphics, mouseX, mouseY, this.hoveredSlot);
        // Extra inventory parts (SimpleBuilding: resonance button between the extra head slot and the off-hand slot, as in the inventory)
        com.simplelib.api.client.InventoryExtrasClient.foreground(graphics, this.font, this.minecraft, this.leftPos + this.layout.vanillaX(),
                this.topPos, mouseX, mouseY, this.hoveredSlot, this.menu.getCarried());
        this.xMouse = mouseX;
        this.yMouse = mouseY;
    }

    @Override
    protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractSlots(graphics, mouseX, mouseY);
        this.recipeBookComponent.extractGhostRecipe(graphics, false);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        if (UiStyleToggle.isEnabled()) {
            return; // Kasten-Stil: Symbole statt Text (Vorschau rucksack-*), auch kein "Handwerk"
        }
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        BackpackTier tier = this.menu.tier();
        int x = this.leftPos + this.layout.vanillaX();
        int y = this.topPos;

        // Ein Kasten im Kasten-Stil (SimpleLib); sonst das ganze Fenster in einem Stueck, dann der
        // obere Vanilla-Bereich ohne dessen Rand und die Toenungen.
        if (styled(graphics, this.menu, this.layout, this.leftPos, this.topPos)) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, x + 26, y + 8, x + 75, y + 78, 30, 0.0625F,
                    this.xMouse, this.yMouse, this.minecraft.player);
            return;
        }
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

        com.simplelib.api.client.InventoryExtrasClient.background(graphics, x, y);
        InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, x + 26, y + 8, x + 75, y + 78, 30, 0.0625F,
                this.xMouse, this.yMouse, this.minecraft.player);
    }

    // ------------------------------------------------------------------ box style (from SimpleBuilding's ModScreenStyle)

    /** Frame of a box at the top and the sides, and at the bottom (with the 2 px shadow); SimpleBuilding's ModScreenLayout. */
    static final int FRAME = 5, FRAME_BOTTOM = 7;
    /** Row of the seam in the backpack's one box (the crafting part above, backpack rows from 84). */
    static final int SEAM_Y = BackpackLayout.FIRST_ROW_Y - 3;
    static final UiPalette LEATHER = UiPalette.derived(0xFF8E6440);

    /** Backpack colours: a dyed backpack in its dye (muted like a shulker box), Enderite violet, else leather. */
    public static UiPalette palette(BackpackTier tier, int dye) {
        if (dye != DyedStorage.UNDYED) return UiPalette.derived(UiPalette.mix(0xFF000000 | dye, 0xFF808080, 0.4));
        return tier == BackpackTier.ENDERITE ? UiPalette.ENDERITE : LEATHER;
    }

    /** Slot colour of the styled backpack's own slots. */
    public static int slotColor(BackpackMenu menu) {
        return palette(menu.tier(), menu.openData().dyeColor()).slot();
    }

    /**
     * The backpack in one box (only 5 free rows above the backpack rows): armour, player preview, offhand and 2x2 crafting
     * on the block colour, the backpack rows and the extra columns on a tinted strip of the light inventory panel behind a seam.
     */
    static boolean styled(GuiGraphicsExtractor g, BackpackMenu menu, BackpackLayout layout, int left, int top) {
        if (!UiStyleToggle.isEnabled()) return false;
        UiPalette p = palette(menu.tier(), menu.openData().dyeColor());
        UiMotifs.Kind kind = menu.openData().dyeColor() == DyedStorage.UNDYED && menu.tier() == BackpackTier.ENDERITE
                ? UiMotifs.Kind.ENDER : UiMotifs.Kind.LEATHER;
        int width = layout.imageWidth(), height = layout.imageHeight();
        int vx = layout.vanillaX();
        UiBoxes.box(g, left, top, width, height, p);
        List<int[]> avoid = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (slot.isActive() && slot.y < BackpackLayout.FIRST_ROW_Y) avoid.add(new int[] {left + slot.x, top + slot.y, left + slot.x + 17, top + slot.y + 17});
        }
        avoid.add(new int[] {left + vx + 26, top + 8, left + vx + 76, top + 78});
        avoid.add(new int[] {left + vx + 135, top + 29, left + vx + 151, top + 40});
        UiMotifs.draw(g, 0, 0, kind, left + FRAME, top + FRAME, width - 2 * FRAME, SEAM_Y - FRAME, p, avoid, 7);
        UiBoxes.seam(g, left + FRAME, top + SEAM_Y, width - 2 * FRAME, height - FRAME_BOTTOM - SEAM_Y, p);
        UiBoxes.inset(g, left + vx + 26, top + 8, 49, 70, p);
        UiSymbols.engrave(g, UiSymbols.ARROW_SMALL, left + vx + 135, top + 29, p);
        int tint = UiPalette.mix(UiPalette.INVENTORY.fill(), p.fill(), 0.28);
        int rows = layout.tier().rows();
        if (rows > 0) g.fill(left + vx + 6, top + BackpackLayout.FIRST_ROW_Y - 2, left + vx + 170,
                top + BackpackLayout.FIRST_ROW_Y + rows * BackpackLayout.SLOT, tint);
        for (Slot slot : menu.slots) {
            if (slot instanceof BackpackSlot b && b.isExtraColumn()) {
                g.fill(left + slot.x - 1, top + slot.y - 1, left + slot.x + 17, top + slot.y + 17, tint);
            }
        }
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) continue;
            boolean own = slot instanceof BackpackSlot || slot.y < BackpackLayout.FIRST_ROW_Y;
            UiBoxes.slot(g, left + slot.x, top + slot.y, own ? p : UiPalette.INVENTORY);
        }
        return true;
    }

    /** Toenung der Rucksack-Reihen: braun ohne Farbstoff, sonst die Farbe mit geringer Deckkraft. */
    public static int rowTint(int dyeColor) {
        return DyedStorage.slotTint(dyeColor, TINT_BACKPACK_ROW);
    }

    private static Identifier background(String tier) {
        return SimpleStorage.id("textures/gui/container/backpack/" + tier + ".png");
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
        if (BackpackKeyHandler.KEY != null && BackpackKeyHandler.KEY.matches(event)) {
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
    protected void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput) {
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
