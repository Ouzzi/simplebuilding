package com.simplemobheads.mixin;

import com.simplemobheads.helmet.HeadHelmets;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** BS28b-1: a head that is broken or replaced drops the helmet it wears (Vanilla skull block entities have no contents to drop). */
@Mixin(BlockEntity.class)
public abstract class HeadHelmetDropMixin {
    @Inject(method = "preRemoveSideEffects", at = @At("HEAD"))
    private void simplemobheads$dropHelmet(BlockPos pos, BlockState state, CallbackInfo ci) {
        if ((Object) this instanceof SkullBlockEntity skull) {
            HeadHelmets.dropFrom(skull, pos);
        }
    }
}
