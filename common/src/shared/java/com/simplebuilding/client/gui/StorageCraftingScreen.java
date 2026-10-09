package com.simplebuilding.client.gui;

import com.simplebuilding.screen.StorageCraftingMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen of the Storage Crafting Table (queue N26): Vanilla's crafting screen with its recipe book, drawn in the mod's
 * container style on 26.3 ({@link ModScreenStyle#storageCraftingTable}) and as the plain crafting table otherwise.
 */
public class StorageCraftingScreen extends AbstractRecipeBookScreen<StorageCraftingMenu> {
    private static final Identifier CRAFTING_TABLE_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/crafting_table.png");

    public StorageCraftingScreen(StorageCraftingMenu menu, Inventory inventory, Component title) {
        super(menu, new CraftingRecipeBookComponent(menu), inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = ModScreenStyle.ACTIVE ? 8 : 29;
    }

    @Override
    protected ScreenPosition getRecipeBookButtonPosition() {
        return new ScreenPosition(this.leftPos + ModScreenLayout.CRAFTING_BOOK_X, this.height / 2 - 83 + ModScreenLayout.CRAFTING_BOOK_Y);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!ModScreenStyle.storageCraftingTableLabels(graphics, this.font, this.title, this.titleLabelX, this.titleLabelY)) {
            super.extractLabels(graphics, mouseX, mouseY);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = this.leftPos;
        int yo = (this.height - this.imageHeight) / 2;
        if (!ModScreenStyle.storageCraftingTable(graphics, this.menu, this.font, this.title, xo, yo, this.imageWidth)) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, CRAFTING_TABLE_LOCATION, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        }
    }
}
