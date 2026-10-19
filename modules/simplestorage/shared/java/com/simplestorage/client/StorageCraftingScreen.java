package com.simplestorage.client;

import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiMotifs;
import com.simplelib.api.client.ui.UiPalette;
import com.simplelib.api.client.ui.UiStyleToggle;
import com.simplelib.api.client.ui.UiSymbols;
import com.simplestorage.table.StorageCraftingMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Screen of the Storage Crafting Table (queue N26): Vanilla's crafting screen with its recipe book, drawn in the family's
 * container style with SimpleLib's style blocks ({@code com.simplelib.api.client.ui}, principle 6a) - oak box with the
 * wood motif, light inventory box, big result slot, dotted arrow - and as the plain crafting table when the style is off.
 * Same picture as SimpleBuilding drew before split stage 8a.
 */
public class StorageCraftingScreen extends AbstractRecipeBookScreen<StorageCraftingMenu> {
    private static final Identifier CRAFTING_TABLE_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/crafting_table.png");
    /** Oak of the crafting table. */
    static final UiPalette OAK = UiPalette.derived(0xFFA27A4A);
    /** Recipe book button (relative to the image): Vanilla's crafting table spot, left of the grid's middle row. */
    static final int BOOK_X = 5, BOOK_Y = 34;
    /** First slot of the player's main inventory and the frames of the style (UiBoxes). */
    private static final int INVENTORY_X = 8, INVENTORY_Y = 84, GAP = 2, INVENTORY_HEIGHT = UiBoxes.FRAME + 58 + 17 + UiBoxes.FRAME_BOTTOM;

    public StorageCraftingScreen(StorageCraftingMenu menu, Inventory inventory, Component title) {
        super(menu, new CraftingRecipeBookComponent(menu), inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = UiStyleToggle.isEnabled() ? 8 : 29;
    }

    @Override
    protected ScreenPosition getRecipeBookButtonPosition() {
        return new ScreenPosition(this.leftPos + BOOK_X, this.height / 2 - 83 + BOOK_Y);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!UiStyleToggle.isEnabled()) {
            super.extractLabels(graphics, mouseX, mouseY);
            return;
        }
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, OAK.label(), false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int left = this.leftPos;
        int top = (this.height - this.imageHeight) / 2;
        if (!UiStyleToggle.isEnabled()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, CRAFTING_TABLE_LOCATION, left, top, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
            return;
        }
        int containerHeight = INVENTORY_Y - UiBoxes.FRAME - GAP;
        UiBoxes.box(graphics, left, top, this.imageWidth, containerHeight, OAK);
        UiBoxes.box(graphics, left + INVENTORY_X - 8, top + INVENTORY_Y - UiBoxes.FRAME, 176, INVENTORY_HEIGHT, UiPalette.INVENTORY);
        Slot result = this.menu.getSlot(StorageCraftingMenu.RESULT_SLOT);
        List<int[]> avoid = new ArrayList<>();
        for (Slot slot : this.menu.slots) {
            if (slot.isActive() && !(slot.container instanceof Inventory)) {
                avoid.add(new int[] {left + slot.x, top + slot.y, left + slot.x + 17, top + slot.y + 17});
            }
        }
        avoid.add(new int[] {left + 4, top + 4, left + 12 + this.font.width(this.title), top + 15});
        avoid.add(new int[] {left + result.x - 4, top + result.y - 4, left + result.x + 21, top + result.y + 21});
        avoid.add(new int[] {left + 89, top + 34, left + 113, top + 51});
        avoid.add(new int[] {left + BOOK_X, top + BOOK_Y, left + BOOK_X + 20, top + BOOK_Y + 18});
        UiMotifs.draw(graphics, 0, 0, UiMotifs.Kind.WOOD, left + UiBoxes.FRAME, top + UiBoxes.FRAME, this.imageWidth - 2 * UiBoxes.FRAME,
                containerHeight - UiBoxes.FRAME - UiBoxes.FRAME_BOTTOM, OAK, avoid, 7);
        for (Slot slot : this.menu.slots) {
            if (!slot.isActive()) continue;
            if (slot == result) UiBoxes.bigSlot(graphics, left + slot.x, top + slot.y, OAK);
            else UiBoxes.slot(graphics, left + slot.x, top + slot.y, slot.container instanceof Inventory ? UiPalette.INVENTORY : OAK);
        }
        UiSymbols.engrave(graphics, UiSymbols.ARROW_DOTTED, left + 90, top + 35, OAK);
    }
}
