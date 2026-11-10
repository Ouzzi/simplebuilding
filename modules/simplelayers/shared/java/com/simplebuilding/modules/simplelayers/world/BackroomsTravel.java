package com.simplebuilding.modules.simplelayers.world;

import com.simplebuilding.modules.simplelayers.SimpleLayers;
import com.simplebuilding.modules.simplelayers.config.LayersConfig;
import com.simplebuilding.modules.simplelayers.world.gen.BackroomsLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
 * Ways into, between and out of the Backrooms (concept §2.1, BACK18 levels): the rift of a broken Glitched Bedrock or at the bottom
 * of the entrance shaft (any entity, into Level 0), rifts inside the Backrooms (walls: one level deeper; floor holes: the
 * Poolrooms), Maintenance Doors, optionally the void below the Overworld ({@code backrooms.voidToBackrooms}), and the Exit Door
 * back to the Overworld surface above the entry (with a Backdoor Key: 1500-3000 blocks away). Arrival is always the level's free
 * arrival spot above the same x/z ({@link BackroomsLevel#arrival}); players see the level's name in the action bar (A14).
 */
public final class BackroomsTravel {
    public static final double ARRIVAL_Y = BackroomsGrid.FLOOR_Y + 1;
    public static final int COOLDOWN = 100, FAR_MIN = 1500, FAR_MAX = 3000;

    private BackroomsTravel() {}

    /** Rift contact at {@code pos}: everything but riders goes in, once per cooldown; returns the entity in the new level (or null). */
    public static @Nullable Entity enterFromRift(ServerLevel level, Entity entity, BlockPos pos) {
        if (!SimpleLayers.backroomsOn() || entity.isPassenger() || entity.isOnPortalCooldown() || !entity.canUsePortal(false)) return null;
        BackroomsLevel here = SimpleLayers.levelOf(level);
        BackroomsLevel target = here == null ? BackroomsLevel.LOBBY : here.riftTarget(pos.getY());
        return target == null ? null : travel(level.getServer(), entity, target);
    }

    /** Sends {@code entity} into Level 0 below its x/z; returns the entity in the new level (or null). */
    public static @Nullable Entity enter(MinecraftServer server, Entity entity) {
        return travel(server, entity, BackroomsLevel.LOBBY);
    }

    /** Sends {@code entity} to the arrival spot of {@code target} above its x/z (null when that level is not loaded). */
    public static @Nullable Entity travel(MinecraftServer server, Entity entity, BackroomsLevel target) {
        ServerLevel to = server.getLevel(SimpleLayers.levelKey(target));
        if (to == null) return null;
        if (entity instanceof ServerPlayer player && !SimpleLayers.inBackrooms(player.level())) {
            BackroomsEntries.get(server).put(player.getUUID(), player.blockPosition());
        }
        int[] spot = target.arrival(BackroomsChunkGenerator.seed(to.getChunkSource().randomState()), entity.getBlockX(), entity.getBlockZ());
        Vec3 pos = new Vec3(spot[0] + 0.5, spot[1], spot[2] + 0.5);
        // Load/generate the arrival chunk first (like surface() for the exit): the copy joins a loaded entity section at once instead of
        // waiting in a hidden one until the portal ticket's chunk is generated (FIX172).
        to.getChunk(spot[0] >> 4, spot[2] >> 4);
        Entity moved = entity.teleport(new TeleportTransition(to, pos, Vec3.ZERO, entity.getYRot(), entity.getXRot(), TeleportTransition.PLACE_PORTAL_TICKET));
        if (moved != null) {
            moved.setPortalCooldown(COOLDOWN);
            moved.resetFallDistance();
            to.playSound(null, moved.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 0.5F, 0.6F);
            if (moved instanceof ServerPlayer player) player.sendOverlayMessage(Component.translatable("message.simplelayers.level." + target.id));
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
        return SimpleLayers.inBackrooms(level);
    }
}
