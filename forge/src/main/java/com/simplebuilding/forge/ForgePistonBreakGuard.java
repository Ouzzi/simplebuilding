package com.simplebuilding.forge;

import com.simplebuilding.platform.PistonBreakGuard;
import com.simplebuilding.platform.PistonEventProbe;
import com.simplebuilding.platform.PlatformServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.level.PistonEvent;

/**
 * Forge's side of {@link PistonBreakGuard}: {@code PistonEvent.Pre} for the piston once per action
 * ({@link #mayMove}), the event Forge protection mods use for pistons and that vanilla's
 * {@code triggerEvent} would only fire after the mod had already broken the block.
 *
 * <p><b>No block break event on Forge</b> (audit N5, checked 2026-09-27 against Forge 65.1.3): Forge
 * 26.2 ships no {@code FakePlayer}/{@code FakePlayerFactory}, and {@code BlockEvent.BreakEvent}
 * needs a real {@code Player}. Building an own {@code ServerPlayer} without a connection would
 * crash listeners that message the "player", so {@link #mayBreak} allows everything and the piston
 * event alone decides here. The config option {@code pistonsFireBreakEvents} has nothing to switch
 * on Forge. Should Forge ship a fake player again, {@code mayBreak} is where the event goes.
 */
public final class ForgePistonBreakGuard implements PistonBreakGuard {

    private ForgePistonBreakGuard() {
    }

    public static void install() {
        PlatformServices.setPistonBreakGuard(new ForgePistonBreakGuard());
        PistonEventProbe.declare(true, false);
        // An ordinary listener like a protection mod's; it refuses only what a game test registered.
        // EventBus 7: a Predicate listener cancels by returning true.
        PistonEvent.Pre.BUS.addListener((PistonEvent.Pre event) -> PistonEventProbe.pistonRefused(event.getPos()));
    }

    @Override
    public boolean mayMove(ServerLevel level, BlockPos piston, Direction facing) {
        return !ForgeEventFactory.onPistonMovePre(level, piston, facing, true);
    }

    @Override
    public boolean mayBreak(ServerLevel level, BlockPos piston, Direction facing, BlockPos target, BlockState state) {
        return true;
    }
}
