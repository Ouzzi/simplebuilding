package com.simplebuilding.modules.simplechallenges.core;

import com.simplebuilding.modules.simplechallenges.SimpleChallenges;
import com.simplebuilding.modules.simplechallenges.net.CellInfoPayload;
import com.simplebuilding.modules.simplechallenges.price.Price;
import com.simplebuilding.modules.simplechallenges.unlock.UnlockGrid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * The crosshair line (S3): twice a second the server finds the locked cell next to the free area that each player looks at and
 * tells the client its price and whether the player can pay it. Only a changed answer is sent.
 */
public final class LookInfo {
    private static final double REACH = 32;
    private static final float MAX_PITCH = 50;
    private static final Map<UUID, CellInfoPayload> LAST = new HashMap<>();

    private LookInfo() {}

    /** The answer for one player (pure of sending, for tests): {@link CellInfoPayload#NONE} when no locked cell is in view. */
    public static CellInfoPayload of(Active a, ServerPlayer p) {
        if (a.mode == null || Math.abs(p.getXRot()) > MAX_PITCH) return CellInfoPayload.NONE;
        String dim = p.level().dimension().identifier().toString();
        UnlockGrid g = a.grid(dim);
        if (g == null || Enforcement.exempt(p, a)) return CellInfoPayload.NONE;
        Vec3 look = p.getLookAngle();
        long k = UnlockService.lookedCell(g, p.getX(), p.getZ(), look.x, look.z, REACH);
        if (k == UnlockGrid.NONE) return CellInfoPayload.NONE;
        int cx = UnlockGrid.keyX(k), cz = UnlockGrid.keyZ(k);
        Price price = UnlockService.priceOf(a, dim, cx, cz);
        int have = switch (price.currency()) {
            case ITEMS -> UnlockService.count(p, price.item());
            case CREDITS -> Credits.of(a, p);
            case XP -> p.experienceLevel;
        };
        return new CellInfoPayload(true, cx * g.cell(), cz * g.cell(), price.currency().id(), price.item(), price.count(), have, UnlockService.canPay(a, p, price));
    }

    /** Every 10 ticks: sends each player with the mod the line of the cell in view, if it changed. */
    public static void tick(Active a) {
        if (a.mode == null) return;
        for (ServerPlayer p : a.server.getPlayerList().getPlayers()) {
            if (!SimpleChallenges.canSend.test(p, CellInfoPayload.TYPE)) continue;
            CellInfoPayload now = of(a, p);
            if (now.equals(LAST.get(p.getUUID()))) continue;
            LAST.put(p.getUUID(), now);
            SimpleChallenges.toClient.accept(p, now);
        }
    }

    public static void forget(UUID player) {
        LAST.remove(player);
    }

    public static void clear() {
        LAST.clear();
    }

}
