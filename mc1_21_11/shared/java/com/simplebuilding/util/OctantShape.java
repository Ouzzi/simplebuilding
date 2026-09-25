package com.simplebuilding.util;

import com.simplebuilding.items.custom.OctantItem;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;

/**
 * Die Form einer Oktant-Auswahl als Block-Praedikat: welche Bloecke der Bounding Box von Pos1/Pos2
 * zur Figur (Quader, Zylinder, Kugel, Pyramide, Prisma, Rechteck, Ellipse) gehoeren.
 *
 * <p>Eine Quelle fuer beide Seiten: die Vorschau im Client ({@code BlockHighlightRenderer})
 * zeichnet genau diese Bloecke, der Blaupausen-Scan am Kartentisch nimmt genau diese Bloecke auf.
 */
public final class OctantShape {
    private OctantShape() {
    }

    /** Gewaehlte Form (Standard Quader). */
    public static OctantItem.SelectionShape shape(CompoundTag nbt) {
        String shapeName = nbt.getStringOr("Shape", "");
        OctantItem.SelectionShape shape = OctantItem.SelectionShape.CUBOID;
        try { if (!shapeName.isEmpty()) shape = OctantItem.SelectionShape.valueOf(shapeName); } catch (Exception ignored) {}
        return shape;
    }

    /** Ausrichtung der Figur (Standard +Y). */
    public static Direction orientation(CompoundTag nbt) {
        int orientIdx = nbt.getIntOr("Orientation", 1); // 0=+X, 1=+Y, 2=+Z, 3=-X, 4=-Y, 5=-Z
        return switch (orientIdx) {
            case 0 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            case 3 -> Direction.WEST;
            case 4 -> Direction.DOWN;
            case 5 -> Direction.NORTH;
            default -> Direction.UP;
        };
    }

    public static CompoundTag data(ItemStack octant) {
        return octant.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    /** Bounding Box (Blockgrenzen, max exklusiv) einer vollstaendigen Auswahl, sonst {@code null}. */
    public static AABB bounds(CompoundTag nbt) {
        BlockPos pos1 = corner(nbt, "Pos1");
        BlockPos pos2 = corner(nbt, "Pos2");
        if (pos1 == null || pos2 == null) {
            return null;
        }
        // wie guiDrawHelper.getFullArea (die Klasse ist clientseitig)
        return new AABB(Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()),
                Math.max(pos1.getX(), pos2.getX()) + 1, Math.max(pos1.getY(), pos2.getY()) + 1, Math.max(pos1.getZ(), pos2.getZ()) + 1);
    }

    public static BlockPos corner(CompoundTag nbt, String key) {
        return nbt.getIntArray(key).filter(a -> a.length == 3).map(a -> new BlockPos(a[0], a[1], a[2])).orElse(null);
    }

    /** Das Praedikat der Auswahl eines Oktanten; {@code null} ohne beide Ecken. */
    public static Predicate<BlockPos> of(ItemStack octant) {
        CompoundTag nbt = data(octant);
        AABB bounds = bounds(nbt);
        return bounds == null ? null : predicate(shape(nbt), orientation(nbt), bounds);
    }

    public static Predicate<BlockPos> predicate(OctantItem.SelectionShape shape, Direction orientation, AABB bounds) {
        return switch (shape) {
            // Fix: Uses strict < bounds.maxY to exclude the block above the selection
            case CYLINDER, ELLIPSE -> p -> {
                // Normalize point relative to bounds center
                AABB tBounds = transformToY(bounds, orientation.getAxis());
                BlockPos transformed = transformToY(p, orientation, tBounds);
                // Check Height (Y in transformed space)
                if (transformed.getY() < tBounds.minY || transformed.getY() >= tBounds.maxY) return false;
                return isPointInEllipse(transformed.getX() + 0.5, transformed.getZ() + 0.5, tBounds);
            };
            case SPHERE -> p -> isPointInEllipsoid(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, bounds);
            case PYRAMID -> p -> {
                 AABB tBounds = transformToY(bounds, orientation.getAxis());
                 BlockPos transformed = transformToY(p, orientation, tBounds);
                 return isPointInPyramid(transformed.getX() + 0.5, transformed.getY() + 0.5, transformed.getZ() + 0.5, tBounds);
            };
            case TRIANGLE -> p -> { // Prism
                 AABB tBounds = transformToY(bounds, orientation.getAxis());
                 BlockPos transformed = transformToY(p, orientation, tBounds);
                 return isPointInPrism(transformed.getX() + 0.5, transformed.getY() + 0.5, transformed.getZ() + 0.5, tBounds);
            };
            case RECTANGLE, CUBOID -> p -> p.getX() >= bounds.minX && p.getX() < bounds.maxX
                    && p.getY() >= bounds.minY && p.getY() < bounds.maxY
                    && p.getZ() >= bounds.minZ && p.getZ() < bounds.maxZ;
        };
    }

    // --- TRANSFORM HELPERS (Rotate space to Y-up) ---
    private static BlockPos transformToY(BlockPos p, Direction orientation, AABB transformedBounds) {
        BlockPos transformed = switch (orientation.getAxis()) {
            case Y -> p;
            case X -> new BlockPos(p.getY(), p.getX(), p.getZ());
            case Z -> new BlockPos(p.getX(), p.getZ(), p.getY());
        };

        if (orientation.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            int flippedY = (int) (transformedBounds.minY + transformedBounds.maxY - 1 - transformed.getY());
            return new BlockPos(transformed.getX(), flippedY, transformed.getZ());
        }

        return transformed;
    }

    private static AABB transformToY(AABB b, Direction.Axis orientation) {
        if (orientation == Direction.Axis.Y) return b;
        if (orientation == Direction.Axis.X) return new AABB(b.minY, b.minX, b.minZ, b.maxY, b.maxX, b.maxZ);
        return new AABB(b.minX, b.minZ, b.minY, b.maxX, b.maxZ, b.maxY);
    }

    // --- MATH HELPERS ---
    private static boolean isPointInEllipse(double x, double z, AABB b) {
        double width = b.maxX - b.minX; double length = b.maxZ - b.minZ;
        double cx = b.minX + width / 2.0; double cz = b.minZ + length / 2.0;
        double rx = width / 2.0; double rz = length / 2.0;
        if (rx <= 0 || rz <= 0) return false;
        return Math.pow(x - cx, 2) / Math.pow(rx, 2) + Math.pow(z - cz, 2) / Math.pow(rz, 2) <= 1.0;
    }
    private static boolean isPointInEllipsoid(double x, double y, double z, AABB b) {
        double w = b.maxX - b.minX; double h = b.maxY - b.minY; double l = b.maxZ - b.minZ;
        double cx = b.minX + w/2.0; double cy = b.minY + h/2.0; double cz = b.minZ + l/2.0;
        double rx = w/2.0; double ry = h/2.0; double rz = l/2.0;
        if (rx<=0||ry<=0||rz<=0) return false;
        return Math.pow(x-cx,2)/Math.pow(rx,2) + Math.pow(y-cy,2)/Math.pow(ry,2) + Math.pow(z-cz,2)/Math.pow(rz,2) <= 1.0;
    }
    private static boolean isPointInPyramid(double x, double y, double z, AABB b) {
        double w = b.maxX - b.minX; double h = b.maxY - b.minY; double l = b.maxZ - b.minZ;
        double cx = b.minX + w/2.0; double cz = b.minZ + l/2.0;
        double rx = w/2.0; double rz = l/2.0;
        if (y < b.minY || y >= b.maxY) return false;
        double progress = (y - b.minY) / h;
        double crx = rx * (1.0 - progress);
        double crz = rz * (1.0 - progress);
        return Math.abs(x - cx) <= crx && Math.abs(z - cz) <= crz;
    }
    private static boolean isPointInPrism(double x, double y, double z, AABB b) {
        double wX = b.maxX - b.minX; double h = b.maxY - b.minY; double wZ = b.maxZ - b.minZ;
        if (y < b.minY || y >= b.maxY) return false;
        boolean alongZ = wZ > wX;
        double progress = (y - b.minY) / h;
        if (alongZ) {
            double cx = b.minX + wX / 2.0;
            return Math.abs(x - cx) <= (wX / 2.0) * (1.0 - progress);
        } else {
            double cz = b.minZ + wZ / 2.0;
            return Math.abs(z - cz) <= (wZ / 2.0) * (1.0 - progress);
        }
    }
}
