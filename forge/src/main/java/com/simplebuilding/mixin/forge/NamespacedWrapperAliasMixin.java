package com.simplebuilding.mixin.forge;

import com.simplebuilding.datafix.LegacyItemIds;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forge twin of {@code MappedRegistryAliasMixin}: Forge wraps the built-in registries in its
 * package-private {@code NamespacedWrapper}, which overrides the lookups without calling
 * {@code super}, so the vanilla mixin never sees them. Same rule: only a missed lookup in the
 * item or block registry is redirected to its renamed entry, see {@link LegacyItemIds}.
 */
@Mixin(targets = "net.minecraftforge.registries.NamespacedWrapper")
public abstract class NamespacedWrapperAliasMixin<T> {

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

    @Unique
    @SuppressWarnings("unchecked")
    private Optional<Holder.Reference<T>> simplebuilding$lookup(Identifier now) {
        // No recursion: new ids are never keys of the alias tables.
        return ((Registry<T>) (Object) this).get(now);
    }
}
