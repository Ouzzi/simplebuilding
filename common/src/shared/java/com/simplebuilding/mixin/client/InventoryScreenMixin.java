package com.simplebuilding.mixin.client;

import com.simplebuilding.client.gui.TrimStatsPanel;
import com.simplebuilding.util.ExtraHead;
import com.simplebuilding.util.TrimStatsLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * Owner N49 in the normal inventory (E): the frame of the extra head slot (above the off-hand slot, level with the
 * helmet; the Vanilla texture has no frame there) and the resonance button between the two ({@link TrimStatsPanel},
 * which the backpack screen uses as well). A hovered empty head slot names what fits.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {

    @Unique
    private final TrimStatsPanel trimStats = new TrimStatsPanel();

    public InventoryScreenMixin(InventoryMenu screenHandler, Inventory playerInventory, Component text) {
        super(screenHandler, playerInventory, text);
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void simplebuilding$extraHeadFrame(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!com.simplebuilding.framework.api.ContainerStyleHints.isRestyled(this)) {
            TrimStatsPanel.slotFrame(context, this.leftPos + TrimStatsLayout.HEAD_SLOT_X - 1, this.topPos + TrimStatsLayout.HEAD_SLOT_Y - 1);
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void renderTrimStats(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        this.trimStats.render(context, this.font, this.minecraft, this.leftPos + TrimStatsLayout.INVENTORY_X,
                this.topPos + TrimStatsLayout.INVENTORY_Y, mouseX, mouseY);
        if (this.hoveredSlot instanceof ExtraHead.HeadSlot && !this.hoveredSlot.hasItem() && this.menu.getCarried().isEmpty()) {
            context.setTooltipForNextFrame(this.font, Component.translatable("gui.simplebuilding.extra_head_slot"), mouseX, mouseY);
        }
    }
}
