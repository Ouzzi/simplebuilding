package com.simplebuilding.modules.simplestats.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simplebuilding.modules.simplestats.StatsRuntime;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/** A suspicious block that is brushed away completely counts for the brush. */
@Mixin(BrushableBlockEntity.class)
public abstract class BrushableBlockEntityMixin {
    @WrapMethod(method = "brush")
    private boolean simplestats$brushed(long gameTime, ServerLevel level, LivingEntity user, Direction direction, ItemStack brush, Operation<Boolean> original) {
        BlockState before = ((BrushableBlockEntity) (Object) this).getBlockState();
        boolean done = original.call(gameTime, level, user, direction, brush);
        if (done && user instanceof ServerPlayer player) StatsRuntime.onBrushed(player, brush, before);
        return done;
    }
}
