package com.simpletrims.mixin;

import com.simpletrims.placed.PlacedTemplates;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sneak + use with a trim template places it ({@link PlacedTemplates#tryPlace}); templates do not override {@code useOn}. */
@Mixin(Item.class)
public abstract class ItemUseOnMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void simpletrims$placeTemplate(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = PlacedTemplates.tryPlace(context);
        if (result != null) cir.setReturnValue(result);
    }
}
