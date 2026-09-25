package com.simplebuilding.mixin.client;

import com.simplebuilding.client.BackpackKeyHandler;
import com.simplebuilding.client.gui.tooltip.ReinforcedBundleTooltipSubmenuHandler;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin extends Screen {

    protected HandledScreenMixin(Component title) {
        super(title);
    }

    // Zugriff auf die geschützte Methode in AbstractContainerScreen, um Handler hinzuzufügen
    @Shadow
    protected abstract void addItemSlotMouseAction(ItemSlotMouseAction handler);

    /**
     * Wir injizieren uns in die 'init' Methode.
     * Dort fügt Vanilla seinen Bundle-Handler hinzu. Wir fügen einfach unseren dazu.
     * Minecraft geht dann beim Scrollen die Liste durch und prüft 'isApplicableTo'.
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void simplebuilding$addCustomBundleHandler(CallbackInfo ci) {
        if (this.minecraft != null) {
            this.addItemSlotMouseAction(new ReinforcedBundleTooltipSubmenuHandler(this.minecraft));
        }
    }

    /**
     * Die Rucksack-Taste schliesst, was sie geoeffnet hat (Rucksack, Inventar, Kreativ-Inventar), so wie E
     * das Inventar schliesst. Am Methodenanfang statt an E's {@code matches}-Aufruf: NeoForge ersetzt den
     * durch {@code isActiveAndMatches}. Hier kommt ein Tastendruck ohnehin nur an, wenn die Unterklasse ihn
     * nicht selbst braucht (Suchfeld des Kreativ-Inventars, Rezeptbuch-Suche) - dieselbe Lage, in der E
     * schliesst. Frueher schloss B nur den {@code BackpackScreen}; ohne getragenen Rucksack oeffnet B aber das
     * Vanilla-Inventar, und das blieb offen (Besitzer-Befund 2026-09-25).
     */
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$backpackKeyCloses(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (BackpackKeyHandler.closesOnBackpackKey(this, event)) {
            this.onClose();
            cir.setReturnValue(true);
        }
    }
}