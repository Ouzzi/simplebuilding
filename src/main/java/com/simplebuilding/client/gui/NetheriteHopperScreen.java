package com.simplebuilding.client.gui;

import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.networking.SetHopperGhostItemPayload;
import com.simplebuilding.networking.ToggleHopperFilterPayload;
import com.simplebuilding.screen.NetheriteHopperScreenHandler;
import com.simplebuilding.util.HopperFilterMode;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
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
    private static final Identifier TEXTURE = new Identifier("textures/gui/container/hopper.png");
    private ButtonWidget filterButton;

    public NetheriteHopperScreen(NetheriteHopperScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 133;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int buttonX = this.x + 44 + (5 * 18) + 4;
        int buttonY = this.y + 19;

        this.filterButton = this.addDrawableChild(ButtonWidget.builder(Text.empty(), btn ->
                ClientPlayNetworking.send(new ToggleHopperFilterPayload())
        ).dimensions(buttonX, buttonY, 18, 18).build());
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
        drawMouseoverTooltip(matrices, mouseX, mouseY);

        HopperFilterMode mode = this.handler.getSyncedFilterMode();

        this.textRenderer.draw(matrices, Text.literal("Filter:"), this.filterButton.getX() - 2, this.filterButton.getY() - 12, 0xFF404040);

        if (mode == HopperFilterMode.NONE) {
            this.itemRenderer.renderInGuiWithOverrides(new ItemStack(Items.BARRIER), this.filterButton.getX() + 1, this.filterButton.getY() + 1);
        } else {
            String text = (mode == HopperFilterMode.WHITELIST) ? "OK" : "T";
            int color = (mode == HopperFilterMode.WHITELIST) ? 0xFF55FF55 : 0xFFFFAA00;
            int textWidth = this.textRenderer.getWidth(text);
            this.textRenderer.drawWithShadow(matrices, text, this.filterButton.getX() + (18 - textWidth) / 2f, this.filterButton.getY() + 5, color);
        }

        if (this.filterButton.isHovered()) {
            renderTooltip(matrices, mode.getText(), mouseX, mouseY);
        }

        if (this.handler.getBlockEntity() instanceof ModHopperBlockEntity be && mode != HopperFilterMode.NONE) {
            for (int i = 0; i < 5; i++) {
                Slot slot = this.handler.slots.get(i);
                ItemStack ghostStack = be.getGhostItem(i);

                if (!ghostStack.isEmpty()) {
                    int slotX = this.x + slot.x;
                    int slotY = this.y + slot.y;

                    DrawableHelper.fill(matrices, slotX, slotY, slotX + 16, slotY + 16, 0x60FFAA00);

                    if (slot.getStack().isEmpty()) {
                        this.itemRenderer.renderInGuiWithOverrides(ghostStack, slotX, slotY);

                        if (isPointWithinBounds(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                            List<Text> tooltip = new ArrayList<>();
                            tooltip.add(Text.literal("Filtered Item:").formatted(Formatting.GOLD));
                            tooltip.add(ghostStack.getName());
                            renderTooltip(matrices, tooltip, mouseX, mouseY);
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.handler.getSyncedFilterMode() != HopperFilterMode.NONE) {
            Slot hoveredSlot = this.getSlotAt(mouseX, mouseY);

            if (hoveredSlot != null && hoveredSlot.getIndex() < 5) {
                ItemStack cursorStack = this.handler.getCursorStack();
                ClientPlayNetworking.send(new SetHopperGhostItemPayload(hoveredSlot.getIndex(), cursorStack));

                if (this.handler.getBlockEntity() instanceof ModHopperBlockEntity be) {
                    be.setGhostItemClient(hoveredSlot.getIndex(), cursorStack);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private Slot getSlotAt(double mouseX, double mouseY) {
        for (Slot slot : this.handler.slots) {
            if (this.isPointWithinBounds(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        this.drawTexture(matrices, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }
}