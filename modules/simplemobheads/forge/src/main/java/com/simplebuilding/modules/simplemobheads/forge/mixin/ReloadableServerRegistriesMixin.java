package com.simplebuilding.modules.simplemobheads.forge.mixin;

import com.simplemobheads.drop.HeadDrops;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Forge: the head pools go into charged_creeper once per reload, after holders bind and before loot validation. */
@Mixin(ReloadableServerRegistries.class)
public abstract class ReloadableServerRegistriesMixin {
    @Inject(method = "createAndValidateFullContext", at = @At("HEAD"))
    private static void simplemobheads$injectHeadPools(LayeredRegistryAccess<RegistryLayer> layers, HolderLookup.Provider context,
                                                      RegistryAccess.Frozen loaded, CallbackInfoReturnable<ReloadableServerRegistries.LoadResult> cir) {
        var lookup = HolderLookup.Provider.create(java.util.stream.Stream.concat(context.listRegistries(), loaded.listRegistries()));
        loaded.lookupOrThrow(Registries.LOOT_TABLE).listElements().forEach(holder ->
                HeadDrops.apply(holder.key(), pool -> holder.value().addPool(pool.build()), lookup));
    }
}
