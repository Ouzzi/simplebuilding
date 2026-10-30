package com.simplebuilding.modules.simplechallenges.core;

import com.simplebuilding.modules.simplechallenges.SimpleChallenges;
import com.simplebuilding.modules.simplechallenges.unlock.UnlockGrid;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Keeps the borders (S1). Three layers: the entity collision (mixin {@code Entity.collide}: every entity on both sides stops at a
 * locked cell like at a wall, no entity border, no rubber banding), block changes inside locked cells are refused (mixin
 * {@code Level.setBlock}, server), and a tick that brings anyone found inside a locked cell (teleport, ender pearl, respawn,
 * spawn) back to the nearest free cell. All of it starts with the {@link Gate}.
 */
public final class Enforcement {
    private static final int RESCUE_RADIUS = 512;
    /** Last position of each player that was inside the free area. */
    private static final Map<UUID, double[]> SAFE = new HashMap<>();

    private Enforcement() {}

    /** True when this entity ignores the borders (creative or spectator players if the server allows it). */
    public static boolean exempt(Entity e, View view) {
        return view.exemptCreative() && e instanceof Player p && (p.isCreative() || p.isSpectator());
    }

    /** The movement after the borders: the same instance when nothing limits it. */
    public static Vec3 clampMove(Entity e, Vec3 move) {
        if (move.x == 0 && move.z == 0) return move;
        Level level = e.level();
        View view = Gate.view(level);
        if (view == null) return move;
        UnlockGrid g = view.grid(Gate.dim(level));
        if (g == null || exempt(e, view)) return move;
        AABB b = e.getBoundingBox();
        double[] c = g.clamp(b.minX, b.maxX, b.minZ, b.maxZ, move.x, move.z);
        return c[0] == move.x && c[1] == move.z ? move : new Vec3(c[0], move.y, c[1]);
    }

    /** True when a block change at this position must be refused (server side only). */
    public static boolean blocksChange(Level level, BlockPos pos) {
        if (level.isClientSide()) return false;
        UnlockGrid g = Gate.grid(level);
        return g != null && !g.isFree(g.cellOf(pos.getX()), g.cellOf(pos.getZ()));
    }

    /** Each server tick: players inside a locked cell go back; every 20 ticks the same for other entities. */
    public static void tick(MinecraftServer server) {
        Active a = ChallengeRuntime.server();
        if (a == null || !SimpleChallenges.on()) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) rescue(a, player, true);
        if (server.getTickCount() % 20 == 0) {
            for (String dim : a.dimensions()) {
                ServerLevel level = a.level(dim);
                if (level == null || a.grid(dim) == null) continue;
                for (Entity e : level.getAllEntities()) {
                    if (!(e instanceof Player) && !e.isRemoved() && !e.isPassenger()) rescue(a, e, false);
                }
            }
        }
    }

    public static void forget(UUID player) {
        SAFE.remove(player);
    }

    public static void clear() {
        SAFE.clear();
    }

    /** Brings one entity back if its position is in a locked cell; remembers safe positions of players. */
    public static void rescue(Active a, Entity e, boolean player) {
        if (!(e.level() instanceof ServerLevel level) || e.isRemoved()) return;
        String dim = level.dimension().identifier().toString();
        UnlockGrid g = a.grid(dim);
        if (g == null || exempt(e, a)) return;
        int cx = g.cellOf(e.getX()), cz = g.cellOf(e.getZ());
        if (g.isFree(cx, cz)) {
            if (player) SAFE.put(e.getUUID(), new double[]{e.getX(), e.getY(), e.getZ()});
            return;
        }
        double tx, ty, tz;
        double[] safe = player ? SAFE.get(e.getUUID()) : null;
        if (safe != null && g.isFreeAtBlock(safe[0], safe[2])) {
            tx = safe[0];
            ty = safe[1];
            tz = safe[2];
        } else {
            long near = g.nearestFree(cx, cz, RESCUE_RADIUS);
            if (near == UnlockGrid.NONE) return;
            int nx = UnlockGrid.keyX(near), nz = UnlockGrid.keyZ(near);
            double m = Math.min(0.4, g.cell() / 2.0);
            tx = Math.max(nx * (double) g.cell() + m, Math.min((nx + 1) * (double) g.cell() - m, e.getX()));
            tz = Math.max(nz * (double) g.cell() + m, Math.min((nz + 1) * (double) g.cell() - m, e.getZ()));
            AABB moved = e.getBoundingBox().move(tx - e.getX(), 0, tz - e.getZ());
            ty = level.noCollision(e, moved) ? e.getY() : level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(tx), (int) Math.floor(tz));
        }
        e.teleportTo(level, tx, ty, tz, Set.of(), e.getYRot(), e.getXRot(), true);
        e.setDeltaMovement(Vec3.ZERO);
    }
}
