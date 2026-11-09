package com.simplegadgets.mixin.client;

import com.simplegadgets.client.DetectorGlint;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Haengt den Randschimmer des Detectors ({@link DetectorGlint}; G3: aus SimpleBuildings ItemDecorationsMixin) an Vanillas
 * Item-Dekorationen. Dieselbe Methode zeichnet Schnellleiste, Inventar, Kreativinventar und
 * Container; NeoForge ruft darin zusaetzlich seine Item-Decorators auf, {@code RETURN} liegt auf
 * jedem Loader hinter allem.
 */
@Mixin(GuiGraphicsExtractor.class)
public class DetectorGlintMixin {

    @Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At("RETURN"))
    private void simplegadgets$detectorGlint(Font font, ItemStack stack, int x, int y, @Nullable String countText, CallbackInfo ci) {
        if (!stack.isEmpty()) {
            DetectorGlint.render((GuiGraphicsExtractor) (Object) this, stack, x, y);
        }
    }
}
