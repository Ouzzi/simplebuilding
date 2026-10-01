package com.simplebuilding.modules.simpletweaks.mixin.claims;

import com.simplebuilding.modules.simpletweaks.claims.ClaimAutomation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The permission preview sees the same head-free world as the real retraction resolver. */
@Mixin(PistonStructureResolver.class)
public abstract class ClaimPistonResolverMixin {
    @WrapOperation(method={"resolve","addBlockLine","addBranchingBlocks"},at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState claims$preview(Level level,BlockPos pos,Operation<BlockState> original){
        return pos.equals(ClaimAutomation.RETRACTION_HEAD.get())?Blocks.AIR.defaultBlockState():original.call(level,pos);
    }
}
