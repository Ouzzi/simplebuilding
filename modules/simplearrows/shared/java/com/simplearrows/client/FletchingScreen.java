package com.simplearrows.client;

import com.simplearrows.fletching.FletchingMenu;
import com.simplearrows.fletching.FletchingRecipes;
import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiMotifs;
import com.simplelib.api.client.ui.UiPalette;
import com.simplelib.api.client.ui.UiSlotIcons;
import com.simplelib.api.client.ui.UiStyleToggle;
import com.simplelib.api.client.ui.UiSymbols;
import java.util.ArrayList;
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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Bildschirm des Befiederungstischs wie die Werkbank ({@code CraftingScreen}): Vanilla-Hintergrund der Werkbank ohne die
 * sechs Felder, die der Pfeil nicht braucht; Spitze, Schaft und Befiederung in einer Reihe, das Ergebnis rechts. Das
 * Rezeptbuch-Symbol sitzt links der Teile-Reihe ({@link FletchingMenu#BOOK_X}, {@link FletchingMenu#BOOK_Y}) und oeffnet links
 * Vanillas Rezeptbuch mit drei Reitern (Spitze, Schaft, Befiederung); ein Klick legt das Teil ein. Ueber dem Pfeil liegt der
 * Trank-Slot (WE25-9).
 *
 * <p>Mit SimpleLibs Kastenstil ({@link UiStyleToggle}) zeichnet er Holzkasten, Inventar-Kasten, Pfeil-Logo (WD25-12) und die
 * Gravur (VIS24-9, Variante A) - seit b-arrows03 hier statt in SimpleBuildings {@code ModScreenStyle}, gleiche Masse und Farben.
 */
public class FletchingScreen extends AbstractRecipeBookScreen<FletchingMenu> {
    private static final Identifier CRAFTING_TABLE_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/crafting_table.png");
    /** Hintergrundfarbe der Vanilla-Container; uebermalt das Werkbank-Gitter vor dem Neusetzen der Teile. */
    private static final int PANEL = 0xFFC6C6C6;
    static final UiPalette STYLE = UiPalette.derived(0xFFC5B485);
    /** Box frames (top/sides, bottom with shadow), divider and inventory box (Vanilla's 14 free rows, 176 wide). */
    private static final int FRAME = UiBoxes.FRAME, FRAME_BOTTOM = UiBoxes.FRAME_BOTTOM, GAP = 2, SIDE = 8;

    static {
        // N37: empty-slot icons in the table's slot colour (player slots light).
        UiSlotIcons.register((screen, slot) -> screen.getMenu() instanceof FletchingMenu
                ? (slot.container instanceof Inventory ? UiPalette.INVENTORY : STYLE) : null);
    }

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
        this.titleLabelX = UiStyleToggle.isEnabled() ? 8 : 29;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (UiStyleToggle.isEnabled()) {
            graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, STYLE.label(), false);
            return;
        }
        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    protected ScreenPosition getRecipeBookButtonPosition() {
        return new ScreenPosition(this.leftPos + FletchingMenu.BOOK_X, this.topPos + FletchingMenu.BOOK_Y);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = this.leftPos;
        int yo = (this.height - this.imageHeight) / 2;
        if (UiStyleToggle.isEnabled()) {
            styled(graphics, xo, yo);
            return;
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, CRAFTING_TABLE_LOCATION, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        // Werkbank-Teile an der mittigen Reihe (WD25-12): Feld oben leeren, dann mittlere Gitterreihe (29,34 54x18),
        // Pfeil (90,35 22x15) und Ergebnisrahmen (119,30 26x26) aus dem Vanilla-Bild an die Slots setzen.
        graphics.fill(xo + 7, yo + 15, xo + 169, yo + 72, PANEL);
        int px = FletchingMenu.PART_X, py = FletchingMenu.ROW_Y, rx = FletchingMenu.RESULT_X;
        graphics.blit(RenderPipelines.GUI_TEXTURED, CRAFTING_TABLE_LOCATION, xo + px - 1, yo + py - 1, 29.0F, 34.0F, 54, 18, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CRAFTING_TABLE_LOCATION, xo + px + 60, yo + py, 90.0F, 35.0F, 22, 15, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CRAFTING_TABLE_LOCATION, xo + rx - 5, yo + py - 5, 119.0F, 30.0F, 26, 26, 256, 256);
        // WE25-9: the potion slot above the arrow, one cell of the Vanilla grid.
        graphics.blit(RenderPipelines.GUI_TEXTURED, CRAFTING_TABLE_LOCATION, xo + FletchingMenu.POTION_X - 1, yo + FletchingMenu.POTION_Y - 1,
                29.0F, 16.0F, 18, 18, 256, 256);
    }

    /** The container style: wooden box, light inventory box, slot groove, slots, the arrow logo and the engraving. */
    private void styled(GuiGraphicsExtractor g, int left, int top) {
        int[] inv = FletchingMenu.inventoryOrigin(this.menu);
        int boxH = inv[1] - FRAME - GAP;
        UiBoxes.box(g, left, top, this.imageWidth, boxH, STYLE);
        UiBoxes.box(g, left + inv[0] - SIDE, top + inv[1] - FRAME, 176, FRAME + 58 + 17 + FRAME_BOTTOM, UiPalette.INVENTORY);
        Slot result = this.menu.getSlot(FletchingMenu.RESULT_SLOT);
        // WD25-12: the row (book, feather, shaft, tip, arrow, result) centred in the box; positions from the menu
        int px = FletchingMenu.PART_X, py = FletchingMenu.ROW_Y, ax = px + ARROW_DX, ay = py + 1;
        List<int[]> avoid = new ArrayList<>();
        for (Slot slot : this.menu.slots) {
            if (slot.isActive() && !(slot.container instanceof Inventory)) {
                avoid.add(new int[] {left + slot.x, top + slot.y, left + slot.x + 17, top + slot.y + 17});
            }
        }
        avoid.add(new int[] {left + 4, top + 4, left + 12 + this.font.width(this.title), top + 15});
        avoid.add(new int[] {left + result.x - 4, top + result.y - 4, left + result.x + 21, top + result.y + 21});
        avoid.add(new int[] {left + ax, top + py - 4, left + ax + 22, top + py + 21});
        avoid.add(new int[] {left + px - 26, top + py - 4, left + px + 55, top + py + 21});
        // VIS24-9 (FLETCH=gravur, variant A): the engraved fletched arrow in the free space above the part row.
        avoid.add(new int[] {left + px + GRAVUR_DX - 2, top + GRAVUR_Y - 2, left + px + GRAVUR_DX + GRAVUR.width() + 2,
                top + GRAVUR_Y + GRAVUR.height() + 2});
        // The recipe book button sits left of the part row (crafting-table spot); keep the motif clear.
        avoid.add(new int[] {left + FletchingMenu.BOOK_X, top + FletchingMenu.BOOK_Y, left + FletchingMenu.BOOK_X + FletchingMenu.BOOK_W,
                top + FletchingMenu.BOOK_Y + FletchingMenu.BOOK_H});
        UiMotifs.draw(g, 0, 0, UiMotifs.Kind.WOOD, left + FRAME, top + FRAME, this.imageWidth - 2 * FRAME, boxH - FRAME - FRAME_BOTTOM,
                STYLE, avoid, 6);
        // The three part slots (feather, shaft, tip) sit in one row after the recipe book button.
        for (int k = 0; k < 54; k++) {
            int x = px + k;
            g.fill(left + x, top + py + 8, left + x + 1, top + py + 9, STYLE.light());
            g.fill(left + x, top + py + 7, left + x + 2, top + py + 8, STYLE.slot());
        }
        for (Slot slot : this.menu.slots) {
            if (!slot.isActive()) continue;
            if (slot == result) UiBoxes.bigSlot(g, left + slot.x, top + slot.y, STYLE);
            else UiBoxes.slot(g, left + slot.x, top + slot.y, slot.container instanceof Inventory ? UiPalette.INVENTORY : STYLE);
        }
        UiSymbols.engrave(g, FLETCHED_ARROW, left + ax, top + ay, STYLE);
        UiSymbols.engrave(g, GRAVUR, left + px + GRAVUR_DX, top + GRAVUR_Y, STYLE);
    }

    /**
     * VIS24-9, owner FLETCH=gravur (variant A of the b-arrows01 preview, no motif ticked = A): a fletched arrow flying up to the
     * right, engraved like the station arrows in the free space above feather, shaft and tip (left of the potion slot, WE25-9).
     */
    static final UiSymbols.Bitmap GRAVUR = UiSymbols.Bitmap.of(
            ".................####.",
            "..................###.",
            ".................#.##.",
            "...............##...#.",
            ".............##.......",
            "...........##.........",
            ".........##...........",
            ".......##.............",
            ".#.#.##...............",
            "..###.................",
            ".#.##.................",
            "#.#.#.................",
            ".#.#..................");
    /** Left edge relative to the feather slot and top of the engraving (title line above, part row below). */
    static final int GRAVUR_DX = 15, GRAVUR_Y = 17;

    /** The fletching arrow's left edge relative to the feather slot (Vanilla crafting table: arrow 62 px right of the grid). */
    static final int ARROW_DX = 62;
    /**
     * WD25-12 "Pfeil-Logo": the fletching table's arrow is a real arrow - the chunky head of {@link UiSymbols#ARROW}, a
     * 3 px shaft and two swept feathers at the tail (22x15 like the other station arrows; preview variant A).
     */
    static final UiSymbols.Bitmap FLETCHED_ARROW = UiSymbols.Bitmap.of(
            "..............#.......",
            "..............##......",
            "##............###.....",
            ".##...........####....",
            "..##..........#####...",
            "...##.........#####...",
            "#...###############...",
            "##...###############..",
            "#...###############...",
            "...##.........#####...",
            "..##..........#####...",
            ".##...........####....",
            "##............###.....",
            "..............##......",
            "..............#.......");
}
