package com.simplebuilding.modules.simpletweaks.mixin.claims;

import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=DropperBlock.class,priority=1100)
public abstract class ClaimDropperMixin {
    @Inject(method="dispenseFrom",at=@At("HEAD"),cancellable=true)
    private void claims$drop(ServerLevel level,BlockState state,BlockPos pos,CallbackInfo ci){
        if(!Claims.transfer(level,pos,pos.relative(state.getValue(DispenserBlock.FACING))))ci.cancel();
    }
}
