package com.simplebuilding.modules.simpleearth.view;

/**
 * One display version of the earth. {@link #step} gets the orbit position relative to the activation point A (the cube's
 * centre on tile 0, 0) and the frame time and returns the frame's {@link Scene}. Each version keeps its own state (current
 * tile, orientation, animations), so a version switch starts it fresh with {@link #reset}.
 */
public abstract class EarthView {
    protected double edge = 65536, half = 32768;
    protected int curTx, curTz;
    protected boolean started;

    public final Scene step(double px, double pz, double dt, ViewParams params) {
        double e = Math.max(16, params.edge);
        if (e != edge) started = false;
        edge = e;
        half = e / 2;
        int tx = tileOf(px), tz = tileOf(pz);
        if (!started || Math.abs(tx - curTx) + Math.abs(tz - curTz) > 3) {
            curTx = tx;
            curTz = tz;
            reset(u(px), v(pz), params);
            started = true;
        }
        Scene s = frame(px, pz, Math.max(0, Math.min(0.25, dt)), params);
        s.curTx = curTx;
        s.curTz = curTz;
        return s;
    }

    /** Fresh state for the current tile at face coordinates u, v. */
    protected abstract void reset(double u, double v, ViewParams params);

    protected abstract Scene frame(double px, double pz, double dt, ViewParams params);

    public final int tileOf(double c) {
        return (int) Math.floor((c + half) / edge);
    }

    /** Face coordinate (-1..1) of {@code px} on the current tile. */
    protected final double u(double px) {
        return (px - curTx * edge) / half;
    }

    protected final double v(double pz) {
        return (pz - curTz * edge) / half;
    }

    public int currentTileX() {
        return curTx;
    }

    public int currentTileZ() {
        return curTz;
    }

    /** All twelve versions, index 0 = V1. */
    public static EarthView create(int version) {
        return switch (version) {
            case 1 -> new CubeJumpView(1);
            case 2 -> new CubeJumpView(2);
            case 3 -> new CubeJumpView(3);
            case 4 -> new RollingCubeView(false);
            case 5 -> new RollingCubeView(true);
            case 6 -> new FoldingNetView(FoldingNetView.Style.PLAIN);
            case 7 -> new FoldingNetView(FoldingNetView.Style.CELLS);
            case 8 -> new FoldingNetView(FoldingNetView.Style.INVERSION);
            case 9 -> new FoldShellView();
            case 10 -> new CubeShapeView(CubeShapeView.Style.TIP);
            case 11 -> new CubeShapeView(CubeShapeView.Style.CELLS);
            case 12 -> new CubeShapeView(CubeShapeView.Style.CORNER);
            default -> new FoldingNetView(FoldingNetView.Style.PLAIN);
        };
    }
}
