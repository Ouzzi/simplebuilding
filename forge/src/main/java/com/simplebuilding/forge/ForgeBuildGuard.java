package com.simplebuilding.forge;

import com.simplebuilding.platform.BuildGuard;
import com.simplebuilding.platform.PlatformServices;
import com.simplebuilding.platform.ProtectionProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.level.BlockEvent;

/**
 * Forge's side of {@link BuildGuard}: {@code BlockEvent.BreakEvent} with the real player (refused
 * when denied, as {@code ForgeHooks#onBlockBreakEvent} reads it, and - stricter than Forge's own
 * break path - also when a listener only cancelled it) and
 * {@code BlockEvent.EntityPlaceEvent} through {@code ForgeEventFactory#onBlockPlace}. Unlike the
 * piston guard this needs no fake player: the tools are always used by a real one.
 */
public final class ForgeBuildGuard implements BuildGuard {

    private ForgeBuildGuard() {
    }

    public static void install() {
        PlatformServices.setBuildGuard(new ForgeBuildGuard());
        // Ordinary listeners like a protection mod's; they refuse only what a game test registered.
        // EventBus 7: a Predicate listener cancels by returning true. The break listener also sets
        // DENY, because that is the only thing Forge's own break path reads:
        // ServerPlayerGameMode#destroyBlock -> ForgeHooks#onBlockBreakEvent fires the event and checks
        // getResult().isDenied(), never the cancellation (BreakEvent's javadoc names DENY as the way
        // to refuse). A cancel-only listener would let every vanilla break through - and every area
        // block of the sledgehammer, which breaks through destroyBlock like a player.
        BlockEvent.BreakEvent.BUS.addListener((BlockEvent.BreakEvent event) -> {
            if (!ProtectionProbe.refused(event.getPos())) {
                return false;
            }
            event.setResult(Result.DENY);
            return true;
        });
        BlockEvent.EntityPlaceEvent.BUS.addListener((BlockEvent.EntityPlaceEvent event) -> ProtectionProbe.refused(event.getPos()));
    }

    @Override
    public boolean mayBreak(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, player, Result.DEFAULT);
        boolean cancelled = BlockEvent.BreakEvent.BUS.post(event);
        return !cancelled && !event.getResult().isDenied();
    }

    @Override
    public boolean mayPlace(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        return !ForgeEventFactory.onBlockPlace(player, snapshot, Direction.UP);
    }
}
