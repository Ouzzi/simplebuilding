package com.simplebuilding.client.gui;

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
import net.minecraft.util.Identifier;

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
        })
        .dimensions(buttonX, buttonY, 18, 18)
        .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);

        // Update Button Text/Tooltip basierend auf aktuellem Status
        if (this.handler.getBlockEntity() instanceof NetheriteHopperBlockEntity be) {
            HopperFilterMode mode = be.getFilterMode();

            // --- Button Overlay Rendern (Icon/Text) ---
            // Wir rendern manuell über den Button an dessen Position

            // Icon Position (zentriert im 18x18 Button)
            int iconX = this.filterButton.getX() + 1;
            int iconY = this.filterButton.getY() + 1;

            // KORREKTUR: Keine Matrix-Translation mehr (Z-Achse wird durch Zeichenreihenfolge bestimmt)

            if (mode == HopperFilterMode.NONE) {
                // "Parkverbot" -> Barriere Item rendern
                context.drawItem(new ItemStack(Items.BARRIER), iconX, iconY);
            } else {
                // Text rendern für die anderen Modi
                String text = (mode == HopperFilterMode.WHITELIST) ? "✔" : "T";
                int color = mode.getColor();

                // Text zentrieren
                int textWidth = this.textRenderer.getWidth(text);
                int textX = this.filterButton.getX() + (this.filterButton.getWidth() - textWidth) / 2;
                int textY = this.filterButton.getY() + (this.filterButton.getHeight() - 8) / 2;

                context.drawText(this.textRenderer, text, textX, textY, color, true);
            }
            context.getMatrices().popMatrix();

            // Tooltip für den Button setzen (wird angezeigt, wenn Maus drüber ist)
            if (this.filterButton.isHovered()) {
                context.drawTooltip(this.textRenderer, mode.getText(), mouseX, mouseY);
            }

            // --- Ghost Items rendern ---
            if (mode != HopperFilterMode.NONE) {
                for(int i = 0; i < 5; i++) {
                    // Wir greifen sicherheitshalber über die Slots auf das echte Item zu
                    // Annahme: Die ersten 5 Slots im Handler gehören dem Hopper
                    ItemStack realStack = ItemStack.EMPTY;
                    if (i < this.handler.slots.size()) {
                        realStack = this.handler.slots.get(i).getStack();
                    }

                    ItemStack ghostStack = be.getGhostItem(i);

                    // Nur rendern, wenn der Slot leer ist, aber ein Ghost Item gesetzt ist
                    if (realStack.isEmpty() && !ghostStack.isEmpty()) {
                        int slotX = this.x + 44 + (i * 18);
                        int slotY = this.y + 20;

                        // Item rendern (Draw Order sorgt für Sichtbarkeit über dem Hintergrund)
                        context.drawItem(ghostStack, slotX, slotY);

                        // Grauer Schleier darüber
                        context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x60808080);

                        // Tooltip
                        if (isPointWithinBounds(44 + (i * 18), 20, 16, 16, mouseX, mouseY)) {
                            context.drawItemTooltip(this.textRenderer, ghostStack, mouseX, mouseY);
                        }
                    }
                }
            }
        }
    }

    // FIX FÜR GLITCH: Abfangen der Klicks auf die Slots, um Ghost Items zu setzen
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (this.handler.getBlockEntity() instanceof NetheriteHopperBlockEntity be) {
            // Nur wenn Filter aktiv ist, greifen wir in die Logik ein
            if (be.getFilterMode() != HopperFilterMode.NONE) {

                // Wir nutzen click.x() und click.y() aus dem Record
                Slot hoveredSlot = this.getSlotAt(click.x(), click.y());

                // Prüfen ob Klick auf Hopper-Slot (Index < 5)
                if (hoveredSlot != null && hoveredSlot.getIndex() < 5) {

                    ItemStack cursorStack = this.handler.getCursorStack();
                    ClientPlayNetworking.send(new SetHopperGhostItemPayload(hoveredSlot.getIndex(), cursorStack));

                    // WICHTIG: return true verhindert, dass "super.mouseClicked" aufgerufen wird.
                    // Das verhindert, dass das Item "echt" in den Slot gelegt wird, was den visuellen Glitch behebt.
                    return true;
                }
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