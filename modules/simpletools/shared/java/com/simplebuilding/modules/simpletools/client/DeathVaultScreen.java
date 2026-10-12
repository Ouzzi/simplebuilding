package com.simplebuilding.modules.simpletools.client;

import com.simplebuilding.modules.simpletools.vault.DeathVaultMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Death Vault screen in the Vanilla chest style ({@code generic_54}, four rows): the lost stacks on top (still lying
 * ones dimmed), the offering slot and the "Offer" button in the fourth row, the inventory below.
 */
public class DeathVaultScreen extends AbstractContainerScreen<DeathVaultMenu> {
    private static final Identifier CHEST = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int FILL = 0xFFC6C6C6, DIM = 0x99303030;
    private Button offer;

    public DeathVaultScreen(DeathVaultMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, DeathVaultMenu.WIDTH, DeathVaultMenu.HEIGHT);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        offer = addRenderableWidget(Button.builder(Component.translatable("screen.simpletools.death_vault.offer"), b -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, DeathVaultMenu.BUTTON_OFFER);
        }).bounds(leftPos + DeathVaultMenu.BUTTON_X, topPos + DeathVaultMenu.OFFER_Y - 1, DeathVaultMenu.BUTTON_W, 18).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int top = DeathVaultMenu.ROWS * 18 + 17;
        g.blit(RenderPipelines.GUI_TEXTURED, CHEST, leftPos, topPos, 0.0F, 0.0F, imageWidth, top, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, CHEST, leftPos, topPos + top, 0.0F, 126.0F, imageWidth, 96, 256, 256);
        // Fourth row: only the offering slot is real; cover the other slot frames with the panel colour.
        int rowY = topPos + DeathVaultMenu.OFFER_Y - 1;
        g.fill(leftPos + 7, rowY, leftPos + DeathVaultMenu.OFFER_X - 1, rowY + 18, FILL);
        g.fill(leftPos + DeathVaultMenu.OFFER_X + 17, rowY, leftPos + 169, rowY + 18, FILL);
    }

    @Override
    public void extractSlot(GuiGraphicsExtractor g, Slot slot, int mouseX, int mouseY) {
        super.extractSlot(g, slot, mouseX, mouseY);
        if (slot.index < DeathVaultMenu.SHOWN && slot.hasItem() && slot.index >= menu.data(DeathVaultMenu.D_SHOWN_LOST)) {
            g.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, DIM);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        if (offer != null) {
            int cost = Math.max(1, menu.data(DeathVaultMenu.D_COST));
            offer.active = menu.data(DeathVaultMenu.D_LOST) > 0 && menu.offering().getCount() >= cost;
        }
        super.extractRenderState(g, mouseX, mouseY, a);
        if (offer != null && offer.isHovered()) {
            g.setTooltipForNextFrame(font, Component.translatable("screen.simpletools.death_vault.offer.tooltip",
                    menu.data(DeathVaultMenu.D_COST), menu.data(DeathVaultMenu.D_MAX)), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        Component counts = Component.translatable("screen.simpletools.death_vault.counts", menu.data(DeathVaultMenu.D_LOST), menu.data(DeathVaultMenu.D_LYING));
        g.text(font, counts, imageWidth - 8 - font.width(counts), titleLabelY, 0xFF404040, false);
        g.text(font, Component.translatable("screen.simpletools.death_vault.offering"), 8, DeathVaultMenu.OFFER_Y + 5, 0xFF404040, false);
    }
}
