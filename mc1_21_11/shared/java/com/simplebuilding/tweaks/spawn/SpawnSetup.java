package com.simplebuilding.tweaks.spawn;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.TweaksConfig;
import com.simplebuilding.tweaks.block.TweaksBlocks;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelData;

/**
 * Erstbeitritt und eigener Weltspawn (Simple Tweaks: FirstJoinHandler, WorldSpawnHandler).
 */
public final class SpawnSetup {
    /**
     * Derselbe Spieler-Tag wie in Simple Tweaks: wer die Geschenke dort schon bekommen hat, bekommt
     * sie nach dem Umstieg nicht noch einmal.
     */
    public static final String FIRST_JOIN_TAG = "simpletweaks.first_join";

    private SpawnSetup() {
    }

    public static void onPlayerJoin(ServerPlayer player) {
        if (player.getTags().contains(FIRST_JOIN_TAG)) {
            return;
        }
        TweaksConfig config = SimpleTweaks.config();
        // Eine abgeschaltete Familie (pads.enable...) wird auch nicht verschenkt.
        int teleporters = config.pads.enableSpawnTeleporters ? clampGift(config.spawn.firstJoinTeleporterCount) : 0;
        int elytraPads = config.pads.enableElytraPads ? clampGift(config.spawn.firstJoinElytraPadCount) : 0;
        giveStarterItems(player, teleporters, elytraPads);
        player.addTag(FIRST_JOIN_TAG);
    }

    /** Config-Menge auf 0..64 begrenzt, wie im Befehl {@code /simplebuilding tweaks spawn}. */
    public static int clampGift(int amount) {
        return Math.max(0, Math.min(64, amount));
    }

    /**
     * Gibt {@code teleporters} Spawn-Teleporter und {@code elytraPads} Elytra-Pads (je 0 = keine);
     * seit 2026-09-26 zwei getrennte Werte, beide standardmaessig 0.
     */
    public static void giveStarterItems(ServerPlayer player, int teleporters, int elytraPads) {
        if (teleporters > 0) {
            ItemStack teleporter = new ItemStack(TweaksBlocks.SPAWN_TELEPORTER, teleporters);
            teleporter.set(DataComponents.CUSTOM_NAME,
                    Component.translatable("item.simplebuilding.home_teleporter").withStyle(ChatFormatting.AQUA));
            if (!player.getInventory().add(teleporter)) {
                player.drop(teleporter, false);
            }
        }
        // Simple Tweaks gab das Pad nur, wenn der Teleporter NICHT ins Inventar passte; behoben.
        if (elytraPads > 0) {
            ItemStack elytraPad = new ItemStack(TweaksBlocks.ELYTRA_PAD, elytraPads);
            if (!player.getInventory().add(elytraPad)) {
                player.drop(elytraPad, false);
            }
        }
        Component message;
        if (teleporters > 0 && elytraPads > 0) {
            message = Component.translatable("message.simplebuilding.first_join", teleporters, elytraPads);
        } else if (teleporters > 0) {
            message = Component.translatable("message.simplebuilding.first_join.teleporters", teleporters);
        } else if (elytraPads > 0) {
            message = Component.translatable("message.simplebuilding.first_join.elytra_pads", elytraPads);
        } else {
            return;
        }
        player.sendSystemMessage(message.copy().withStyle(ChatFormatting.GREEN));
    }

    /** Beim Laden der Oberwelt: Weltspawn auf die Config-Koordinaten setzen (y = -1: oberster Block). */
    public static void onLevelLoad(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        TweaksConfig.Spawn config = SimpleTweaks.config().spawn;
        if (!config.useCustomWorldSpawn) {
            return;
        }
        BlockPos target = resolveWorldSpawn(level, config.xCoordSpawnPoint, config.yCoordSpawnPoint, config.zCoordSpawnPoint);
        if (!level.getRespawnData().pos().equals(target)) {
            level.setRespawnData(LevelData.RespawnData.of(level.dimension(), target, 0.0f, 0.0f));
            Simplebuilding.LOGGER.info("Simple Tweaks: Weltspawn auf {} gesetzt", target.toShortString());
        }
    }

    public static BlockPos resolveWorldSpawn(ServerLevel level, int x, int y, int z) {
        if (y == -1) {
            level.getChunk(x >> 4, z >> 4);
            y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            if (y <= level.getMinY()) {
                y = 70;
                Simplebuilding.LOGGER.warn("Simple Tweaks: keine sichere Spawnhoehe gefunden, setze 70");
            }
        }
        return new BlockPos(x, y, z);
    }
}
