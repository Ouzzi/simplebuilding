package com.simplebuilding.platform;

import com.simplebuilding.blocks.entity.custom.ModHopperBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class PlatformServices {
    private static HopperSync hopperSync = HopperSync.NOOP;
    private static PlayerPacketSender playerPacketSender = PlayerPacketSender.NOOP;
    private static ItemAutomation itemAutomation = ItemAutomation.NOT_INSTALLED;
    private static PistonBreakGuard pistonBreakGuard = PistonBreakGuard.ALLOW;

    private PlatformServices() {
    }

    public static void setHopperSync(HopperSync hopperSync) {
        PlatformServices.hopperSync = hopperSync != null ? hopperSync : HopperSync.NOOP;
    }

    public static void setPlayerPacketSender(PlayerPacketSender playerPacketSender) {
        PlatformServices.playerPacketSender = playerPacketSender != null ? playerPacketSender : PlayerPacketSender.NOOP;
    }

    public static PlayerPacketSender playerPacketSender() {
        return playerPacketSender;
    }

    public static void broadcastHopperGhostItem(ModHopperBlockEntity blockEntity, int slot, ItemStack stack) {
        hopperSync.broadcastGhostItem(blockEntity, slot, stack);
    }

    public static boolean canSendToPlayer(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return playerPacketSender.canSend(player, type);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        playerPacketSender.send(player, payload);
    }

    public static void setItemAutomation(ItemAutomation itemAutomation) {
        PlatformServices.itemAutomation = itemAutomation != null ? itemAutomation : ItemAutomation.NOT_INSTALLED;
    }

    public static ItemAutomation itemAutomation() {
        return itemAutomation;
    }

    public static void setPistonBreakGuard(PistonBreakGuard guard) {
        PlatformServices.pistonBreakGuard = guard != null ? guard : PistonBreakGuard.ALLOW;
    }

    public static PistonBreakGuard pistonBreakGuard() {
        return pistonBreakGuard;
    }

    /**
     * Whether a mod piston may destroy {@code target} (see {@link PistonBreakGuard}): never outside
     * the world border, otherwise whatever the loader's listeners say.
     */
    public static boolean mayPistonBreak(ServerLevel level, BlockPos piston, Direction facing, BlockPos target, BlockState state) {
        if (!level.getWorldBorder().isWithinBounds(target)) {
            return false;
        }
        return pistonBreakGuard.mayBreak(level, piston, facing, target, state);
    }
}
