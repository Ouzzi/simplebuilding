package com.simplebuilding.mixin;

import com.simplebuilding.util.PlacedBundles;
import com.simplebuilding.util.PlacedTemplates;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Schleichen + Rechtsklick mit einem Kleinteil oder Ei legt es auf den Boden, bis zu vier auf einen Fleck
 * ({@link com.simplebuilding.util.PlacedSmallParts}), mit einer Schmiedevorlage oder Blaupause legt sie ab ({@link PlacedTemplates}),
 * mit einem Vanilla-Buendel auf eine Oberseite stellt es ab ({@link PlacedBundles}). Am
 * Basis-{@code Item#useOn}, weil weder {@code SmithingTemplateItem} noch die schlichten
 * Aufwertungsvorlagen der Mod es ueberschreiben; Items mit eigenem {@code useOn} erreicht der Haken
 * nie, und fuer alle anderen Items liefert {@code tryPlace} sofort null.
 */
@Mixin(Item.class)
public abstract class ItemUseOnMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$placeTemplate(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        // Kleinteile und Eier auf den Boden (bis zu 4 auf einem Fleck) vor den Vorlagen; an Wand und Decke legen sich
        // Kleinteile weiter wie eine Vorlage ab.
        InteractionResult result = com.simplebuilding.util.PlacedSmallParts.tryPlace(context);
        if (result == null) {
            result = PlacedTemplates.tryPlace(context);
        }
        if (result == null) {
            // Vanilla-Buendel (alle Farben); die Buendel der Mod fragen in ReinforcedBundleItem#useOn selbst.
            result = PlacedBundles.tryPlace(context);
        }
        if (result != null) {
            cir.setReturnValue(result);
        }
    }
}
