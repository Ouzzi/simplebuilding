package com.simplebuilding.dev.testcentre;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Zeichenflaeche eines Abschnitts: sammelt {@link TcOp}s in lokalen Koordinaten und merkt sich die
 * Ausdehnung. Blickrichtung der Besucher ist +z; Rueckwaende stehen bei grossem z, Rahmen und Schilder
 * an ihnen zeigen nach Norden (-z).
 */
public final class TcCanvas {

    /** Wandmaterial der Ausstellungswaende (wie die Handarbeit des Besitzers). */
    public static final BlockState WALL = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    /** Zweites Wandmaterial fuer Pfeiler und Kanten. */
    public static final BlockState TRIM = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();

    private final List<TcOp> ops = new ArrayList<>();
    private final Map<String, BlockPos> anchors = new LinkedHashMap<>();
    private int maxX;
    private int maxY;
    private int maxZ;

    public List<TcOp> ops() {
        return ops;
    }

    /** Benannte Punkte (lokal), die andere Abschnitte brauchen - etwa die Mitte des Dunkelraums. */
    public Map<String, BlockPos> anchors() {
        return anchors;
    }

    public void anchor(String name, int x, int y, int z) {
        anchors.put(name, new BlockPos(x, y, z));
    }

    /** Breite in x (letzte belegte Spalte + 1). */
    public int width() {
        return maxX + 1;
    }

    /** Tiefe in z (letzte belegte Reihe + 1). */
    public int depth() {
        return maxZ + 1;
    }

    /** Hoehe ueber dem Boden (hoechste belegte Lage + 1). */
    public int height() {
        return maxY + 1;
    }

    private void grow(BlockPos pos) {
        maxX = Math.max(maxX, pos.getX());
        maxY = Math.max(maxY, pos.getY());
        maxZ = Math.max(maxZ, pos.getZ());
    }

    private void add(TcOp op) {
        if (op.pos().getX() < 0 || op.pos().getZ() < 0 || op.pos().getY() < -1) {
            throw new IllegalArgumentException("test centre op outside its section: " + op);
        }
        grow(op.pos());
        ops.add(op);
    }

    // ------------------------------------------------------------------ Grundformen

    public void place(int x, int y, int z, BlockState state) {
        add(new TcOp.Place(new BlockPos(x, y, z), state));
    }

    public void place(int x, int y, int z, Block block) {
        place(x, y, z, block.defaultBlockState());
    }

    public void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                    place(x, y, z, state);
                }
            }
        }
    }

    public void frame(int x, int y, int z, Direction facing, ItemStack stack) {
        add(new TcOp.Frame(new BlockPos(x, y, z), facing, stack.copy()));
    }

    /** Rahmen mit einem Oktanten, dessen Auswahl (lokal) schon gesetzt ist. */
    public void octantFrame(int x, int y, int z, Direction facing, BlockPos cornerA, BlockPos cornerB) {
        add(new TcOp.OctantFrame(new BlockPos(x, y, z), facing, cornerA, cornerB));
    }

    /** Rahmen mit einer Blaupause, gescannt beim Bau aus der Auswahl vom Kartentisch {@code table} aus. */
    public void blueprintFrame(int x, int y, int z, Direction facing, BlockPos cornerA, BlockPos cornerB, BlockPos table) {
        add(new TcOp.BlueprintFrame(new BlockPos(x, y, z), facing, cornerA, cornerB, table));
    }

    public void sign(int x, int y, int z, Direction facing, Component... lines) {
        add(new TcOp.Sign(new BlockPos(x, y, z), facing, List.of(Arrays.copyOf(lines, Math.min(4, lines.length)))));
    }

    /** Ruestungsstaender; {@code gear}: Kopf, Brust, Beine, Fuesse, Haupthand, Nebenhand. */
    public void stand(int x, int y, int z, float yaw, List<ItemStack> gear, Component name) {
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack stack : gear) {
            copy.add(stack.copy());
        }
        add(new TcOp.Stand(new BlockPos(x, y, z), yaw, copy, name));
    }

    public void contents(int x, int y, int z, List<ItemStack> stacks) {
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack stack : stacks) {
            copy.add(stack.copy());
        }
        add(new TcOp.Fill(new BlockPos(x, y, z), copy));
    }

    public void command(int x, int y, int z, Direction facing, String command, Component... label) {
        add(new TcOp.Command(new BlockPos(x, y, z), facing, command, List.of(label)));
        // Knopf davor und Schild darueber belegen auch Platz.
        grow(new BlockPos(x, y + 1, z));
    }

    // ------------------------------------------------------------------ Wandhelfer

    /** Rueckwand von {@code x0} bis {@code x1} bei {@code z}, Lagen 0 bis {@code h - 1}, mit Kante oben. */
    public void backWall(int x0, int x1, int z, int h) {
        fill(x0, 0, z, x1, h - 2, z, WALL);
        fill(x0, h - 1, z, x1, h - 1, z, TRIM);
    }

    /** Rahmen an der Wand bei {@code wallZ} (der Rahmen haengt im Block davor und zeigt nach Norden). */
    public void wallFrame(int x, int y, int wallZ, ItemStack stack) {
        place(x, y, wallZ, WALL);
        frame(x, y, wallZ - 1, Direction.NORTH, stack);
    }

    /** Schild an der Wand bei {@code wallZ}. */
    public void wallSign(int x, int y, int wallZ, Component... lines) {
        place(x, y, wallZ, WALL);
        sign(x, y, wallZ - 1, Direction.NORTH, lines);
    }

    /**
     * Rahmenraster an der Wand bei {@code wallZ}: spaltenweise von oben nach unten, beginnend bei
     * {@code x0}, {@code rows} Reihen, unterste Reihe bei {@code y0}. Mit {@code labels} steht unter
     * jedem Rahmen ein Schild (Reihenabstand 2). Liefert die erste freie Spalte danach.
     */
    public int frameGrid(int x0, int y0, int wallZ, List<ItemStack> items, List<List<Component>> labels, int rows) {
        int pitch = labels == null ? 1 : 2;
        for (int i = 0; i < items.size(); i++) {
            int col = i / rows;
            int row = i % rows;
            int x = x0 + col;
            int y = y0 + (rows - 1 - row) * pitch + (labels == null ? 0 : 1);
            wallFrame(x, y, wallZ, items.get(i));
            if (labels != null) {
                wallSign(x, y - 1, wallZ, labels.get(i).toArray(Component[]::new));
            }
        }
        int cols = (items.size() + rows - 1) / rows;
        return x0 + cols;
    }

    /** Eine beschriftete Zeile fuer {@link #rowsPanel}. */
    public record Line(Component label, List<ItemStack> stacks) {
    }

    /**
     * Zeilenweise Tafel an der Wand bei {@code wallZ}: je Zeile links ein Schild bei {@code x0}, rechts
     * davon die Rahmen; die erste Zeile liegt bei {@code topY}, jede weitere eine Lage tiefer.
     * Liefert die erste freie Spalte rechts der breitesten Zeile.
     */
    public int rowsPanel(int x0, int topY, int wallZ, List<Line> lines) {
        int end = x0 + 1;
        for (int i = 0; i < lines.size(); i++) {
            int y = topY - i;
            Line line = lines.get(i);
            wallSign(x0, y, wallZ, line.label());
            for (int j = 0; j < line.stacks().size(); j++) {
                wallFrame(x0 + 1 + j, y, wallZ, line.stacks().get(j));
            }
            end = Math.max(end, x0 + 1 + line.stacks().size());
        }
        return end;
    }

    /** Kopfschild eines Abschnitts: Titel und Untertitel an der Rueckwand. */
    public void title(int x, int y, int wallZ, Component title, Component subtitle) {
        wallSign(x, y, wallZ, TcText.bold(title), subtitle);
    }
}
