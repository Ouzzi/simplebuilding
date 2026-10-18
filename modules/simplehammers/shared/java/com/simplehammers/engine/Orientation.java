package com.simplehammers.engine;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;

/**
 * Where a reshaped block faces: the clicked face and the hit point (edge band 25 %) - the same rule as SimpleBuilding's
 * chisel ({@code ChiselItem#applyIntuitiveOrientation}), copied so the hammer reshapes the same way on its own.
 */
public final class Orientation {
    private Orientation() {}

    public static BlockState applyIntuitiveOrientation(BlockState state, net.minecraft.core.Direction side, Vec3 hit, Player player) {
        // Toleranz für "Mitte" (z.B. 0.2 bedeutet 20% Randbereich auf jeder Seite)
        double margin = 0.25;

        // Lokale Koordinaten (0.0 bis 1.0)
        double x = hit.x;
        double y = hit.y;
        double z = hit.z;
        net.minecraft.core.Direction orientation = side;
        boolean isEdge = false;

        if (side.getAxis() == net.minecraft.core.Direction.Axis.Y) { // Oben oder Unten geklickt
            if (x < margin) { orientation = net.minecraft.core.Direction.WEST; isEdge = true; }
            else if (x > 1 - margin) { orientation = net.minecraft.core.Direction.EAST; isEdge = true; }
            else if (z < margin) { orientation = net.minecraft.core.Direction.NORTH; isEdge = true; }
            else if (z > 1 - margin) { orientation = net.minecraft.core.Direction.SOUTH; isEdge = true; }
        }
        else if (side.getAxis() == net.minecraft.core.Direction.Axis.X) { // Ost oder West geklickt
            if (y < margin) { orientation = net.minecraft.core.Direction.DOWN; isEdge = true; }
            else if (y > 1 - margin) { orientation = net.minecraft.core.Direction.UP; isEdge = true; }
            else if (z < margin) { orientation = net.minecraft.core.Direction.NORTH; isEdge = true; }
            else if (z > 1 - margin) { orientation = net.minecraft.core.Direction.SOUTH; isEdge = true; }
        }
        else if (side.getAxis() == net.minecraft.core.Direction.Axis.Z) { // Nord oder Süd geklickt
            if (y < margin) { orientation = net.minecraft.core.Direction.DOWN; isEdge = true; }
            else if (y > 1 - margin) { orientation = net.minecraft.core.Direction.UP; isEdge = true; }
            else if (x < margin) { orientation = net.minecraft.core.Direction.WEST; isEdge = true; }
            else if (x > 1 - margin) { orientation = net.minecraft.core.Direction.EAST; isEdge = true; }
        }

        // --- ANWENDUNG AUF BLÖCKE ---

        // 1. Pillars (Logs, Quartz Pillar, etc.)
        if (state.hasProperty(RotatedPillarBlock.AXIS)) {
            net.minecraft.core.Direction.Axis axis;
            if (isEdge) {
                axis = orientation.getAxis();
            } else {
                axis = side.getAxis();
            }
            return state.setValue(RotatedPillarBlock.AXIS, axis);
        }

        // 2. Stairs (Treppen)
        if (state.hasProperty(StairBlock.FACING)) {
            net.minecraft.core.Direction facing;
            if (isEdge && orientation.getAxis().isHorizontal()) {
                facing = orientation.getOpposite();
            } else {
                facing = player.getDirection();
            }
            state = state.setValue(StairBlock.FACING, facing);

            // Half (Oben/Unten)
            Half half;
            if ((side == net.minecraft.core.Direction.UP && !isEdge) || (y < 0.5 && !isEdge)) {
                half = Half.BOTTOM;
            } else if ((side == net.minecraft.core.Direction.DOWN && !isEdge) || (y > 0.5 && !isEdge)) {
                half = Half.TOP;
            } else {
                if (y > 0.5) half = Half.TOP; else half = Half.BOTTOM;
            }
            if (orientation == net.minecraft.core.Direction.UP) half = Half.BOTTOM;
            if (orientation == net.minecraft.core.Direction.DOWN) half = Half.TOP;

            state = state.setValue(StairBlock.HALF, half);
            return state;
        }

        // 3. Rods (End Rods, Lightning Rods, etc.)
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.setValue(BlockStateProperties.FACING, isEdge ? orientation : side);
        }

        return state;
    }
}
