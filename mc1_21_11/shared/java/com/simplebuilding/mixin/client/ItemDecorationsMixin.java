package com.simplebuilding.mixin.client;

import com.simplebuilding.client.render.OreDetectorGlint;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Haengt den Randschimmer des Erzdetektors ({@link OreDetectorGlint}) an Vanillas
 * Item-Dekorationen. MC 1.21.11: die Methode heisst noch {@code GuiGraphics#renderItemDecorations}
 * (ab 26.2 {@code GuiGraphicsExtractor#itemDecorations}); NeoForge ruft darin zusaetzlich seine
 * Item-Decorators auf, {@code RETURN} liegt auf jedem Loader hinter allem.
 */
@Mixin(GuiGraphics.class)
public class ItemDecorationsMixin {

    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At("RETURN"))
    private void simplebuilding$oreDetectorGlint(Font font, ItemStack stack, int x, int y, @Nullable String countText, CallbackInfo ci) {
        if (!stack.isEmpty()) {
            OreDetectorGlint.render((GuiGraphics) (Object) this, stack, x, y);
        }
    }
}
