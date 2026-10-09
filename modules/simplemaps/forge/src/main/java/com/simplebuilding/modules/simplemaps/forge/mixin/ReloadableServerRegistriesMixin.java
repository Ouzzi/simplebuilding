package com.simplebuilding.modules.simplemaps.forge.mixin;

import com.simplebuilding.modules.simplemaps.forge.ModuleLoot;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Inject once per reload, after holders bind and before loot validation; no global loading context. */
@Mixin(ReloadableServerRegistries.class)
public abstract class ReloadableServerRegistriesMixin {
    @Inject(method = "createAndValidateFullContext", at = @At("HEAD"))
    private static void simplemaps$injectLoadedLoot(LayeredRegistryAccess<RegistryLayer> layers,
            HolderLookup.Provider context, RegistryAccess.Frozen loaded,
            CallbackInfoReturnable<ReloadableServerRegistries.LoadResult> cir) {
        ModuleLoot.inject(context, loaded);
    }
}
