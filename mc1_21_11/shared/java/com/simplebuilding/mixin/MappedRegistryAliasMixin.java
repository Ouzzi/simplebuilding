package com.simplebuilding.mixin;

import com.simplebuilding.datafix.LegacyItemIds;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Old item ids of renamed items resolve to the renamed item, see {@link LegacyItemIds}. Only a
 * lookup that missed is redirected, and only in the item registry; everything else returns
 * exactly what vanilla returned. {@code DefaultedMappedRegistry#getValue} calls
 * {@code super.getValue} first, so the item registry's air fallback comes after the alias.
 * Forge replaces the built-in registries with its own {@code NamespacedWrapper}, which overrides
 * these methods; {@code NamespacedWrapperAliasMixin} does the same there.
 */
@Mixin(MappedRegistry.class)
public abstract class MappedRegistryAliasMixin<T> {

    @Inject(method = "get(Lnet/minecraft/resources/Identifier;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$aliasById(Identifier id, CallbackInfoReturnable<Optional<Holder.Reference<T>>> cir) {
        if (cir.getReturnValue().isPresent()) return;
        Identifier now = LegacyItemIds.renamedIn(this, id);
        if (now != null) {
            cir.setReturnValue(this.simplebuilding$lookup(now));
        }
    }

    @Inject(method = "get(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$aliasByKey(ResourceKey<T> key, CallbackInfoReturnable<Optional<Holder.Reference<T>>> cir) {
        if (cir.getReturnValue().isPresent() || key == null) return;
        Identifier now = LegacyItemIds.renamedIn(this, key.identifier());
        if (now != null) {
            cir.setReturnValue(this.simplebuilding$lookup(now));
        }
    }

    @Inject(method = "getValue(Lnet/minecraft/resources/Identifier;)Ljava/lang/Object;", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$aliasValueById(Identifier id, CallbackInfoReturnable<T> cir) {
        if (cir.getReturnValue() != null) return;
        Identifier now = LegacyItemIds.renamedIn(this, id);
        if (now != null) {
            this.simplebuilding$lookup(now).ifPresent(holder -> cir.setReturnValue(holder.value()));
        }
    }

    @Inject(method = "getValue(Lnet/minecraft/resources/ResourceKey;)Ljava/lang/Object;", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$aliasValueByKey(ResourceKey<T> key, CallbackInfoReturnable<T> cir) {
        if (cir.getReturnValue() != null || key == null) return;
        Identifier now = LegacyItemIds.renamedIn(this, key.identifier());
        if (now != null) {
            this.simplebuilding$lookup(now).ifPresent(holder -> cir.setReturnValue(holder.value()));
        }
    }

    @Inject(method = "containsKey(Lnet/minecraft/resources/Identifier;)Z", at = @At("RETURN"), cancellable = true)
    private void simplebuilding$aliasContains(Identifier id, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        Identifier now = LegacyItemIds.renamedIn(this, id);
        if (now != null && this.simplebuilding$lookup(now).isPresent()) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    @SuppressWarnings("unchecked")
    private Optional<Holder.Reference<T>> simplebuilding$lookup(Identifier now) {
        // No recursion: new ids are never keys of LegacyItemIds.RENAMED, so this lookup is not redirected again.
        return ((MappedRegistry<T>) (Object) this).get(now);
    }
}
