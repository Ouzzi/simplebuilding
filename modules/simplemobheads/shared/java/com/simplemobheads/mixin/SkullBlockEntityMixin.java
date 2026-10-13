package com.simplemobheads.mixin;

import com.mojang.serialization.DataResult;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A placed head keeps its variant ({@code simplemobheads:variant}) like every other item component Vanilla does not read
 * itself: in {@code BlockEntity#components}, saved with the chunk and copied back into the drop. Vanilla's update tag of a
 * skull is {@code saveCustomOnly} - without the components - so the client would draw the base look. This adds them; the
 * client loads update tags through {@code loadWithComponents} (Vanilla and NeoForge).
 */
@Mixin(SkullBlockEntity.class)
public abstract class SkullBlockEntityMixin {
    @Inject(method = "getUpdateTag", at = @At("RETURN"))
    private void simplemobheads$sendComponents(HolderLookup.Provider registries, CallbackInfoReturnable<CompoundTag> cir) {
        DataComponentMap components = ((SkullBlockEntity) (Object) this).components();
        if (components.isEmpty()) {
            return;
        }
        DataResult<Tag> encoded = DataComponentMap.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), components);
        encoded.result().ifPresent(tag -> cir.getReturnValue().put("components", tag));
    }
}
