package com.simplebuilding.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 26.3 (split stage 7): the area break is Simple Hammers' (its own loader events); SimpleBuilding's break listener keeps
 * only the protection part it always had - a claim probe is no break, a protected block is not broken. The 26.2 twin
 * (common/src/mc26_2/java) also breaks the hammer's area.
 */
public final class SledgehammerUsageEvent {
    private SledgehammerUsageEvent() {
    }

    public static boolean handleBeforeBlockBreak(Level world, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (BuildPermissions.isProbing()) return true;
        return com.simplebuilding.api.WorldPermissions.mayChange(world, player, pos, com.simplebuilding.api.WorldPermissions.Action.DESTROY);
    }
}
