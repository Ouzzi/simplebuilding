package com.simplebuilding.modules.simplelayers.world;

import com.simplebuilding.modules.simplelayers.SimpleLayers;
import com.simplebuilding.modules.simplelayers.config.LayersConfig;
import com.simplebuilding.modules.simplelayers.backrooms.stack.BackroomsStackGenerator;
import com.simplebuilding.modules.simplelayers.backrooms.stack.BandStack;
import com.simplebuilding.modules.simplelayers.backrooms.stack.Bands;
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
 * Ways into and out of the stacked Backrooms besides the portal doors (BACK22 core K6): the old rift contact and the void option
 * lead to the Lobby (level 1) above the same x/z, a stray Maintenance Door one level deeper, the Exit Door back to the Overworld
 * surface above the entry (with a Backdoor Key: 1500-3000 blocks away). Arrival is the level's free spot ({@code BandLayout.arrival})
 * in a loaded chunk (FIX172); players see the level's name in the action bar (A14).
 */
public final class BackroomsTravel {
    public static final int COOLDOWN = 100, FAR_MIN = 1500, FAR_MAX = 3000;

    private BackroomsTravel() {}

    /** Rift contact: everything but riders goes into the Lobby (the rift is no longer generated, fg11 a), once per cooldown. */
    public static @Nullable Entity enterFromRift(ServerLevel level, Entity entity, BlockPos pos) {
        if (!SimpleLayers.backroomsOn() || entity.isPassenger() || entity.isOnPortalCooldown() || !entity.canUsePortal(false)) return null;
        int here = SimpleLayers.bandOf(level, pos.getY());
        return travel(level.getServer(), entity, here == 0 ? 1 : Math.min(BandStack.BANDS, here + 1));
    }

    /** Sends {@code entity} into the Lobby below its x/z; returns the entity in the new level (or null). */
    public static @Nullable Entity enter(MinecraftServer server, Entity entity) {
        return travel(server, entity, 1);
    }

    /** The free arrival spot of level {@code band} above (x, z) in world coordinates. */
    public static BlockPos arrival(long seed, int band, int x, int z) {
        int[] spot = Bands.get(band).layout().arrival(seed, x, z);
        return new BlockPos(spot[0], BandStack.worldY(band, spot[1]), spot[2]);
    }

    /** Sends {@code entity} to the arrival spot of level {@code band} above its x/z (null when the stack is not loaded). */
    public static @Nullable Entity travel(MinecraftServer server, Entity entity, int band) {
        ServerLevel to = server.getLevel(SimpleLayers.STACK);
        if (to == null) return null;
        if (entity instanceof ServerPlayer player && !SimpleLayers.inBackrooms(player.level())) {
            BackroomsEntries.get(server).put(player.getUUID(), player.blockPosition());
        }
        BlockPos spot = arrival(BackroomsStackGenerator.seed(to.getChunkSource().randomState()), band, entity.getBlockX(), entity.getBlockZ());
        Vec3 pos = Vec3.atBottomCenterOf(spot);
        // Load/generate the arrival chunk first: the copy joins a loaded entity section at once (FIX172).
        to.getChunk(spot.getX() >> 4, spot.getZ() >> 4);
        Entity moved = entity.teleport(new TeleportTransition(to, pos, Vec3.ZERO, entity.getYRot(), entity.getXRot(), TeleportTransition.PLACE_PORTAL_TICKET));
        if (moved != null) {
            moved.setPortalCooldown(COOLDOWN);
            moved.resetFallDistance();
            to.playSound(null, moved.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 0.5F, 0.6F);
            if (moved instanceof ServerPlayer player) player.sendOverlayMessage(Component.translatable("message.simplelayers.level." + Bands.get(band).id()));
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
