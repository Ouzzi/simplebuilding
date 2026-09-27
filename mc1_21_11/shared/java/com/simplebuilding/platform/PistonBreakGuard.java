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
 *   <li><b>{@link #mayMove} - the piston event, once per piston action.</b> NeoForge and Forge fire
 *       {@code PistonEvent.Pre} for the piston here (the event vanilla's own {@code triggerEvent}
 *       fires only <em>after</em> the mod would have broken the block). A normal breaker extension
 *       therefore shows two {@code PistonEvent.Pre}: this one before the break and vanilla's own
 *       right after it, when the head moves out; a breach (no extension) shows exactly one, however
 *       many cells the enderite piston goes through. Before 2026-09-27 the mod fired one per
 *       destroyed block, up to four per extension (audit N16). Fabric has no piston event.</li>
 *   <li><b>{@link #mayBreak} - the block break event with a fake player, once per block.</b>
 *       NeoForge: {@code BreakBlockEvent} ({@code BlockEvent.BreakEvent} on 1.21.11) with
 *       {@code FakePlayerFactory.getMinecraft}; Fabric: {@code PlayerBlockBreakEvents.BEFORE} with
 *       {@code FakePlayer.get}, a refusal fires {@code CANCELED}. Forge 26.2 ships no fake player and
 *       its {@code BlockEvent.BreakEvent} needs a real one, so on Forge only the piston event decides
 *       (audit N5). Only while the config option {@code pistonsFireBreakEvents} is on (default):
 *       a protection mod that refuses every fake player stops the mod's pistons everywhere, and
 *       quest or statistics mods may count a piston break as a player break (audit N4). Off, only
 *       the piston event and the world border decide.</li>
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
     * The block break event for one block (see the class comment); the caller has already checked
     * the world border and {@link #mayMove}, and the config switch {@code pistonsFireBreakEvents}
     * is the loader implementation's to read.
     *
     * @param piston the position of the piston doing the breaking
     * @param facing where it points
     * @param target the block about to be destroyed
     * @param state  its state
     * @return {@code false} when a listener refused, then the block stays
     */
    boolean mayBreak(ServerLevel level, BlockPos piston, Direction facing, BlockPos target, BlockState state);

    /**
     * The piston event, asked once per piston action before the first block is destroyed.
     *
     * @return {@code false} when a listener refused, then nothing is destroyed
     */
    default boolean mayMove(ServerLevel level, BlockPos piston, Direction facing) {
        return true;
    }

    PistonBreakGuard ALLOW = (level, piston, facing, target, state) -> true;
}
