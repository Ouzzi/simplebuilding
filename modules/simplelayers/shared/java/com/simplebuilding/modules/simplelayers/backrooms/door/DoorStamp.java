package com.simplebuilding.modules.simplelayers.backrooms.door;

import com.simplebuilding.modules.simplelayers.LayersRegistry;
import com.simplebuilding.modules.simplelayers.backrooms.api.Band;
import com.simplebuilding.modules.simplelayers.backrooms.api.CorePiece;
import com.simplebuilding.modules.simplelayers.backrooms.api.DoorEnd;
import com.simplebuilding.modules.simplelayers.backrooms.stack.BandStack;
import com.simplebuilding.modules.simplelayers.backrooms.stack.Bands;
import com.simplebuilding.modules.simplelayers.block.BackroomsDoorBlock;
import com.simplelib.api.portal.PortalDoorBlock;
import com.simplelib.api.portal.PortalLook;
import com.simplelib.api.portal.PortalRule;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * The core stamps every door end of a level (BACK22 core §4.2 step 5, K-R5.2): door (both halves, double door with mirrored hinges),
 * frame left/right at both heights, lintel, wall behind, floor below, free spot in front, and - for doors that can be dormant or
 * broken - the maintenance box as the upper left frame block (fg16 a). The level only chose the place ({@code BandLayout.doorEnd}).
 */
public final class DoorStamp {
    /** Where a stamp writes: world x/z, band-local ly. */
    @FunctionalInterface
    public interface Put {
        void put(int x, int ly, int z, BlockState state);
    }

    private DoorStamp() {}

    /** All door ends of level {@code band} in chunk (cx, cz). */
    public static void stamp(long seed, int band, int cx, int cz, Put put) {
        Band b = Bands.get(band);
        if (band < BandStack.BANDS) {
            DoorEnd down = DoorNet.down(seed, band, cx, cz);
            if (down != null) stampEnd(put, b, down, band + 1, b.doorRule().portalRule());
        }
        if (band > 1) {
            DoorEnd up = DoorNet.up(seed, band, cx, cz);
            if (up != null) stampEnd(put, b, up, band - 1, Bands.get(band - 1).doorRule().portalRule());
        } else {
            DoorEnd entrance = Entrances.lobbyEnd(seed, cx, cz);
            if (entrance != null) stampEnd(put, b, entrance, 0, PortalRule.TWO_WAY);
        }
    }

    public static void stampEnd(Put put, Band band, DoorEnd e, int symbol, PortalRule rule) {
        BlockState hull = band.state(CorePiece.HULL), air = Blocks.AIR.defaultBlockState();
        int f = e.facing(), cw = (f + 1) & 3, ly = e.ly();
        Direction facing = Direction.from2DDataValue(f);
        for (int i = 0; i < e.width(); i++) {
            int x = e.leafX(i), z = e.leafZ(i);
            int bx = x + DoorEnd.DX[f], bz = z + DoorEnd.DZ[f], fx = x - DoorEnd.DX[f], fz = z - DoorEnd.DZ[f];
            put.put(x, ly - 1, z, hull);
            put.put(x, ly + 2, z, hull);
            for (int h = -1; h <= 1; h++) put.put(bx, ly + h, bz, hull);
            if (i > 0) put.put(fx, ly - 1, fz, hull);
            put.put(fx, ly, fz, air);
            put.put(fx, ly + 1, fz, air);
            BlockState door = door(facing, i == 1 ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT, symbol, rule);
            put.put(x, ly, z, door);
            put.put(x, ly + 1, z, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        }
        int lx = e.x() - DoorEnd.DX[cw], lz = e.z() - DoorEnd.DZ[cw];
        int rx = e.leafX(e.width()), rz = e.leafZ(e.width());
        boolean box = rule.initial() != com.simplelib.api.portal.PortalState.OPEN || rule.mendable();
        put.put(lx, ly, lz, hull);
        put.put(lx, ly + 1, lz, box ? LayersRegistry.MAINTENANCE_BOX.defaultBlockState() : hull);
        put.put(rx, ly, rz, hull);
        put.put(rx, ly + 1, rz, hull);
    }

    /** The lower half of a Backrooms door. */
    public static BlockState door(Direction facing, DoorHingeSide hinge, int symbol, PortalRule rule) {
        return LayersRegistry.BACKROOMS_DOOR.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HINGE, hinge)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER).setValue(DoorBlock.OPEN, false).setValue(BackroomsDoorBlock.SYMBOL, symbol)
                .setValue(PortalDoorBlock.PORTAL, PortalLook.of(rule.initial()));
    }
}
