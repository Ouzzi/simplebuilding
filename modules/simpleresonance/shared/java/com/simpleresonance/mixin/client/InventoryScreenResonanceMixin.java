package com.simpleresonance.mixin.client;

import com.simpleresonance.client.ResonanceLayout;
import com.simpleresonance.client.ResonancePanel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The resonance button in the normal inventory (E), right of the player model (MOB27-3 R2; before in SimpleBuilding's
 * {@code InventoryScreenMixin}). The spot is free in Vanilla, so Simple Resonance shows it on its own as well.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenResonanceMixin extends AbstractContainerScreen<InventoryMenu> {
    public InventoryScreenResonanceMixin(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void simpleresonance$button(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ResonancePanel.render(graphics, this.font, this.minecraft, this.leftPos + ResonanceLayout.INVENTORY_X,
                this.topPos + ResonanceLayout.INVENTORY_Y, mouseX, mouseY);
    }
}
