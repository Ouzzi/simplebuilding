package com.simplebuilding.client.gui;

import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import com.simplebuilding.networking.ToggleHopperFilterPayload;
import com.simplebuilding.screen.NetheriteHopperScreenHandler; // WICHTIG: Korrekter Handler
import com.simplebuilding.util.HopperFilterMode;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.Identifier;

public class NetheriteHopperScreen extends HandledScreen<NetheriteHopperScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of("textures/gui/container/hopper.png");

    public NetheriteHopperScreen(NetheriteHopperScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 133;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int startX = this.x + 44;
        int startY = this.y + 4;

        for(int i = 0; i < 5; i++) {
            final int slotId = i;
            this.addDrawableChild(ButtonWidget.builder(Text.literal("F"), btn -> {
                        ClientPlayNetworking.send(new ToggleHopperFilterPayload(slotId));
                    })
                    .dimensions(startX + (i * 18), startY, 12, 12)
                    .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Toggle Filter Mode")))
                    .build());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);

        ModHopperBlockEntity be = this.handler.getBlockEntity();
        if (be == null) return;

        for(int i = 0; i < 5; i++) {
            HopperFilterMode mode = be.getFilterMode(i);

            if (mode != HopperFilterMode.NONE) {
                // KORRIGIERT: Zugriff auf Slots über handler.slots statt getInventory()
                ItemStack realStack = this.handler.getSlot(i).getStack();
                ItemStack ghostStack = be.getGhostItem(i);

                if (realStack.isEmpty() && !ghostStack.isEmpty()) {
                    int slotX = this.x + 44 + (i * 18);
                    int slotY = this.y + 20;

                    context.getMatrices().pushMatrix();
                    context.drawItem(ghostStack, slotX, slotY);
                    context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x80808080);
                    context.getMatrices().popMatrix();

                    if (isPointWithinBounds(44 + (i*18), 20, 16, 16, mouseX, mouseY)) {
                        context.drawItemTooltip(this.textRenderer, ghostStack, mouseX, mouseY);
                    }
                }
            }
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight, 256, 256);
    }
}