package com.simplebuilding.modules.simplelayers.world;

import com.simplebuilding.modules.simplelayers.LayersRegistry;
import com.simplebuilding.modules.simplelayers.SimpleLayers;
import com.simplebuilding.modules.simplelayers.config.LayersConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The way between the Overworld and the Cloud Sea (concept §1.2 variant B). Rising in the Overworld from at most
 * {@code clouds.entryY} to above it leads into the Cloud Sea at y {@link #ARRIVAL_Y} over the same x/z (scale 1:1); falling below
 * y 0 there leads back to the Overworld 20 blocks under the entry height, with 3 s Slow Falling. Only upward crossings count, so
 * nobody bounces at the border (hysteresis 20). Riders travel with their mount (the root vehicle is moved, passengers follow).
 *
 * <p>Simple Earth (orbit view above its activation height): the Cloud Sea never catches players at or above the height a
 * {@link com.simplelib.api.HeightHandover} provider announces for the Overworld, and who rises above
 * {@link #HANDOVER_Y} in the Cloud Sea is handed to the Overworld just above that height. Without a provider the Cloud Sea
 * has a soft ceiling there.
 */
public final class CloudTransition {
    public static final double ARRIVAL_Y = 16.0;
    /** Above this the Cloud Sea hands over to the Overworld's orbit (or is a soft ceiling); the dimension is 512 high. */
    public static final int HANDOVER_Y = 500;
    public static final int SLOW_FALL_TICKS = 60;
    private static final Map<UUID, Double> LAST_Y = new HashMap<>();

    private CloudTransition() {}

    /** The rule alone (tested without a world): does this move from {@code prevY} to {@code y} enter the Cloud Sea? */
    public static boolean entersClouds(double prevY, double y, int entryY, OptionalInt takeover) {
        return prevY <= entryY && y > entryY && (takeover.isEmpty() || y < takeover.getAsInt());
    }

    public static void tick(MinecraftServer server) {
        if (!SimpleLayers.cloudsOn()) {
            LAST_Y.clear();
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator()) continue;
            double y = player.getY();
            Double prev = LAST_Y.put(player.getUUID(), y);
            var dim = player.level().dimension();
            if (dim == Level.OVERWORLD) {
                OptionalInt takeover = com.simplelib.api.HeightHandover.takeoverY(player, Level.OVERWORLD);
                if (prev != null && entersClouds(prev, y, LayersConfig.cloudEntryY, takeover)) {
                    toClouds(server, player.getRootVehicle());
                    LAST_Y.remove(player.getUUID());
                }
            } else if (dim == SimpleLayers.CLOUD_SEA) {
                if (y < 0.0) {
                    toOverworld(server, player.getRootVehicle(), LayersConfig.cloudReturnY(), true);
                    LAST_Y.remove(player.getUUID());
                } else if (y > HANDOVER_Y) {
                    OptionalInt takeover = com.simplelib.api.HeightHandover.takeoverY(player, Level.OVERWORLD);
                    if (takeover.isPresent() && takeover.getAsInt() > LayersConfig.cloudEntryY) {
                        toOverworld(server, player.getRootVehicle(), takeover.getAsInt() + 16, false);
                        LAST_Y.remove(player.getUUID());
                    } else {
                        softCeiling(player);
                    }
                }
            }
        }
    }

    /** Moves {@code entity} (with its passengers) into the Cloud Sea above its x/z; a 3 x 3 cloud platform catches those not flying. */
    public static @Nullable Entity toClouds(MinecraftServer server, Entity entity) {
        ServerLevel target = server.getLevel(SimpleLayers.CLOUD_SEA);
        if (target == null) return null;
        Vec3 pos = new Vec3(entity.getX(), freeY(target, BlockPos.containing(entity.getX(), ARRIVAL_Y, entity.getZ())), entity.getZ());
        boolean flying = entity instanceof net.minecraft.world.entity.LivingEntity living && living.isFallFlying()
                || entity instanceof ServerPlayer p && p.getAbilities().flying;
        if (!flying) platform(target, BlockPos.containing(pos).below());
        Entity moved = entity.teleport(new TeleportTransition(target, pos, entity.getDeltaMovement(), entity.getYRot(), entity.getXRot(),
                TeleportTransition.PLACE_PORTAL_TICKET));
        if (moved != null) moved.resetFallDistance();
        return moved;
    }

    /** Back to the Overworld at {@code y} over the same x/z; {@code fell} = from below (Slow Falling + a hint, A14). */
    public static @Nullable Entity toOverworld(MinecraftServer server, Entity entity, double y, boolean fell) {
        ServerLevel target = server.overworld();
        Vec3 pos = new Vec3(entity.getX(), y, entity.getZ());
        Entity moved = entity.teleport(new TeleportTransition(target, pos, entity.getDeltaMovement(), entity.getYRot(), entity.getXRot(),
                TeleportTransition.PLACE_PORTAL_TICKET));
        if (moved == null) return null;
        moved.resetFallDistance();
        if (fell) {
            for (Entity e : moved.getIndirectPassengers()) slowFall(e);
            slowFall(moved);
        }
        return moved;
    }

    private static void slowFall(Entity e) {
        if (e instanceof net.minecraft.world.entity.LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALL_TICKS, 0, false, false, true));
        }
        if (e instanceof ServerPlayer player) player.sendOverlayMessage(Component.translatable("simplelayers.travel.fell_from_clouds"));
    }

    /** {@link #ARRIVAL_Y}, or the first height above it with two free blocks (an island bottom there is never entered). */
    static double freeY(ServerLevel level, BlockPos start) {
        level.getChunk(start.getX() >> 4, start.getZ() >> 4);
        BlockPos.MutableBlockPos p = start.mutable();
        while (p.getY() < HANDOVER_Y - 2 && !(level.getBlockState(p).getCollisionShape(level, p).isEmpty()
                && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty())) p.move(0, 1, 0);
        return p.getY();
    }

    /** A 3 x 3 Cloud Block platform below {@code centre} where there is air (never replaces anything). */
    public static void platform(ServerLevel level, BlockPos centre) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = centre.offset(dx, 0, dz);
                if (level.getBlockState(p).isAir()) level.setBlock(p, LayersRegistry.CLOUD_BLOCK.defaultBlockState(), 3);
            }
        }
    }

    private static void softCeiling(ServerPlayer player) {
        Vec3 v = player.getDeltaMovement();
        if (v.y > 0.0) {
            player.setDeltaMovement(v.x, -0.1, v.z);
            player.needsSync = true;
        }
    }

    public static void forget(UUID player) {
        LAST_Y.remove(player);
    }
}
