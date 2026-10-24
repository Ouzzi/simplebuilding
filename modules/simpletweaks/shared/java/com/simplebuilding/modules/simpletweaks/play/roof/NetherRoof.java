package com.simplebuilding.modules.simpletweaks.play.roof;

import com.simplebuilding.modules.simpletweaks.play.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * T26 (off by default): nobody stays on the Nether roof. A survival/adventure player at or above Y {@value #ROOF} in the
 * Nether is put back to the last place below the roof (or a free spot below it); operators of {@code bypassPermission}
 * are exempt when {@code opBypass} is on.
 */
public final class NetherRoof {
    public static final int ROOF = 128;
    static final Map<UUID, Vec3> SAFE = new HashMap<>();

    /** A player left: forget the last safe place. */
    public static void forget(ServerPlayer p) { SAFE.remove(p.getUUID()); }
    /** Server stop. */
    public static void forgetAll() { SAFE.clear(); }

    public static boolean active() { return Play.on() && Play.server() != null && Play.config().netherRoof().enabled(); }

    public static void tick(MinecraftServer server) {
        if (!active() || server.getTickCount() % 5 != 0) return;
        var level = server.getLevel(Level.NETHER);
        if (level == null) return;
        for (var p : List.copyOf(level.players())) check(level, p);
    }
    /** True when the player was moved down. */
    public static boolean check(ServerLevel level, ServerPlayer p) {
        if (exempt(p)) return false;
        if (p.getY() < ROOF) { if (p.onGround()) SAFE.put(p.getUUID(), p.position()); return false; }
        moveDown(level, p);
        return true;
    }
    /** Creative/spectator players and (with {@code opBypass}) operators may stay on the roof. */
    public static boolean exempt(ServerPlayer p) {
        if (p.isCreative() || p.isSpectator()) return true;
        var c = Play.config().netherRoof();
        return c.opBypass() && c.bypassPermission() > 0 && PlayPerms.has(p, c.bypassPermission());
    }
    /** Remembers {@code at} as the last safe place of {@code id} (what {@link #check} does for a player on the ground). */
    public static void remember(UUID id, Vec3 at) { SAFE.put(id, at); }
    /** Disconnect or server stop: the remembered place goes (it would be hours old, maybe in another world). */
    public static void forget(UUID id) { SAFE.remove(id); }
    public static void forgetAll() { SAFE.clear(); }
    /** Two air blocks over a solid block, no fluid: the remembered place may be old, so it is checked again before the teleport. */
    static boolean standable(ServerLevel level, BlockPos at) {
        return level.getBlockState(at).isAir() && level.getBlockState(at.above()).isAir()
                && level.getBlockState(at.below()).isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP)
                && level.getFluidState(at.below()).isEmpty();
    }
    /** Back to the last place below the roof, or a free spot under it. */
    public static void moveDown(ServerLevel level, ServerPlayer p) {
        var back = SAFE.get(p.getUUID());
        if (back == null || back.distanceToSqr(p.getX(), back.y, p.getZ()) > 64 * 64 || !level.hasChunkAt(BlockPos.containing(back))
                || !standable(level, BlockPos.containing(back))) back = below(level, p.blockPosition());
        p.stopRiding();
        p.teleportTo(back.x, back.y, back.z);
        p.resetFallDistance();
        p.sendSystemMessage(PlayText.t("roof.blocked"));
    }
    /** A free spot (two air blocks over a solid block, no lava) under the roof near {@code pos}. */
    static Vec3 below(ServerLevel level, BlockPos pos) {
        for (int r = 0; r <= 4; r++) for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
            for (int y = ROOF - 6; y > level.getMinY() + 1; y--) {
                var at = new BlockPos(pos.getX() + dx, y, pos.getZ() + dz);
                if (standable(level, at)) return Vec3.atBottomCenterOf(at);
            }
        }
        return new Vec3(pos.getX() + .5, ROOF - 8, pos.getZ() + .5);
    }
    private NetherRoof() {}
}
