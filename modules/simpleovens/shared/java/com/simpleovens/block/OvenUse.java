package com.simpleovens.block;

import com.simplelib.api.InWorldUpgrades;
import com.simplelib.api.SimpleLibApi;
import com.simpleovens.SimpleOvens;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The upgrade click on a furnace (split plan §2.6, axe/hammer principle). The steps are the entries of
 * {@code data/simpleovens/in_world_upgrades} (SimpleLib's {@link InWorldUpgrades}): Reinforced -> Netherite with a
 * netherite nugget, Netherite -> Enderite with an enderite nugget in the off hand.
 * <ul>
 *   <li>Alone (SimpleLib's axe ways on): an axe in the main hand runs the upgrade with SimpleLib's strikes.</li>
 *   <li>With a hammer mod (Simple Hammers' sledgehammer, which switches the axe ways off): the click passes on to the
 *       hammer when its owner says it takes it ({@link SimpleLibApi#toolWants}).</li>
 * </ul>
 * Null means: not an upgrade click, the furnace opens its menu.
 */
public final class OvenUse {
    private OvenUse() {
    }

    public static @Nullable InteractionResult useItemOn(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!SimpleOvens.on()) return null;
        if (InWorldUpgrades.axeWants(state, player, hand)) {
            if (level instanceof ServerLevel server) InWorldUpgrades.axeStrike(server, pos, player);
            return InteractionResult.SUCCESS;
        }
        if (SimpleLibApi.toolWants(state, level, pos, player, hand)) return InteractionResult.PASS;
        return null;
    }
}
