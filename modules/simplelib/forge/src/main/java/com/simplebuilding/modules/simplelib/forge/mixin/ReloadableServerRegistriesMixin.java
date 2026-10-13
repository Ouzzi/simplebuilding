package com.simplebuilding.modules.simplelib.forge.mixin;

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

/**
 * Forge: SimpleLib's material loot ({@code com.simplelib.material.MaterialLoot}) once per reload, after the holders bind
 * and before loot validation (Forge's LootTableLoadEvent comes too early) - the pattern of Simple Maps.
 */
@Mixin(ReloadableServerRegistries.class)
public abstract class ReloadableServerRegistriesMixin {
    @Inject(method = "createAndValidateFullContext", at = @At("HEAD"))
    private static void simplelib$injectMaterialLoot(LayeredRegistryAccess<RegistryLayer> layers,
            HolderLookup.Provider context, RegistryAccess.Frozen loaded,
            CallbackInfoReturnable<ReloadableServerRegistries.LoadResult> cir) {
        var lookup = HolderLookup.Provider.create(java.util.stream.Stream.concat(context.listRegistries(), loaded.listRegistries()));
        loaded.lookupOrThrow(Registries.LOOT_TABLE).listElements()
                .forEach(holder -> com.simplelib.material.MaterialLoot.apply(holder.key(), pool -> holder.value().addPool(pool.build()), lookup));
    }
}
