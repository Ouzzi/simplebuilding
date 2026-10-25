package com.simplebuilding.modules.simpleearth.orbit;

import com.simplebuilding.modules.simpleearth.SimpleEarth;
import com.simplebuilding.modules.simpleearth.config.EarthServerConfig;
import com.simplebuilding.modules.simpleearth.net.OrbitConfigPayload;
import com.simplebuilding.modules.simpleearth.net.OrbitMovePayload;
import com.simplebuilding.modules.simpleearth.net.OrbitStatePayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.border.WorldBorder;

/**
 * Server side of the orbit (concept §6.2, E4/E6): above the activation height the point (x, z) becomes the centre A of the
 * player's earth; above the render switch the figure is parked (stays over A's neighbourhood, only the orbit position glides,
 * capped by the orbit speed) - so the server never loads chunks for a 256 b/s flight. Below the switch the player re-enters
 * at the orbit position with slow falling; below the activation height everything is Vanilla again. Only players whose
 * client has Simple Earth take part.
 */
public final class OrbitTracker {
    /** Below the switch by this many blocks before re-entry, below the activation height before switching off (no flicker). */
    public static final int HYSTERESIS = 16;
    private static final Map<UUID, State> STATES = new HashMap<>();

    public static final class State {
        public boolean active, parked;
        public double ax, az, ox, oz, parkX, parkZ;
        public int activationY = 1024, switchY = 1536, edge = 65536;
        public double speed = 256;
        int sendIn;
        boolean dirty = true;

        /** Applies (and clamps) a client wish. */
        public void configure(int activation, int sw, int e, double spd, int maxBuildY) {
            activationY = EarthServerConfig.activationY(activation, maxBuildY);
            switchY = EarthServerConfig.switchY(sw, activationY);
            edge = EarthServerConfig.edge(e);
            speed = EarthServerConfig.speed(spd);
            dirty = true;
        }
    }

    private OrbitTracker() {}

    public static synchronized State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> {
            State s = new State();
            s.configure(1024, 1536, 65536, 256, player.level().getMaxY());
            return s;
        });
    }

    public static synchronized State peek(UUID id) {
        return STATES.get(id);
    }

    public static synchronized void forget(UUID id) {
        STATES.remove(id);
    }

    public static synchronized void clear() {
        STATES.clear();
    }

    public static boolean allowed(ServerPlayer player) {
        return SimpleEarth.on() && EarthServerConfig.dimensions.contains(player.level().dimension().identifier().toString());
    }

    public static void handleConfig(ServerPlayer player, OrbitConfigPayload p) {
        State s = state(player);
        s.configure(p.activationY(), p.switchY(), p.edge(), p.speed(), player.level().getMaxY());
    }

    /**
     * A glide step from the client: only while parked, capped to the server limit {@code maxOrbitSpeed} (plus 25 % for tick
     * jitter; the client scales the orbit speed with its height), stopped at the border. Parked players load no chunks.
     */
    public static void handleMove(ServerPlayer player, OrbitMovePayload p) {
        State s = state(player);
        if (!s.parked || !Float.isFinite(p.dx()) || !Float.isFinite(p.dz())) return;
        double dx = p.dx(), dz = p.dz(), len = Math.hypot(dx, dz), cap = EarthServerConfig.maxOrbitSpeed / 20.0 * 1.25;
        if (len > cap) {
            dx *= cap / len;
            dz *= cap / len;
        }
        WorldBorder border = player.level().getWorldBorder();
        s.ox = Math.max(border.getMinX() + 1, Math.min(border.getMaxX() - 1, s.ox + dx));
        s.oz = Math.max(border.getMinZ() + 1, Math.min(border.getMaxZ() - 1, s.oz + dz));
    }

    /** Once per server tick for every player. */
    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) tick(player, SimpleEarth.canSend.test(player, OrbitStatePayload.TYPE));
    }

    public static void tick(ServerPlayer player, boolean hasMod) {
        State s = state(player);
        if (!hasMod || !allowed(player) || player.isSpectator() && !s.active) {
            if (s.active) leave(player, s);
            return;
        }
        double y = player.getY();
        if (!s.active) {
            if (y >= s.activationY && (!EarthServerConfig.requireFlight || flying(player))) {
                s.active = true;
                s.ax = centre(player.getX(), s.edge);
                s.az = centre(player.getZ(), s.edge);
                s.ox = player.getX();
                s.oz = player.getZ();
                s.dirty = true;
            }
        } else if (y < s.activationY - HYSTERESIS) {
            leave(player, s);
        } else if (!s.parked && y >= s.switchY) {
            s.parked = true;
            s.parkX = player.getX();
            s.parkZ = player.getZ();
            s.ox = s.parkX;
            s.oz = s.parkZ;
            s.dirty = true;
        } else if (s.parked && y < s.switchY - HYSTERESIS) {
            reenter(player, s);
        } else if (s.parked) {
            // keep the figure over the park point (the client holds it; a client that does not is pulled back)
            if (Math.hypot(player.getX() - s.parkX, player.getZ() - s.parkZ) > 3) {
                player.teleportTo(player.level(), s.parkX, y, s.parkZ, Set.of(), player.getYRot(), player.getXRot(), false);
            }
        } else {
            s.ox = player.getX();
            s.oz = player.getZ();
        }
        if (s.dirty || (s.active && --s.sendIn <= 0)) send(player, s);
    }

    /** E4: the cube centre for a crossing at {@code c} - there (standard) or the nearest point of the global edge grid. */
    public static double centre(double c, int edge) {
        return EarthServerConfig.globalGrid ? Math.round(c / edge) * (double) edge : c;
    }

    /** E5 with {@code requireFlight}: creative flight or elytra. */
    static boolean flying(ServerPlayer player) {
        return player.getAbilities().flying || player.isFallFlying();
    }

    /** Re-entry (E6): to the orbit position (or with reentryAtOrbit off the park point) at the current height, slow falling unless flying. */
    static void reenter(ServerPlayer player, State s) {
        s.parked = false;
        ServerLevel level = player.level();
        if (!EarthServerConfig.reentryAtOrbit) {
            s.ox = s.parkX;
            s.oz = s.parkZ;
        }
        player.teleportTo(level, s.ox, player.getY(), s.oz, Set.of(), player.getYRot(), player.getXRot(), false);
        if (!player.getAbilities().flying && EarthServerConfig.slowFallSeconds > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, EarthServerConfig.slowFallSeconds * 20, 0, false, false));
        }
        s.dirty = true;
    }

    static void leave(ServerPlayer player, State s) {
        if (s.parked) reenter(player, s);
        s.active = false;
        s.parked = false;
        s.dirty = true;
        send(player, s);
    }

    /** On logout while parked: drop the figure at the orbit position so the next login starts there (same height). */
    public static void disconnect(ServerPlayer player) {
        State s = peek(player.getUUID());
        if (s != null && s.parked) {
            if (EarthServerConfig.reentryAtOrbit) player.setPos(s.ox, player.getY(), s.oz);
            else player.setPos(s.parkX, player.getY(), s.parkZ);
        }
        forget(player.getUUID());
    }

    static void send(ServerPlayer player, State s) {
        s.dirty = false;
        s.sendIn = 20;
        List<float[]> others = new ArrayList<>();
        if (EarthServerConfig.showOthers && s.active) {
            for (ServerPlayer o : player.level().players()) {
                State os = o == player ? null : peek(o.getUUID());
                if (os != null && os.parked && others.size() < OrbitStatePayload.MAX_OTHERS) others.add(new float[]{(float) os.ox, (float) os.oz});
            }
        }
        SimpleEarth.toClient.accept(player, new OrbitStatePayload(allowed(player), s.active, s.parked, s.ax, s.az, s.ox, s.oz,
                s.activationY, s.switchY, s.edge, (float) s.speed, player.level().getChunkSource().getGenerator().getSeaLevel(),
                EarthServerConfig.exploredOnly, EarthServerConfig.minGrid, EarthServerConfig.maxOrbitSpeed, others));
    }
}
