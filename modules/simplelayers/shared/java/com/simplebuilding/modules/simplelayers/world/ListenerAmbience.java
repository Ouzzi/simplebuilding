package com.simplebuilding.modules.simplelayers.world;

import com.simplebuilding.modules.simplelayers.LayersRegistry;
import com.simplebuilding.modules.simplelayers.SimpleLayers;
import com.simplebuilding.modules.simplelayers.config.LayersConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * "The Listener" (concept §2.1, owner question 2 answered by the plan: passive atmosphere only). With
 * {@code backrooms.listener} on, now and then (about every 4-8 minutes per player) footsteps sound behind the player, 20
 * blocks away, come closer in five steps over a few seconds and stop. Only this player hears them. No creature, no damage,
 * no effect.
 */
public final class ListenerAmbience {
    static final int STEPS = 5, STEP_TICKS = 16, START_DISTANCE = 20;
    private static final Map<UUID, int[]> STATE = new HashMap<>(); // {ticks to next event, step, ticks to next step}

    private ListenerAmbience() {}

    public static void tick(MinecraftServer server) {
        if (!SimpleLayers.backroomsOn() || !LayersConfig.listener) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!SimpleLayers.inBackrooms(player.level()) || player.isSpectator()) continue;
            int[] s = STATE.computeIfAbsent(player.getUUID(), id -> new int[] {nextDelay(player), -1, 0});
            if (s[1] < 0) {
                if (--s[0] <= 0) {
                    s[1] = 0;
                    s[2] = 0;
                }
                continue;
            }
            if (--s[2] > 0) continue;
            step(player, s[1]);
            s[2] = STEP_TICKS;
            if (++s[1] >= STEPS) {
                s[1] = -1;
                s[0] = nextDelay(player);
            }
        }
    }

    /** One step of an approach: behind the player, from {@link #START_DISTANCE} blocks closer each step. */
    static void step(ServerPlayer player, int step) {
        Vec3 back = Vec3.directionFromRotation(0.0F, player.getYRot()).scale(-1.0);
        double distance = START_DISTANCE - step * 3.0;
        Vec3 at = player.position().add(back.scale(distance));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(LayersRegistry.LISTENER_STEPS), SoundSource.HOSTILE,
                at.x, at.y, at.z, 0.9F, 0.9F + step * 0.02F, player.getRandom().nextLong()));
    }

    static int nextDelay(ServerPlayer player) {
        return 4800 + player.getRandom().nextInt(4800);
    }

    public static void forget(UUID player) {
        STATE.remove(player);
    }
}
