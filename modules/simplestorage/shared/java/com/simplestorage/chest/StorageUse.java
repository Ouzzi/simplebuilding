package com.simplestorage.chest;

import com.simplelib.api.InWorldUpgrades;
import com.simplelib.api.SimpleLibApi;
import com.simplestorage.SimpleStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The upgrade click on a chest or shulker box (split plan §2.6, axe/hammer principle; same as Simple Ovens' furnaces).
 * The steps are the entries of {@code data/simplestorage/in_world_upgrades} (SimpleLib {@link InWorldUpgrades}):
 * copper chest -&gt; Reinforced (cracked diamond), Reinforced -&gt; Netherite (netherite nugget), Netherite -&gt; Enderite
 * (enderite nugget); shulker boxes take twice the blows and two of the material.
 * <ul>
 *   <li>Alone (SimpleLib's axe ways on): an axe in the main hand runs the upgrade with SimpleLib's strikes.</li>
 *   <li>With a hammer mod (Simple Hammers switches the axe ways off): the click passes on to the hammer when its owner
 *       says it takes it ({@link SimpleLibApi#toolWants}).</li>
 * </ul>
 * Null means: not an upgrade click, the block opens its menu.
 */
public final class StorageUse {
    private StorageUse() {
    }

    public static @Nullable InteractionResult upgradeClick(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!SimpleStorage.on()) return null;
        if (InWorldUpgrades.axeWants(state, player, hand)) {
            if (level instanceof ServerLevel server) InWorldUpgrades.axeStrike(server, pos, player);
            return InteractionResult.SUCCESS;
        }
        if (SimpleLibApi.toolWants(state, level, pos, player, hand)) return InteractionResult.PASS;
        return null;
    }
}
