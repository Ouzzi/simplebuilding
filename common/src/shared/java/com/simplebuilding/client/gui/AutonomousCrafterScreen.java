package com.simplebuilding.client.gui;

import com.simplebuilding.screen.AutonomousCrafterMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Autonomous Crafter: Vanilla's crafter screen (slots switch off with a click on an empty slot, the redstone sign)
 * plus the filter key of the mod hoppers. Switching and the key go to the server as Vanilla menu button clicks.
 */
public class AutonomousCrafterScreen extends AbstractContainerScreen<AutonomousCrafterMenu> {
    private static final Identifier DISABLED_SLOT = Identifier.withDefaultNamespace("container/crafter/disabled_slot");
    private static final Identifier POWERED = Identifier.withDefaultNamespace("container/crafter/powered_redstone");
    private static final Identifier UNPOWERED = Identifier.withDefaultNamespace("container/crafter/unpowered_redstone");
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/crafter.png");
    private static final Component DISABLED_SLOT_TOOLTIP = Component.translatable("gui.togglable_slot");

    private final Player player;
    private Button filterButton;

    public AutonomousCrafterScreen(AutonomousCrafterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.player = inventory.player;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = ModScreenStyle.ACTIVE ? 8 : (this.imageWidth - this.font.width(this.title)) / 2;
        this.filterButton = this.addRenderableWidget(ModScreenStyle.crafterFilterButton(
                this.leftPos + AutonomousCrafterMenu.FILTER_BUTTON_X, this.topPos + AutonomousCrafterMenu.FILTER_BUTTON_Y,
                btn -> pressButton(AutonomousCrafterMenu.BUTTON_FILTER), this.menu));
    }

    private void pressButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput input) {
        if (slot != null && slotId >= 0 && slotId < 9 && !slot.hasItem() && !this.player.isSpectator()) {
            switch (input) {
                case PICKUP -> {
                    if (this.menu.isSlotDisabled(slotId)) toggle(slotId, true);
                    else if (this.menu.getCarried().isEmpty()) toggle(slotId, false);
                }
                case SWAP -> {
                    ItemStack hotbar = this.player.getInventory().getItem(buttonNum);
                    if (this.menu.isSlotDisabled(slotId) && !hotbar.isEmpty()) toggle(slotId, true);
                }
                default -> {
                }
            }
        }
        super.slotClicked(slot, slotId, buttonNum, input);
    }

    private void toggle(int slotId, boolean enable) {
        pressButton(slotId);
        this.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4F, enable ? 1.0F : 0.75F);
    }

    @Override
    public void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
        if (slot.index < 9 && this.menu.isSlotDisabled(slot.index)) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, DISABLED_SLOT, slot.x - 1, slot.y - 1, 18, 18);
        } else {
            super.extractSlot(graphics, slot, mouseX, mouseY);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        if (!ModScreenStyle.ACTIVE) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.menu.isPowered() ? POWERED : UNPOWERED,
                    this.leftPos + 97, this.topPos + 35, 16, 16);
        }
        if (this.hoveredSlot != null && this.hoveredSlot.index < 9 && !this.menu.isSlotDisabled(this.hoveredSlot.index)
                && this.menu.getCarried().isEmpty() && !this.hoveredSlot.hasItem() && !this.player.isSpectator()) {
            graphics.setTooltipForNextFrame(this.font, DISABLED_SLOT_TOOLTIP, mouseX, mouseY);
        }
        if (this.filterButton.isHovered()) {
            graphics.setTooltipForNextFrame(this.font, this.menu.filterMode().getText(), mouseX, mouseY);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        if (!ModScreenStyle.autonomousCrafter(graphics, this.menu, this.font, this.title, this.leftPos, this.topPos, this.imageWidth)) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!ModScreenStyle.autonomousCrafterLabels(graphics, this.font, this.title, this.titleLabelX, this.titleLabelY)) {
            super.extractLabels(graphics, mouseX, mouseY);
        }
    }
}
