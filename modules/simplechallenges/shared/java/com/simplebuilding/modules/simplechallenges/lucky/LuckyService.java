package com.simplebuilding.modules.simplechallenges.lucky;

import com.simplebuilding.modules.simplechallenges.SimpleChallenges;
import com.simplebuilding.modules.simplechallenges.core.Active;
import com.simplebuilding.modules.simplechallenges.core.ChallengeRuntime;
import com.simplebuilding.modules.simplechallenges.core.Gate;
import com.simplebuilding.modules.simplechallenges.preset.LuckyMode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lucky Block / Random Drops (S5): a block a player breaks drops another block instead of itself. The hook is Fabric's
 * "before block break": it breaks the block without drops and throws the rolled block out, or lets Vanilla go on. Without a
 * lucky or randomdrops mode in the running challenge it returns at once (safety lock). No tick work.
 */
public final class LuckyService {
    private LuckyService() {}

    /** The block that replaces the drop of a broken block, or null when this break is left to Vanilla. */
    public static Block rolled(Active a, ServerLevel level, BlockState broken) {
        LuckyMode mode = a.preset.lucky();
        if (mode == null || a.lucky == null || !BlockCatalog.includable(broken.getBlock())) return null;
        if (mode.kind() == LuckyMode.Kind.FIXED) return a.lucky.partner(a.state.seed, broken.getBlock());
        Block pity = LuckyTable.starter(a.state.luckyBreaks, mode.pity());
        return pity != null ? pity : a.lucky.pick(level.getRandom().nextDouble());
    }

    /**
     * Fabric BEFORE hook. @return false = the break was handled here (the vanilla break is cancelled), true = Vanilla goes on.
     * Creative players, blocks with contents (chests, furnaces...), blocks that give nothing anyway (wrong tool) and blocks that
     * are no survival blocks keep their normal break.
     */
    public static boolean beforeBreak(Level level, net.minecraft.world.entity.player.Player player, BlockPos pos, BlockState state) {
        Active a = ChallengeRuntime.server();
        if (a == null || a.preset.lucky() == null || level.isClientSide() || !(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer)) return true;
        if (!SimpleChallenges.on() || Gate.creativeCheck.test(player) || state.hasBlockEntity() || !player.hasCorrectToolForDrops(state)) return true;
        Block drop = rolled(a, sl, state);
        if (drop == null || drop.asItem() == net.minecraft.world.item.Items.AIR) return true;
        ItemStack tool = player.getMainHandItem();
        sl.destroyBlock(pos, false, player, 512);
        tool.mineBlock(sl, state, pos, player);
        player.causeFoodExhaustion(0.005F);
        Block.popResource(sl, pos, new ItemStack(drop.asItem()));
        a.state.luckyBreaks++;
        a.state.setDirty();
        return false;
    }
}
