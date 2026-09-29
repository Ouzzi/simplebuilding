package com.simplebuilding.platform;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fabric's side of {@link BuildGuard}. Breaks fire {@code PlayerBlockBreakEvents.BEFORE} with the
 * real player, a refusal fires {@code CANCELED} like a refused hand break. Fabric API has no block
 * place event; claim mods on Fabric guard their claims through the break event, so the placement
 * check fires {@code BEFORE} for the cell about to be filled (with the block that is still there,
 * usually air) and nothing else - a fake-placement check that needs no second hook.
 */
public final class FabricBuildGuard implements BuildGuard {

    private FabricBuildGuard() {
    }

    public static void install() {
        PlatformServices.setBuildGuard(new FabricBuildGuard());
        // An ordinary listener like a claim mod's; it refuses only what a game test registered.
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> !ProtectionProbe.refused(pos));
    }

    @Override
    public boolean mayBreak(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        boolean allowed = PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, blockEntity);
        if (!allowed) {
            PlayerBlockBreakEvents.CANCELED.invoker().onBlockBreakCanceled(level, player, pos, state, blockEntity);
        }
        return allowed;
    }

    @Override
    public boolean mayPlace(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        BlockState current = level.getBlockState(pos);
        return PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, current, level.getBlockEntity(pos));
    }
}
