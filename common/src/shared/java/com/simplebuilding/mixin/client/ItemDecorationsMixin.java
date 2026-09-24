package com.simplebuilding.mixin.client;

import com.simplebuilding.client.render.OreDetectorGlint;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Haengt den Randschimmer des Erzdetektors ({@link OreDetectorGlint}) an Vanillas
 * Item-Dekorationen. Dieselbe Methode zeichnet Schnellleiste, Inventar, Kreativinventar und
 * Container; NeoForge ruft darin zusaetzlich seine Item-Decorators auf, {@code RETURN} liegt auf
 * jedem Loader hinter allem.
 */
@Mixin(GuiGraphicsExtractor.class)
public class ItemDecorationsMixin {

    @Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At("RETURN"))
    private void simplebuilding$oreDetectorGlint(Font font, ItemStack stack, int x, int y, @Nullable String countText, CallbackInfo ci) {
        if (!stack.isEmpty()) {
            OreDetectorGlint.render((GuiGraphicsExtractor) (Object) this, stack, x, y);
        }
    }
}
