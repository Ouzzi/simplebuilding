package com.simplebuilding.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 26.3 (split stage 7): the area break is Simple Hammers' ({@link com.simplehammers.engine.SledgehammerUsageEvent}, its
 * own loader events). SimpleBuilding's break listener keeps the protection part it always had - a claim probe is no
 * break, a protected block is not broken - and then hands over to the module; the second call for the same break (the
 * module's own listener) finds the area already broken and does nothing. The 26.2 twin (common/src/mc26_2/java) is
 * SimpleBuilding's own area break.
 */
public final class SledgehammerUsageEvent extends com.simplehammers.engine.SledgehammerUsageEvent {
    private SledgehammerUsageEvent() {
    }

    public static boolean handleBeforeBlockBreak(Level world, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (BuildPermissions.isProbing()) return true;
        if (!com.simplebuilding.api.WorldPermissions.mayChange(world, player, pos, com.simplebuilding.api.WorldPermissions.Action.DESTROY)) return false;
        return com.simplehammers.engine.SledgehammerUsageEvent.handleBeforeBlockBreak(world, player, pos, state, blockEntity);
    }
}
