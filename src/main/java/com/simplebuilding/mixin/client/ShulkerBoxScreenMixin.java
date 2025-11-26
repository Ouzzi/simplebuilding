package com.simplebuilding.mixin.client;

import com.simplebuilding.util.IEnchantableShulkerBox;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ShulkerBoxScreen.class)
public abstract class ShulkerBoxScreenMixin extends HandledScreen<ShulkerBoxScreenHandler> {

    public ShulkerBoxScreenMixin(ShulkerBoxScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderEnchantmentTooltip(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int titleX = this.x + 8;
        int titleY = this.y + 6;
        // Wir schätzen die Breite, da getWidth() oft private ist oder wir nutzen eine feste Box
        if (mouseX >= titleX && mouseX <= titleX + 100 && mouseY >= titleY && mouseY <= titleY + 12) {

            // Wir nutzen Reflection oder einen einfachen Cast, falls möglich.
            // Da das Inventar im Handler meist private ist, ist ein Accessor am besten.
            // ABER für den schnellen Test: Wir greifen auf das Inventar des Screens zu, falls möglich? Nein.

            // Wir nutzen den Accessor, den wir vorher erstellt hatten (ShulkerBoxScreenHandlerAccessor).
            // Falls du den gelöscht hast, hier eine einfachere Variante ohne Accessor:
            // Wir rendern einfach, wenn das GUI offen ist und die BE Daten hat.
            // Da wir Client-Side schwer an die BE im Handler kommen ohne Accessor:

            // HIER: Nutze bitte den Accessor aus dem vorherigen Schritt. Er ist notwendig.
            try {
                Inventory inv = ((ShulkerBoxScreenHandlerAccessor)this.handler).getInventory();
                if (inv instanceof IEnchantableShulkerBox box) {
                    ItemEnchantmentsComponent enchants = box.simplebuilding$getEnchantments();
                    if (enchants != null && !enchants.isEmpty()) {
                        List<Text> lines = new ArrayList<>();
                        lines.add(Text.translatable("container.enchantments").formatted(Formatting.GOLD));
                        enchants.getEnchantmentEntries().forEach(e ->
                                lines.add(Enchantment.getName(e.getKey(), e.getIntValue()).copy().formatted(Formatting.GRAY))
                        );
                        context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
                    }
                }
            } catch (Exception e) {
                // Fallback, falls Accessor fehlt
            }
        }
    }
}