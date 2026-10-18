package com.simplecrucibles.crucible;

import com.simplecrucibles.api.SimpleCruciblesApi;
import com.simplelib.api.HammerActions;
import com.simplelib.api.SimpleLibApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The hammer ways of the crucibles (split stage 7; were SimpleBuilding's {@code CrucibleCompat}): with a hammer mod
 * (SimpleLib {@link HammerActions}, no import of it) the hammer builds the iron crucible on an iron block, attaches a
 * barrel and upgrades crucibles strike by strike (owner N51). Without one nobody asks these hooks and the axe ways stay.
 */
public final class HammerWays {
    /** Durability per build / attach strike of a hammer. */
    public static final int HAMMER_DAMAGE_PER_STRIKE = 2;

    private HammerWays() {}

    public static void register() {
        HammerActions.registerUse(HammerActions.Stage.PERMITTED, HammerWays::use);
        SimpleLibApi.registerToolUse(HammerWays::wants);
        HammerActions.registerStaged(SimpleCruciblesApi::stagedUpgrade, new HammerActions.Staged() {
            @Override
            public int strikes() {
                return SimpleCruciblesApi.crucibleUpgradeStrikes();
            }

            @Override
            public int strikesPerUnit() {
                return SimpleCruciblesApi.crucibleUpgradeStrikesPerUnit();
            }

            @Override
            public int units(BlockState state) {
                return SimpleCruciblesApi.crucibleUpgradeUnits(state);
            }

            @Override
            public void strike(ServerLevel level, BlockPos pos, Block to, int done, ItemStack material) {
                SimpleCruciblesApi.crucibleUpgradeStrike(level, pos, to, done, material);
            }
        });
    }

    /** A hammer click on a crucible/barrel reaches the hammer instead of the menu when it upgrades or attaches. */
    private static boolean wants(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !SimpleLibApi.isHammer(player.getMainHandItem())) return false;
        if (HammerActions.upgradeTakesClick(state, level, pos, player, hand)) return true;
        return SimpleCruciblesApi.canAttach(level, pos, state);
    }

    /** Build the iron crucible on an iron block (off hand: walls, then handles) or attach a barrel; null otherwise. */
    private static @Nullable InteractionResult use(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || context.getHand() != InteractionHand.MAIN_HAND) return null;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        ItemStack tool = context.getItemInHand();
        if (SimpleCruciblesApi.canAttach(level, pos, state)) {
            return SimpleCruciblesApi.attachStrike(level, pos, player, tool, HAMMER_DAMAGE_PER_STRIKE) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        if (SimpleCruciblesApi.canBuildStrike(state, player.getOffhandItem())) {
            boolean hit = SimpleCruciblesApi.buildStrike(level, pos, player, tool, HAMMER_DAMAGE_PER_STRIKE);
            if (hit && player instanceof ServerPlayer server && level.getBlockState(pos).getBlock()
                    == BuiltInRegistries.BLOCK.getValue(com.simplecrucibles.SimpleCrucibles.id("iron_crucible"))) {
                SimpleCruciblesApi.crucibleBuilt(server);
            }
            return hit ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        return null;
    }
}
