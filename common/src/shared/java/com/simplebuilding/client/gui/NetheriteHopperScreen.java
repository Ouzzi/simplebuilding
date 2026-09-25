package com.simplebuilding.client.gui;

import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.networking.SetHopperGhostItemPayload;
import com.simplebuilding.networking.ToggleHopperFilterPayload;
import com.simplebuilding.screen.NetheriteHopperScreenHandler;
import com.simplebuilding.util.HopperFilterMode;
import com.simplebuilding.platform.ClientNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;

public class NetheriteHopperScreen extends AbstractContainerScreen<NetheriteHopperScreenHandler> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/hopper.png");
    private Button filterButton;

    public NetheriteHopperScreen(NetheriteHopperScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 176, 133);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // Positionierung: Rechts neben den 5 Slots
        int buttonX = this.leftPos + 44 + (5 * 18) + 4;
        int buttonY = this.topPos + 19;

        // Button erstellen (Text lassen wir leer, wir zeichnen das Icon selber drüber)
        this.filterButton = this.addRenderableWidget(Button.builder(Component.empty(), btn -> {
            ClientNetworking.send(new ToggleHopperFilterPayload());
        }).bounds(buttonX, buttonY, 18, 18).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        HopperFilterMode mode = this.menu.getSyncedFilterMode();

        // 1. Label: im Beschriftungs-Durchgang (extractLabels/renderLabels), wie Titel und Inventar.

        // 2. Button Overlay
        if (mode == HopperFilterMode.NONE) {
            context.item(new ItemStack(Items.BARRIER), this.filterButton.getX() + 1, this.filterButton.getY() + 1);
        } else {
            String text = (mode == HopperFilterMode.WHITELIST) ? "✔" : "T";
            int color = (mode == HopperFilterMode.WHITELIST) ? 0xFF55FF55 : 0xFFFFAA00;
            int textWidth = this.font.width(text);
            context.text(this.font, text, this.filterButton.getX() + (18 - textWidth) / 2, this.filterButton.getY() + 5, color, true);
        }

        if (this.filterButton.isHovered()) {
            context.setTooltipForNextFrame(this.font, mode.getText(), mouseX, mouseY);
        }

        // 3. Tooltip der Geister-Items. Overlay und Symbol liegen seit 2026-09 im Hintergrund-
        // Durchgang, also UNTER den echten Items und der Slot-Hervorhebung, wie bei Vanilla.
        if (this.menu.getBlockEntity() instanceof ModHopperBlockEntity be && mode != HopperFilterMode.NONE) {
            for (int i = 0; i < 5; i++) {
                Slot slot = this.menu.slots.get(i);
                ItemStack ghostStack = be.getGhostItem(i);
                if (!ghostStack.isEmpty() && slot.getItem().isEmpty()
                        && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(Component.translatable("container.simplebuilding.hopper_filter.ghost").withStyle(ChatFormatting.GOLD));
                    tooltip.add(ghostStack.getHoverName());
                    context.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
                }
            }
        }
    }

    // FIX FÜR GLITCH: Abfangen der Klicks auf die Slots, um Ghost Items zu setzen
    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        // Nutzen der synchronisierten Daten
        if (this.menu.getSyncedFilterMode() != HopperFilterMode.NONE) {
            Slot hoveredSlot = this.getHoveredSlot(click.x(), click.y());

            // Nur eingreifen, wenn wir auf einen der 5 Hopper-Slots klicken. Der MENUE-Index
            // entscheidet, nicht der Container-Index: HopperMenu haengt das Spielerinventar mit
            // addStandardInventorySlots an, und dort hat die Hotbar die Container-Indizes 0..8 -
            // "getContainerSlot() < 5" verschluckte die Klicks auf die Hotbar-Slots 1 bis 5 und
            // schrieb stattdessen ein Geister-Item in den Trichter.
            if (hoveredSlot != null && hoveredSlot.index < 5) {
                ItemStack cursorStack = this.menu.getCarried();

                // Senden des Pakets (jetzt crash-sicher auch mit leerem Stack)
                ClientNetworking.send(new SetHopperGhostItemPayload(hoveredSlot.getContainerSlot(), cursorStack));

                // Client-seitiges Update für sofortiges Feedback
                if (this.menu.getBlockEntity() instanceof ModHopperBlockEntity be) {
                    be.setGhostItemClient(hoveredSlot.getContainerSlot(), cursorStack);
                }

                return true; // Event konsumieren, damit kein echtes Item gelegt wird
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private Slot getHoveredSlot(double x, double y) {
        for (Slot slot : this.menu.slots) {
            if (this.isHovering(slot.x, slot.y, 16, 16, x, y)) {
                return slot;
            }
        }
        return null;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        if (this.menu.getBlockEntity() instanceof ModHopperBlockEntity be && this.menu.getSyncedFilterMode() != HopperFilterMode.NONE) {
            for (int i = 0; i < 5; i++) {
                Slot slot = this.menu.slots.get(i);
                ItemStack ghostStack = be.getGhostItem(i);

                if (!ghostStack.isEmpty()) {
                    int slotX = this.leftPos + slot.x;
                    int slotY = this.topPos + slot.y;

                    context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x60FFAA00);

                    if (slot.getItem().isEmpty()) {
                        context.item(ghostStack, slotX, slotY);
                    }
                }
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        super.extractLabels(context, mouseX, mouseY);
        // Relativ zur Bildecke: rechts ueber dem Filterknopf, in der Zeile des Titels.
        context.text(this.font, Component.translatable("container.simplebuilding.hopper_filter"),
                this.filterButton.getX() - this.leftPos - 2, this.filterButton.getY() - this.topPos - 12, 0xFF404040, false);
    }
}