package com.simplebuilding.items.custom;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class RotatorItem extends Item {
    public RotatorItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);
        PlayerEntity player = context.getPlayer();
        boolean isSneaking = player != null && player.isSneaking();

        // 1. Rand-Erkennung (ca. 2 Pixel am Rand des Blocks)
        Direction rimDirection = getRimDirection(context, 0.125);

        // 2. Neuen Status berechnen
        BlockState newState = calculateNewState(state, context.getSide(), rimDirection, isSneaking);

        if (newState != null && newState != state) {
            if (!world.isClient()) {
                world.setBlockState(pos, newState, Block.NOTIFY_ALL);
                world.playSound(null, pos, SoundEvents.ITEM_SPYGLASS_USE, SoundCategory.BLOCKS, 1.0f, 1.0f);

                if (player != null) {
                    context.getStack().damage(1, player, EquipmentSlot.MAINHAND);
                }
            }
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Nullable
    private Direction getRimDirection(ItemUsageContext context, double margin) {
        Vec3d hitPos = context.getHitPos().subtract(Vec3d.of(context.getBlockPos()));
        Direction face = context.getSide();

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
        if (state.getProperties().contains(Properties.AXIS)) {
            return handleAxisRotation(state, clickedFace, rimDirection);
        }

        // --- PISTON / FURNACE (Facing) ---
        Property<Direction> facingProp = getFacingProperty(state);
        if (facingProp != null) {
            return handleFacingRotation(state, facingProp, clickedFace, rimDirection, isSneaking);
        }

        // --- ROTATION (0-15) ---
        if (state.getProperties().contains(Properties.ROTATION)) {
            int current = state.get(Properties.ROTATION);
            int change = isSneaking ? -1 : 1;
            if (rimDirection != null) change *= 4;
            int next = (current + change + 16) % 16;
            return state.with(Properties.ROTATION, next);
        }

        return null;
    }

    // --- LOGIC: AXIS (Der wichtigste Fix) ---
    private BlockState handleAxisRotation(BlockState state, Direction clickedFace, Direction rimDirection) {
        Direction.Axis currentAxis = state.get(Properties.AXIS);

        // 1. Rand-Klick: Richte Achse parallel zum Rand aus.
        if (rimDirection != null) {
            return state.with(Properties.AXIS, rimDirection.getAxis());
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
            return state.with(Properties.AXIS, clickedAxis);
        } else {
            // Block zeigt bereits auf uns zu (oder weg). Wir rotieren zur nächsten Achse.
            // Zyklus: X -> Y -> Z -> X
            return state.with(Properties.AXIS, nextAxis(currentAxis));
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
        Direction currentFacing = state.get(prop);
        Collection<Direction> validDirections = prop.getValues();

        // 1. Rand-Klick (Kippen)
        if (rimDirection != null) {
            // Prio 1: Kippen in Richtung des Rands
            if (validDirections.contains(rimDirection)) return state.with(prop, rimDirection);
            // Prio 2: Kippen weg vom Rand
            if (validDirections.contains(rimDirection.getOpposite())) return state.with(prop, rimDirection.getOpposite());
        }

        // 2. Zentrum-Klick (Roll)
        // Rotiere um die Achse der Fläche, auf die geklickt wurde.

        // Beispiel: Piston schaut UP. Ich klicke auf die Seite (NORTH).
        // Piston soll sich jetzt im Kreis drehen (UP -> EAST -> DOWN -> WEST).
        // Das ist eine Rotation um die X-Achse (da wir auf Z schauen).

        // WICHTIG: Wenn der Block parallel zur Klick-Achse liegt (Piston schaut NORTH, wir klicken NORTH),
        // dann rotieren wir ihn "um sich selbst" (Uhrzeigersinn).

        Direction nextFacing = rotateAroundAxis(currentFacing, clickedFace.getAxis(), isSneaking);

        // Spezialfall: Wenn Rotation keine Änderung bringt (weil Block auf Achse liegt),
        // erzwingen wir eine Änderung zur "nächsten" Seite.
        if (nextFacing == currentFacing && validDirections.size() > 1) {
            // Wenn ich Piston (NORTH) von vorne anklicke, soll er nicht NORTH bleiben.
            // Er soll z.B. UP werden.
            nextFacing = getStandardRotationStart(clickedFace.getAxis(), validDirections);

            // Wenn wir zufällig wieder beim alten landen, nimm den nächsten in der Liste.
            if (nextFacing == currentFacing) {
                nextFacing = cycleDirectionList(currentFacing, isSneaking, validDirections);
            }
        }

        if (validDirections.contains(nextFacing)) {
            return state.with(prop, nextFacing);
        }

        // Fallback: Zyklus durch Liste
        return state.with(prop, cycleDirectionList(currentFacing, isSneaking, validDirections));
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
            return counterClockwise ? dir.rotateYCounterclockwise() : dir.rotateYClockwise();
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
            if (prop.getName().equals("facing") && prop.getType() == Direction.class) return (Property<Direction>) prop;
            if (prop.getName().equals("horizontal_facing") && prop.getType() == Direction.class) return (Property<Direction>) prop;
            if (prop.getName().equals("hopper_facing") && prop.getType() == Direction.class) return (Property<Direction>) prop;
        }
        return null;
    }
}