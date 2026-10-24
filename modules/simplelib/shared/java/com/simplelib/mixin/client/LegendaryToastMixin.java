package com.simplelib.mixin.client;

import com.simplelib.api.LegendaryMoments;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * W24-3 (owner 23.10.): a legendary advancement plays SimpleLib's choir, so Vanilla's challenge jingle stays silent
 * ({@code null} = no toast sound, ToastManager skips it). Switch {@code legendaryMuteToast}, see
 * {@link LegendaryMoments#muteToast}.
 */
@Mixin(AdvancementToast.class)
public abstract class LegendaryToastMixin {
    @Shadow @Final private AdvancementHolder advancement;

    @Inject(method = "getSoundEvent", at = @At("HEAD"), cancellable = true)
    private void simplelib$muteLegendary(CallbackInfoReturnable<SoundEvent> cir) {
        if (LegendaryMoments.muteToast(this.advancement)) cir.setReturnValue(null);
    }
}
