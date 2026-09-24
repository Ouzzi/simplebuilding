package com.simplebuilding.mixin;

import com.simplebuilding.util.ShearsWoolInteraction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Schere auf Wolle ergibt Faeden ({@link ShearsWoolInteraction}); alles andere bleibt Vanilla. */
@Mixin(ShearsItem.class)
public abstract class ShearsItemMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$shearWool(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = ShearsWoolInteraction.tryShearWool(context);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }
}
