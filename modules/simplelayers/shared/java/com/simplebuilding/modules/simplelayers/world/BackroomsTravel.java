package com.simplebuilding.modules.simplelayers.world;

import com.simplebuilding.modules.simplelayers.SimpleLayers;
import com.simplebuilding.modules.simplelayers.config.LayersConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Ways into and out of the Backrooms (concept §2.1): the rift of a broken Glitched Bedrock (any entity), optionally the void
 * below the Overworld ({@code backrooms.voidToBackrooms}), and the Exit Door back to the Overworld surface above the entry
 * (with a Backdoor Key: 1500-3000 blocks away). Arrival is always the centre of a grid cell at y 2 (never inside a wall).
 */
public final class BackroomsTravel {
    public static final double ARRIVAL_Y = BackroomsGrid.FLOOR_Y + 1;
    public static final int COOLDOWN = 100, FAR_MIN = 1500, FAR_MAX = 3000;

    private BackroomsTravel() {}

    /** Rift contact: everything but riders goes in, once per cooldown. */
    public static void enterFromRift(ServerLevel level, Entity entity) {
        if (!SimpleLayers.backroomsOn() || level.dimension() == SimpleLayers.BACKROOMS || entity.isPassenger() || entity.isOnPortalCooldown()
                || !entity.canUsePortal(false)) return;
        enter(level.getServer(), entity);
    }

    /** Sends {@code entity} into the Backrooms below its x/z; returns the entity in the new level (or null). */
    public static @Nullable Entity enter(MinecraftServer server, Entity entity) {
        ServerLevel target = server.getLevel(SimpleLayers.BACKROOMS);
        if (target == null) return null;
        if (entity instanceof ServerPlayer player) BackroomsEntries.get(server).put(player.getUUID(), player.blockPosition());
        Vec3 pos = new Vec3(BackroomsGrid.cellCentre(entity.getBlockX()) + 0.5, ARRIVAL_Y, BackroomsGrid.cellCentre(entity.getBlockZ()) + 0.5);
        Entity moved = entity.teleport(new TeleportTransition(target, pos, Vec3.ZERO, entity.getYRot(), entity.getXRot(), TeleportTransition.PLACE_PORTAL_TICKET));
        if (moved != null) {
            moved.setPortalCooldown(COOLDOWN);
            moved.resetFallDistance();
            target.playSound(null, moved.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 0.6F, 0.6F);
        }
        return moved;
    }

    /** Exit Door: back to the Overworld surface above the entry (or the world spawn); {@code far} = Backdoor Key. */
    public static boolean exit(ServerPlayer player, boolean far) {
        MinecraftServer server = player.level().getServer();
        ServerLevel overworld = server.overworld();
        BlockPos entry = BackroomsEntries.get(server).get(player.getUUID());
        if (entry == null) entry = overworld.getRespawnData().pos();
        int x = entry.getX(), z = entry.getZ();
        if (far) {
            RandomSource random = player.getRandom();
            double angle = random.nextDouble() * Math.PI * 2.0;
            int distance = FAR_MIN + random.nextInt(FAR_MAX - FAR_MIN + 1);
            x += (int) Math.round(Math.cos(angle) * distance);
            z += (int) Math.round(Math.sin(angle) * distance);
        }
        Vec3 pos = surface(overworld, x, z);
        ServerPlayer moved = (ServerPlayer) player.teleport(new TeleportTransition(overworld, pos, Vec3.ZERO, player.getYRot(), player.getXRot(),
                TeleportTransition.PLACE_PORTAL_TICKET));
        if (moved == null) return false;
        BackroomsEntries.get(server).remove(moved.getUUID());
        moved.setPortalCooldown(COOLDOWN);
        overworld.playSound(null, moved.blockPosition(), SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.8F);
        return true;
    }

    /** Standing position on the ground at (x, z) (loads/generates that chunk). */
    public static Vec3 surface(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new Vec3(x + 0.5, Math.max(level.getMinY() + 1, y), z + 0.5);
    }

    /** Option {@code backrooms.voidToBackrooms}: players 32 blocks below the Overworld floor fall into the Backrooms. */
    public static void tickVoid(MinecraftServer server) {
        if (!SimpleLayers.backroomsOn() || !LayersConfig.voidToBackrooms) return;
        ServerLevel overworld = server.overworld();
        for (ServerPlayer player : overworld.players()) {
            if (player.getY() < overworld.getMinY() - 32 && !player.isSpectator()) enter(server, player.getRootVehicle());
        }
    }

    public static boolean inBackrooms(Level level) {
        return level.dimension() == SimpleLayers.BACKROOMS;
    }
}
