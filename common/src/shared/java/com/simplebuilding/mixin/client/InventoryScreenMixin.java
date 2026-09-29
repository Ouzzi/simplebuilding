package com.simplebuilding.mixin.client;

import com.simplebuilding.client.gui.TrimStatsPanel;
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
 * Das Resonanz-Feld der Besatz-Boni im normalen Inventar (E), seit 2026-09-29 ohne Knopf. Die Logik steckt in
 * {@link TrimStatsPanel}, das auch der Rucksack-Bildschirm benutzt.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {

    @Unique
    private final TrimStatsPanel trimStats = new TrimStatsPanel();

    public InventoryScreenMixin(InventoryMenu screenHandler, Inventory playerInventory, Component text) {
        super(screenHandler, playerInventory, text);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void renderTrimStats(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        this.trimStats.render(context, this.font, this.minecraft, this.leftPos, this.topPos, mouseX, mouseY);
    }
}
