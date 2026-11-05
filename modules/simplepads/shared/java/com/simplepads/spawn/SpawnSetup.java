package com.simplepads.spawn;

import com.simplepads.SimplePads;
import com.simplepads.PadsConfig;
import com.simplepads.block.PadsBlocks;
import com.simplepads.util.PadsVersion;
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
 * Erstbeitritt (Simple Tweaks: FirstJoinHandler): Spawn-Teleporter und Elytra-Pads als Geschenk. Der eigene Weltspawn
 * bleibt in SimpleBuilding (keine Pads).
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
        PadsConfig config = SimplePads.config();
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
            ItemStack teleporter = new ItemStack(PadsBlocks.SPAWN_TELEPORTER);
            teleporter.set(DataComponents.CUSTOM_NAME,
                    Component.translatable("item.simplepads.spawn_teleporter.home_name").withStyle(ChatFormatting.AQUA));
            give(player, teleporter, teleporters);
        }
        // Simple Tweaks gab das Pad nur, wenn der Teleporter NICHT ins Inventar passte; behoben.
        if (elytraPads > 0) {
            give(player, new ItemStack(PadsBlocks.ELYTRA_PAD), elytraPads);
        }
        Component message;
        if (teleporters > 0 && elytraPads > 0) {
            message = Component.translatable("message.simplepads.first_join", teleporters, elytraPads);
        } else if (teleporters > 0) {
            message = Component.translatable("message.simplepads.first_join.teleporters", teleporters);
        } else if (elytraPads > 0) {
            message = Component.translatable("message.simplepads.first_join.elytra_pads", elytraPads);
        } else {
            return;
        }
        player.sendSystemMessage(message.copy().withStyle(ChatFormatting.GREEN));
    }

    /**
     * Gibt {@code count} Stueck einzeln (Pads stapeln seit 2026-09-28 nicht); was nicht ins Inventar
     * passt, faellt vor die Fuesse.
     */
    private static void give(ServerPlayer player, ItemStack template, int count) {
        for (int i = 0; i < count; i++) {
            ItemStack one = template.copyWithCount(1);
            if (!player.getInventory().add(one)) {
                PadsVersion.drop(player, one, false, false);
            }
        }
    }
}
