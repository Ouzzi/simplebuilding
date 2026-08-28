package com.simplebuilding.items.custom;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

public class RotatorItem extends Item {
    public RotatorItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = world.getBlockState(pos);
        Player player = context.getPlayer();
        boolean isSneaking = player != null && player.isShiftKeyDown();

        // 1. Rand-Erkennung (ca. 2 Pixel am Rand des Blocks)
        Direction rimDirection = getRimDirection(context, 0.125);

        // 2. Neuen Status berechnen
        BlockState newState = calculateNewState(state, context.getClickedFace(), rimDirection, isSneaking);

        if (newState != null && newState != state) {
            if (!world.isClientSide()) {
                world.setBlock(pos, newState, Block.UPDATE_ALL);
                world.playSound(null, pos, SoundEvents.SPYGLASS_USE, SoundSource.BLOCKS, 1.0f, 1.0f);

                if (player != null) {
                    context.getItemInHand().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Nullable
    private Direction getRimDirection(UseOnContext context, double margin) {
        Vec3 hitPos = context.getClickLocation().subtract(Vec3.atLowerCornerOf(context.getClickedPos()));
        Direction face = context.getClickedFace();

        double x = hitPos.x;
        double y = hitPos.y;
        double z = hitPos.z;

        if (face.getAxis() == Direction.Axis.Y) { // Oben/Unten
            if (x < margin) return Direction.WEST;
            if (x > 1 - margin) return Direction.EAST;
            if (z < margin) return Direction.NORTH;
            if (z > 1 - margin) return Direction.SOUTH;
        }
        else if (face.getAxis() == Direction.Axis.X) { // Ost/West
            if (y < margin) return Direction.DOWN;
            if (y > 1 - margin) return Direction.UP;
            if (z < margin) return Direction.NORTH;
            if (z > 1 - margin) return Direction.SOUTH;
        }
        else if (face.getAxis() == Direction.Axis.Z) { // Nord/Süd
            if (y < margin) return Direction.DOWN;
            if (y > 1 - margin) return Direction.UP;
            if (x < margin) return Direction.WEST;
            if (x > 1 - margin) return Direction.EAST;
        }

        return null;
    }

    private BlockState calculateNewState(BlockState state, Direction clickedFace, @Nullable Direction rimDirection, boolean isSneaking) {

        // --- LOG (Axis) ---
        if (state.getProperties().contains(BlockStateProperties.AXIS)) {
            return handleAxisRotation(state, clickedFace, rimDirection);
        }

        // --- PISTON / FURNACE (Facing) ---
        Property<Direction> facingProp = getFacingProperty(state);
        if (facingProp != null) {
            return handleFacingRotation(state, facingProp, clickedFace, rimDirection, isSneaking);
        }

        // --- ROTATION (0-15) ---
        if (state.getProperties().contains(BlockStateProperties.ROTATION_16)) {
            int current = state.getValue(BlockStateProperties.ROTATION_16);
            int change = isSneaking ? -1 : 1;
            if (rimDirection != null) change *= 4;
            int next = (current + change + 16) % 16;
            return state.setValue(BlockStateProperties.ROTATION_16, next);
        }

        return null;
    }

    // --- LOGIC: AXIS (Der wichtigste Fix) ---
    private BlockState handleAxisRotation(BlockState state, Direction clickedFace, Direction rimDirection) {
        Direction.Axis currentAxis = state.getValue(BlockStateProperties.AXIS);

        // 1. Rand-Klick: Richte Achse parallel zum Rand aus.
        if (rimDirection != null) {
            return state.setValue(BlockStateProperties.AXIS, rimDirection.getAxis());
        }

        // 2. Zentrum-Klick: Wechsel zwischen der geklickten Achse und der aktuellen.
        // Das fühlt sich am natürlichsten an.
        // Wenn ich auf die Seite (X) eines stehenden Stammes (Y) klicke -> Stamm wird X.
        // Wenn er schon X ist -> Stamm wird Y (oder Z, je nach dritter Dimension).

        // Neue Logik: Zyklus zwischen den zwei Achsen, die NICHT die Blickrichtung sind? Nein.
        // Besser: Wenn Block nicht in Blickrichtung liegt, drehe ihn in Blickrichtung.
        // Wenn er schon in Blickrichtung liegt, drehe ihn in die Dritte.

        Direction.Axis clickedAxis = clickedFace.getAxis();

        if (currentAxis != clickedAxis) {
            return state.setValue(BlockStateProperties.AXIS, clickedAxis);
        } else {
            // Block zeigt bereits auf uns zu (oder weg). Wir rotieren zur nächsten Achse.
            // Zyklus: X -> Y -> Z -> X
            return state.setValue(BlockStateProperties.AXIS, nextAxis(currentAxis));
        }
    }

    private Direction.Axis nextAxis(Direction.Axis axis) {
        return switch (axis) {
            case X -> Direction.Axis.Y;
            case Y -> Direction.Axis.Z;
            case Z -> Direction.Axis.X;
        };
    }

    // --- LOGIC: FACING ---
    private BlockState handleFacingRotation(BlockState state, Property<Direction> prop, Direction clickedFace, Direction rimDirection, boolean isSneaking) {
        Direction currentFacing = state.getValue(prop);
        Collection<Direction> validDirections = prop.getPossibleValues();

        if (rimDirection != null) {
            if (validDirections.contains(rimDirection)) return state.setValue(prop, rimDirection);
            if (validDirections.contains(rimDirection.getOpposite())) return state.setValue(prop, rimDirection.getOpposite());
        }


        Direction nextFacing = rotateAroundAxis(currentFacing, clickedFace.getAxis(), isSneaking);

        if (nextFacing == currentFacing && validDirections.size() > 1) {
            nextFacing = getStandardRotationStart(clickedFace.getAxis(), validDirections);

            if (nextFacing == currentFacing) {
                nextFacing = cycleDirectionList(currentFacing, isSneaking, validDirections);
            }
        }

        if (validDirections.contains(nextFacing)) {
            return state.setValue(prop, nextFacing);
        }

        // Fallback: Zyklus durch Liste
        return state.setValue(prop, cycleDirectionList(currentFacing, isSneaking, validDirections));
    }

    private Direction cycleDirectionList(Direction current, boolean backwards, Collection<Direction> valid) {
        List<Direction> list = valid.stream().toList();
        int index = list.indexOf(current);
        int next = (index + (backwards ? -1 : 1) + list.size()) % list.size();
        return list.get(next);
    }

    private Direction getStandardRotationStart(Direction.Axis axis, Collection<Direction> valid) {
        // Prio-Liste für Startwerte, wenn man auf Pole klickt
        List<Direction> preference;
        if (axis == Direction.Axis.Y) preference = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
        else if (axis == Direction.Axis.X) preference = List.of(Direction.UP, Direction.NORTH, Direction.DOWN, Direction.SOUTH);
        else preference = List.of(Direction.UP, Direction.EAST, Direction.DOWN, Direction.WEST);

        for (Direction d : preference) {
            if (valid.contains(d)) return d;
        }
        return valid.iterator().next();
    }

    // Uhrzeigersinn Rotation um Achse
    private Direction rotateAroundAxis(Direction dir, Direction.Axis axis, boolean counterClockwise) {
        if (dir.getAxis() == axis) return dir;

        if (axis == Direction.Axis.Y) {
            return counterClockwise ? dir.getCounterClockWise() : dir.getClockWise();
        }

        if (axis == Direction.Axis.X) {
            // Rotation um X (Seitenansicht)
            // Uhr: UP -> NORTH -> DOWN -> SOUTH
            if (dir == Direction.UP) return counterClockwise ? Direction.SOUTH : Direction.NORTH;
            if (dir == Direction.NORTH) return counterClockwise ? Direction.UP : Direction.DOWN;
            if (dir == Direction.DOWN) return counterClockwise ? Direction.NORTH : Direction.SOUTH;
            if (dir == Direction.SOUTH) return counterClockwise ? Direction.DOWN : Direction.UP;
            return dir;
        }

        if (axis == Direction.Axis.Z) {
            // Rotation um Z (Vorderansicht)
            // Uhr: UP -> EAST -> DOWN -> WEST
            if (dir == Direction.UP) return counterClockwise ? Direction.WEST : Direction.EAST;
            if (dir == Direction.EAST) return counterClockwise ? Direction.UP : Direction.DOWN;
            if (dir == Direction.DOWN) return counterClockwise ? Direction.EAST : Direction.WEST;
            if (dir == Direction.WEST) return counterClockwise ? Direction.DOWN : Direction.UP;
            return dir;
        }
        return dir;
    }

    @SuppressWarnings("unchecked")
    private Property<Direction> getFacingProperty(BlockState state) {
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equals("facing") && prop.getValueClass() == Direction.class) return (Property<Direction>) prop;
            if (prop.getName().equals("horizontal_facing") && prop.getValueClass() == Direction.class) return (Property<Direction>) prop;
            if (prop.getName().equals("hopper_facing") && prop.getValueClass() == Direction.class) return (Property<Direction>) prop;
        }
        return null;
    }
}