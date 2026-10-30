package com.simplelib.mixin;

import com.simplelib.api.CreativeTabPolicy;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * IG31-3: after Vanilla built the contents of every creative tab (and after Fabric's own pagination, which injects at the same
 * place with the default priority - ours is later) the Simple mods' tab rules apply: {@link CreativeTabPolicy}.
 */
@Mixin(value = CreativeModeTabs.class, priority = 2000)
public abstract class CreativeModeTabsPolicyMixin {
    @Inject(method = "buildAllTabContents", at = @At("TAIL"))
    private static void simplelib$tabPolicy(CreativeModeTab.ItemDisplayParameters parameters, CallbackInfo ci) {
        CreativeTabPolicy.apply();
    }
}
