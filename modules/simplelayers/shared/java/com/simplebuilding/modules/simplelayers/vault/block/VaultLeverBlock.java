package com.simplebuilding.modules.simplelayers.vault.block;

import com.simplebuilding.modules.simplelayers.vault.VaultPuzzles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Vault Lever (UNDER21): a Vanilla lever that also switches the light riddle's lamps ({@link VaultPuzzles#leverPulled}); unbreakable. */
public class VaultLeverBlock extends LeverBlock {
    public VaultLeverBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        InteractionResult result = super.useWithoutItem(state, level, pos, player, hit);
        if (level instanceof ServerLevel server) VaultPuzzles.leverPulled(server, pos, player);
        return result;
    }
}
