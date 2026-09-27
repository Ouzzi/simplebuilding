package com.simplebuilding.neoforge;

import com.simplebuilding.platform.PistonBreakGuard;
import com.simplebuilding.platform.PistonEventProbe;
import com.simplebuilding.platform.PlatformServices;
import com.simplebuilding.util.PistonBreach;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * NeoForge's side of {@link PistonBreakGuard}: {@code PistonEvent.Pre} for the piston once per
 * action ({@link #mayMove}) - the event protection mods use for pistons, which vanilla's
 * {@code triggerEvent} would only fire after the mod had already broken the block - then, while
 * {@code pistonsFireBreakEvents} is on, {@code BreakBlockEvent} with NeoForge's fake player for each
 * block ({@link #mayBreak}). Either one cancelled keeps the block.
 */
public final class NeoForgePistonBreakGuard implements PistonBreakGuard {

    private NeoForgePistonBreakGuard() {
    }

    public static void install() {
        PlatformServices.setPistonBreakGuard(new NeoForgePistonBreakGuard());
        PistonEventProbe.declare(true, true);
        // Ordinary listeners like a protection mod's; they refuse only what a game test registered.
        NeoForge.EVENT_BUS.addListener((BreakBlockEvent event) -> {
            if (PistonEventProbe.breakRefused(event.getPos())) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((PistonEvent.Pre event) -> {
            if (PistonEventProbe.pistonRefused(event.getPos())) {
                event.setCanceled(true);
            }
        });
    }

    @Override
    public boolean mayMove(ServerLevel level, BlockPos piston, Direction facing) {
        return !EventHooks.onPistonMovePre(level, piston, facing, true);
    }

    @Override
    public boolean mayBreak(ServerLevel level, BlockPos piston, Direction facing, BlockPos target, BlockState state) {
        if (!PistonBreach.fireBreakEvents()) {
            return true;
        }
        BreakBlockEvent event = new BreakBlockEvent(level, target, state, FakePlayerFactory.getMinecraft(level));
        return !NeoForge.EVENT_BUS.post(event).isCanceled();
    }
}
