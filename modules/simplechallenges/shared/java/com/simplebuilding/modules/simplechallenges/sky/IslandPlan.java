package com.simplebuilding.modules.simplechallenges.sky;

import com.simplebuilding.modules.simplechallenges.price.WeightedItem;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The start islands of the Skyblock mode as plain data (no world access, so the shapes are testable): a list of blocks relative
 * to the spawn column (0, 0) whose top grass layer lies at dy = 0, the chest with its items and the standing place. Every island
 * covers the column (0, 0), so the fixed spawn at block 0,0 always stands on it.
 */
public final class IslandPlan {
    public record Block(int dx, int dy, int dz, String block) {}

    public record Stack(String item, int count) {}

    private final List<Block> blocks = new ArrayList<>();
    private final Set<Long> columns = new LinkedHashSet<>();
    /** Chest position (dy above the top layer already included) and the facing as a Direction name. */
    public final int chestX, chestY, chestZ;
    public final String chestFacing;
    public final List<Stack> chest;

    private IslandPlan(int chestX, int chestY, int chestZ, String facing, List<Stack> chest) {
        this.chestX = chestX;
        this.chestY = chestY;
        this.chestZ = chestZ;
        this.chestFacing = facing;
        this.chest = chest;
    }

    public List<Block> blocks() {
        return blocks;
    }

    public boolean covers(int x, int z) {
        return columns.contains(((long) x << 32) ^ (z & 0xFFFFFFFFL));
    }

    private void add(int x, int y, int z, String block) {
        blocks.add(new Block(x, y, z, block));
        columns.add(((long) x << 32) ^ (z & 0xFFFFFFFFL));
    }

    private void layerRect(int x0, int z0, int x1, int z1, int top, int dirtLayers, String dirt) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                add(x, top, z, dirt.equals("minecraft:sand") ? "minecraft:sand" : "minecraft:grass_block");
                for (int k = 1; k <= dirtLayers; k++) add(x, top - k, z, dirt);
            }
        }
    }

    private void tree(int x, int z, int height) {
        for (int y = 1; y <= height; y++) add(x, y, z, "minecraft:oak_log");
        for (int dy = height - 1; dy <= height; dy++) {
            int r = dy == height ? 1 : 2;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx == 0 && dz == 0 && dy <= height) continue; // the trunk
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue; // rounded corners
                    add(x + dx, dy + 1, z + dz, "minecraft:oak_leaves");
                }
            }
        }
        add(x, height + 2, z, "minecraft:oak_leaves");
    }

    public static IslandPlan of(String style) {
        List<Stack> items = switch (style) {
            case "mini" -> List.of(new Stack("minecraft:ice", 1), new Stack("minecraft:bone_meal", 3), new Stack("minecraft:oak_sapling", 1));
            case "desert" -> List.of(new Stack("minecraft:water_bucket", 1), new Stack("minecraft:cactus", 2), new Stack("minecraft:melon_seeds", 1));
            default -> List.of(new Stack("minecraft:ice", 2), new Stack("minecraft:lava_bucket", 1), new Stack("minecraft:bone_meal", 3),
                    new Stack("minecraft:pumpkin_seeds", 1), new Stack("minecraft:melon_slice", 1), new Stack("minecraft:sugar_cane", 1),
                    new Stack("minecraft:oak_sapling", 1));
        };
        switch (style) {
            case "mini" -> {
                IslandPlan p = new IslandPlan(-1, 1, -1, "SOUTH", items);
                p.layerRect(-1, -1, 1, 1, 0, 2, "minecraft:dirt");
                p.add(0, -3, 0, "minecraft:bedrock");
                p.tree(1, 1, 4);
                return p;
            }
            case "desert" -> {
                IslandPlan p = new IslandPlan(-2, 1, -2, "SOUTH", items);
                p.layerRect(-2, -2, 2, 2, 0, 2, "minecraft:sand");
                p.add(0, -3, 0, "minecraft:bedrock");
                p.add(2, 1, 2, "minecraft:cactus");
                p.add(2, 2, 2, "minecraft:cactus");
                return p;
            }
            default -> {
                // the classic L: a 3x3 square round the spawn and two arms of 3 blocks width
                IslandPlan p = new IslandPlan(-1, 1, -1, "SOUTH", items);
                p.layerRect(-1, -1, 1, 1, 0, 2, "minecraft:dirt");
                p.layerRect(2, -1, 6, 1, 0, 2, "minecraft:dirt");
                p.layerRect(-1, 2, 1, 6, 0, 2, "minecraft:dirt");
                p.add(0, -3, 0, "minecraft:bedrock");
                p.tree(0, 5, 4);
                return p;
            }
        }
    }

    /** The place the player stands: on the top layer of the spawn column. */
    public static int standY() {
        return 1;
    }

    /** What the island gives by hand (weights), the price pool of an unlock preset on it: its dirt, sand, wood and leaves. */
    public static List<WeightedItem> reachable(String style) {
        return switch (style) {
            case "desert" -> List.of(new WeightedItem("minecraft:sand", 20, 64), new WeightedItem("minecraft:cactus", 2, 64));
            case "mini" -> List.of(new WeightedItem("minecraft:dirt", 10, 64), new WeightedItem("minecraft:oak_log", 4, 64),
                    new WeightedItem("minecraft:oak_leaves", 6, 64));
            default -> List.of(new WeightedItem("minecraft:dirt", 20, 64), new WeightedItem("minecraft:oak_log", 4, 64),
                    new WeightedItem("minecraft:oak_leaves", 8, 64));
        };
    }
}
