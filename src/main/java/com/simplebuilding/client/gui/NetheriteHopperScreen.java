package com.simplebuilding.client.gui;

import com.simplebuilding.blocks.entity.custom.NetheriteHopperBlockEntity;
import com.simplebuilding.networking.ToggleHopperFilterPayload;
import com.simplebuilding.screen.NetheriteHopperScreenHandler;
import com.simplebuilding.util.HopperFilterMode;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class NetheriteHopperScreen extends HandledScreen<NetheriteHopperScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of("textures/gui/container/hopper.png"); // Vanilla Textur oder deine eigene

    public NetheriteHopperScreen(NetheriteHopperScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 133;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        
        // Buttons über den Slots hinzufügen
        // Hopper Slots sind bei (44, 20) + i * 18 in der Standard Textur
        int xStart = this.x + 44;
        int yPos = this.y + 5; // Etwas über den Slots

        for (int i = 0; i < 5; i++) {
            final int slotIndex = i;
            // Kleiner Button (10x10) oder ähnliches
            this.addDrawableChild(ButtonWidget.builder(Text.literal("F"), button -> {
                // Sende Packet an Server
                ClientPlayNetworking.send(new ToggleHopperFilterPayload(slotIndex));
            })
            .dimensions(xStart + (i * 18), yPos, 16, 12) // Position anpassen
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Toggle Filter Mode")))
            .build());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        
        // Ghost Items rendern
        NetheriteHopperBlockEntity be = this.handler.getBlockEntity();
        if (be != null) {
            // Hopper Slots sind normalerweise Slot 0-4 im Handler
            for (int i = 0; i < 5; i++) {
                HopperFilterMode mode = be.getFilterMode(i);
                if (mode != HopperFilterMode.NONE) {
                    ItemStack realStack = this.handler.getInventory().getStack(i); // Das echte Inventar
                    ItemStack ghostStack = be.getGhostItem(i);

                    // Nur rendern, wenn der echte Slot leer ist, aber ein Ghost Item gesetzt ist
                    if (realStack.isEmpty() && !ghostStack.isEmpty()) {
                        int slotX = this.x + 44 + (i * 18);
                        int slotY = this.y + 20;

                        // Zeichne Ghost Item halbtransparent
                        context.getMatrices().push();
                        context.getMatrices().translate(0,0, 100); // Über den Slots, unter dem Tooltip
                        
                        // Fake Render: Wir rendern das Item
                        context.drawItem(ghostStack, slotX, slotY);
                        // Grauer Schleier drüber zeichnen für "Ghost"-Effekt
                        context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x80808080); // Halbtransparent grau
                        
                        context.getMatrices().pop();
                        
                        // Tooltip Logik: Wenn Maus drüber, zeige Ghost Item Tooltip
                        if (isPointWithinBounds(44 + (i * 18), 20, 16, 16, mouseX, mouseY)) {
                            context.drawItemTooltip(this.textRenderer, ghostStack, mouseX, mouseY);
                        }
                    }
                    
                    // Button Farbe/Text aktualisieren basierend auf Modus (Optional)
                    // Das würde erfordern, dass wir Referenzen zu den Buttons in init() speichern und hier updaten.
                }
            }
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(TEXTURE, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }
}