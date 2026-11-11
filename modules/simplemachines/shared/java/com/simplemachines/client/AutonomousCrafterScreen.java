package com.simplemachines.client;

import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiFilterButton;
import com.simplelib.api.client.ui.UiMotifs;
import com.simplelib.api.client.ui.UiPalette;
import com.simplelib.api.client.ui.UiStyleToggle;
import com.simplelib.api.client.ui.UiSymbols;
import com.simplemachines.crafter.AutonomousCrafterMenu;
import java.util.ArrayList;
import java.util.List;
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
 * The Autonomous Crafter's menu (SimpleBuilding's {@code AutonomousCrafterScreen} with its 26.3 container style, MACH32-MOD stage 4b;
 * look unchanged): Vanilla's crafter screen (slots switch off with a click on an empty slot, the redstone sign) plus the filter key of
 * the mod hoppers. Switching and the key go to the server as Vanilla menu button clicks. Style on: the crafter-stone box with the
 * redstone motif, the light inventory box, the big result slot, the redstone dust arrow (red while a signal stops it) and over it
 * the filter caption and key. Style off: Vanilla's crafter background and redstone sign.
 */
public class AutonomousCrafterScreen extends AbstractContainerScreen<AutonomousCrafterMenu> {
    private static final Identifier DISABLED_SLOT = Identifier.withDefaultNamespace("container/crafter/disabled_slot");
    private static final Identifier POWERED = Identifier.withDefaultNamespace("container/crafter/powered_redstone");
    private static final Identifier UNPOWERED = Identifier.withDefaultNamespace("container/crafter/unpowered_redstone");
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/crafter.png");
    private static final Component DISABLED_SLOT_TOOLTIP = Component.translatable("gui.togglable_slot");
    /** SimpleBuilding's crafter palette (ModScreenStyle.AUTONOMOUS_CRAFTER): crafter stone with a hint of the copper of its textures. */
    public static final UiPalette PALETTE = UiPalette.derived(0xFF5E5651);
    private static final int GAP = 2;
    private static final int INVENTORY_HEIGHT = UiBoxes.FRAME + 58 + 17 + UiBoxes.FRAME_BOTTOM;

    private final Player player;
    private Button filterButton;

    public AutonomousCrafterScreen(AutonomousCrafterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.player = inventory.player;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = UiStyleToggle.isEnabled() ? 8 : (this.imageWidth - this.font.width(this.title)) / 2;
        this.filterButton = this.addRenderableWidget(new UiFilterButton(this.leftPos + AutonomousCrafterMenu.FILTER_BUTTON_X,
                this.topPos + AutonomousCrafterMenu.FILTER_BUTTON_Y, btn -> pressButton(AutonomousCrafterMenu.BUTTON_FILTER),
                () -> this.menu.filterMode().ordinal(), () -> PALETTE));
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
        if (!UiStyleToggle.isEnabled()) {
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
        if (!styled(graphics, this.leftPos, this.topPos)) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (UiStyleToggle.isEnabled()) {
            graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, PALETTE.label(), false);
            return;
        }
        super.extractLabels(graphics, mouseX, mouseY);
    }

    /** The container style as SimpleBuilding drew it (ModScreenStyle.autonomousCrafter): same boxes, motif seed, arrow, key and slots. */
    private boolean styled(GuiGraphicsExtractor g, int left, int top) {
        if (!UiStyleToggle.isEnabled()) return false;
        int invX = Integer.MAX_VALUE, invY = Integer.MAX_VALUE;
        for (Slot slot : this.menu.slots) {
            if (!(slot.container instanceof Inventory) || slot.getContainerSlot() < 9 || slot.getContainerSlot() >= 36) continue;
            invX = Math.min(invX, slot.x);
            invY = Math.min(invY, slot.y);
        }
        int containerHeight = invY - UiBoxes.FRAME - GAP;
        UiBoxes.box(g, left, top, this.imageWidth, containerHeight, PALETTE);
        UiBoxes.box(g, left + invX - 8, top + invY - UiBoxes.FRAME, 176, INVENTORY_HEIGHT, UiPalette.INVENTORY);
        Slot result = this.menu.getSlot(AutonomousCrafterMenu.RESULT_SLOT);
        int keyX = left + AutonomousCrafterMenu.FILTER_BUTTON_X, keyY = top + AutonomousCrafterMenu.FILTER_BUTTON_Y;
        int captionX = keyX - 3 - UiFilterButton.LABEL_WIDTH;
        List<int[]> avoid = new ArrayList<>();
        for (Slot slot : this.menu.slots) {
            if (!slot.isActive() || slot.container instanceof Inventory) continue;
            avoid.add(new int[] {left + slot.x, top + slot.y, left + slot.x + 17, top + slot.y + 17});
        }
        avoid.add(new int[] {left + 4, top + 4, left + 12 + this.font.width(this.title), top + 15});
        avoid.add(new int[] {left + 99, top + 36, left + 121, top + 52});
        avoid.add(new int[] {captionX - 2, keyY, keyX + UiFilterButton.SIZE + 1, keyY + UiFilterButton.SIZE});
        avoid.add(new int[] {left + result.x - 4, top + result.y - 4, left + result.x + 21, top + result.y + 21});
        UiMotifs.draw(g, 0, 0, UiMotifs.Kind.REDSTONE, left + UiBoxes.FRAME, top + UiBoxes.FRAME, this.imageWidth - 2 * UiBoxes.FRAME,
                containerHeight - UiBoxes.FRAME - UiBoxes.FRAME_BOTTOM, PALETTE, avoid, 5);
        for (Slot slot : this.menu.slots) {
            if (!slot.isActive()) continue;
            if (slot == result) UiBoxes.bigSlot(g, left + slot.x, top + slot.y, PALETTE);
            else UiBoxes.slot(g, left + slot.x, top + slot.y, slot.container instanceof Inventory ? UiPalette.INVENTORY : PALETTE);
        }
        UiSymbols.redstoneArrow(g, left + 99, top + 36, PALETTE, this.menu.isPowered());
        UiFilterButton.label(g, captionX, keyY + 3, PALETTE);
        return true;
    }
}
