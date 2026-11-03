package com.simplemachines.client;

import com.simplelib.api.client.ui.UiBoxes;
import com.simplelib.api.client.ui.UiFilterButton;
import com.simplelib.api.client.ui.UiMotifs;
import com.simplelib.api.client.ui.UiPalette;
import com.simplelib.api.client.ui.UiStyleToggle;
import com.simplemachines.hopper.HopperTier;
import com.simplemachines.hopper.TierHopperMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The mod hoppers' menu (SimpleBuilding's {@code NetheriteHopperScreen} with its 26.3 container style, stage 3). Filter principle
 * (docs/ai/PRINZIPIEN-FILTER.md): the real items in the five slots are the filter, a click is an ordinary click. The filter key
 * (SimpleLib's {@link UiFilterButton}) sends menu button {@link TierHopperMenu#FILTER_BUTTON}. Style on: the tier's box with its
 * motif, the light inventory box, the caption (funnel + colon) before the key; style off: Vanilla's hopper.png.
 */
public class TierHopperScreen extends AbstractContainerScreen<TierHopperMenu> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/hopper.png");
    private static final int INVENTORY_GAP = 2;
    private static final int INVENTORY_HEIGHT = UiBoxes.FRAME + 58 + 17 + UiBoxes.FRAME_BOTTOM;
    private Button filterButton;

    public TierHopperScreen(TierHopperMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, TierHopperMenu.IMAGE_HEIGHT);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    public static UiPalette palette(HopperTier tier) {
        return switch (tier) {
            case REINFORCED -> UiPalette.REINFORCED;
            case NETHERITE -> UiPalette.NETHERITE;
            case ENDERITE -> UiPalette.ENDERITE;
        };
    }

    static UiMotifs.Kind motif(HopperTier tier) {
        return switch (tier) {
            case REINFORCED -> UiMotifs.Kind.METAL;
            case NETHERITE -> UiMotifs.Kind.NETHER;
            case ENDERITE -> UiMotifs.Kind.ENDER;
        };
    }

    @Override
    protected void init() {
        super.init();
        this.filterButton = this.addRenderableWidget(new UiFilterButton(this.leftPos + TierHopperMenu.FILTER_BUTTON_X,
                this.topPos + TierHopperMenu.FILTER_BUTTON_Y, button -> pressFilter(),
                () -> this.menu.getSyncedFilterMode().ordinal(), () -> palette(this.menu.tier())));
    }

    private void pressFilter() {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, TierHopperMenu.FILTER_BUTTON);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        if (this.filterButton.isHovered()) {
            context.setTooltipForNextFrame(this.font, this.menu.getSyncedFilterMode().getText(), mouseX, mouseY);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        if (styled(context, this.leftPos, this.topPos)) return;
        int shift = TierHopperMenu.INVENTORY_SHIFT;
        // hopper.png stretched by the shift (the plain rows 38..44 under the slots repeated), slot frames copied to the centred row.
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, 38, 256, 256);
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos + 38, 0, 38, this.imageWidth, shift, 256, 256);
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos + 38 + shift, 0, 38, this.imageWidth, 133 - 38, 256, 256);
        context.fill(this.leftPos + 43, this.topPos + 19, this.leftPos + 43 + 5 * 18, this.topPos + 37, 0xFFC6C6C6);
        for (int i = 0; i < 5; i++) {
            Slot slot = this.menu.slots.get(i);
            context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + slot.x - 1, this.topPos + slot.y - 1, 43, 19, 18, 18, 256, 256);
        }
    }

    /** Owner N23: the five slots, a gap with the filter caption, then the key - the whole row centred. */
    private boolean styled(GuiGraphicsExtractor g, int left, int top) {
        if (!UiStyleToggle.isEnabled()) return false;
        HopperTier tier = this.menu.tier();
        UiPalette p = palette(tier);
        int invX = Integer.MAX_VALUE, invY = Integer.MAX_VALUE;
        for (Slot slot : this.menu.slots) {
            if (!(slot.container instanceof Inventory) || slot.getContainerSlot() < 9 || slot.getContainerSlot() >= 36) continue;
            invX = Math.min(invX, slot.x);
            invY = Math.min(invY, slot.y);
        }
        int containerHeight = invY - UiBoxes.FRAME - INVENTORY_GAP;
        UiBoxes.box(g, left, top, this.imageWidth, containerHeight, p);
        UiBoxes.box(g, left + invX - 8, top + invY - UiBoxes.FRAME, 176, INVENTORY_HEIGHT, UiPalette.INVENTORY);
        int keyX = left + TierHopperMenu.FILTER_BUTTON_X, keyY = top + TierHopperMenu.FILTER_BUTTON_Y;
        int captionX = keyX - 3 - UiFilterButton.LABEL_WIDTH;
        List<int[]> avoid = new ArrayList<>();
        for (Slot slot : this.menu.slots) {
            if (!slot.isActive() || slot.container instanceof Inventory) continue;
            avoid.add(new int[] {left + slot.x, top + slot.y, left + slot.x + 17, top + slot.y + 17});
        }
        avoid.add(new int[] {left + 4, top + 4, left + 12 + this.font.width(this.title), top + 15});
        avoid.add(new int[] {captionX - 2, keyY, keyX + UiFilterButton.SIZE + 1, keyY + UiFilterButton.SIZE + 1});
        UiMotifs.draw(g, 0, 0, motif(tier), left + UiBoxes.FRAME, top + UiBoxes.FRAME, this.imageWidth - 2 * UiBoxes.FRAME,
                containerHeight - UiBoxes.FRAME - UiBoxes.FRAME_BOTTOM, p, avoid, 7);
        for (Slot slot : this.menu.slots) {
            if (slot.isActive()) UiBoxes.slot(g, left + slot.x, top + slot.y, slot.container instanceof Inventory ? UiPalette.INVENTORY : p);
        }
        UiFilterButton.label(g, captionX, keyY + 3, p);
        return true;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        if (UiStyleToggle.isEnabled()) {
            // Only the title in the box's label colour; the caption before the key says "Filter".
            context.text(this.font, this.title, this.titleLabelX, this.titleLabelY, palette(this.menu.tier()).label(), false);
            return;
        }
        super.extractLabels(context, mouseX, mouseY);
        context.text(this.font, Component.translatable("container.simplemachines.hopper_filter"),
                this.filterButton.getX() - this.leftPos - 2, this.filterButton.getY() - this.topPos - 12, 0xFF404040, false);
    }
}
