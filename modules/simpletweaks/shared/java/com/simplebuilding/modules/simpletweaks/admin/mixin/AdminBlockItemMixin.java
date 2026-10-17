package com.simplebuilding.modules.simpletweaks.admin.mixin;

import com.simplebuilding.modules.simpletweaks.admin.blocklog.BlockRecorder;
import net.minecraft.server.level.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Block log: a player placed a block. */
@Mixin(BlockItem.class)
public abstract class AdminBlockItemMixin {
    @Inject(method = "placeBlock", at = @At("RETURN"))
    private void simpletweaks$placed(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && context.getLevel() instanceof ServerLevel level && context.getPlayer() instanceof ServerPlayer player)
            BlockRecorder.placed(level, player, context.getClickedPos());
    }
}
