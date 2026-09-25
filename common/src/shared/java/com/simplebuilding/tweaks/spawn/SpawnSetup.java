package com.simplebuilding.tweaks.spawn;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.TweaksConfig;
import com.simplebuilding.tweaks.block.TweaksBlocks;
import com.simplebuilding.version.McVersion;
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
        if (player.entityTags().contains(FIRST_JOIN_TAG)) {
            return;
        }
        int amount = Math.max(0, Math.min(64, SimpleTweaks.config().spawn.spawnTeleporterCount));
        if (amount > 0) {
            giveStarterItems(player, amount);
        }
        player.addTag(FIRST_JOIN_TAG);
    }

    public static void giveStarterItems(ServerPlayer player, int amount) {
        ItemStack teleporter = new ItemStack(TweaksBlocks.SPAWN_TELEPORTER, amount);
        teleporter.set(DataComponents.CUSTOM_NAME,
                Component.translatable("item.simplebuilding.home_teleporter").withStyle(ChatFormatting.AQUA));
        ItemStack elytraPad = new ItemStack(TweaksBlocks.ELYTRA_PAD, amount);
        // Simple Tweaks gab das Pad nur, wenn der Teleporter NICHT ins Inventar passte; behoben.
        if (!player.getInventory().add(teleporter)) {
            McVersion.drop(player, teleporter, false, false);
        }
        if (!player.getInventory().add(elytraPad)) {
            McVersion.drop(player, elytraPad, false, false);
        }
        player.sendSystemMessage(Component.translatable("message.simplebuilding.first_join", amount).withStyle(ChatFormatting.GREEN));
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
