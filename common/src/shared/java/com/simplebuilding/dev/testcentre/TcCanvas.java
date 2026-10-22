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
    /** Wandplaetze, an denen ein Ausstellungsstueck als Block steht ({@link #wallFrame}); Rueckwaende lassen sie aus. */
    private final java.util.Set<BlockPos> exhibits = new java.util.HashSet<>();
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

    /** Uebernimmt alle Schritte und Anker von {@code other}, um {@code dx} Spalten nach +x verschoben. */
    public void append(TcCanvas other, int dx) {
        append(other, dx, 0);
    }

    /** Wie {@link #append(TcCanvas, int)}, zusaetzlich um {@code dz} Reihen nach +z verschoben. */
    public void append(TcCanvas other, int dx, int dz) {
        BlockPos offset = new BlockPos(dx, 0, dz);
        for (TcOp op : other.ops) {
            add(op.moved(offset));
        }
        other.anchors.forEach((name, pos) -> anchors.put(name, pos.offset(offset)));
        other.exhibits.forEach(pos -> exhibits.add(pos.offset(offset)));
        grow(new BlockPos(other.maxX + dx, other.maxY, other.maxZ + dz));
    }

    // ------------------------------------------------------------------ Grundformen

    public void place(int x, int y, int z, BlockState state) {
        add(new TcOp.Place(new BlockPos(x, y, z), state));
    }

    public void place(int x, int y, int z, Block block) {
        place(x, y, z, block.defaultBlockState());
    }

    /**
     * Eine Haengematte der Farbe {@code cloth} zwischen den (schon gesetzten) Ankern {@code spot}: ihre Bloecke als
     * {@link TcOp.Place}, dann der Verknuepfungsschritt {@link TcOp.Hammock} fuer die Block-Entities.
     */
    public void hammock(com.simplebuilding.blocks.custom.HammockLayout.Spot spot, Block cloth) {
        com.simplebuilding.blocks.custom.HammockLayout.states(cloth, com.simplebuilding.blocks.ModBlocks.HAMMOCK_ROPE, spot)
                .forEach((pos, state) -> place(pos.getX(), pos.getY(), pos.getZ(), state));
        add(new TcOp.Hammock(spot.anchor(), spot.dx(), spot.dz()));
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
        octantFrame(x, y, z, facing, cornerA, cornerB, "CUBOID");
    }

    /** Wie oben, mit Figur ({@code OctantItem.SelectionShape}-Name, Spitze nach oben). */
    public void octantFrame(int x, int y, int z, Direction facing, BlockPos cornerA, BlockPos cornerB, String shape) {
        add(new TcOp.OctantFrame(new BlockPos(x, y, z), facing, cornerA, cornerB, shape));
    }

    /** Rahmen mit einer Blaupause, gescannt beim Bau aus der Auswahl vom Kartentisch {@code table} aus. */
    public void blueprintFrame(int x, int y, int z, Direction facing, BlockPos cornerA, BlockPos cornerB, BlockPos table) {
        add(new TcOp.BlueprintFrame(new BlockPos(x, y, z), facing, cornerA, cornerB, table));
    }

    public void sign(int x, int y, int z, Direction facing, Component... lines) {
        add(new TcOp.Sign(new BlockPos(x, y, z), facing, List.of(Arrays.copyOf(lines, Math.min(4, lines.length)))));
    }

    /** Strukturvorlage {@code template} (TZ22-3) mit der unteren Nordwest-Ecke bei x/y/z; der Abschnitt waechst um ihre Groesse. */
    public void structure(int x, int y, int z, net.minecraft.resources.Identifier template, net.minecraft.core.Vec3i size, int frames) {
        TcOp.Structure op = new TcOp.Structure(new BlockPos(x, y, z), template, size, frames);
        add(op);
        grow(op.far());
    }

    /** Ruestungsstaender; {@code gear}: Kopf, Brust, Beine, Fuesse, Haupthand, Nebenhand. */
    public void stand(int x, int y, int z, float yaw, List<ItemStack> gear, Component name) {
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack stack : gear) {
            copy.add(stack.copy());
        }
        add(new TcOp.Stand(new BlockPos(x, y, z), yaw, copy, name, false));
    }

    /** Trainingspuppe (nur mit {@code McVersion.TRAINING_DUMMY}); {@code gear} wie bei {@link #stand}. */
    public void dummy(int x, int y, int z, float yaw, List<ItemStack> gear, Component name) {
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack stack : gear) {
            copy.add(stack.copy());
        }
        add(new TcOp.Stand(new BlockPos(x, y, z), yaw, copy, name, true));
    }

    public void contents(int x, int y, int z, List<ItemStack> stacks) {
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack stack : stacks) {
            copy.add(stack.copy());
        }
        add(new TcOp.Fill(new BlockPos(x, y, z), copy));
    }

    /**
     * Befehlsblock mit Knopf davor (Seite {@code facing}) und Schild darueber. Der Block steht auf Glas
     * und traegt oben einen Kantenstein fuer das Schild; beides gehoert zur Planung, damit der
     * Isolationstest es sieht. Glas leitet keinen Strom: ein Knopf versorgt seinen Befehlsblock STARK,
     * und ein stark versorgter Leiterblock versorgt jeden Befehlsblock daneben mit.
     */
    public void command(int x, int y, int z, Direction facing, String command, Component... label) {
        if (y > 0) {
            place(x, y - 1, z, Blocks.GLASS);
        }
        add(new TcOp.Command(new BlockPos(x, y, z), facing, command, List.of(label)));
        place(x, y + 1, z, TRIM);
    }

    // ------------------------------------------------------------------ Wandhelfer

    /**
     * Rueckwand von {@code x0} bis {@code x1} bei {@code z}, Lagen 0 bis {@code h - 1}, mit Kante oben. Plaetze, an denen
     * schon ein Ausstellungsstueck als Block steht ({@link #wallFrame}), bleiben, wie sie sind.
     */
    public void backWall(int x0, int x1, int z, int h) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int y = 0; y < h; y++) {
                if (!exhibits.contains(new BlockPos(x, y, z))) {
                    place(x, y, z, y == h - 1 ? TRIM : WALL);
                }
            }
        }
    }

    /**
     * Ein Stapel an der Wand bei {@code wallZ}: was sich in eine Wand setzen laesst ({@link Exhibits#wallState}), steht dort
     * selbst als Block (TZ22-2: kein Rahmen); was an einer Wand haengen kann ({@link Exhibits#wallFrontState}), haengt als
     * Block davor; alles andere haengt im Rahmen davor (Rahmen zeigt nach Norden).
     */
    public void wallFrame(int x, int y, int wallZ, ItemStack stack) {
        BlockState block = Exhibits.wallState(stack);
        if (block != null) {
            place(x, y, wallZ, block);
            exhibits.add(new BlockPos(x, y, wallZ));
            return;
        }
        place(x, y, wallZ, WALL);
        BlockState front = Exhibits.wallFrontState(stack);
        if (front != null) {
            // Haengt wie der Rahmen am Wandblock dahinter (Lichtscheibe, Knopf, Wandfackel, Wandkopf ...).
            place(x, y, wallZ - 1, front);
            return;
        }
        frame(x, y, wallZ - 1, Direction.NORTH, stack);
    }

    /** Schild an der Wand bei {@code wallZ}. */
    public void wallSign(int x, int y, int wallZ, Component... lines) {
        place(x, y, wallZ, WALL);
        sign(x, y, wallZ - 1, Direction.NORTH, lines);
    }

    /**
     * Wandschild zu einem Weg (TZ22-8): die Zeilen kommen aus dem Weg ({@link TcWays.Way#lines}), {@code title} steht
     * fett darueber, solange Platz ist (null: ohne Titel).
     */
    public void waySign(int x, int y, int wallZ, TcWays.Way way, @org.jetbrains.annotations.Nullable Component title) {
        waySign(x, y, wallZ, way, way.lines(title));
    }

    /** Wandschild zu einem Weg mit eigenen (aus dem Weg erzeugten) Zeilen, etwa nur dem Ergebnis. */
    public void waySign(int x, int y, int wallZ, TcWays.Way way, List<Component> lines) {
        place(x, y, wallZ, WALL);
        add(new TcOp.Sign(new BlockPos(x, y, wallZ - 1), Direction.NORTH, List.copyOf(lines.subList(0, Math.min(4, lines.size()))), way));
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

    /** Hoechstens so viele Zeilen traegt eine Wand von {@link #shelves} (TZ22-5: Waende hoechstens 6 hoch). */
    public static final int SHELF_LINES = 4;
    /** Abstand paralleler Regalwaende in z: Wand, drei Felder Gang, Wand. */
    public static final int SHELF_PITCH = 4;
    /** Hoehe einer Regalwand: {@link #SHELF_LINES} Zeilen, Titel, Kante. */
    public static final int SHELF_HEIGHT = SHELF_LINES + 2;

    /**
     * Wie {@link #rowsPanel}, aber hoechstens {@link #SHELF_LINES} Zeilen je Wand (oberste Zeile y 3, unterste y 0); weitere
     * Zeilen laufen auf parallele Wände je {@link #SHELF_PITCH} dahinter weiter - Regalgaenge statt hoher oder ellenlanger
     * Waende (TZ22-5). Die Waende selbst setzt {@link #shelfWalls}. Liefert die erste freie Spalte.
     */
    public int shelves(int x0, int firstWallZ, List<Line> lines) {
        int end = x0 + 1;
        for (int k = 0; k * SHELF_LINES < lines.size(); k++) {
            List<Line> chunk = lines.subList(k * SHELF_LINES, Math.min(lines.size(), (k + 1) * SHELF_LINES));
            end = Math.max(end, rowsPanel(x0, SHELF_LINES - 1, firstWallZ + k * SHELF_PITCH, chunk));
        }
        return end;
    }

    /** Wie viele Regalwaende {@link #shelves} fuer {@code lines} Zeilen braucht (mindestens eine). */
    public static int shelfCount(int lines) {
        return Math.max(1, (lines + SHELF_LINES - 1) / SHELF_LINES);
    }

    /** Die Rueckwaende fuer {@code count} Regale ab {@code firstWallZ}, je {@link #SHELF_HEIGHT} hoch. */
    public void shelfWalls(int x0, int x1, int firstWallZ, int count) {
        for (int k = 0; k < count; k++) {
            backWall(x0, x1, firstWallZ + k * SHELF_PITCH, SHELF_HEIGHT);
        }
    }

    /**
     * Eine Gruppe einer {@link #exhibition}: Schild, Stapel in Anzeigereihenfolge und davor Bloecke ohne Item
     * ({@link Exhibits#blockStates}, je Eintrag ein Feld).
     */
    public record Group(Component label, List<ItemStack> stacks, List<BlockState> blocks) {
        public Group(Component label, List<ItemStack> stacks) {
            this(label, stacks, List.of());
        }
    }

    /** Tiefe des Bodenrasters einer {@link #exhibition}. */
    public static final int EXHIBIT_DEPTH = 6;
    /** Rueckwand einer {@link #exhibition}: hinter dem Raster ein Feld Gang, dann die Wand. */
    public static final int EXHIBIT_WALL_Z = EXHIBIT_DEPTH + 2;
    /** Hoehe der Rueckwand einer {@link #exhibition}: Schilder auf y 1, Kante auf y 2. */
    public static final int EXHIBIT_WALL_HEIGHT = 3;
    /** Sockel unter einem liegenden Rahmen im Raster einer {@link #exhibition}. */
    public static final BlockState PLINTH = TRIM;

    /**
     * Ausstellung wie der Holzbereich (TZ22-5, Besitzer 22.10.): je Gruppe Spalten dicht an dicht auf dem Boden (z 1 bis
     * {@link #EXHIBIT_DEPTH}). Was nur einen festen Boden braucht ({@link Exhibits#floorStates}), steht dort selbst als
     * Block (TZ22-2); alles andere liegt im Rahmen auf einem Sockel ({@link #PLINTH}, Rahmen nach oben). Ein Schild je
     * Gruppe an der niedrigen Rueckwand ueber ihrer ersten Spalte, eine Spalte Luft zwischen den Gruppen. Ab {@code x0};
     * liefert die erste freie Spalte. Die Rueckwand ({@link #EXHIBIT_WALL_Z}, {@link #EXHIBIT_WALL_HEIGHT}) setzt der Aufrufer.
     */
    public int exhibition(int x0, List<Group> groups) {
        int x = x0;
        for (Group group : groups) {
            List<ItemStack> stacks = group.stacks().stream().filter(stack -> !stack.isEmpty()).toList();
            int cells = group.blocks().size() + stacks.size();
            if (cells == 0) {
                continue;
            }
            for (int b = 0; b < group.blocks().size(); b++) {
                place(x + b / EXHIBIT_DEPTH, 0, 1 + b % EXHIBIT_DEPTH, group.blocks().get(b));
            }
            for (int s = 0; s < stacks.size(); s++) {
                int i = group.blocks().size() + s;
                int cx = x + i / EXHIBIT_DEPTH;
                int cz = 1 + i % EXHIBIT_DEPTH;
                List<BlockState> states = Exhibits.floorStates(stacks.get(s));
                if (states.isEmpty()) {
                    place(cx, 0, cz, PLINTH);
                    frame(cx, 1, cz, Direction.UP, stacks.get(s));
                } else {
                    for (int y = 0; y < states.size(); y++) {
                        place(cx, y, cz, states.get(y));
                    }
                }
            }
            wallSign(x, 1, EXHIBIT_WALL_Z, TcText.bold(group.label()));
            x += (cells + EXHIBIT_DEPTH - 1) / EXHIBIT_DEPTH + 1;
        }
        return x;
    }

    /** Kopfschild eines Abschnitts: Titel und Untertitel an der Rueckwand. */
    public void title(int x, int y, int wallZ, Component title, Component subtitle) {
        wallSign(x, y, wallZ, TcText.bold(title), subtitle);
    }
}
