package com.simplebuilding.modules.simpletweaks.admin.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.simplebuilding.modules.simpletweaks.admin.blocklog.BlockRecorder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/**
 * Block log (F): a block burned away or turned into fire. Wraps the two calls in {@code checkBurnOut} (the method itself has
 * loader-specific parameters on NeoForge/Forge, the calls are the same everywhere).
 */
@Mixin(FireBlock.class)
public abstract class AdminFireMixin {
    @WrapOperation(method = "checkBurnOut", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean simpletweaks$burnAway(Level level, BlockPos pos, boolean moving, Operation<Boolean> original) {
        String[] before = level instanceof ServerLevel s && BlockRecorder.fire() ? BlockRecorder.worldBefore(s, pos) : null;
        boolean done = original.call(level, pos, moving);
        if (before != null && done) BlockRecorder.worldBroke((ServerLevel) level, "#fire", 'F', pos, before);
        return done;
    }

    @WrapOperation(method = "checkBurnOut", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean simpletweaks$burnToFire(Level level, BlockPos pos, BlockState fire, Operation<Boolean> original) {
        String[] before = level instanceof ServerLevel s && BlockRecorder.fire() ? BlockRecorder.worldBefore(s, pos) : null;
        boolean done = original.call(level, pos, fire);
        if (before != null && done) BlockRecorder.worldBroke((ServerLevel) level, "#fire", 'F', pos, before);
        return done;
    }
}
