package com.simplebuilding.modules.simplechallenges.client;

import com.simplebuilding.modules.simplechallenges.preset.LuckyMode;
import com.simplebuilding.modules.simplechallenges.preset.Preset;
import com.simplebuilding.modules.simplechallenges.preset.StartArea;
import com.simplebuilding.modules.simplechallenges.preset.UnlockMode;
import com.simplebuilding.modules.simplechallenges.preset.WorldMode;
import com.simplebuilding.modules.simplechallenges.sky.IslandPlan;
import com.simplebuilding.modules.simplechallenges.sky.OneBlockPhases;
import com.simplebuilding.modules.simplechallenges.sky.SkyGridPlan;
import com.simplebuilding.modules.simplechallenges.skyline.Layout;
import com.simplebuilding.modules.simplechallenges.unlock.UnlockGrid;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * The picture on the selection screen (S3): the shape of the start area on a map for Unlock presets, the island from above for
 * the Skyblock presets, the block ladder of the One Block, the lattice of the Sky Grid. Flat colour fills only (no world, no
 * textures), so it works before any world exists. An Unlock preset on an island shows both: the free cells lie over the island.
 */
public final class PresetPreview {
    static final int BG = 0xFF161719, FREE = 0xFF5DA05D, LOCKED = 0xFF2E3034, ORIGIN = 0xFFFFFFFF, SHADE = 0xA0101012;

    private PresetPreview() {}

    /** A map colour for a block id: a few well known blocks by hand, every other from a hash of its name. */
    public static int blockColor(String id) {
        return switch (id) {
            case "minecraft:grass_block" -> 0xFF5B9A3C;
            case "minecraft:dirt" -> 0xFF86603F;
            case "minecraft:sand" -> 0xFFDBCF9A;
            case "minecraft:oak_log", "minecraft:birch_log", "minecraft:spruce_log" -> 0xFF6B5130;
            case "minecraft:oak_leaves" -> 0xFF2F6B2A;
            case "minecraft:stone", "minecraft:cobblestone" -> 0xFF7D7D7D;
            case "minecraft:water" -> 0xFF3A5FCD;
            case "minecraft:bedrock" -> 0xFF333333;
            case "minecraft:cactus" -> 0xFF2D7A33;
            case "minecraft:chest" -> 0xFFA9742B;
            default -> {
                int h = id.hashCode();
                int r = 90 + ((h >> 16) & 0x7F), gr = 90 + ((h >> 8) & 0x7F), b = 90 + (h & 0x7F);
                yield 0xFF000000 | (r << 16) | (gr << 8) | b;
            }
        };
    }

    /** Top block per column of an island ({@code key = x<<32 ^ z}), for drawing from above. */
    static Map<Long, String> topBlocks(IslandPlan plan) {
        Map<Long, int[]> height = new HashMap<>();
        Map<Long, String> out = new HashMap<>();
        for (IslandPlan.Block b : plan.blocks()) {
            long k = ((long) b.dx() << 32) ^ (b.dz() & 0xFFFFFFFFL);
            int[] h = height.get(k);
            if (h == null || b.dy() > h[0]) {
                height.put(k, new int[]{b.dy()});
                out.put(k, b.block());
            }
        }
        return out;
    }

    /** Draws the preview of a preset into the box and returns the caption line (cells and blocks, or the mode). */
    public static Component draw(GuiGraphicsExtractor g, Font font, Preset p, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, BG);
        WorldMode world = p.world();
        UnlockMode unlock = p.unlock();
        if (world != null && world.kind() == WorldMode.Kind.ISLAND) return island(g, p, x, y, w, h);
        if (world != null && world.kind() == WorldMode.Kind.ONEBLOCK) return oneBlock(g, font, x, y, w, h);
        if (world != null && world.kind() == WorldMode.Kind.SKYGRID) return skyGrid(g, world.spacing(), x, y, w, h);
        if (p.skyline() != null) return skyline(g, p.skyline().layout(), p.skyline().hasEggs(), x, y, w, h);
        if (unlock != null) return map(g, unlock, x, y, w, h);
        if (p.lucky() != null) return lucky(g, font, p.lucky().kind(), x, y, w, h);
        g.centeredText(font, Component.translatable("simplechallenges.ui.no_preview"), x + w / 2, y + h / 2 - 4, 0xFFA0A0A0);
        return Component.empty();
    }

    private static Component map(GuiGraphicsExtractor g, UnlockMode u, int x, int y, int w, int h) {
        Set<Long> cells = StartArea.cells(u.start());
        int[] b = StartArea.bounds(cells);
        int minX = Math.min(b[0], -2) - 1, minZ = Math.min(b[1], -2) - 1, maxX = Math.max(b[2], 2) + 1, maxZ = Math.max(b[3], 2) + 1;
        int cols = maxX - minX + 1, rows = maxZ - minZ + 1;
        int px = Math.max(1, Math.min((w - 6) / cols, (h - 6) / rows));
        int ox = x + (w - cols * px) / 2, oy = y + (h - rows * px) / 2;
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                int gap = px >= 4 ? 1 : 0;
                int x0 = ox + (cx - minX) * px, z0 = oy + (cz - minZ) * px;
                g.fill(x0, z0, x0 + px - gap, z0 + px - gap, cells.contains(UnlockGrid.key(cx, cz)) ? FREE : LOCKED);
            }
        }
        int ox0 = ox - minX * px, oz0 = oy - minZ * px;
        g.outline(ox0 - 1, oz0 - 1, px + 1, px + 1, ORIGIN);
        return Component.translatable("simplechallenges.ui.caption.area", cells.size(), (b[2] - b[0] + 1) * u.cell(), (b[3] - b[1] + 1) * u.cell());
    }

    private static Component island(GuiGraphicsExtractor g, Preset p, int x, int y, int w, int h) {
        WorldMode world = p.world();
        IslandPlan plan = IslandPlan.of(world.style());
        Map<Long, String> top = topBlocks(plan);
        int minX = 99, maxX = -99, minZ = 99, maxZ = -99, groundX = 0, groundZ = 0, gMinX = 99, gMaxX = -99, gMinZ = 99, gMaxZ = -99;
        for (IslandPlan.Block b : plan.blocks()) {
            minX = Math.min(minX, b.dx());
            maxX = Math.max(maxX, b.dx());
            minZ = Math.min(minZ, b.dz());
            maxZ = Math.max(maxZ, b.dz());
            if (b.dy() <= 0) {
                gMinX = Math.min(gMinX, b.dx());
                gMaxX = Math.max(gMaxX, b.dx());
                gMinZ = Math.min(gMinZ, b.dz());
                gMaxZ = Math.max(gMaxZ, b.dz());
            }
        }
        groundX = gMaxX - gMinX + 1;
        groundZ = gMaxZ - gMinZ + 1;
        int cols = maxX - minX + 3, rows = maxZ - minZ + 3;
        int px = Math.max(2, Math.min((w - 6) / cols, (h - 6) / rows));
        int ox = x + (w - cols * px) / 2 + px, oy = y + (h - rows * px) / 2 + px;
        UnlockMode u = p.unlock();
        Set<Long> free = u == null ? null : StartArea.cells(u.start());
        for (var e : top.entrySet()) {
            int bx = (int) (e.getKey() >> 32), bz = (int) (long) e.getKey();
            int x0 = ox + (bx - minX) * px, z0 = oy + (bz - minZ) * px;
            g.fill(x0, z0, x0 + px, z0 + px, blockColor(e.getValue()));
            if (free != null && !free.contains(UnlockGrid.key(Math.floorDiv(bx, u.cell()), Math.floorDiv(bz, u.cell())))) g.fill(x0, z0, x0 + px, z0 + px, SHADE);
        }
        int sx = ox + (0 - minX) * px, sz = oy + (0 - minZ) * px;
        g.outline(sx - 1, sz - 1, px + 2, px + 2, ORIGIN);
        return Component.translatable(u == null ? "simplechallenges.ui.caption.island" : "simplechallenges.ui.caption.island_unlock", groundX, groundZ);
    }

    private static Component oneBlock(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h) {
        int n = OneBlockPhases.PHASES.size();
        int gap = Math.max(18, Math.min(30, (w - 8) / n));
        int x0 = x + (w - n * gap) / 2 + (gap - 16) / 2, yy = y + h / 2 - 8;
        for (int i = 0; i < n; i++) {
            String id = OneBlockPhases.PHASES.get(i).get(0).block();
            g.item(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id))), x0 + i * gap, yy);
            g.centeredText(font, String.valueOf(i + 1), x0 + i * gap + 8, yy + 19, 0xFFA0A0A0);
        }
        return Component.translatable("simplechallenges.ui.caption.oneblock", OneBlockPhases.PHASE_LENGTH, n);
    }

    private static Component skyGrid(GuiGraphicsExtractor g, int spacing, int x, int y, int w, int h) {
        int step = Math.max(6, spacing * 3);
        int nx = (w - 8) / step, nz = (h - 8) / step;
        int ox = x + (w - nx * step) / 2 + step / 2, oy = y + (h - nz * step) / 2 + step / 2;
        for (int i = 0; i < nx; i++) {
            for (int k = 0; k < nz; k++) {
                String id = SkyGridPlan.blockAt(1234, spacing, i * spacing, Math.floorDiv(64, spacing) * spacing, k * spacing);
                g.fill(ox + i * step - 2, oy + k * step - 2, ox + i * step + 3, oy + k * step + 3, blockColor(id == null ? "minecraft:stone" : id));
            }
        }
        return Component.translatable("simplechallenges.ui.caption.skygrid", spacing);
    }

    /** The first blocks of a Skyline from above: each block one dot, the colour runs through the rainbow with the number (the sequence itself is the registry's). */
    private static Component skyline(GuiGraphicsExtractor g, Layout layout, boolean eggs, int x, int y, int w, int h) {
        int n = layout == Layout.LINEAR ? 24 : layout == Layout.GRID ? 49 : 81;
        int minX = 0, maxX = 0, minZ = 0, maxZ = 0;
        int[][] cells = new int[n][];
        for (int i = 0; i < n; i++) {
            cells[i] = layout.cell(i);
            minX = Math.min(minX, cells[i][0]);
            maxX = Math.max(maxX, cells[i][0]);
            minZ = Math.min(minZ, cells[i][1]);
            maxZ = Math.max(maxZ, cells[i][1]);
        }
        int cols = maxX - minX + 1, rows = maxZ - minZ + 1;
        int step = Math.max(3, Math.min((w - 10) / cols, (h - 10) / rows));
        int ox = x + (w - cols * step) / 2, oy = y + (h - rows * step) / 2;
        int dot = Math.max(2, step - 2);
        for (int i = 0; i < n; i++) {
            int px = ox + (cells[i][0] - minX) * step, pz = oy + (cells[i][1] - minZ) * step;
            g.fill(px, pz, px + dot, pz + dot, 0xFF000000 | java.awt.Color.HSBtoRGB(i * 0.013F, 0.55F, 0.95F));
        }
        int sx = ox - minX * step, sz = oy - minZ * step;
        g.outline(sx - 1, sz - 1, dot + 2, dot + 2, ORIGIN);
        if (eggs) g.fill(Math.max(x + 2, ox - 2 * step), oy + (-minZ) * step, Math.max(x + 3, ox - 2 * step + dot), oy + (-minZ) * step + dot, 0xFFE0B040);
        return Component.translatable("simplechallenges.ui.caption.skyline." + layout.id);
    }

    private static final String[] LUCKY_ICONS = {"minecraft:oak_log", "minecraft:cobblestone", "minecraft:gold_block", "minecraft:tnt", "minecraft:diamond_block"};

    /** A row of blocks for the Lucky modes. */
    private static Component lucky(GuiGraphicsExtractor g, Font font, LuckyMode.Kind kind, int x, int y, int w, int h) {
        int gap = Math.max(20, Math.min(30, (w - 8) / LUCKY_ICONS.length));
        int x0 = x + (w - LUCKY_ICONS.length * gap) / 2 + (gap - 16) / 2, yy = y + h / 2 - 8;
        for (int i = 0; i < LUCKY_ICONS.length; i++) g.item(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(LUCKY_ICONS[i]))), x0 + i * gap, yy);
        return Component.translatable(kind == LuckyMode.Kind.FIXED ? "simplechallenges.ui.caption.randomdrops" : "simplechallenges.ui.caption.lucky");
    }
}
