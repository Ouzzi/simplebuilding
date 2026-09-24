package com.simplebuilding.mixin.forge;

import com.simplebuilding.forge.ForgeLootEvents;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forges LootTableLoadEvent liefert keinen Registry-Zugriff, die gemeinsamen Loot-Anpassungen brauchen
 * ihn aber (Verzauberungen der Buecher). Die Loot-Tabellen werden in genau diesem Aufruf geladen - der
 * Zugriff fuer die Ladeebene wird vorher fuer ForgeLootEvents hinterlegt.
 */
@Mixin(ReloadableServerRegistries.class)
public abstract class ReloadableServerRegistriesMixin {
    @Inject(method = "reload", at = @At("HEAD"))
    private static void simplebuilding$rememberLootContext(LayeredRegistryAccess<RegistryLayer> context,
            List<Registry.PendingTags<?>> updatedContextTags, ResourceManager manager, Executor executor,
            CallbackInfoReturnable<CompletableFuture<ReloadableServerRegistries.LoadResult>> cir) {
        ForgeLootEvents.setLoadingRegistries(context.getAccessForLoading(RegistryLayer.RELOADABLE));
    }
}
