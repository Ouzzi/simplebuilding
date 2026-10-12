package com.simplebuilding.modules.simplelib.forge.mixin;

import com.simplelib.api.IdMoves;
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
 * Forge twin of {@code IdMovesRegistryMixin}: Forge wraps the built-in registries in its package-private
 * {@code NamespacedWrapper}, which overrides the lookups without calling {@code super}, so the vanilla mixin never sees
 * them. Same rule: only a missed lookup is redirected, see {@link IdMoves}. The defaulted wrapper (items) answers
 * {@code getValue}/{@code containsKey} straight from the ForgeRegistry; {@code SimpleLibForge} adds the explicit moves
 * there as Forge registry aliases.
 */
@Mixin(targets = "net.minecraftforge.registries.NamespacedWrapper")
public abstract class NamespacedWrapperIdMovesMixin<T> {

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

    @Unique
    @SuppressWarnings("unchecked")
    private Optional<Holder.Reference<T>> simplelib$lookup(Identifier now) {
        // A miss here may move once more along a chain; IdMoves.move refuses loops, so this ends.
        return ((Registry<T>) (Object) this).get(now);
    }
}
