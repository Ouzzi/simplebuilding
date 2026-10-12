package com.simplelib.mixin;

import com.simplelib.api.IdMoves;
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
 * Moved ids resolve to their new entries, see {@link IdMoves}. Only a lookup that missed is redirected; everything
 * else returns exactly what vanilla returned. {@code DefaultedMappedRegistry#getValue} calls {@code super.getValue}
 * first, so the item registry's air fallback comes after the move. Forge wraps the built-in registries in its own
 * {@code NamespacedWrapper}, which overrides these methods; {@code NamespacedWrapperIdMovesMixin} does the same there.
 */
@Mixin(MappedRegistry.class)
public abstract class IdMovesRegistryMixin<T> {

    @Inject(method = "get(Lnet/minecraft/resources/Identifier;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private void simplelib$moveById(Identifier id, CallbackInfoReturnable<Optional<Holder.Reference<T>>> cir) {
        if (cir.getReturnValue().isPresent()) return;
        Identifier now = IdMoves.targetIn(this, id);
        if (now != null) cir.setReturnValue(this.simplelib$lookup(now));
    }

    @Inject(method = "get(Lnet/minecraft/resources/ResourceKey;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private void simplelib$moveByKey(ResourceKey<T> key, CallbackInfoReturnable<Optional<Holder.Reference<T>>> cir) {
        if (cir.getReturnValue().isPresent() || key == null) return;
        Identifier now = IdMoves.targetIn(this, key.identifier());
        if (now != null) cir.setReturnValue(this.simplelib$lookup(now));
    }

    @Inject(method = "getValue(Lnet/minecraft/resources/Identifier;)Ljava/lang/Object;", at = @At("RETURN"), cancellable = true)
    private void simplelib$moveValueById(Identifier id, CallbackInfoReturnable<T> cir) {
        if (cir.getReturnValue() != null) return;
        Identifier now = IdMoves.targetIn(this, id);
        if (now != null) this.simplelib$lookup(now).ifPresent(holder -> cir.setReturnValue(holder.value()));
    }

    @Inject(method = "getValue(Lnet/minecraft/resources/ResourceKey;)Ljava/lang/Object;", at = @At("RETURN"), cancellable = true)
    private void simplelib$moveValueByKey(ResourceKey<T> key, CallbackInfoReturnable<T> cir) {
        if (cir.getReturnValue() != null || key == null) return;
        Identifier now = IdMoves.targetIn(this, key.identifier());
        if (now != null) this.simplelib$lookup(now).ifPresent(holder -> cir.setReturnValue(holder.value()));
    }

    @Inject(method = "containsKey(Lnet/minecraft/resources/Identifier;)Z", at = @At("RETURN"), cancellable = true)
    private void simplelib$moveContains(Identifier id, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        Identifier now = IdMoves.targetIn(this, id);
        if (now != null && this.simplelib$lookup(now).isPresent()) cir.setReturnValue(true);
    }

    @Unique
    @SuppressWarnings("unchecked")
    private Optional<Holder.Reference<T>> simplelib$lookup(Identifier now) {
        // A miss here may move once more along a chain; IdMoves.move refuses loops, so this ends.
        return ((MappedRegistry<T>) (Object) this).get(now);
    }
}
