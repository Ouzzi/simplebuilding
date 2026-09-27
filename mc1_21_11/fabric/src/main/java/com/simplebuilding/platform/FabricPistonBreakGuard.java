package com.simplebuilding.platform;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fabric's side of {@link PistonBreakGuard}: Fabric has no piston event, so a mod piston's break
 * goes through {@code PlayerBlockBreakEvents.BEFORE} with Fabric's fake player - the hook claim
 * mods on Fabric already watch for hand breaks. A refusal fires {@code CANCELED}, like a refused
 * hand break.
 */
public final class FabricPistonBreakGuard implements PistonBreakGuard {

    @Override
    public boolean mayBreak(ServerLevel level, BlockPos piston, Direction facing, BlockPos target, BlockState state) {
        FakePlayer player = FakePlayer.get(level);
        BlockEntity blockEntity = level.getBlockEntity(target);
        boolean allowed = PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, target, state, blockEntity);
        if (!allowed) {
            PlayerBlockBreakEvents.CANCELED.invoker().onBlockBreakCanceled(level, player, target, state, blockEntity);
        }
        return allowed;
    }
}
