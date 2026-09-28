package com.simplebuilding.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Asks the loader whether a player may break or build at one position, the way claim and
 * protection mods expect to be asked: by firing the loader's own block break and block place
 * events for that position, with the real player. The mod's multi-block tools (building wand,
 * blueprint build, octant fill, amethyst lens beam) change many blocks from one click, and vanilla
 * only fires those events for the one block the player clicked; this is what lets a claim mod
 * refuse each of the others.
 *
 * <ul>
 *   <li>Fabric: {@code PlayerBlockBreakEvents.BEFORE} for breaks (a refusal fires
 *       {@code CANCELED}). Fabric has no block place event; the placement check fires the same
 *       {@code BEFORE} event for the cell about to be filled (a fake-placement check: claim mods on
 *       Fabric guard their claims through the break event), without {@code CANCELED}.</li>
 *   <li>NeoForge: {@code BreakBlockEvent} ({@code BlockEvent.BreakEvent} on 1.21.11) and
 *       {@code BlockEvent.EntityPlaceEvent}.</li>
 *   <li>Forge: {@code BlockEvent.BreakEvent} and {@code BlockEvent.EntityPlaceEvent}.</li>
 * </ul>
 *
 * The place event is fired <em>before</em> the block is set (a vanilla placement fires it after
 * and reverts), so its snapshot holds the block that is still there. Each loader installs its
 * guard at start through {@link PlatformServices#setBuildGuard}; without one (plain game tests)
 * everything is allowed. The sledgehammer, Vein Miner and Strip Miner need none of this: they break
 * their extra blocks through {@code ServerPlayerGameMode#destroyBlock}, which fires the loader's
 * break event by itself.
 */
public interface BuildGuard {

    /** Whether {@code player} may break (or otherwise destroy or change) the block at {@code pos}. */
    boolean mayBreak(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state);

    /** Whether {@code player} may place {@code state} at {@code pos}. */
    boolean mayPlace(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state);

    BuildGuard ALLOW = new BuildGuard() {
        @Override
        public boolean mayBreak(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
            return true;
        }

        @Override
        public boolean mayPlace(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
            return true;
        }
    };
}
