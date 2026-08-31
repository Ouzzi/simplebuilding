package com.simplebuilding.client.gui;

import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.networking.SetHopperGhostItemPayload;
import com.simplebuilding.networking.ToggleHopperFilterPayload;
import com.simplebuilding.screen.NetheriteHopperScreenHandler;
import com.simplebuilding.util.HopperFilterMode;
import com.simplebuilding.platform.ClientNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
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
        super(handler, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 133;
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
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.renderTooltip(context, mouseX, mouseY);

        HopperFilterMode mode = this.menu.getSyncedFilterMode();

        // 1. Label
        context.drawString(this.font, Component.literal("Filter:"), this.filterButton.getX() - 2, this.filterButton.getY() - 12, 0xFF404040, false);

        // 2. Button Overlay
        if (mode == HopperFilterMode.NONE) {
            context.renderItem(new ItemStack(Items.BARRIER), this.filterButton.getX() + 1, this.filterButton.getY() + 1);
        } else {
            String text = (mode == HopperFilterMode.WHITELIST) ? "✔" : "T";
            int color = (mode == HopperFilterMode.WHITELIST) ? 0xFF55FF55 : 0xFFFFAA00;
            int textWidth = this.font.width(text);
            context.drawString(this.font, text, this.filterButton.getX() + (18 - textWidth) / 2, this.filterButton.getY() + 5, color, true);
        }

        if (this.filterButton.isHovered()) {
            context.setTooltipForNextFrame(this.font, mode.getText(), mouseX, mouseY);
        }

        // 3. Ghost Items & Farb-Overlay
        if (this.menu.getBlockEntity() instanceof ModHopperBlockEntity be && mode != HopperFilterMode.NONE) {
            for (int i = 0; i < 5; i++) {
                Slot slot = this.menu.slots.get(i);
                ItemStack ghostStack = be.getGhostItem(i);

                if (!ghostStack.isEmpty()) {
                    int slotX = this.leftPos + slot.x;
                    int slotY = this.topPos + slot.y;

                    context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x60FFAA00);

                    if (slot.getItem().isEmpty()) {
                        context.renderItem(ghostStack, slotX, slotY);

                        if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                            List<Component> tooltip = new ArrayList<>();
                            tooltip.add(Component.literal("Filtered Item:").withStyle(ChatFormatting.GOLD));
                            tooltip.add(ghostStack.getHoverName());
                            context.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
                        }
                    }
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

            // Nur eingreifen, wenn wir auf einen der 5 Hopper-Slots klicken
            if (hoveredSlot != null && hoveredSlot.getContainerSlot() < 5) {
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
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
    }
}