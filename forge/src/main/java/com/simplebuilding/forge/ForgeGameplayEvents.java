package com.simplebuilding.forge;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.util.DynamicLightHandler;
import com.simplebuilding.util.LegacySpatulaMigration;
import com.simplebuilding.util.SledgehammerEntityInteraction;
import com.simplebuilding.util.SledgehammerUsageEvent;
import com.simplebuilding.util.StripMinerUsageEvent;
import com.simplebuilding.util.VeinMinerUsageEvent;
import com.simplebuilding.util.VersatilityUsageEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge port of the shared gameplay/server events. On Forge these live on the
 * game bus (EventBus 7) via {@code @Mod.EventBusSubscriber}. Cancellable handlers
 * return {@code boolean} (true = cancel) per the EventBus 7 model.
 */
@Mod.EventBusSubscriber(modid = Simplebuilding.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeGameplayEvents {
    private ForgeGameplayEvents() {
    }

    @SubscribeEvent(priority = Priority.HIGH)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        SledgehammerUsageEvent.handleBeforeBlockBreak(level, player, event.getPos(), event.getState(), level.getBlockEntity(event.getPos()));
        StripMinerUsageEvent.handleBeforeBlockBreak(level, player, event.getPos(), event.getState(), level.getBlockEntity(event.getPos()));
        VeinMinerUsageEvent.handleBeforeBlockBreak(level, player, event.getPos(), event.getState(), level.getBlockEntity(event.getPos()));
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        VersatilityUsageEvent.handleAttackBlock(
                event.getEntity(),
                event.getLevel(),
                event.getHand(),
                event.getPos(),
                event.getFace()
        );
    }

    @SubscribeEvent
    public static boolean onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getLevel().isClientSide()) {
            return false;
        }
        InteractionResult result = ModRegistriesForge.handleUseBlock(
                event.getEntity(),
                event.getLevel(),
                event.getHand(),
                event.getHitVec()
        );
        return result != InteractionResult.PASS;
    }

    @SubscribeEvent
    public static boolean onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return false;
        }
        InteractionResult result = SledgehammerEntityInteraction.handleAttackEntity(
                event.getEntity(),
                event.getEntity().level(),
                InteractionHand.MAIN_HAND,
                event.getTarget()
        );
        return result != InteractionResult.PASS;
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        LegacySpatulaMigration.migrateWorlds(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            LegacySpatulaMigration.migratePlayer(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            DynamicLightHandler.onDisconnect(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        if (event.server().getTickCount() % 2 != 0) {
            return;
        }
        for (ServerPlayer player : event.server().getPlayerList().getPlayers()) {
            DynamicLightHandler.tick(player);
        }
    }
}
