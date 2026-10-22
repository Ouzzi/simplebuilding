package com.simplemaps;

import com.simplemaps.net.MapsNetwork;

/**
 * The view code of a framed wayfinder map (owner 21.10.): {@code X,Z,stage}, e.g. {@code 1,1,8:1} or {@code -1,-100,1:64}.
 * X and Z are the chunk (as F3 shows it) at the north-west (top-left) corner of the framed picture; the stage is the zoom
 * button's label: 8:1, 4:1, 2:1 magnify (one block = 8, 4, 2 pixels), 1:1 .. 1:64 are blocks per pixel. A frame is
 * {@link #spanChunks} chunks wide and high, so the neighbour to the east is {@code X + span}. The stored {@link MapsComponents.View}
 * keeps the centre block, so older views stay valid. Plain maths, no client classes (server tests use it).
 */
public final class FrameCode {
    /** Pixels of a framed picture (the Vanilla map size, {@code ViewTexture.SIZE}). */
    public static final int PICTURE = 128, CHUNK = 16;
    /** Zoom stages of the map screen and the frame: the first {@link #NEAR} magnify 1:1 data, then {@link WayfinderData#ZOOMS}. */
    private static final int[] MAGS = {8, 4, 2};
    public static final int NEAR = MAGS.length;
    public static final int[] STAGE_BPP, STAGE_MAG;
    static {
        int n = NEAR + WayfinderData.ZOOMS.length;
        STAGE_BPP = new int[n];
        STAGE_MAG = new int[n];
        for (int i = 0; i < n; i++) {
            STAGE_BPP[i] = i < NEAR ? 1 : WayfinderData.ZOOMS[i - NEAR];
            STAGE_MAG[i] = i < NEAR ? MAGS[i] : 1;
        }
    }

    /** A parsed code: the corner chunk and the stage index, or (stage -1) the lang key suffix of the error. */
    public record Result(int chunkX, int chunkZ, int stage, String error) {
        public boolean ok() {
            return error == null;
        }

        static Result fail(String error) {
            return new Result(0, 0, -1, error);
        }
    }

    private FrameCode() {}

    /** "8:1" for a magnified stage, else "1:4". */
    public static String label(int stage) {
        return STAGE_MAG[stage] > 1 ? STAGE_MAG[stage] + ":1" : "1:" + STAGE_BPP[stage];
    }

    /** Stage of this zoom and magnification, or -1. */
    public static int stageOf(int bpp, int mag) {
        for (int i = 0; i < STAGE_BPP.length; i++) if (STAGE_BPP[i] == bpp && STAGE_MAG[i] == mag) return i;
        return -1;
    }

    /** Stage of a stored view (invalid values were already corrected by the view itself). */
    public static int stageOf(MapsComponents.View view) {
        int stage = stageOf(view.zoom(), view.mag());
        return stage < 0 ? NEAR : stage;
    }

    /** Width (and height) of a framed picture in blocks. */
    public static int spanBlocks(int stage) {
        return PICTURE * STAGE_BPP[stage] / STAGE_MAG[stage];
    }

    /** Width (and height) of a framed picture in chunks: the step to the next frame of a wall. */
    public static int spanChunks(int stage) {
        return spanBlocks(stage) / CHUNK;
    }

    public static String format(int chunkX, int chunkZ, int stage) {
        return chunkX + "," + chunkZ + "," + label(stage);
    }

    /** The view whose picture starts at the north-west corner of this chunk. */
    public static MapsComponents.View view(int chunkX, int chunkZ, int stage) {
        int half = spanBlocks(stage) / 2;
        return new MapsComponents.View(chunkX * CHUNK + half, chunkZ * CHUNK + half, STAGE_BPP[stage], STAGE_MAG[stage]);
    }

    /** The code of a stored view: the chunk at its picture's corner. */
    public static Result of(MapsComponents.View view) {
        int stage = stageOf(view), half = spanBlocks(stage) / 2;
        return new Result(Math.floorDiv(view.x() - half, CHUNK), Math.floorDiv(view.z() - half, CHUNK), stage, null);
    }

    /** The nearest corner chunk whose picture's centre stays inside the world border. */
    public static int clampChunk(double chunk, int stage) {
        int half = spanBlocks(stage) / 2;
        long max = Math.floorDiv(MapsNetwork.MAX_COORD - half, CHUNK), min = -Math.floorDiv(MapsNetwork.MAX_COORD + half, CHUNK);
        return (int) Math.max(min, Math.min(max, Math.round(chunk)));
    }

    /**
     * Blocks a framed picture of this view covers, as {@code {minX, minZ, size}}: the same pixel maths as the frame's
     * picture ({@code ViewTexture}), so the outline in the map screen is exactly what the frame shows.
     */
    public static long[] area(MapsComponents.View view) {
        int zoom = view.zoom(), span = PICTURE / view.mag();
        long left = Math.floorDiv(view.x(), zoom) - span / 2, top = Math.floorDiv(view.z(), zoom) - span / 2;
        return new long[] {left * zoom, top * zoom, (long) span * zoom};
    }

    /**
     * Reads {@code X,Z,stage} or {@code X,Z} (keeps {@code currentStage}). Spaces are ignored. Errors (lang key suffixes):
     * {@code format} (not two whole numbers and an optional stage), {@code scale} (no such stage), {@code range} (the
     * picture's centre would lie outside the world border).
     */
    public static Result parse(String text, int currentStage) {
        String[] parts = text.replace(" ", "").split(",", -1);
        if (parts.length != 2 && parts.length != 3) return Result.fail("format");
        long x, z;
        try {
            x = Long.parseLong(parts[0]);
            z = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            boolean digits = parts[0].matches("-?\\d+") && parts[1].matches("-?\\d+");
            return Result.fail(digits ? "range" : "format");
        }
        if (Math.abs(x) > MapsNetwork.MAX_COORD || Math.abs(z) > MapsNetwork.MAX_COORD) return Result.fail("range");
        int stage = currentStage;
        if (parts.length == 3) {
            stage = -1;
            for (int i = 0; i < STAGE_BPP.length; i++) if (label(i).equals(parts[2])) stage = i;
            if (stage < 0) return Result.fail(parts[2].matches("\\d+:\\d+") ? "scale" : "format");
        }
        long half = spanBlocks(stage) / 2;
        long cx = x * CHUNK + half, cz = z * CHUNK + half;
        if (Math.abs(cx) > MapsNetwork.MAX_COORD || Math.abs(cz) > MapsNetwork.MAX_COORD) return Result.fail("range");
        return new Result((int) x, (int) z, stage, null);
    }
}
