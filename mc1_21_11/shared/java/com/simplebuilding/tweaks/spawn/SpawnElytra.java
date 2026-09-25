package com.simplebuilding.tweaks.spawn;

import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.TweaksConfig;
import com.simplebuilding.tweaks.component.TweaksComponents;
import com.simplebuilding.tweaks.item.TweaksItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Spawn-Elytra im Spawnbereich (Simple Tweaks: {@code SpawnHandler}). Wird von den Loadern beim
 * Betreten, nach dem Wiedereinstieg und jeden Server-Tick aufgerufen; der Flugzeit-Timer braucht
 * jeden Tick, alles andere laeuft einmal pro Sekunde ({@code fullPass}).
 */
public final class SpawnElytra {

    /** Kulanz nach Verlassen eines Elytra-Pads, in Ticks. */
    public static final int PAD_GRACE_TICKS = 60;

    private SpawnElytra() {
    }

    public static void recharge(ItemStack stack, TweaksConfig.Spawn config) {
        stack.set(TweaksComponents.FLIGHT_TIME, config.flightTimeSeconds * 20);
        stack.set(TweaksComponents.BOOST_LEVEL, 1.0f);
    }

    /** Aus dem Spawnbereich stammende Elytren schuetzen vor Fall- und Kinetikschaden. */
    public static void rechargeAsSpawnElytra(ItemStack stack, TweaksConfig.Spawn config) {
        recharge(stack, config);
        stack.set(TweaksComponents.IS_SAFE_ELYTRA, true);
    }

    public static void onJoinOrRespawn(ServerPlayer player) {
        tick(player, true);
    }

    public static void serverTick(net.minecraft.server.MinecraftServer server) {
        boolean fullPass = server.getTickCount() % 20 == 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tick(player, fullPass);
        }
    }

    /** Mitte des Spawnbereichs (Weltspawn oder eigene Koordinaten). */
    public static BlockPos center(Player player) {
        TweaksConfig.Spawn config = SimpleTweaks.config().spawn;
        if (config.useWorldSpawnAsCenter && player.level().getServer() != null) {
            return player.level().getServer().getRespawnData().pos();
        }
        return new BlockPos(config.customSpawnElytraX, 0, config.customSpawnElytraZ);
    }

    /** Quadratischer Bereich (Radius je Achse), Hoehe egal - wie in Simple Tweaks. */
    public static boolean insideSpawn(Player player) {
        BlockPos center = center(player);
        int radius = SimpleTweaks.config().spawn.spawnElytraRadius;
        return Math.abs(player.getX() - center.getX()) <= radius && Math.abs(player.getZ() - center.getZ()) <= radius;
    }

    public static void tick(ServerPlayer player, boolean fullPass) {
        TweaksConfig.Spawn config = SimpleTweaks.config().spawn;
        // Simple Tweaks brach hier bei ausgeschalteter Spawn-Elytra ganz ab - dann liefen auch
        // Timer und Aufraeumen der Elytren von Elytra-Pads nie. Jetzt haengt nur der Spawnbereich
        // am Schalter.
        boolean spawnEnabled = config.giveElytraOnSpawn;
        if (!spawnEnabled && !player.getItemBySlot(EquipmentSlot.CHEST).is(TweaksItems.SPAWN_ELYTRA)
                && !player.containerMenu.getCarried().is(TweaksItems.SPAWN_ELYTRA) && !fullPass) {
            return;
        }

        // 1. Aufraeumen: die Spawn-Elytra gibt es nur im Brust-Slot, nie im Inventar oder am Cursor.
        if (fullPass) {
            if (player.containerMenu.getCarried().is(TweaksItems.SPAWN_ELYTRA)) {
                player.containerMenu.setCarried(ItemStack.EMPTY);
            }
            var inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.is(TweaksItems.SPAWN_ELYTRA) && !isChestSlot(player, i)) {
                    inventory.removeItemNoUpdate(i);
                }
            }
        }

        boolean inside = spawnEnabled && insideSpawn(player);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean wearing = chest.is(TweaksItems.SPAWN_ELYTRA);

        if (wearing && fullPass) {
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, true, false, false));
        }

        long now = player.level().getGameTime();
        Long lastPad = wearing ? chest.get(TweaksComponents.LAST_PAD_TICK) : null;
        boolean recentlyOnPad = lastPad != null && now - lastPad < PAD_GRACE_TICKS;
        boolean validLocation = inside || recentlyOnPad;

        if (inside) {
            if (fullPass) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
                if (wearing) {
                    rechargeAsSpawnElytra(chest, config);
                } else if (chest.isEmpty()) {
                    ItemStack elytra = new ItemStack(TweaksItems.SPAWN_ELYTRA);
                    rechargeAsSpawnElytra(elytra, config);
                    player.setItemSlot(EquipmentSlot.CHEST, elytra);
                }
            }
        } else if (wearing) {
            Integer ticksLeft = chest.get(TweaksComponents.FLIGHT_TIME);
            int maxTicks = config.flightTimeSeconds * 20;
            if (ticksLeft == null) {
                ticksLeft = maxTicks;
            }
            if (player.isFallFlying()) {
                ticksLeft--;
            }
            chest.set(TweaksComponents.FLIGHT_TIME, ticksLeft);

            boolean timeUp = ticksLeft <= 0;
            boolean landed = player.onGround() && !player.isFallFlying();
            if (timeUp || (!validLocation && landed)) {
                player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                player.displayClientMessage(Component.translatable("message.simplebuilding.spawn_elytra.expired").withStyle(ChatFormatting.YELLOW), true);
            } else if (ticksLeft == 200) {
                player.displayClientMessage(Component.translatable("message.simplebuilding.spawn_elytra.expires_soon").withStyle(ChatFormatting.RED), true);
            }
        }
    }

    /** Simple Tweaks liess Slot 38 (Brust) stehen; hier: derselbe Stapel wie im Brust-Slot. */
    private static boolean isChestSlot(ServerPlayer player, int index) {
        return player.getInventory().getItem(index) == player.getItemBySlot(EquipmentSlot.CHEST);
    }
}
