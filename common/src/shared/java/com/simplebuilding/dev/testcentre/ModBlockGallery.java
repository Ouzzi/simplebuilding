package com.simplebuilding.dev.testcentre;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * DW04D-1 (Besitzer 04.11.: "alle bloecke in logischer reihenfolge nebeneinander ... wall, octet, slab, stair, block cracked,
 * mossy, chiseled"): je Mod der Familie ein beschilderter Bereich, darin eine Spalte je Material-Familie (Basisblock und
 * Varianten), Zeilen je Formtyp auf gleicher Hoehe ueber alle Spalten eines Bands, Spalten nach Farbe gereiht; dahinter die
 * weiteren Bloecke und die nicht setzbaren Items (Rahmen flach auf dem Boden) in Gruppen nach Namensmuster.
 *
 * <p>Nichts ist von Hand gelistet: Formtyp aus Block-Klasse/Block-Tag (Octets ueber ihre Zellen-Eigenschaft {@code o000}),
 * Familie aus der Id (Form-Endung und Varianten-Woerter weg, {@link #materialKey}). Vanilla-Bloecke mit gleichem Schluessel
 * stehen als Bezug in der Spalte mit (Mod-Bloecke anderer Mods nie: jede Mod-Id steht genau einmal). Ausnahmen sind nur die
 * vorhandenen ({@link TestCentreLayout#EXCLUDED}, Tab-Platzhalter).
 */
public final class ModBlockGallery {

    /** Abschnitt in der Zentrale ({@link TestCentreLayout#DISTRICTS}). */
    public static final String SECTION = "mod_blocks";
    /** Hoechstens so breit wird ein Band/Bereich (Abschnitt mit Ausgabe-Knopf bleibt unter {@link TestCentreLayout#MAX_ROW_WIDTH}). */
    public static final int MAX_WIDTH = TestCentreLayout.MAX_ROW_WIDTH - TestCentreLayout.KIOSK_WIDTH - 2;
    /** Abstand der Familien-Spalten (eine Spalte Luft: Mauern/Zaeune verbinden sich nicht). */
    public static final int COLUMN_PITCH = 2;
    /** So viele Eintraege stehen in einem Feld uebereinander (Tueren einzeln). */
    public static final int STACK = 4;
    /** Reihen einer Gruppe weiterer Bloecke/Items. */
    static final int GROUP_ROWS = 4;
    /** Abstand der Mod-Bereiche. */
    static final int AREA_GAP = 3;

    /** Formtyp, Reihenfolge = Zeilen von vorn (Gang) nach hinten. */
    public enum Form {
        OCTET, SLAB, STAIRS, WALL, FENCE, FENCE_GATE, PANE, TRAPDOOR, DOOR, BLOCK;

        int stack() {
            return this == DOOR ? 1 : STACK;
        }
    }

    /** Ein gesetzter Familien-Eintrag: Lage des untersten Blocks im Canvas, Band und Spalte der Familie. */
    public record Cell(String namespace, int band, String family, Form form, Item item, boolean vanilla, BlockPos pos) {
        Cell moved(int dx, int dz) {
            return new Cell(namespace, band, family, form, item, vanilla, pos.offset(dx, 0, dz));
        }
    }

    /** Ergebnis: Schritte und die Familien-Zellen (fuer Tests). */
    public record Gallery(TcCanvas canvas, List<Cell> cells) {
    }

    private record Entry(Item item, List<BlockState> states, Form form, String key, int rank, boolean vanilla) {
        int height() {
            return Math.max(1, states.size());
        }
    }

    private ModBlockGallery() {
    }

    // ------------------------------------------------------------------ Ableitung

    /** Namensraeume der Familie mit mindestens einem ausstellbaren Item; SimpleBuilding zuerst, dann nach Id. */
    public static List<String> namespaces() {
        java.util.TreeSet<String> out = new java.util.TreeSet<>();
        for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
            if (TcContext.isFamily(id) && shown(id)) {
                out.add(id.getNamespace());
            }
        }
        List<String> list = new ArrayList<>(out);
        if (list.remove(com.simplebuilding.Simplebuilding.MOD_ID)) {
            list.addFirst(com.simplebuilding.Simplebuilding.MOD_ID);
        }
        return list;
    }

    /** Ob ein Familien-Item in den Bereich gehoert (nicht ausgenommen, kein Tab-Platzhalter). */
    public static boolean shown(Identifier id) {
        return !TestCentreLayout.EXCLUDED.containsKey(id.toString()) && !TcContext.isSpacerId(id);
    }

    /** Die Bloecke, mit denen ein Item steht (unten zuerst), leer = Rahmen. */
    static List<BlockState> states(Item item) {
        ItemStack stack = new ItemStack(item);
        List<BlockState> states = Exhibits.floorStates(stack);
        if (states.isEmpty() && item instanceof BlockItem blockItem) {
            List<BlockState> base = Exhibits.blockStates(blockItem.getBlock());
            states = base.isEmpty() ? List.of() : List.of(base.getFirst());
        }
        return states;
    }

    /** Formtyp eines gesetzten Zustands, null = kein Familien-Teil. */
    static Form formOf(BlockState state) {
        Block block = state.getBlock();
        if (block.getStateDefinition().getProperty("o000") instanceof BooleanProperty) {
            return Form.OCTET;
        }
        if (block instanceof SlabBlock || state.is(BlockTags.SLABS)) {
            return Form.SLAB;
        }
        if (block instanceof StairBlock || state.is(BlockTags.STAIRS)) {
            return Form.STAIRS;
        }
        if (block instanceof WallBlock || state.is(BlockTags.WALLS)) {
            return Form.WALL;
        }
        if (block instanceof FenceGateBlock || state.is(BlockTags.FENCE_GATES)) {
            return Form.FENCE_GATE;
        }
        if (block instanceof FenceBlock || state.is(BlockTags.FENCES)) {
            return Form.FENCE;
        }
        if (block instanceof IronBarsBlock) {
            return Form.PANE;
        }
        if (block instanceof TrapDoorBlock) {
            return Form.TRAPDOOR;
        }
        if (block instanceof DoorBlock) {
            return Form.DOOR;
        }
        try {
            if (state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) && !state.hasBlockEntity()) {
                return Form.BLOCK;
            }
        } catch (RuntimeException e) {
            return null;
        }
        return null;
    }

    // QW11: Regel identisch zu com.simplelib.api.TabSort (SimpleLib sieht Common nicht in allen Loadern); CrossModGameTest prueft die Gleichheit.
    private static final List<String> FORM_SUFFIXES = List.of("_octet", "_stairs", "_slab", "_wall", "_fence_gate", "_fence",
            "_pane", "_trapdoor", "_door");
    /** Varianten-Woerter, Reihenfolge = Rang in der Spalte (Basis zuerst, dann geschliffen ... gemeisselt, Saeule). */
    private static final List<String> VARIANT_WORDS = List.of("planks", "block", "log", "stem", "wood", "hyphae", "stripped",
            "cobbled", "polished", "smooth", "cut", "bricks", "brick", "tiles", "tile", "cracked", "mossy", "chiseled", "pillar",
            "waxed", "exposed", "weathered", "oxidized");

    /** Material-Schluessel einer Id: Form-Endung und Varianten-Woerter weg ({@code mossy_stone_brick_wall} -> {@code stone}). */
    public static String materialKey(String path) {
        String p = path;
        for (String suffix : FORM_SUFFIXES) {
            if (p.endsWith(suffix) && p.length() > suffix.length()) {
                p = p.substring(0, p.length() - suffix.length());
                break;
            }
        }
        List<String> kept = new ArrayList<>();
        for (String word : p.split("_")) {
            if (!VARIANT_WORDS.contains(word)) {
                kept.add(word);
            }
        }
        if (kept.isEmpty()) {
            // "bricks", "brick_stairs": das Material ist das Wort selbst.
            return p.replace("bricks", "brick").replace("tiles", "tile");
        }
        return String.join("_", kept);
    }

    /** Rang in der Spalte: das spaeteste Varianten-Wort zaehlt ({@code mossy_stone_bricks} nach {@code stone_bricks}). */
    static int rank(String path) {
        int rank = -1;
        for (String word : path.split("_")) {
            rank = Math.max(rank, VARIANT_WORDS.indexOf(word));
        }
        return rank;
    }

    private static Entry entry(Item item, boolean vanilla) {
        List<BlockState> states = states(item);
        Form form = states.isEmpty() ? null : formOf(states.getFirst());
        String path = TcContext.id(item).getPath();
        return new Entry(item, states, form, materialKey(path), rank(path), vanilla);
    }

    private static Map<String, List<Entry>> vanillaByKey;

    /** Vanilla-Bloecke mit Form je Material-Schluessel (ohne gewachste Doppel), einmal je Lauf. */
    private static synchronized Map<String, List<Entry>> vanilla() {
        if (vanillaByKey == null) {
            Map<String, List<Entry>> out = new HashMap<>();
            for (Item item : BuiltInRegistries.ITEM) {
                Identifier id = TcContext.id(item);
                if (!id.getNamespace().equals("minecraft") || !(item instanceof BlockItem) || id.getPath().startsWith("waxed_")
                        || id.getPath().startsWith("infested_")) {
                    continue;
                }
                Entry entry = entry(item, true);
                if (entry.form() != null) {
                    out.computeIfAbsent(entry.key(), key -> new ArrayList<>()).add(entry);
                }
            }
            vanillaByKey = out;
        }
        return vanillaByKey;
    }

    // ------------------------------------------------------------------ Aufbau

    /** Alle Bereiche ({@link #namespaces()}) nebeneinander, Reihen hoechstens {@link #MAX_WIDTH} breit. */
    public static Gallery all() {
        return gallery(namespaces());
    }

    /** Die Bereiche der Namensraeume, gepackt. */
    public static Gallery gallery(List<String> namespaces) {
        TcCanvas canvas = new TcCanvas();
        List<Cell> cells = new ArrayList<>();
        int x = 0;
        int z = 0;
        int rowDepth = 0;
        for (String ns : namespaces) {
            Gallery area = area(ns);
            if (area.canvas().ops().isEmpty()) {
                continue;
            }
            int width = area.canvas().width();
            if (x > 0 && x + width > MAX_WIDTH) {
                z += rowDepth + AREA_GAP;
                x = 0;
                rowDepth = 0;
            }
            canvas.append(area.canvas(), x, z);
            for (Cell cell : area.cells()) {
                cells.add(cell.moved(x, z));
            }
            x += width + AREA_GAP;
            rowDepth = Math.max(rowDepth, area.canvas().depth());
        }
        return new Gallery(canvas, cells);
    }

    /** Der Bereich eines Mods: Kopfschild, Familien-Baender, weitere Bloecke, Items. */
    public static Gallery area(String ns) {
        TcCanvas c = new TcCanvas();
        List<Cell> cells = new ArrayList<>();
        Map<String, List<Entry>> families = new TreeMap<>();
        List<Entry> blocks = new ArrayList<>();
        List<Entry> items = new ArrayList<>();
        List<Item> own = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = TcContext.id(item);
            if (id.getNamespace().equals(ns) && item != Items.AIR && shown(id)) {
                own.add(item);
            }
        }
        own.sort(Comparator.comparing(item -> TcContext.id(item).getPath()));
        if (own.isEmpty()) {
            return new Gallery(c, cells);
        }
        Map<String, Integer> formsPerKey = new HashMap<>();
        List<Entry> entries = new ArrayList<>();
        for (Item item : own) {
            Entry entry = entry(item, false);
            entries.add(entry);
            if (entry.form() != null) {
                formsPerKey.merge(entry.key(), 1, Integer::sum);
            }
        }
        for (Entry entry : entries) {
            if (entry.form() == null) {
                (entry.states().isEmpty() ? items : blocks).add(entry);
                continue;
            }
            // Ein voller Block ohne Formteil, Variante oder Vanilla-Bezug ist keine Familie (Maschinen, Lichter ...).
            boolean family = entry.form() != Form.BLOCK || formsPerKey.get(entry.key()) > 1
                    || vanilla().containsKey(entry.key());
            if (family) {
                families.computeIfAbsent(entry.key(), key -> new ArrayList<>()).add(entry);
            } else {
                blocks.add(entry);
            }
        }
        families.forEach((key, list) -> {
            for (Entry entry : vanilla().getOrDefault(key, List.of())) {
                if (list.stream().noneMatch(member -> member.item() == entry.item())) {
                    list.add(entry);
                }
            }
            list.sort(Comparator.comparingInt(Entry::rank).thenComparing(entry -> TcContext.id(entry.item()).getPath()));
        });

        int familyCount = families.size();
        c.wallSign(0, 0, 1, TcText.bold(TcText.lit(modName(ns))), TcText.t("modblocks.families", "%s families", familyCount),
                TcText.t("modblocks.items", "%s items", own.size()));
        int z = 2;
        // Familien nach Farbe, dann in Baender von hoechstens MAX_WIDTH.
        List<Map.Entry<String, List<Entry>>> ordered = new ArrayList<>(families.entrySet());
        ordered.sort(Comparator.comparingDouble((Map.Entry<String, List<Entry>> family) -> colourOrder(family.getValue()))
                .thenComparing(family -> family.getKey()));
        int perBand = Math.max(1, (MAX_WIDTH + 1) / COLUMN_PITCH);
        for (int band = 0; band * perBand < ordered.size(); band++) {
            List<Map.Entry<String, List<Entry>>> columns = ordered.subList(band * perBand, Math.min(ordered.size(), (band + 1) * perBand));
            Map<Form, Integer> rows = new java.util.EnumMap<>(Form.class);
            for (Map.Entry<String, List<Entry>> family : columns) {
                for (Form form : Form.values()) {
                    long count = family.getValue().stream().filter(entry -> entry.form() == form).count();
                    rows.merge(form, (int) ((count + form.stack() - 1) / form.stack()), Math::max);
                }
            }
            Map<Form, Integer> rowZ = new java.util.EnumMap<>(Form.class);
            int at = z;
            for (Form form : Form.values()) {
                rowZ.put(form, at);
                at += rows.getOrDefault(form, 0);
            }
            for (int col = 0; col < columns.size(); col++) {
                String key = columns.get(col).getKey();
                int x = col * COLUMN_PITCH;
                Map<Form, Integer> index = new java.util.EnumMap<>(Form.class);
                Map<Long, Integer> heights = new HashMap<>();
                for (Entry entry : columns.get(col).getValue()) {
                    int i = index.merge(entry.form(), 1, Integer::sum) - 1;
                    int cz = rowZ.get(entry.form()) + i / entry.form().stack();
                    long cellKey = cz;
                    int y = heights.getOrDefault(cellKey, 0);
                    for (int s = 0; s < entry.states().size(); s++) {
                        c.place(x, y + s, cz, entry.states().get(s));
                    }
                    heights.put(cellKey, y + entry.height());
                    cells.add(new Cell(ns, band, key, entry.form(), entry.item(), entry.vanilla(), new BlockPos(x, y, cz)));
                }
            }
            z = at + 2;
        }

        // Weitere Bloecke und Items: Gruppen nach Namensmuster, je Gruppe ein Schild.
        z = groups(c, z, grouped(blocks), COLUMN_PITCH, 2, ModBlockGallery::placeEntry);
        groups(c, z, grouped(items), 1, 1, (canvas, entry, x, cz) ->
                canvas.frame(x, 0, cz, Direction.UP, new ItemStack(entry.item())));
        return new Gallery(c, cells);
    }

    @FunctionalInterface
    private interface Put {
        void put(TcCanvas canvas, Entry entry, int x, int z);
    }

    private static void placeEntry(TcCanvas canvas, Entry entry, int x, int z) {
        for (int s = 0; s < entry.states().size(); s++) {
            canvas.place(x, s, z, entry.states().get(s));
        }
    }

    /**
     * Legt Gruppen ab {@code z0} in Zeilen (hoechstens {@link #MAX_WIDTH}); je Gruppe Schild an einem Pfosten, darunter
     * {@link #GROUP_ROWS} Reihen. Liefert das naechste freie z.
     */
    private static int groups(TcCanvas c, int z0, Map<String, List<Entry>> groups, int pitchX, int pitchZ, Put put) {
        if (groups.isEmpty()) {
            return z0;
        }
        int depth = 2 + GROUP_ROWS * pitchZ;
        int x = 0;
        int z = z0;
        int maxColumns = Math.max(1, MAX_WIDTH / pitchX);
        for (Map.Entry<String, List<Entry>> group : groups.entrySet()) {
            List<Entry> list = group.getValue();
            for (int start = 0; start < list.size(); start += maxColumns * GROUP_ROWS) {
                List<Entry> part = list.subList(start, Math.min(list.size(), start + maxColumns * GROUP_ROWS));
                int columns = (part.size() + GROUP_ROWS - 1) / GROUP_ROWS;
                int width = Math.max(1, (columns - 1) * pitchX + 1);
                if (x > 0 && x + width > MAX_WIDTH) {
                    x = 0;
                    z += depth + 1;
                }
                c.wallSign(x, 0, z + 1, TcText.bold(TcText.lit(TestCentreSections.pretty(group.getKey()))),
                        TcText.lit(String.valueOf(list.size())));
                for (int i = 0; i < part.size(); i++) {
                    put.put(c, part.get(i), x + (i / GROUP_ROWS) * pitchX, z + 2 + (i % GROUP_ROWS) * pitchZ);
                }
                x += width + 2;
            }
        }
        return z + depth + 2;
    }

    /**
     * Gruppen nach Namensmuster: das letzte Wort der Id, wenn es noch ein zweites Item traegt ({@code iron_horseshoe},
     * {@code gold_horseshoe}), sonst das erste ({@code music_disc_x}), sonst "other". Gruppen nach Name, "other" zuletzt.
     */
    static Map<String, List<Entry>> grouped(List<Entry> entries) {
        Map<String, Integer> last = new HashMap<>();
        Map<String, Integer> first = new HashMap<>();
        for (Entry entry : entries) {
            String[] words = TcContext.id(entry.item()).getPath().split("_");
            last.merge(words[words.length - 1], 1, Integer::sum);
            first.merge(words[0], 1, Integer::sum);
        }
        Map<String, List<Entry>> out = new TreeMap<>();
        List<Entry> other = new ArrayList<>();
        for (Entry entry : entries) {
            String[] words = TcContext.id(entry.item()).getPath().split("_");
            String key = last.get(words[words.length - 1]) > 1 ? words[words.length - 1]
                    : first.get(words[0]) > 1 ? words[0] : null;
            if (key == null) {
                other.add(entry);
            } else {
                out.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
            }
        }
        Map<String, List<Entry>> ordered = new LinkedHashMap<>(out);
        if (!other.isEmpty()) {
            ordered.put("other", other);
        }
        return ordered;
    }

    /** "simpleblocks" -> "Simple Blocks". */
    public static String modName(String ns) {
        String rest = ns.startsWith("simple") ? ns.substring("simple".length()) : ns;
        String head = ns.startsWith("simple") ? "Simple " : "";
        return head + (rest.isEmpty() ? "" : rest.substring(0, 1).toUpperCase(Locale.ROOT) + rest.substring(1));
    }

    /**
     * Farb-Reihung einer Familie (Kartenfarbe ihres ersten vollen Blocks, sonst des ersten Eintrags): bunte nach Farbton
     * (0 bis 1), graue danach nach Helligkeit (hell zuerst).
     */
    static double colourOrder(List<Entry> family) {
        Entry rep = family.stream().filter(entry -> entry.form() == Form.BLOCK).findFirst().orElse(family.getFirst());
        int rgb;
        try {
            rgb = rep.states().getFirst().getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).col;
        } catch (RuntimeException e) {
            rgb = 0x808080;
        }
        double r = ((rgb >> 16) & 255) / 255.0;
        double g = ((rgb >> 8) & 255) / 255.0;
        double b = (rgb & 255) / 255.0;
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double delta = max - min;
        if (max == 0 || delta / max < 0.2) {
            return 2.0 - max; // grau: 1..2, hell zuerst
        }
        double hue;
        if (max == r) {
            hue = ((g - b) / delta) / 6.0;
        } else if (max == g) {
            hue = ((b - r) / delta + 2) / 6.0;
        } else {
            hue = ((r - g) / delta + 4) / 6.0;
        }
        return hue < 0 ? hue + 1 : hue;
    }

    /** Alle Familien-Items eines Bereichs, die genau einmal stehen muessen (fuer Tests). */
    public static Set<Item> items(String ns) {
        Set<Item> out = new java.util.LinkedHashSet<>();
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = TcContext.id(item);
            if (id.getNamespace().equals(ns) && item != Items.AIR && shown(id)) {
                out.add(item);
            }
        }
        return out;
    }
}
