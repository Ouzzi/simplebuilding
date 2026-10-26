package com.simplebuilding.modules.simpleearth.orbit;

import com.simplebuilding.modules.simpleearth.SimpleEarth;
import com.simplebuilding.modules.simpleearth.config.EarthServerConfig;
import com.simplebuilding.modules.simpleearth.net.OrbitConfigPayload;
import com.simplebuilding.modules.simpleearth.net.OrbitStatePayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server side of the orbit (concept §6.2, E4; WG26-1 owner 26.10.: no parking). Above the activation height the point
 * (x, z) becomes the fixed centre A of the player's earth; from then on the player moves freely - creative flight, elytra,
 * anything - and where he is relative to A is where he is over the earth, like a map: with an edge of 2048 blocks, 1024
 * blocks north/east/south/west the face edge lies under him. Nothing holds, pulls back or teleports the player; below the
 * activation height everything is Vanilla again. Only players whose client has Simple Earth take part.
 */
public final class OrbitTracker {
    /** Below the activation height by this many blocks before switching off (no flicker). */
    public static final int HYSTERESIS = 16;
    private static final Map<UUID, State> STATES = new HashMap<>();

    public static final class State {
        /** active: above the activation height (A fixed); parked: above the render switch (the earth is shown, nothing held). */
        public boolean active, parked;
        public double ax, az, ox, oz;
        public int activationY = 1024, switchY = 1536, edge = EDGE_DEFAULT;
        int sendIn;
        boolean dirty = true;

        /** Applies (and clamps) a client wish. */
        public void configure(int activation, int sw, int e, int maxBuildY) {
            activationY = EarthServerConfig.activationY(activation, maxBuildY);
            switchY = EarthServerConfig.switchY(sw, activationY);
            edge = EarthServerConfig.edge(e);
            dirty = true;
        }
    }

    /** E1 standard since WG26-1 (owner's example: an earth 2048 blocks wide). */
    public static final int EDGE_DEFAULT = 2048;

    private OrbitTracker() {}

    public static synchronized State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> {
            State s = new State();
            s.configure(1024, 1536, EDGE_DEFAULT, player.level().getMaxY());
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
        s.configure(p.activationY(), p.switchY(), p.edge(), player.level().getMaxY());
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
                s.dirty = true;
            }
        } else if (y < s.activationY - HYSTERESIS) {
            leave(player, s);
            return;
        }
        if (!s.active) return;
        s.ox = player.getX();
        s.oz = player.getZ();
        boolean above = y >= s.switchY;
        if (above != s.parked) {
            s.parked = above;
            s.dirty = true;
        }
        if (s.dirty || --s.sendIn <= 0) send(player, s);
    }

    /** E4: the cube centre for a crossing at {@code c} - there (standard) or the nearest point of the global edge grid. */
    public static double centre(double c, int edge) {
        return EarthServerConfig.globalGrid ? Math.round(c / edge) * (double) edge : c;
    }

    /** E5 with {@code requireFlight}: creative flight or elytra. */
    static boolean flying(ServerPlayer player) {
        return player.getAbilities().flying || player.isFallFlying();
    }

    static void leave(ServerPlayer player, State s) {
        s.active = false;
        s.parked = false;
        s.dirty = true;
        send(player, s);
    }

    public static void disconnect(ServerPlayer player) {
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
                s.activationY, s.switchY, s.edge, player.level().getChunkSource().getGenerator().getSeaLevel(),
                EarthServerConfig.exploredOnly, EarthServerConfig.minGrid, others));
    }
}
