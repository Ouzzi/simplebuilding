package com.simplebuilding.neoforge;

import com.simplebuilding.platform.PistonBreakGuard;
import com.simplebuilding.platform.PlatformServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * NeoForge's side of {@link PistonBreakGuard}: first {@code PistonEvent.Pre} for the piston - the
 * event protection mods use for pistons, which vanilla's {@code triggerEvent} would only fire
 * after the mod had already broken the block - then {@code BreakBlockEvent} with NeoForge's fake
 * player for the block itself. Either one cancelled keeps the block.
 */
public final class NeoForgePistonBreakGuard implements PistonBreakGuard {

    private NeoForgePistonBreakGuard() {
    }

    public static void install() {
        PlatformServices.setPistonBreakGuard(new NeoForgePistonBreakGuard());
    }

    @Override
    public boolean mayBreak(ServerLevel level, BlockPos piston, Direction facing, BlockPos target, BlockState state) {
        if (EventHooks.onPistonMovePre(level, piston, facing, true)) {
            return false;
        }
        BreakBlockEvent event = new BreakBlockEvent(level, target, state, FakePlayerFactory.getMinecraft(level));
        return !NeoForge.EVENT_BUS.post(event).isCanceled();
    }
}
