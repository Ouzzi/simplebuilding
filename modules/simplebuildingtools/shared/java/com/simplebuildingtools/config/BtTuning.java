package com.simplebuildingtools.config;

import com.simplebuildingtools.SimpleBuildingTools;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/** Read access to {@link BtConfig} as SimpleBuilding's {@code ServerTuning} offered it (on a client the server's values). */
public final class BtTuning {
    private BtTuning() {
    }

    public static int blueprintBlocksPerTick() {
        return SimpleBuildingTools.config().blueprint.maxBlocksPerTick;
    }

    /** Picture import on (server switch); on a client the server's value. */
    public static boolean pictureImport() {
        return SimpleBuildingTools.config().blueprint.pictureImport;
    }

    /** Permission level needed for the picture import (0..4, 0 = everyone). */
    public static int picturePermission() {
        return SimpleBuildingTools.config().blueprint.picturePermission;
    }

    /** Longest picture side in blocks (16..128); on a client the server's value. */
    public static int pictureMaxSide() {
        return SimpleBuildingTools.config().blueprint.pictureMaxSide;
    }

    public static boolean wandHungerCost() {
        return SimpleBuildingTools.config().wands.hungerCost;
    }

    public static double wandHungerMultiplier() {
        return SimpleBuildingTools.config().wands.hungerMultiplier;
    }

    /** True (and a red action bar line for a server player) when the blueprint is switched off. */
    public static boolean blueprintDenied(@Nullable Player player) {
        if (SimpleBuildingTools.config().blueprint.enabled) return false;
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.simplebuildingtools.feature_disabled").withStyle(ChatFormatting.RED));
        }
        return true;
    }
}
