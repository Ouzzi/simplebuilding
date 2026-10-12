package com.simplelib.mixin;

import com.simplelib.api.IdMoves;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerAdvancementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Moved advancements ({@link IdMoves}, table {@code minecraft:advancement}): a lookup of an id that does not exist
 * answers with the advancement under its new id. Player progress files are read through this lookup
 * ({@code PlayerAdvancements#applyFrom}), so old progress lands on the moved advancement and is saved under its new
 * id; commands naming the old id work too. Existing ids are untouched.
 */
@Mixin(ServerAdvancementManager.class)
public abstract class IdMovesAdvancementsMixin {

    @Inject(method = "get(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/advancements/AdvancementHolder;", at = @At("RETURN"), cancellable = true)
    private void simplelib$moveAdvancement(Identifier id, CallbackInfoReturnable<AdvancementHolder> cir) {
        if (cir.getReturnValue() != null) return;
        Identifier now = IdMoves.advancement(id);
        if (now != null) cir.setReturnValue(((ServerAdvancementManager) (Object) this).get(now));
    }
}
