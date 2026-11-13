package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.DeceiverEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Audit simplemobs P2-5: the Deceiver's illusions are Vanilla mobs tagged {@link DeceiverEntity#TAG_FAKE} (kept loaded with
 * {@code setPersistenceRequired}); the list that clears them lives only in memory, so a saved fake would stand forever after an
 * unload or restart. Fakes are never written to the chunk: they vanish with their chunk like the {@code DisguiseMarker}.
 * Not tied to the sub-mod switch, so fakes from before a switch-off are not saved either.
 */
@Mixin(Entity.class)
public abstract class DeceiverFakeSaveMixin {
    @Inject(method = "shouldBeSaved", at = @At("HEAD"), cancellable = true)
    private void simplemobs$neverSaveFakes(CallbackInfoReturnable<Boolean> cir) {
        if (((Entity) (Object) this).entityTags().contains(DeceiverEntity.TAG_FAKE)) cir.setReturnValue(false);
    }
}
