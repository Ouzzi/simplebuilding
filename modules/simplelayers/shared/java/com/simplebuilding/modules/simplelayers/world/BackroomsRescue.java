package com.simplebuilding.modules.simplelayers.world;

import com.simplebuilding.modules.simplelayers.SimpleLayers;
import com.simplebuilding.modules.simplelayers.backrooms.stack.BandStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Migration of BACK18 worlds (BACK22 core K6, concept §7): the five old Backrooms dimensions are gone. A player saved in one of
 * them is put into the Overworld by Vanilla at the old coordinates (y 0..63) - usually inside the ground. On joining, a player in
 * the Overworld who still has a Backrooms entry ({@link BackroomsEntries}, cleared by every exit and by respawning outside) and
 * stands below the surface in that height band is moved onto the surface above his entry point. Also: the stack's own players
 * see only entities of their level ({@code TrackingFilters}, concept §3.4).
 */
public final class BackroomsRescue {
    /** Height band of the old 64-high Backrooms dimensions. */
    public static final int OLD_MIN_Y = 0, OLD_MAX_Y = 64;

    private BackroomsRescue() {}

    /** Whether a joining player looks stranded from an old Backrooms dimension. */
    public static boolean stranded(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (player.level() != server.overworld() || BackroomsEntries.get(server).get(player.getUUID()) == null) return false;
        double y = player.getY();
        if (y < OLD_MIN_Y || y >= OLD_MAX_Y) return false;
        ServerLevel overworld = server.overworld();
        int top = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, player.getBlockX(), player.getBlockZ());
        return y < top - 2;
    }

    /** Moves a stranded player onto the surface above his entry point; returns whether it did. */
    public static boolean onJoin(ServerPlayer player) {
        if (!stranded(player)) return false;
        MinecraftServer server = player.level().getServer();
        BlockPos entry = BackroomsEntries.get(server).get(player.getUUID());
        Vec3 pos = BackroomsTravel.surface(server.overworld(), entry.getX(), entry.getZ());
        Entity moved = player.teleport(new TeleportTransition(server.overworld(), pos, Vec3.ZERO, player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING));
        if (moved == null) return false;
        BackroomsEntries.get(server).remove(player.getUUID());
        SimpleLayers.LOG.info("BACK22 migration: moved {} out of an old Backrooms dimension to {}", player.getName().getString(), moved.blockPosition());
        return true;
    }

    /** After a respawn outside the stack the entry is stale (died inside): forget it. */
    public static void afterRespawn(ServerPlayer player) {
        if (!SimpleLayers.inBackrooms(player.level())) BackroomsEntries.get(player.level().getServer()).remove(player.getUUID());
    }

    /** Tracking filter: inside the stack only entities of the same level (nearest level for the bedrock between). */
    public static boolean sameLevel(ServerPlayer viewer, Entity entity) {
        return sameLevel(viewer.level(), viewer.getY(), entity.level(), entity.getY());
    }

    public static boolean sameLevel(net.minecraft.world.level.Level viewerLevel, double viewerY, net.minecraft.world.level.Level entityLevel, double entityY) {
        if (!SimpleLayers.inBackrooms(viewerLevel) || entityLevel != viewerLevel) return true;
        return BandStack.nearestBand((int) Math.floor(viewerY)) == BandStack.nearestBand((int) Math.floor(entityY));
    }
}
