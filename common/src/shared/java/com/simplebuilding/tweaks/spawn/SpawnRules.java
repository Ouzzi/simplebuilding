package com.simplebuilding.tweaks.spawn;

import com.simplebuilding.tweaks.SimpleTweaks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Regeln hinter den ServerPlayer-Mixins: gesperrte Dimensionen und exakter Spawn. */
public final class SpawnRules {
    private SpawnRules() {
    }

    public static boolean forceExactSpawn() {
        return SimpleTweaks.config().spawn.forceExactSpawn;
    }

    /** true = Wechsel in diese Welt abbrechen (Nether/End per Config gesperrt), mit Meldung. */
    public static boolean blocksDimensionChange(ServerPlayer player, @Nullable ServerLevel destination) {
        if (destination == null || destination == player.level()) {
            return false;
        }
        if (destination.dimension() == Level.NETHER && !SimpleTweaks.config().dimensions.allowNether) {
            player.sendOverlayMessage(Component.translatable("message.simplebuilding.dimension.nether_disabled").withStyle(ChatFormatting.RED));
            return true;
        }
        if (destination.dimension() == Level.END && !SimpleTweaks.config().dimensions.allowEnd) {
            player.sendOverlayMessage(Component.translatable("message.simplebuilding.dimension.end_disabled").withStyle(ChatFormatting.RED));
            return true;
        }
        return false;
    }

    /**
     * Exakter Wiedereinstieg: im Bett genau auf der Bettmitte, ohne eigenen Wiedereinstiegspunkt genau
     * auf der Mitte des Weltspawn-Blocks. null = unveraendert lassen.
     */
    public static @Nullable TeleportTransition exactRespawn(ServerPlayer player, @Nullable TeleportTransition original,
                                                            TeleportTransition.PostTeleportTransition post) {
        if (!forceExactSpawn() || original == null) {
            return null;
        }
        ServerLevel level = original.newLevel();
        BlockPos pos = BlockPos.containing(original.position());
        if (level.getBlockState(pos).getBlock() instanceof BedBlock) {
            Vec3 exact = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5625, pos.getZ() + 0.5);
            return new TeleportTransition(level, exact, Vec3.ZERO, original.yRot(), original.xRot(), post);
        }
        if (player.getRespawnConfig() == null) {
            BlockPos spawn = level.getRespawnData().pos();
            Vec3 exact = new Vec3(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
            return new TeleportTransition(level, exact, Vec3.ZERO, original.yRot(), original.xRot(), post);
        }
        return null;
    }
}
