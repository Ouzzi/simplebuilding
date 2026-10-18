package com.simplebuilding.modules.simpleoptserver;

/**
 * S5a view distance by server load, own implementation (ServerCore describes "dynamic view/simulation distance from
 * MSPT"). Every {@code stepSeconds} the 100-tick MSPT average is compared with {@code targetMspt}: above it the view
 * distance drops by one (down to {@code minView}), below 80 % of it the distance grows by one again (up to the
 * server.properties value). The simulation distance moves too only with {@code adjustSimulation} (default off: it
 * changes farms, crops and spawning far away, i.e. gameplay). Bounds: never above server.properties, never below 3.
 */
public final class DynamicDistance {
    public static final int FLOOR = 3;
    public static final double RAISE_BELOW = 0.8;

    public record Params(double targetMspt, int minView, int minSimulation, int stepSeconds, boolean adjustSimulation) {
        public static final Params DEFAULTS = new Params(45, 6, 5, 15, false);
    }

    /** Current distances; view/sim never leave [floor, max]. */
    public record Distances(int view, int simulation) {}

    private DynamicDistance() {}

    /** One step of the rule; {@code maxView}/{@code maxSim} = the server.properties values. */
    public static Distances step(Distances now, double mspt, Params p, int maxView, int maxSim) {
        int minView = Math.min(Math.max(p.minView(), FLOOR), Math.max(maxView, FLOOR));
        int minSim = Math.min(Math.max(p.minSimulation(), FLOOR), Math.max(maxSim, FLOOR));
        int view = clamp(now.view(), minView, Math.max(maxView, FLOOR));
        int sim = p.adjustSimulation() ? clamp(now.simulation(), minSim, Math.max(maxSim, FLOOR)) : now.simulation();
        if (mspt > p.targetMspt()) {
            if (view > minView) view--;
            else if (p.adjustSimulation() && sim > minSim) sim--;
        } else if (mspt < p.targetMspt() * RAISE_BELOW) {
            if (p.adjustSimulation() && sim < maxSim) sim++;
            else if (view < maxView) view++;
        }
        return new Distances(view, sim);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Tick-driven state: decides once per step interval while players are online; null = nothing to change. */
    public static final class Controller {
        private final int maxView, maxSim;
        private long ticks;

        public Controller(int maxView, int maxSim) {
            this.maxView = maxView;
            this.maxSim = maxSim;
        }

        public int maxView() {
            return maxView;
        }

        public int maxSimulation() {
            return maxSim;
        }

        public Distances onTick(int players, double mspt, Distances now, Params p) {
            if (++ticks % (p.stepSeconds() * 20L) != 0 || players <= 0) return null;
            Distances next = step(now, mspt, p, maxView, maxSim);
            return next.equals(now) ? null : next;
        }
    }
}
