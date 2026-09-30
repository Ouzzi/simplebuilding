package com.simplebuilding.modules.simpletweaks.mixin;

import com.simplebuilding.modules.simpletweaks.LegacyAliases;
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

/** Resolves only missing legacy names through public SimpleBuilding registry IDs. */
@Mixin(MappedRegistry.class)
public abstract class LegacyRegistryMixin<T> {

    @Inject(method = "get(Lnet/minecraft/resources/Identifier;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private void simpletweaks$aliasById(Identifier id, CallbackInfoReturnable<Optional<Holder.Reference<T>>> cir) {
        if (cir.getReturnValue().isPresent()) return;
        Identifier now = LegacyAliases.target(this, id);
        if (now != null) {
            cir.setReturnValue(this.simpletweaks$lookup(now));
        }
    }

    @Inject(method = "get(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private void simpletweaks$aliasByKey(ResourceKey<T> key, CallbackInfoReturnable<Optional<Holder.Reference<T>>> cir) {
        if (cir.getReturnValue().isPresent() || key == null) return;
        Identifier now = LegacyAliases.target(this, key.identifier());
        if (now != null) {
            cir.setReturnValue(this.simpletweaks$lookup(now));
        }
    }

    @Inject(method = "getValue(Lnet/minecraft/resources/Identifier;)Ljava/lang/Object;", at = @At("RETURN"), cancellable = true)
    private void simpletweaks$aliasValueById(Identifier id, CallbackInfoReturnable<T> cir) {
        if (cir.getReturnValue() != null) return;
        Identifier now = LegacyAliases.target(this, id);
        if (now != null) {
            this.simpletweaks$lookup(now).ifPresent(holder -> cir.setReturnValue(holder.value()));
        }
    }

    @Inject(method = "getValue(Lnet/minecraft/resources/ResourceKey;)Ljava/lang/Object;", at = @At("RETURN"), cancellable = true)
    private void simpletweaks$aliasValueByKey(ResourceKey<T> key, CallbackInfoReturnable<T> cir) {
        if (cir.getReturnValue() != null || key == null) return;
        Identifier now = LegacyAliases.target(this, key.identifier());
        if (now != null) {
            this.simpletweaks$lookup(now).ifPresent(holder -> cir.setReturnValue(holder.value()));
        }
    }

    @Inject(method = "containsKey(Lnet/minecraft/resources/Identifier;)Z", at = @At("RETURN"), cancellable = true)
    private void simpletweaks$aliasContains(Identifier id, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        Identifier now = LegacyAliases.target(this, id);
        if (now != null && this.simpletweaks$lookup(now).isPresent()) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    @SuppressWarnings("unchecked")
    private Optional<Holder.Reference<T>> simpletweaks$lookup(Identifier now) {
        // Targets have another namespace; aliases cannot recurse.
        return ((MappedRegistry<T>) (Object) this).get(now);
    }
}
