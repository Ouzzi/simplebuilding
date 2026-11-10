package com.simplebuilding.modules.simplelayers.vault.block;

import com.simplebuilding.modules.simplelayers.vault.VaultPuzzles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Ascent Altar core (UNDER21): the middle of the altar; right-click tells how many seals are set (A14). Unbreakable. */
public class AscentAltarBlock extends Block {
    public AscentAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) VaultPuzzles.altarStatus(server, pos, player);
        return InteractionResult.SUCCESS;
    }
}
