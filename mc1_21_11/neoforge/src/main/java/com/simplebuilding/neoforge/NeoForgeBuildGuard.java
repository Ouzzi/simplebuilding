package com.simplebuilding.neoforge;

import com.simplebuilding.platform.BuildGuard;
import com.simplebuilding.platform.PlatformServices;
import com.simplebuilding.platform.ProtectionProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * NeoForge's side of {@link BuildGuard}: {@code BlockEvent.BreakEvent} with the real player for breaks,
 * {@code BlockEvent.EntityPlaceEvent} (through {@code EventHooks#onBlockPlace}) for placements -
 * the two events NeoForge protection mods listen to. The place event is fired before the block is
 * set, so its snapshot still holds the block that is there now.
 */
public final class NeoForgeBuildGuard implements BuildGuard {

    private NeoForgeBuildGuard() {
    }

    public static void install() {
        PlatformServices.setBuildGuard(new NeoForgeBuildGuard());
        // Ordinary listeners like a protection mod's; they refuse only what a game test registered.
        NeoForge.EVENT_BUS.addListener((BlockEvent.BreakEvent event) -> {
            if (ProtectionProbe.refused(event.getPos())) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((BlockEvent.EntityPlaceEvent event) -> {
            if (ProtectionProbe.refused(event.getPos())) {
                event.setCanceled(true);
            }
        });
    }

    @Override
    public boolean mayBreak(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        return !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled();
    }

    @Override
    public boolean mayPlace(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        return !EventHooks.onBlockPlace(player, snapshot, Direction.UP);
    }
}
