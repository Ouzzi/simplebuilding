package com.simplebuilding.client.gui;

import com.simplebuilding.networking.ToggleHopperFilterPayload;
import com.simplebuilding.screen.ModHopperScreenHandler;
import com.simplebuilding.screen.NetheriteHopperScreenHandler;
import com.simplebuilding.util.HopperFilterMode;
import com.simplebuilding.platform.ClientNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The mod hoppers' menu. Filter principle (docs/ai/PRINZIPIEN-FILTER.md): the real items in the five slots are the
 * filter, so a click is an ordinary click - there are no ghost items to set any more. The filter key cycles the mode
 * (off / exact / same kind).
 */
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
        int buttonX = this.leftPos + ModHopperScreenHandler.FILTER_BUTTON_X;
        int buttonY = this.topPos + ModHopperScreenHandler.FILTER_BUTTON_Y;
        // 26.3: SimpleLib's filter key in the container style (ModScreenStyle); 26.2: a plain button, the mode drawn on top.
        this.filterButton = this.addRenderableWidget(ModScreenStyle.hopperFilterButton(buttonX, buttonY,
                btn -> ClientNetworking.send(new ToggleHopperFilterPayload()), this.menu));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        HopperFilterMode mode = this.menu.getSyncedFilterMode();

        // Button Overlay (26.2; im Kasten-Stil zeichnet die Taste ihr Symbol selbst)
        if (!ModScreenStyle.ACTIVE && mode == HopperFilterMode.NONE) {
            context.item(new ItemStack(Items.BARRIER), this.filterButton.getX() + 1, this.filterButton.getY() + 1);
        } else if (!ModScreenStyle.ACTIVE) {
            String text = (mode == HopperFilterMode.WHITELIST) ? "✔" : "T";
            int color = (mode == HopperFilterMode.WHITELIST) ? 0xFF55FF55 : 0xFFFFAA00;
            int textWidth = this.font.width(text);
            context.text(this.font, text, this.filterButton.getX() + (18 - textWidth) / 2, this.filterButton.getY() + 5, color, true);
        }

        if (this.filterButton.isHovered()) {
            context.setTooltipForNextFrame(this.font, mode.getText(), mouseX, mouseY);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        if (ModScreenStyle.hopper(context, this.menu, this.font, this.title, this.leftPos, this.topPos, this.imageWidth)) {
            return;
        }
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        if (ModHopperScreenHandler.FIRST_SLOT_X != 44) {
            // The style is switched off for a comparison on a line whose slots are centred: hopper.png has its slot
            // frames at Vanilla's x, so they are painted over and copied to where the slots really are.
            context.fill(this.leftPos + 43, this.topPos + 19, this.leftPos + 43 + 5 * 18, this.topPos + 37, 0xFFC6C6C6);
            for (int i = 0; i < 5; i++) {
                Slot slot = this.menu.slots.get(i);
                context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + slot.x - 1, this.topPos + slot.y - 1,
                        43, 19, 18, 18, 256, 256);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        // Kasten-Stil: nur der Titel in der Label-Farbe; "Filter" zeigen Trichter-Symbol + Doppelpunkt vor der Taste.
        if (ModScreenStyle.hopperLabels(context, this.font, this.menu, this.title, this.titleLabelX, this.titleLabelY)) {
            return;
        }
        super.extractLabels(context, mouseX, mouseY);
        // Relativ zur Bildecke: rechts ueber dem Filterknopf, in der Zeile des Titels.
        context.text(this.font, Component.translatable("container.simplebuilding.hopper_filter"),
                this.filterButton.getX() - this.leftPos - 2, this.filterButton.getY() - this.topPos - 12, 0xFF404040, false);
    }
}
