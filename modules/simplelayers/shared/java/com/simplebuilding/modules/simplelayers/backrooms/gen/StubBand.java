package com.simplebuilding.modules.simplelayers.backrooms.gen;

import com.simplebuilding.modules.simplelayers.backrooms.api.Band;
import com.simplebuilding.modules.simplelayers.backrooms.api.BandLayout;
import com.simplebuilding.modules.simplelayers.backrooms.api.BandPiece;
import com.simplebuilding.modules.simplelayers.backrooms.api.BandView;
import com.simplebuilding.modules.simplelayers.backrooms.api.CorePiece;
import com.simplebuilding.modules.simplelayers.backrooms.api.DoorRule;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A stub level (BACK22 core §6, owner: Vanilla blocks only until BACK22c is accepted): a connected grid of 8×8 rooms, 5 high, floor
 * at ly 0, a light in every ceiling, every wall with a 3-wide opening in its middle, hull above. {@code walls = false} = an open hall
 * (Wheat Field stub). The level branch replaces it in its package.
 */
public class StubBand implements Band {
    public enum StubPiece implements BandPiece {
        AIR, FLOOR, WALL, CEILING, LIGHT;

        @Override
        public boolean passable() {
            return this == AIR;
        }
    }

    public static final int CELL = 8, CEILING = 6;
    private final String id;
    private final DoorRule rule;
    private final BandView view;
    private final Supplier<BlockState> floor, wall, ceiling, light;
    private final BandLayout layout;

    public StubBand(String id, DoorRule rule, int fog, boolean walls, Supplier<BlockState> floor, Supplier<BlockState> wall,
                    Supplier<BlockState> ceiling, Supplier<BlockState> light) {
        this.id = id;
        this.rule = rule;
        this.view = new BandView(fog);
        this.floor = floor;
        this.wall = wall;
        this.ceiling = ceiling;
        this.light = light;
        this.layout = new Layout(walls);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public BandLayout layout() {
        return layout;
    }

    @Override
    public BlockState state(BandPiece piece) {
        if (piece == StubPiece.FLOOR) return floor.get();
        if (piece == StubPiece.WALL || piece == CorePiece.HULL) return wall.get();
        if (piece == StubPiece.CEILING) return ceiling.get();
        if (piece == StubPiece.LIGHT) return light.get();
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public DoorRule doorRule() {
        return rule;
    }

    @Override
    public BandView view() {
        return view;
    }

    public record Layout(boolean walls) implements BandLayout {
        @Override
        public BandPiece at(long seed, int x, int ly, int z) {
            if (ly <= 0) return StubPiece.FLOOR;
            if (ly > CEILING) return CorePiece.HULL;
            int ix = Math.floorMod(x, CELL), iz = Math.floorMod(z, CELL);
            if (ly == CEILING) return ix == 4 && iz == 4 ? StubPiece.LIGHT : StubPiece.CEILING;
            if (!walls) return StubPiece.AIR;
            boolean lineX = ix == 0, lineZ = iz == 0;
            if (lineX && lineZ) return StubPiece.WALL;
            if (lineX || lineZ) {
                int along = lineX ? iz : ix;
                return along >= 3 && along <= 5 && ly <= 3 ? StubPiece.AIR : StubPiece.WALL;
            }
            return StubPiece.AIR;
        }

        @Override
        public int[] arrival(long seed, int x, int z) {
            return new int[] {Math.floorDiv(x, CELL) * CELL + 4, 1, Math.floorDiv(z, CELL) * CELL + 4};
        }

        @Override
        public boolean hullSection(long seed, int chunkX, int chunkZ, int section) {
            return section >= 1;
        }
    }
}
