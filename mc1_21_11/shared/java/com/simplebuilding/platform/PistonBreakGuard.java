package com.simplebuilding.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Asks the loader whether a mod piston may destroy a block - the netherite breaker's normal
 * break, every block of a paid breach ({@code PistonBreach}), the enderite piston's deep breach.
 * Vanilla pistons never destroy anything, so no loader has an event made for this; claim and
 * protection mods listen to two other things instead, and the implementations fire those:
 *
 * <ul>
 *   <li><b>NeoForge and Forge:</b> {@code PistonEvent.Pre} for the piston (the event vanilla's own
 *       {@code triggerEvent} fires only <em>after</em> the mod would have broken the block), then
 *       on NeoForge the block break event with the loader's fake player
 *       ({@code FakePlayerFactory.getMinecraft}). Forge 26.2 ships no fake player, so there the
 *       piston event alone decides.</li>
 *   <li><b>Fabric:</b> {@code PlayerBlockBreakEvents.BEFORE} with Fabric's fake player
 *       ({@code FakePlayer.get}); Fabric has no piston event. A refusal fires
 *       {@code PlayerBlockBreakEvents.CANCELED}, as a refused hand break would.</li>
 * </ul>
 *
 * A fake player owns no claim, so a protection mod that guards claims against strangers refuses
 * it - which is exactly the rule wanted here: a piston outside a claim must not bore into it.
 * Each loader installs its guard at start through {@link PlatformServices#setPistonBreakGuard};
 * without one (plain game tests) everything is allowed. Asked on the server only, and only once the
 * piston really goes through with it (signal confirmed, block breakable), directly before the
 * block is destroyed.
 */
@FunctionalInterface
public interface PistonBreakGuard {

    /**
     * @param piston the position of the piston doing the breaking
     * @param facing where it points
     * @param target the block about to be destroyed
     * @param state  its state
     * @return {@code false} when a listener refused, then the block stays
     */
    boolean mayBreak(ServerLevel level, BlockPos piston, Direction facing, BlockPos target, BlockState state);

    PistonBreakGuard ALLOW = (level, piston, facing, target, state) -> true;
}
