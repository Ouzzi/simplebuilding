package com.simplebuilding.neoforge;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.util.LegacySpatulaMigration;
import com.simplebuilding.util.SledgehammerEntityInteraction;
import com.simplebuilding.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
// NeoForge 21.11.45: das Block-Break-Event heisst noch BlockEvent.BreakEvent;
// die eigenstaendige Klasse net.neoforged.neoforge.event.level.block.BreakBlockEvent
// gibt es erst ab der 26.x-Linie. getPlayer()/getPos()/getState() sind identisch.
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = Simplebuilding.MOD_ID)
public final class NeoForgeGameplayEvents {
    private NeoForgeGameplayEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled()) {
            return;
        }
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
        // Only the initial click — LeftClickBlock also fires every tick while mining
        // is held; Fabric's AttackBlockCallback fires once per attack (M2).
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }
        VersatilityUsageEvent.handleAttackBlock(
                event.getEntity(),
                event.getLevel(),
                event.getHand(),
                event.getPos(),
                event.getFace()
        );
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getLevel().isClientSide()) {
            return;
        }
        InteractionResult result = ModRegistriesNeoForge.handleUseBlock(
                event.getEntity(),
                event.getLevel(),
                event.getHand(),
                event.getHitVec()
        );
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        InteractionResult result = SledgehammerEntityInteraction.handleAttackEntity(
                event.getEntity(),
                event.getEntity().level(),
                InteractionHand.MAIN_HAND,
                event.getTarget()
        );
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerStarted(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
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
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 2 != 0) {
            return;
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            DynamicLightHandler.tick(player);
        }
    }
}
