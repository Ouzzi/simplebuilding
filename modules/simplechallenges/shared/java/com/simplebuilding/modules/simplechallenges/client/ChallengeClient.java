package com.simplebuilding.modules.simplechallenges.client;

import com.simplebuilding.modules.simplechallenges.core.ChallengeRuntime;
import com.simplebuilding.modules.simplechallenges.net.UnlockSyncPayload;
import com.simplebuilding.modules.simplechallenges.unlock.UnlockGrid;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.player.Player;

/**
 * Client side of the Unlock mode (S1): keeps the synced view for the collision mixin and draws the border as a line of red dust
 * at the edge of the free area near the player (toggle: client command {@code /simplechallenges border}). Nothing here runs
 * until the server has sent a challenge.
 */
public final class ChallengeClient {
    private static final DustParticleOptions BORDER = new DustParticleOptions(0xFF3030, 1.0F);
    private static final int RADIUS = 24;
    private static boolean border = true;

    private ChallengeClient() {}

    public static boolean border() {
        return border;
    }

    public static boolean toggleBorder() {
        border = !border;
        return border;
    }

    public static void receive(UnlockSyncPayload p) {
        if (p.cell() == 0) {
            ChallengeRuntime.clearClient();
            return;
        }
        ClientView view = ChallengeRuntime.client() instanceof ClientView v ? v : new ClientView();
        view.apply(p.dimension(), p.cell(), p.full(), p.exemptCreative(), p.keys());
        ChallengeRuntime.installClient(view);
    }

    public static void left() {
        ChallengeRuntime.clearClient();
    }

    /** Every fifth tick: dust along the border edges within {@link #RADIUS} blocks. */
    public static void tick(Minecraft mc) {
        if (!border || mc.level == null || mc.player == null || mc.player.tickCount % 5 != 0) return;
        if (!(ChallengeRuntime.client() instanceof ClientView view)) return;
        UnlockGrid g = view.grid(mc.level.dimension().identifier().toString());
        if (g == null) return;
        Player p = mc.player;
        int c = g.cell();
        List<int[]> edges = g.borderEdges(g.cellOf(p.getX() - RADIUS), g.cellOf(p.getZ() - RADIUS), g.cellOf(p.getX() + RADIUS), g.cellOf(p.getZ() + RADIUS));
        int budget = 400;
        for (int[] e : edges) {
            double x0 = e[0] * (double) c, z0 = e[1] * (double) c;
            boolean alongZ = e[2] < 2;
            double fixed = alongZ ? (e[2] == 0 ? x0 : x0 + c) : (e[2] == 2 ? z0 : z0 + c);
            double start = alongZ ? z0 : x0;
            double step = Math.max(1.0, c / 8.0);
            for (double t = 0; t < c && budget > 0; t += step) {
                double along = start + t + 0.5 * Math.min(step, 1.0);
                double x = alongZ ? fixed : along, z = alongZ ? along : fixed;
                if (Math.abs(x - p.getX()) > RADIUS || Math.abs(z - p.getZ()) > RADIUS) continue;
                for (double dy : new double[]{-0.5, 0.7, 1.9}) {
                    mc.level.addParticle(BORDER, x, p.getY() + dy, z, 0, 0, 0);
                    budget--;
                }
            }
        }
    }
}
