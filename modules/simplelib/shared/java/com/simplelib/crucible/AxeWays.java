package com.simplelib.crucible;

import com.simplelib.api.SimpleLibApi;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Vanilla axe ways on SimpleLib's own blocks (principle 5a): a right click with an axe and the
 * fitting off-hand material reaches the axe instead of opening the crucible/barrel menu.
 */
public final class AxeWays {
    public static boolean wants(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !SimpleLibApi.axeWaysEnabled() || !CrucibleBlankBlock.isAxe(player.getMainHandItem())) return false;
        if (CrucibleUpgrades.stepFor(state, player.getOffhandItem()) != null) return true;
        return SimpleLibApi.canAttach(level, pos, state);
    }

    private AxeWays() {}
}
