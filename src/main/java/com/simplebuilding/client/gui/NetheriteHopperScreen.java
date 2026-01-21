package com.simplebuilding.client.gui;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.blocks.entity.custom.NetheriteHopperBlockEntity;
import com.simplebuilding.networking.SetHopperGhostItemPayload;
import com.simplebuilding.networking.ToggleHopperFilterPayload;
import com.simplebuilding.screen.NetheriteHopperScreenHandler;
import com.simplebuilding.util.HopperFilterMode;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class NetheriteHopperScreen extends HandledScreen<NetheriteHopperScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of("textures/gui/container/hopper.png");
    private ButtonWidget filterButton;

    public NetheriteHopperScreen(NetheriteHopperScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 133;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // Positionierung: Rechts neben den 5 Slots
        int buttonX = this.x + 44 + (5 * 18) + 4;
        int buttonY = this.y + 19;

        // Button erstellen (Text lassen wir leer, wir zeichnen das Icon selber drüber)
        this.filterButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), btn -> {
            ClientPlayNetworking.send(new ToggleHopperFilterPayload());
        }).dimensions(buttonX, buttonY, 18, 18).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta); // Zeichnet Slots und Items
        drawMouseoverTooltip(context, mouseX, mouseY); // Zeichnet Tooltip des echten Items

        HopperFilterMode mode = this.handler.getSyncedFilterMode();

        // 1. Label
        context.drawText(this.textRenderer, Text.literal("Filter:"), this.filterButton.getX() - 2, this.filterButton.getY() - 12, 0xFF404040, false);

        // 2. Button Overlay
        if (mode == HopperFilterMode.NONE) {
            context.drawItem(new ItemStack(Items.BARRIER), this.filterButton.getX() + 1, this.filterButton.getY() + 1);
        } else {
            String text = (mode == HopperFilterMode.WHITELIST) ? "✔" : "T";
            int color = (mode == HopperFilterMode.WHITELIST) ? 0xFF55FF55 : 0xFFFFAA00;
            int textWidth = this.textRenderer.getWidth(text);
            context.drawText(this.textRenderer, text, this.filterButton.getX() + (18 - textWidth) / 2, this.filterButton.getY() + 5, color, true);
        }

        if (this.filterButton.isHovered()) {
            context.drawTooltip(this.textRenderer, mode.getText(), mouseX, mouseY);
        }

    // 3. Ghost Items & Farb-Overlay
    // WICHTIG: Damit 'getBlockEntity()' hier funktioniert, beachte Schritt 2!
    if (this.handler.getBlockEntity() instanceof ModHopperBlockEntity be && mode != HopperFilterMode.NONE) {
        for (int i = 0; i < 5; i++) {
            Slot slot = this.handler.slots.get(i);
            ItemStack ghostStack = be.getGhostItem(i); // Holt das Ghost Item aus der BlockEntity

            // Wenn ein Filter gesetzt ist (Ghost Stack nicht leer)
            if (!ghostStack.isEmpty()) {
                int slotX = this.x + slot.x;
                int slotY = this.y + slot.y;

                // A) Oranges Overlay zeichnen (0x60 = Transparenz, FFAA00 = Orange)
                context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x60FFAA00);

                // B) Ghost Item zeichnen (nur wenn Slot physikalisch leer ist)
                if (slot.getStack().isEmpty()) {
                    context.drawItem(ghostStack, slotX, slotY);

                    // Manuelles Tooltip für Ghost Item
                    if (isPointWithinBounds(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                        List<Text> tooltip = new ArrayList<>();
                        tooltip.add(Text.literal("Filtered Item:").formatted(Formatting.GOLD));
                        tooltip.add(ghostStack.getName());
                        context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
                    }
                }
            }
        }
    }
}

    // FIX FÜR GLITCH: Abfangen der Klicks auf die Slots, um Ghost Items zu setzen
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        // Nutzen der synchronisierten Daten
        if (this.handler.getSyncedFilterMode() != HopperFilterMode.NONE) {
            Slot hoveredSlot = this.getSlotAt(click.x(), click.y());

            // Nur eingreifen, wenn wir auf einen der 5 Hopper-Slots klicken
            if (hoveredSlot != null && hoveredSlot.getIndex() < 5) {
                ItemStack cursorStack = this.handler.getCursorStack();

                // Senden des Pakets (jetzt crash-sicher auch mit leerem Stack)
                ClientPlayNetworking.send(new SetHopperGhostItemPayload(hoveredSlot.getIndex(), cursorStack));

                // Client-seitiges Update für sofortiges Feedback
                if (this.handler.getBlockEntity() instanceof ModHopperBlockEntity be) {
                    be.setGhostItemClient(hoveredSlot.getIndex(), cursorStack);
                }

                return true; // Event konsumieren, damit kein echtes Item gelegt wird
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private Slot getSlotAt(double x, double y) {
        for (Slot slot : this.handler.slots) {
            if (this.isPointWithinBounds(slot.x, slot.y, 16, 16, x, y)) {
                return slot;
            }
        }
        return null;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight, 256, 256);
    }
}