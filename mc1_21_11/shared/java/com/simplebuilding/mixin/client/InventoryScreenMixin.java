package com.simplebuilding.mixin.client;

import com.simplebuilding.client.gui.TrimStatsPanel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * Knopf und Panel der Besatz-Multiplikatoren im normalen Inventar (E). Die Logik steckt in
 * {@link TrimStatsPanel}, das auch der Rucksack-Bildschirm benutzt.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {

    @Unique
    private final TrimStatsPanel trimStats = new TrimStatsPanel();

    public InventoryScreenMixin(InventoryMenu screenHandler, Inventory playerInventory, Component text) {
        super(screenHandler, playerInventory, text);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void initTrimUI(CallbackInfo ci) {
        // Button toggelt die Sichtbarkeit der Stats
        this.addRenderableWidget(this.trimStats.createButton(this.leftPos, this.topPos));
    }

    // MC 1.21.11: Gegenstueck zum 26.2-Hook InventoryScreen#extractRenderState ist hier
    // InventoryScreen#render(GuiGraphics,int,int,float) - dieselbe Stelle in der Zeichenkette.
    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("TAIL"))
    private void renderTrimStats(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        this.trimStats.render(context, this.font, this.minecraft, this.leftPos, this.topPos, mouseX, mouseY);
    }
}
