package com.simplebuilding.tweaks.spawn;

import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.TweaksConfig;
import com.simplebuilding.tweaks.component.TweaksComponents;
import com.simplebuilding.tweaks.item.SpawnElytraItem;
import com.simplebuilding.tweaks.item.TweaksItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

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
        stack.set(TweaksComponents.FLIGHT_TIME, config.flightTicks());
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

    /**
     * Die Dimension des Spawnbereichs: die des Weltspawns (normal die Oberwelt). Vorher galten die
     * Koordinaten in jeder Dimension - Nether und End-Hauptinsel bei 0,0 gaben freie Elytren und
     * Fallschutz (Audit 2026-09-26 #4).
     */
    public static ResourceKey<Level> spawnDimension(Player player) {
        MinecraftServer server = player.level().getServer();
        return server != null ? server.getRespawnData().dimension() : Level.OVERWORLD;
    }

    /**
     * Quadratischer Bereich (Radius je Achse), Hoehe egal - wie in Simple Tweaks - und nur in der
     * Weltspawn-Dimension. Derselbe Bereich gilt fuer den Fallschutz ({@link ElytraDamageRules}),
     * der frueher ein Kreis war und in den Ecken fehlte.
     */
    public static boolean insideSpawn(Player player) {
        if (player.level().dimension() != spawnDimension(player)) {
            return false;
        }
        BlockPos center = center(player);
        int radius = SimpleTweaks.config().spawn.spawnElytraRadius;
        return Math.abs(player.getX() - center.getX()) <= radius && Math.abs(player.getZ() - center.getZ()) <= radius;
    }

    /**
     * Die Spawn-Elytra gibt es nur im Brust-Slot, nie im Inventar oder am Cursor. Laeuft jeden Tick
     * (frueher nur einmal pro Sekunde - in der Luecke liess sie sich in eine Truhe legen, waehrend
     * der Brustplatz leer war und sofort eine neue kam; Audit #3). Fallen gelassene Exemplare
     * verschwinden in {@code SpawnElytraItemEntityMixin}, Container nehmen sie gar nicht an
     * ({@code SpawnElytraSlotMixin}, {@link com.simplebuilding.tweaks.item.SpawnElytraItem}).
     */
    public static void removeStrayElytras(ServerPlayer player) {
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

    public static void tick(ServerPlayer player, boolean fullPass) {
        TweaksConfig.Spawn config = SimpleTweaks.config().spawn;
        // Simple Tweaks brach hier bei ausgeschalteter Spawn-Elytra ganz ab - dann liefen auch
        // Timer und Aufraeumen der Elytren von Elytra-Pads nie. Jetzt haengt nur der Spawnbereich
        // am Schalter.
        boolean spawnEnabled = config.giveElytraOnSpawn;
        // 1. Aufraeumen, jeden Tick.
        removeStrayElytras(player);
        if (!spawnEnabled && !player.getItemBySlot(EquipmentSlot.CHEST).is(TweaksItems.SPAWN_ELYTRA) && !fullPass) {
            return;
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
            int maxTicks = config.flightTicks();
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
                // Keine Bildschirmtexte: abgelegt klingt wie das Anlegen, nur tiefer.
                player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_ELYTRA.value(), SoundSource.PLAYERS, 1.0f, 0.6f);
            } else if (ticksLeft == 200) {
                // 10 s vor Ablauf: eine tiefe Glocke als Warnung.
                player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.0f, 0.5f);
            }
        }
    }

    /** Ohne TweaksItems zu laden (die Mixins fragen das auch vor der Registrierung). */
    public static boolean isSpawnElytra(ItemStack stack) {
        return stack.getItem() instanceof SpawnElytraItem;
    }

    /** Wo eine Spawn-Elytra liegen darf: nur im eigenen Spielerinventar (siehe SpawnElytraSlotMixin). */
    public static boolean mayHold(Container container) {
        return container instanceof Inventory;
    }

    /** Simple Tweaks liess Slot 38 (Brust) stehen; hier: derselbe Stapel wie im Brust-Slot. */
    private static boolean isChestSlot(ServerPlayer player, int index) {
        return player.getInventory().getItem(index) == player.getItemBySlot(EquipmentSlot.CHEST);
    }
}
