package com.simplebuilding.modules.simplemachines;

import com.simplemachines.upgrade.PistonBreach;
import com.simplemachines.upgrade.PistonConfigPayload;
import com.simplemachines.upgrade.PistonEventProbe;
import com.simplemachines.upgrade.PistonGuard;
import com.simplemachines.upgrade.PistonUpgrades;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fabric side of the piston upgrades: the loader guard (Fabric has no piston event; a break goes through
 * {@code PlayerBlockBreakEvents.BEFORE} with Fabric's fake player while {@code pistons.fireBreakEvents} is on, a refusal fires
 * {@code CANCELED}), the piston options for the client at login, and the late hooks once a server starts.
 */
final class PistonsFabric {
    private PistonsFabric() {}

    static void init() {
        PistonGuard.install(new PistonGuard.Loader() {
            @Override
            public boolean mayBreak(ServerLevel level, BlockPos piston, Direction facing, BlockPos target, BlockState state) {
                if (!PistonBreach.fireBreakEvents()) {
                    return true;
                }
                FakePlayer player = FakePlayer.get(level);
                BlockEntity blockEntity = level.getBlockEntity(target);
                boolean allowed;
                // A probe, no real break (SimpleLib's counter: area breakers must not start, like SimpleBuilding's BuildPermissions).
                com.simplelib.api.InteractionGuard.probing++;
                try {
                    allowed = PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, target, state, blockEntity);
                } finally {
                    com.simplelib.api.InteractionGuard.probing--;
                }
                if (!allowed) {
                    PlayerBlockBreakEvents.CANCELED.invoker().onBlockBreakCanceled(level, player, target, state, blockEntity);
                }
                return allowed;
            }
        });
        PistonEventProbe.declare(true);
        // An ordinary listener like a claim mod's, registered only when a game test first refuses a position.
        PistonEventProbe.activateBreakOnFirstUse(() -> PlayerBlockBreakEvents.BEFORE.register(
                (level, player, pos, state, blockEntity) -> !PistonEventProbe.breakRefused(pos)));
        PayloadTypeRegistry.clientboundPlay().register(PistonConfigPayload.ID, PistonConfigPayload.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (ServerPlayNetworking.canSend(handler.player, PistonConfigPayload.ID)) {
                ServerPlayNetworking.send(handler.player, PistonConfigPayload.fromServer());
            }
        });
        ServerLifecycleEvents.SERVER_STARTING.register(server -> PistonUpgrades.registerLateHooks());
    }
}
