package com.simplebuilding.dev.testcentre;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * TZ38 (Besitzer 12.11.: "Testzentrale neu ordnen", Plan {@code docs/ai/PLAN-A-TZ38-2026-11-12.md}; Spaltenplan TZ38b/TZ38c,
 * {@code docs/ai/PLAN-A-TZ38C-2026-11-12.md}): drei geordnete Bereiche vor den Teststationen. Jeder Bereich zeigt fuer sich
 * alles seiner Art (Doppel zwischen den Bereichen sind gewollt, innerhalb eines Bereichs steht jedes Item genau einmal):
 * <ol>
 *   <li>{@link #FAMILIES}: je Block-Variante eine Reihe vom Gang in die Tiefe im gemeinsamen Spaltenplan {@link Slot}
 *       (Kern Octet | Mauer | Stufe | Treppe | Block | rissig | gemeisselt | Saeule/Sonder, Zusatz Zaun | Zauntor | Tuer |
 *       Falltuer | Blech | Druckplatte | Knopf, dann "weitere"); eine leere Spalte zwischen Familien; graue Betonstufen als
 *       Linie zum gespiegelten Gegenband. Als letztes Band-Paar die Rohstoffe/Metalle in denselben Spalten (Metall: Gitter in
 *       der Mauer-, Rost in der rissig-Spalte, Block = geschnitten), ihr Anhang (Bauteile, Material/Erze, Material-Items,
 *       Ausruestung) laeuft dahinter weiter; fehlende Glieder bleiben leer.</li>
 *   <li>{@link #ITEMS}: Besatz-Staender (je Muster, je Material ein voller Satz), dann je Mod eine Linie in
 *       Wiki-Reihenfolge, setzbar = gesetzt, sonst Rahmen; dahinter die Mobs der Mod (ohne KI, unverwundbar, still).</li>
 *   <li>{@link #COMPACT}: alle Bloecke je Mod dicht beieinander, ein Feld Abstand zur naechsten Mod.</li>
 * </ol>
 * Wie die Tab-Waende kommen die Bereiche nach der Abdeckung: sie ordnen, die Stationen bleiben der Ort zum Ausprobieren.
 */
public final class OrderAreas {

    public static final String FAMILIES = "order_families";
    public static final String ITEMS = "order_items";
    public static final String COMPACT = "order_compact";
    /** Reihenfolge in der Welt. */
    public static final List<String> SECTIONS = List.of(FAMILIES, ITEMS, COMPACT);
    /** Breite des Inhalts (der Abschnitt mit Ausgabe-Knopf bleibt unter {@link TestCentreLayout#MAX_ROW_WIDTH}). */
    public static final int MAX_WIDTH = ModBlockGallery.MAX_WIDTH;
    /** Tag jedes Mobs, den die Bereiche spawnen ({@link TcOp.Mob}). */
    public static final String MOB_TAG = "sb_testcentre_mob";
    /** Gang zwischen zwei Band-Paaren. */
    static final int AISLE = 3;
    /** Wo der Inhalt nach Titel-Schild beginnt. */
    static final int START_X = 2;
    static final int START_Z = 3;

    /**
     * Mods in der Reihenfolge der Wiki-Seite ({@code modules/modules.json}: jede Ober-Mod, dann ihre Sub-Mods); geladene
     * Familien-Namensraeume, die hier fehlen, kommen hinten nach Id.
     */
    public static final List<String> WIKI_ORDER = List.of("simplebuilding", "simplemoney", "simpleriding", "simplefun",
            "simpletweaks", "simpledimensions", "simplesandwiches", "simplemaps", "simpletrims", "simplemobs", "simplequestionary",
            "simpleend", "simpleenddragonfight", "simplerespawn", "simplebeacons", "simpleovens", "simplecrucibles", "simplefoundry",
            "simplemobheads", "simpleshops", "simplewanderers", "simpleblocks", "simpleoctets", "simplehammers", "simplestorage",
            "simplemusic", "simplelights", "simplegadgets", "simpleearth", "simplechallenges", "simpleanimals", "simpledinos",
            "simpleores", "simplemachines", "simplearrows", "simpleresonance", "simpleenchanting", "simplepads",
            "simplebuildingtools", "simplechisels", "simplelayers", "simplevault", "simplequalityoflife", "simpleinterfaces",
            "simpleweather", "simplemovement", "simpletools", "simplefarming", "simplemobmanners", "simpleloot", "simplelib",
            "simplelivemap", "simpleaesthetics", "simplemodels", "simplevisuals", "simplesounds", "simplestats",
            "simpleoptimization", "simpleoptrender", "simpleoptmemory", "simpleoptserver");

    /** Ein Eintrag eines Bereichs (fuer Tests): Gruppe (Reihe, Metall, Mod), Spalte, Item, Platz (unterster Block/Rahmen). */
    public record Cell(String group, int column, Item item, BlockPos pos) {
    }

    /**
     * Ein Band des Familien-Bereichs (fuer Tests): Spalte 0 (Octet) liegt bei {@code frontZ}, jede weitere Spalte
     * {@code step} (+1 vorderes, -1 gespiegeltes Band) weiter; {@code lineZ} ist die Betonlinie; {@code columns} = Spalten
     * des Bands ({@link Slot#FIXED} bei Familien, {@link Slot#METAL} bei Metallen, dann die "weiteren").
     */
    public record Band(int x0, int x1, int frontZ, int step, int lineZ, List<Slot> columns) {
    }

    /** Ergebnis eines Bereichs. */
    public record Area(TcCanvas canvas, List<Cell> cells, List<Band> bands) {
    }

    private OrderAreas() {
    }

    // ------------------------------------------------------------------ gemeinsam

    /** Familien-Namensraeume mit ausstellbaren Items in Wiki-Reihenfolge. */
    public static List<String> namespaces() {
        List<String> present = ModBlockGallery.namespaces();
        List<String> out = new ArrayList<>();
        for (String ns : WIKI_ORDER) {
            if (present.contains(ns)) {
                out.add(ns);
            }
        }
        for (String ns : present) {
            if (!out.contains(ns)) {
                out.add(ns);
            }
        }
        return out;
    }

    /** Setzt ein Item (unten zuerst) oder legt es ohne Blockform im Rahmen flach auf den Boden. */
    static void put(TcCanvas c, Item item, int x, int z) {
        List<BlockState> states = ModBlockGallery.states(item);
        if (states.isEmpty()) {
            c.frame(x, 0, z, Direction.UP, new ItemStack(item));
            return;
        }
        for (int s = 0; s < states.size(); s++) {
            c.place(x, s, z, states.get(s));
        }
    }

    /** Ob ein Item den Block eines anderen Items setzt (Wandfackel-Paare u. a.) - steht dann ueber jenes. */
    static boolean placesOther(Item item) {
        return item instanceof BlockItem blockItem && TcContext.itemOf(blockItem.getBlock()) != item;
    }

    /** Alle Items eines Namensraums, die ein Bereich zeigt (Registerreihenfolge). */
    public static List<Item> items(String ns) {
        List<Item> out = new ArrayList<>();
        for (Item item : ModBlockGallery.items(ns)) {
            if (!placesOther(item) && !TcContext.isSpacer(item)) {
                out.add(item);
            }
        }
        return out;
    }

    private static List<String> tokens(String path) {
        return List.of(path.split("_"));
    }

    // ------------------------------------------------------------------ 1. Block-Familien und Metalle (ein Spaltenplan)

    /**
     * Gemeinsamer Spaltenplan aller Reihen des Familien-Bereichs (TZ38b, Besitzer 12.11. ~21:00: F1-F5 = A, Plan
     * {@code docs/ai/PLAN-A-TZ38C-2026-11-12.md}). Der Kern {@link #OCTET} ... {@link #PILLAR} ist fuer jede Reihe gleich
     * (Metall: Mauer = Gitter, Block = geschnitten, rissig = Rost), dahinter die Zusatzformen in der Folge des
     * Vanilla-Kreativ-Tabs ({@link #FENCE} ... {@link #BUTTON}). Normale Familien enden dort ({@link #FIXED}) und haengen nur
     * noch "weitere" an; Metall-Reihen laufen weiter: Bauteile, Material und Fundort, Material-Items, Ausruestung, dann
     * "weitere" ({@link #OTHER}).
     * <p>Muster (nur fuer Metall-Reihen): {@code {p}} Praefix der Stufe ({@code exposed_}, {@code waxed_} ...), {@code {m}}
     * Metall-Wort; {@code =} am Anfang: genau diese Id, bei {@link #STORAGE} nur Bloecke, sonst nur Nicht-Bloecke.
     */
    public enum Slot {
        // Kern - alle Reihen.
        OCTET("{p}cut_{m}_octet", "{p}cut_{m}_block_octet"),
        WALL("{p}{m}_bars", "{p}cut_{m}_wall", "{p}{m}_wall"),
        SLAB("{p}cut_{m}_slab", "{p}{m}_slab"),
        STAIRS("{p}cut_{m}_stairs", "{p}{m}_stairs"),
        BLOCK("{p}cut_{m}", "{p}cut_{m}_block"),
        CRACKED("{p}rusty_{m}", "{p}rusted_{m}", "{p}{m}_rust", "{p}cracked_{m}", "{p}cracked_{m}_block"),
        CHISELED("{p}chiseled_{m}", "{p}chiseled_{m}_block"),
        PILLAR("{p}{m}_pillar"),
        // Zusatz - alle Reihen (Vanilla-Folge Zaun, Zauntor, Tuer, Falltuer, Druckplatte, Knopf; Blech vor der Druckplatte).
        FENCE("{p}{m}_fence"), FENCE_GATE("{p}{m}_fence_gate"), DOOR("{p}{m}_door"), TRAPDOOR("{p}{m}_trapdoor"),
        SHEET("{p}{m}_sheet", "{p}{m}_plate"), PLATE("{p}{m}_pressure_plate"), BUTTON("{p}{m}_button"),
        // Nur Metalle: weitere Bauteile.
        TILES("{p}{m}_tiles"), GRATE("{p}{m}_grate"), POLE("{p}{m}_pole", "{p}{m}_rod"), CHAIN("{p}{m}_chain"),
        LANTERN("{p}{m}_lantern"), LAMP("{p}{m}_bulb", "{p}{m}_lamp"), CHEST("{p}{m}_chest"),
        SPECIAL("{p}{m}_golem_statue", "{m}_mirror"), HOPPER("{m}_hopper"), PIPE("{m}_pipe"),
        // Nur Metalle: Material und Fundort.
        STORAGE("{p}{m}_block", "={p}{m}"), STORAGE_OCTET("{p}{m}_block_octet", "{p}{m}_octet"),
        RAW_BLOCK("raw_{m}_block", "{m}_scrap_block", "layered_raw_{m}"), ORE("{m}_ore"), DEEPSLATE_ORE("deepslate_{m}_ore"),
        NETHER_ORE("nether_{m}_ore"), END_ORE("end_{m}_ore"), LOOSE_ORE("{m}_bearing_gravel", "{m}_bearing_sand"),
        // Nur Metalle: Material-Items (Rohstoff-Kette Roh -> Barren -> Nugget).
        RAW("raw_{m}"), INGOT("{m}_ingot", "={m}", "{m}_gem", "{m}_shard", "{m}_crystal", "{m}_lazuli", "{m}_dust"),
        NUGGET("{m}_nugget"), SCRAP("{m}_scrap"), CORE("{m}_core"),
        // Nur Metalle: Ausruestung (Waffen zusammen, keine Spachtel-Spalte: Spachteln sind seit 09/2026 Meissel).
        HORSE_ARMOR("{m}_horse_armor"), NAUTILUS_ARMOR("{m}_nautilus_armor"), ANIMAL_ARMOR("trex_armor_{m}", "{m}_wolf_armor"),
        HORSESHOE("{m}_horseshoe"), SWORD("{m}_sword"), SPEAR("{m}_spear"), PICKAXE("{m}_pickaxe"), AXE("{m}_axe"),
        SHOVEL("{m}_shovel"), HOE("{m}_hoe"), CHISEL("{m}_chisel"), SLEDGEHAMMER("{m}_sledgehammer"),
        BUILDING_WAND("{m}_building_wand"), HELMET("{m}_helmet"), CHESTPLATE("{m}_chestplate"), LEGGINGS("{m}_leggings"),
        BOOTS("{m}_boots"),
        /** "weitere": alles Uebrige einer Reihe, so viele Spalten, wie die laengste Reihe des Bands braucht. */
        OTHER;

        /** Kern + Zusatz (Feld 1-15): so weit reichen die festen Spalten einer normalen Familien-Reihe. */
        public static final int FIXED = TILES.ordinal();
        /** Feste Spalten einer Metall-Reihe (alles vor {@link #OTHER}). */
        public static final int METAL = OTHER.ordinal();

        final List<String> patterns;

        Slot(String... patterns) {
            this.patterns = List.of(patterns);
        }
    }

    /** Sonderblock einer Reihe in der Saeulen-Spalte (Besitzer: Tiefenschiefer-Ziegel, Tiefenschiefer-Fliesen). */
    static final Map<String, String> SPECIAL = Map.of(
            "deepslate_brick", "minecraft:reinforced_deepslate",
            "deepslate_tile", "simpleblocks:sculk_tiles");

    private static final List<String> ROW_SUFFIXES = List.of("_octet", "_stairs", "_slab", "_wall", "_fence_gate", "_fence",
            "_pressure_plate", "_button", "_pane", "_bars", "_trapdoor", "_door", "_sheet", "_plate");

    /**
     * Reihen-Schluessel: Id ohne Form-Endung, ohne {@code cracked}/{@code chiseled} (eigene Spalten), ohne Endung
     * {@code _block}/{@code _planks}/{@code _pillar}; {@code bricks -> brick}, {@code tiles -> tile}
     * ({@code cracked_deepslate_bricks}, {@code deepslate_brick_wall}, {@code deepslate_bricks_octet} -> {@code deepslate_brick};
     * {@code oak_sheet} -> {@code oak}).
     */
    public static String rowKey(String path) {
        String p = path;
        for (String suffix : ROW_SUFFIXES) {
            if (p.endsWith(suffix) && p.length() > suffix.length()) {
                p = p.substring(0, p.length() - suffix.length());
                break;
            }
        }
        List<String> kept = new ArrayList<>();
        for (String word : p.split("_")) {
            if (word.equals("cracked") || word.equals("chiseled")) {
                continue;
            }
            kept.add(word.equals("bricks") ? "brick" : word.equals("tiles") ? "tile" : word);
        }
        String key = String.join("_", kept);
        for (String tail : List.of("_block", "_planks", "_pillar")) {
            if (key.endsWith(tail) && key.length() > tail.length()) {
                return key.substring(0, key.length() - tail.length());
            }
        }
        return key;
    }

    /** Duenne Platte/Blech ({@code oak_sheet}, {@code silver_sheet}, {@code lead_plate}), keine Druckplatte. */
    private static boolean isSheet(String path) {
        return path.endsWith("_sheet") || path.endsWith("_plate") && !path.endsWith("_pressure_plate");
    }

    /** Spalte eines Items in seiner Reihe, null = keine Familien-Form (Maschinen, Lichter, Items ...). */
    static Slot slot(Item item, List<BlockState> states) {
        String path = TcContext.id(item).getPath();
        if (item instanceof BlockItem && isSheet(path)) {
            return Slot.SHEET;
        }
        if (!states.isEmpty()) {
            BlockState state = states.getFirst();
            Block block = state.getBlock();
            // Auch Druckplatten mit eigener Klasse (Simple Pads: Kupfer-Druckplatte ist ein PadBlock) zaehlen nach Namen.
            if (block instanceof BasePressurePlateBlock || path.endsWith("_pressure_plate")) {
                return Slot.PLATE;
            }
            if (block instanceof ButtonBlock || path.endsWith("_button")) {
                return Slot.BUTTON;
            }
            ModBlockGallery.Form form = ModBlockGallery.formOf(state);
            if (form == null) {
                return null;
            }
            return switch (form) {
                case OCTET -> Slot.OCTET;
                case SLAB -> Slot.SLAB;
                case STAIRS -> Slot.STAIRS;
                // Scheibe und Gitter sind die Mauer-Form von Glas und Metall (TZ38b Ae3); eine echte Mauer geht vor.
                case WALL, PANE -> Slot.WALL;
                case FENCE -> Slot.FENCE;
                case FENCE_GATE -> Slot.FENCE_GATE;
                case TRAPDOOR -> Slot.TRAPDOOR;
                case DOOR -> Slot.DOOR;
                case BLOCK -> blockSlot(path);
            };
        }
        // Im Rahmen (schmelzendes Eis, Pads ...): die Form steht im Namen.
        if (!(item instanceof BlockItem)) {
            return null;
        }
        Map<String, Slot> bySuffix = new LinkedHashMap<>();
        bySuffix.put("_octet", Slot.OCTET);
        bySuffix.put("_stairs", Slot.STAIRS);
        bySuffix.put("_slab", Slot.SLAB);
        bySuffix.put("_wall", Slot.WALL);
        bySuffix.put("_pane", Slot.WALL);
        bySuffix.put("_bars", Slot.WALL);
        bySuffix.put("_fence_gate", Slot.FENCE_GATE);
        bySuffix.put("_fence", Slot.FENCE);
        bySuffix.put("_pressure_plate", Slot.PLATE);
        bySuffix.put("_button", Slot.BUTTON);
        bySuffix.put("_trapdoor", Slot.TRAPDOOR);
        bySuffix.put("_door", Slot.DOOR);
        for (Map.Entry<String, Slot> entry : bySuffix.entrySet()) {
            if (path.endsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        BlockState state = ((BlockItem) item).getBlock().defaultBlockState();
        try {
            return state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) && !state.hasBlockEntity()
                    ? blockSlot(path) : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Spalte eines Items im Familien-Bereich nach seiner Form (ohne Sonderblock-Zuordnung), null = keine Familien-Form. */
    public static Slot slotOf(Item item) {
        return slot(item, ModBlockGallery.states(item));
    }

    private static Slot blockSlot(String path) {
        List<String> words = tokens(path);
        if (words.contains("cracked") || words.contains("rusty") || words.contains("rusted") || path.endsWith("_rust")) {
            return Slot.CRACKED;
        }
        if (words.contains("chiseled")) {
            return Slot.CHISELED;
        }
        return path.endsWith("_pillar") ? Slot.PILLAR : Slot.BLOCK;
    }

    /** Ob ein gesetztes Item eine Scheibe/ein Gitter ist (steht in der Mauer-Spalte nur, wenn die Reihe keine Mauer hat). */
    private static boolean paneLike(Item item, List<BlockState> states) {
        if (states.isEmpty()) {
            String path = TcContext.id(item).getPath();
            return path.endsWith("_pane") || path.endsWith("_bars");
        }
        return ModBlockGallery.formOf(states.getFirst()) == ModBlockGallery.Form.PANE;
    }

    private record Member(Item item, Slot slot, String row, String family, boolean family_ns, boolean weak) {
    }

    private static Member member(Item item) {
        List<BlockState> states = ModBlockGallery.states(item);
        Slot slot = slot(item, states);
        if (slot == null) {
            return null;
        }
        Identifier id = TcContext.id(item);
        String path = id.getPath();
        // Platten zaehlen zur Familie ihres Grundblocks (oak_sheet -> oak).
        String familyPath = isSheet(path) ? path.substring(0, path.lastIndexOf('_')) : path;
        return new Member(item, slot, rowKey(path), ModBlockGallery.materialKey(familyPath), TcContext.isFamily(id),
                slot == Slot.WALL && paneLike(item, states));
    }

    /**
     * Alle Familien-Reihen vor Abzug der Metall-Reihen: Reihen-Schluessel -> Glieder (Familien-Mod-Items, Vanilla als Bezug
     * in vorhandenen Reihen, Sonderbloecke in der Saeulen-Spalte); nur Reihen mit mindestens einem Familien-Mod-Glied.
     */
    static Map<String, List<Member>> rawRows() {
        Map<String, List<Member>> rows = new LinkedHashMap<>();
        Map<String, Integer> familySize = new HashMap<>();
        List<Member> vanilla = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = TcContext.id(item);
            if (item == Items.AIR || TcContext.isSpacer(item) || placesOther(item)) {
                continue;
            }
            boolean family = TcContext.isFamily(id) && ModBlockGallery.shown(id);
            boolean mc = id.getNamespace().equals("minecraft") && !id.getPath().startsWith("waxed_") && !id.getPath().startsWith("infested_");
            if (!family && !mc) {
                continue;
            }
            Member m = member(item);
            if (m == null) {
                continue;
            }
            familySize.merge(m.family(), 1, Integer::sum);
            if (family) {
                rows.computeIfAbsent(m.row(), key -> new ArrayList<>()).add(m);
            } else {
                vanilla.add(m);
            }
        }
        // Eine Reihe zaehlt, wenn ihre Familie (mit Vanilla) mindestens zwei Glieder hat - ein einzelner Vollblock ist keine Familie.
        rows.entrySet().removeIf(entry -> familySize.getOrDefault(entry.getValue().getFirst().family(), 0) < 2);
        for (Member m : vanilla) {
            List<Member> row = rows.get(m.row());
            if (row != null) {
                row.add(m);
            }
        }
        // Sonderbloecke (Besitzer-Beispiele) wandern in die Saeulen-Spalte ihrer Reihe.
        SPECIAL.forEach((row, special) -> {
            List<Member> target = rows.get(row);
            Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(special)).orElse(Items.AIR);
            if (target == null || item == Items.AIR) {
                return;
            }
            for (List<Member> other : rows.values()) {
                other.removeIf(m -> m.item() == item);
            }
            Identifier id = TcContext.id(item);
            target.add(new Member(item, Slot.PILLAR, row, target.getFirst().family(), TcContext.isFamily(id), false));
        });
        rows.values().removeIf(row -> row.stream().noneMatch(Member::family_ns));
        return rows;
    }

    /**
     * Die Reihen des Familien-Bereichs ohne die Items der Metall-Reihen (TZ38b Ae6: keine Doppel), je Familie gruppiert
     * (Familien nach Farbe, Reihen nach Variante).
     */
    static List<Map<String, List<Member>>> familyRows(Map<String, List<Member>> raw, Set<Item> claimed) {
        Map<String, List<Member>> rows = new LinkedHashMap<>();
        raw.forEach((key, members) -> {
            List<Member> left = new ArrayList<>();
            for (Member m : members) {
                if (!claimed.contains(m.item())) {
                    left.add(m);
                }
            }
            if (left.stream().anyMatch(Member::family_ns)) {
                rows.put(key, left);
            }
        });
        // Familien gruppieren.
        Map<String, Map<String, List<Member>>> families = new LinkedHashMap<>();
        rows.forEach((row, members) -> families.computeIfAbsent(members.getFirst().family(), key -> new java.util.TreeMap<>(
                Comparator.comparing(OrderAreas::rowOrder))).put(row, members));
        List<Map.Entry<String, Map<String, List<Member>>>> ordered = new ArrayList<>(families.entrySet());
        ordered.sort(Comparator.comparingDouble((Map.Entry<String, Map<String, List<Member>>> f) -> colour(f.getValue()))
                .thenComparing(Map.Entry::getKey));
        List<Map<String, List<Member>>> out = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<Member>>> family : ordered) {
            Map<String, List<Member>> sorted = new LinkedHashMap<>(family.getValue());
            sorted.values().forEach(list -> list.sort(Comparator.comparing((Member m) -> !m.family_ns())
                    .thenComparing(m -> TcContext.id(m.item()).toString())));
            out.add(sorted);
        }
        return out;
    }

    /** Reihen einer Familie: Grundform, dann Varianten nach Rang; bemooste direkt hinter ihrer Grundform. */
    private static String rowOrder(String row) {
        boolean mossy = tokens(row).contains("mossy");
        String base = mossy ? row.replace("mossy_", "") : row;
        return String.format(java.util.Locale.ROOT, "%03d|%s|%d|%s", ModBlockGallery.rank(base) + 1, base, mossy ? 1 : 0, row);
    }

    private static double colour(Map<String, List<Member>> family) {
        for (List<Member> row : family.values()) {
            for (Member m : row) {
                if (m.slot() == Slot.BLOCK) {
                    List<BlockState> states = ModBlockGallery.states(m.item());
                    if (!states.isEmpty()) {
                        return hue(states.getFirst());
                    }
                }
            }
        }
        return 3.0;
    }

    /** Wie die Galerie: bunte nach Farbton (0..1), graue nach Helligkeit (1..2, hell zuerst). */
    private static double hue(BlockState state) {
        int rgb;
        try {
            rgb = state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).col;
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
            return 2.0 - max;
        }
        double hue = max == r ? ((g - b) / delta) / 6.0 : max == g ? ((b - r) / delta + 2) / 6.0 : ((r - g) / delta + 4) / 6.0;
        return hue < 0 ? hue + 1 : hue;
    }

    /**
     * Graue Betonstufe der Trennlinie: Vanilla-Betonstufe (26.3: {@code minecraft:gray_concrete_slab}), sonst das Beton-Achtel
     * mit voller unterer Lage (Simple Octets), sonst grauer Beton. Per Id, damit es auf jeder Linie kompiliert.
     */
    public static BlockState lineState() {
        Block slab = BuiltInRegistries.BLOCK.getOptional(Identifier.withDefaultNamespace("gray_concrete_slab")).orElse(null);
        if (slab != null) {
            return slab.defaultBlockState();
        }
        Block octet = BuiltInRegistries.BLOCK.getOptional(Identifier.fromNamespaceAndPath("simpleoctets", "gray_concrete_octet"))
                .orElse(null);
        if (octet == null) {
            return BuiltInRegistries.BLOCK.getOptional(Identifier.withDefaultNamespace("gray_concrete")).orElse(Blocks.STONE)
                    .defaultBlockState();
        }
        BlockState state = octet.defaultBlockState();
        for (String name : List.of("o000", "o100", "o001", "o101")) {
            if (octet.getStateDefinition().getProperty(name) instanceof BooleanProperty cell) {
                state = state.setValue(cell, true);
            }
        }
        return state;
    }

    /** Eine Reihe im Band: feste Spalten, danach die weiteren in Reihenfolge; Schild am Gang nur bei Metall-Reihen. */
    private record RowPlan(String group, Map<Slot, Item> fixed, List<Item> others, Component label) {
    }

    /** Familien-Reihe: je Kern-/Zusatzspalte das erste Glied (echte Mauer vor Scheibe), Ueberzaehlige nach Form in "weitere". */
    private static RowPlan familyRow(String key, List<Member> members) {
        List<Member> ordered = new ArrayList<>(members);
        ordered.sort(Comparator.comparing(Member::weak));
        Map<Slot, Item> fixed = new EnumMap<>(Slot.class);
        List<Member> extra = new ArrayList<>();
        for (Member m : ordered) {
            if (m.slot().ordinal() < Slot.FIXED && !fixed.containsKey(m.slot())) {
                fixed.put(m.slot(), m.item());
            } else {
                extra.add(m);
            }
        }
        extra.sort(Comparator.comparingInt(m -> m.slot().ordinal()));
        List<Item> others = new ArrayList<>();
        extra.forEach(m -> others.add(m.item()));
        return new RowPlan(key, fixed, others, null);
    }

    /** Familien auf Baender verteilen: hoechstens {@link #MAX_WIDTH} breit, eine leere Spalte zwischen zwei Familien. */
    private static List<List<List<RowPlan>>> bands(List<List<RowPlan>> families) {
        List<List<List<RowPlan>>> perBand = new ArrayList<>();
        List<List<RowPlan>> current = new ArrayList<>();
        int x = START_X;
        for (List<RowPlan> family : families) {
            int width = family.size();
            if (!current.isEmpty() && x + width > MAX_WIDTH) {
                perBand.add(current);
                current = new ArrayList<>();
                x = START_X;
            }
            current.add(family);
            x += width + 1;
        }
        if (!current.isEmpty()) {
            perBand.add(current);
        }
        return perBand;
    }

    /**
     * Baender paarweise um eine Linie grauer Betonstufen, ab {@code z0}: das vordere Band vom Gang zur Linie (+z), das
     * Gegenband gespiegelt. Jedes Band ist {@code fixed} feste Spalten plus so viele "weitere" tief, wie seine laengste Reihe
     * braucht (Vorder- und Gegenband unabhaengig). Liefert das z nach dem Gang hinter dem letzten Paar.
     */
    private static int pairs(TcCanvas c, List<Cell> cells, List<Band> bands, List<List<List<RowPlan>>> perBand, int fixed, int z0) {
        BlockState line = lineState();
        for (int pair = 0; pair * 2 < perBand.size(); pair++) {
            List<List<List<RowPlan>>> both = perBand.subList(pair * 2, Math.min(perBand.size(), pair * 2 + 2));
            List<List<Slot>> columns = new ArrayList<>();
            for (List<List<RowPlan>> band : both) {
                int others = 0;
                for (List<RowPlan> family : band) {
                    for (RowPlan row : family) {
                        others = Math.max(others, row.others().size());
                    }
                }
                List<Slot> list = new ArrayList<>(List.of(Slot.values()).subList(0, fixed));
                for (int i = 0; i < others; i++) {
                    list.add(Slot.OTHER);
                }
                columns.add(List.copyOf(list));
            }
            int lineZ = z0 + columns.getFirst().size();
            int maxX = START_X;
            for (int side = 0; side < both.size(); side++) {
                int step = side == 0 ? 1 : -1;
                int frontZ = side == 0 ? z0 : lineZ + columns.get(side).size();
                int bx = START_X;
                for (List<RowPlan> family : both.get(side)) {
                    for (RowPlan row : family) {
                        for (Map.Entry<Slot, Item> entry : row.fixed().entrySet()) {
                            int col = entry.getKey().ordinal();
                            put(c, entry.getValue(), bx, frontZ + step * col);
                            cells.add(new Cell(row.group(), col, entry.getValue(), new BlockPos(bx, 0, frontZ + step * col)));
                        }
                        for (int i = 0; i < row.others().size(); i++) {
                            int col = fixed + i;
                            put(c, row.others().get(i), bx, frontZ + step * col);
                            cells.add(new Cell(row.group(), col, row.others().get(i), new BlockPos(bx, 0, frontZ + step * col)));
                        }
                        if (row.label() != null) {
                            // Am Gang vor der Reihe, am Octet-Feld haengend (Zinn/Nickel/Silber sehen sich aehnlich).
                            c.sign(bx, 0, frontZ - step, step > 0 ? Direction.NORTH : Direction.SOUTH, row.label());
                        }
                        bx++;
                    }
                    bx++; // leere Spalte zwischen Familien
                }
                bands.add(new Band(START_X, bx - 2, frontZ, step, lineZ, columns.get(side)));
                maxX = Math.max(maxX, bx - 2);
            }
            for (int lx = START_X; lx <= maxX; lx++) {
                c.place(lx, 0, lineZ, line);
            }
            z0 = lineZ + (both.size() > 1 ? columns.get(1).size() : 0) + 1 + AISLE;
        }
        return z0;
    }

    /**
     * Bereich 1: zuerst die Familien-Band-Paare (Reihe vom Gang zur Betonlinie: Kern, Zusatz, weitere), dahinter als letztes
     * Band-Paar die Metalle im selben Spaltenplan (TZ38b Ae2/F5 = A: vorn Kupfer ... Nihilith, gegenueber gespiegelt die
     * Mod-Erze und Mineralien), ihr Anhang laeuft hinter Feld 15 weiter.
     */
    public static Area families() {
        TcCanvas c = new TcCanvas();
        List<Cell> cells = new ArrayList<>();
        List<Band> bands = new ArrayList<>();
        Map<String, List<Member>> raw = rawRows();
        Map<Item, String> rowOf = new HashMap<>();
        raw.forEach((key, members) -> members.forEach(m -> rowOf.putIfAbsent(m.item(), key)));
        List<MetalRow> metals = metalRows(rowOf);
        Set<Item> claimed = new HashSet<>();
        for (MetalRow row : metals) {
            claimed.addAll(row.slots().values());
            claimed.addAll(row.others());
        }
        List<Map<String, List<Member>>> families = familyRows(raw, claimed);
        // E3: was eine eigene Stein-Familie haette, aber dort nicht mehr steht, faellt nicht heraus - es kommt zu den weiteren.
        Set<Item> inFamilies = new HashSet<>();
        families.forEach(family -> family.values().forEach(members -> members.forEach(m -> inFamilies.add(m.item()))));
        List<List<RowPlan>> familyPlans = new ArrayList<>();
        int rowCount = 0;
        for (Map<String, List<Member>> family : families) {
            List<RowPlan> plans = new ArrayList<>();
            family.forEach((key, members) -> plans.add(familyRow(key, members)));
            rowCount += plans.size();
            familyPlans.add(plans);
        }
        int z = pairs(c, cells, bands, bands(familyPlans), Slot.FIXED, START_Z);
        // Metalle: Stufen eines Metalls (Kupfer, angelaufen ..., gewachst ...) ohne Luecke, sonst eine leere Spalte dazwischen.
        List<List<RowPlan>> front = new ArrayList<>();
        List<List<RowPlan>> back = new ArrayList<>();
        String lastFamily = null;
        for (MetalRow row : metals) {
            List<Item> others = new ArrayList<>(row.others());
            for (Item item : row.deferred()) {
                if (!inFamilies.contains(item)) {
                    others.add(item);
                }
            }
            Item label = row.slots().getOrDefault(Slot.INGOT, row.slots().getOrDefault(Slot.STORAGE, row.slots().values().iterator().next()));
            RowPlan plan = new RowPlan(row.metal().key(), row.slots(), others, TcText.bold(new ItemStack(label).getHoverName()));
            List<List<RowPlan>> side = row.metal().group() == 0 ? front : back;
            if (side.isEmpty() || !row.metal().family().equals(lastFamily)) {
                side.add(new ArrayList<>());
            }
            side.getLast().add(plan);
            lastFamily = row.metal().family();
        }
        if (!metals.isEmpty()) {
            c.wallSign(0, 0, z, TcText.bold(TcText.t("order.metals", "Raw materials & metals")),
                    TcText.t("order.metals.sub", "same columns, extras behind"),
                    TcText.t("order.metals.empty", "empty field = missing"));
            List<List<List<RowPlan>>> metalBands = new ArrayList<>(bands(front));
            metalBands.addAll(bands(back));
            pairs(c, cells, bands, metalBands, Slot.METAL, z);
        }
        c.wallSign(0, 0, 1, TcText.bold(TcText.t("order.families", "Block families")),
                TcText.t("order.families.sub", "%s families, %s rows", families.size(), rowCount),
                TcText.t("order.families.cols", "octet > wall > slab > stairs"),
                TcText.t("order.families.cols2", "block > cracked > chiseled > pillar"));
        return new Area(c, cells, bands);
    }

    // ------------------------------------------------------------------ Metall-Reihen

    /**
     * Ein Rohstoff: Anzeige-Schluessel, Stufen-Praefix ({@code exposed_}, {@code waxed_} ...), Metall-Woerter (erstes = Name,
     * weitere = Aliasse wie {@code golden}), Gruppe (0 Vanilla/SimpleBuilding vorn, 1 Mod-Erze gegenueber, 2 Mineralien), feste
     * Extras je Spalte (Vanilla-Ids ohne Metall-Wort).
     */
    public record Metal(String key, String prefix, List<String> words, int group, Map<Slot, List<String>> extra) {
        Metal(String key, int group, String... words) {
            this(key, "", List.of(words), group, Map.of());
        }

        List<String> seq(String word) {
            List<String> out = new ArrayList<>(prefix.isEmpty() ? List.of() : tokens(prefix.substring(0, prefix.length() - 1)));
            out.addAll(tokens(word));
            return out;
        }

        /** Reihen-Schluessel, die ganz zu dieser Metall-Reihe gehoeren ({@code {p}{m}}, {@code {p}cut_{m}}). */
        Set<String> ownRows() {
            Set<String> out = new HashSet<>();
            for (String word : words) {
                out.add(prefix + word);
                out.add(prefix + "cut_" + word);
            }
            return out;
        }

        /** Familie im Band: alle Stufen eines Metalls stehen ohne Luecke nebeneinander. */
        public String family() {
            return words.getFirst();
        }
    }

    private static Metal copperStage(String prefix) {
        return new Metal(prefix + "copper", prefix, List.of("copper"), 0,
                Map.of(Slot.POLE, List.of("minecraft:" + prefix + "lightning_rod")));
    }

    /**
     * Reihenfolge des Besitzers: Vanilla/SB-Metalle bis Astralit/Nihilith (Kupfer mit Anlauf- und Wachs-Stufen in der Folge des
     * Vanilla-Kreativ-Tabs), gegenueber die Mod-Erze, danach die Mineralien.
     */
    public static final List<Metal> METAL_LIST = List.of(
            copperStage(""), copperStage("exposed_"), copperStage("weathered_"), copperStage("oxidized_"),
            copperStage("waxed_"), copperStage("waxed_exposed_"), copperStage("waxed_weathered_"), copperStage("waxed_oxidized_"),
            new Metal("iron", "", List.of("iron"), 0, Map.of(Slot.PLATE, List.of("minecraft:heavy_weighted_pressure_plate"),
                    Slot.LANTERN, List.of("minecraft:lantern"), Slot.HOPPER, List.of("minecraft:hopper"), Slot.CHAIN, List.of("minecraft:chain"))),
            new Metal("gold", "", List.of("gold", "golden"), 0, Map.of(Slot.PLATE, List.of("minecraft:light_weighted_pressure_plate"))),
            new Metal("rose_gold", 0, "rose_gold"),
            new Metal("rose_bronze", 0, "rose_bronze"),
            new Metal("diamond", 0, "diamond"),
            new Metal("emerald", 0, "emerald"),
            new Metal("netherite", "", List.of("netherite"), 0, Map.of(Slot.NETHER_ORE, List.of("minecraft:ancient_debris"))),
            new Metal("enderite", 0, "enderite"),
            new Metal("dimensional", 0, "dimensional"),
            new Metal("terranite", 0, "terranite"),
            new Metal("zenithite", 0, "zenithite"),
            new Metal("infernite", 0, "infernite"),
            new Metal("rift", 0, "rift"),
            new Metal("sage", 0, "sage"),
            new Metal("astralit", 0, "astralit"),
            new Metal("nihilith", 0, "nihilith"),
            new Metal("silver", 1, "silver"),
            new Metal("lead", 1, "lead"),
            new Metal("zinc", 1, "zinc"),
            new Metal("nickel", 1, "nickel"),
            new Metal("tin", 1, "tin"),
            new Metal("brass", 1, "brass"),
            new Metal("bronze", 1, "bronze"),
            new Metal("stainless_steel", 1, "stainless_steel"),
            new Metal("redstone", 2, "redstone"),
            new Metal("lapis", 2, "lapis"),
            new Metal("quartz", 2, "quartz"),
            new Metal("coal", 2, "coal"),
            new Metal("amethyst", 2, "amethyst"));

    /** Woerter, die ein Item einem anderen Thema zuordnen (Farben, Schach, Lichtscheiben, Ender-/Astral-/Nihil-Bloecke). */
    private static final Set<String> FOREIGN = Set.of("white", "orange", "magenta", "light", "yellow", "lime", "pink", "gray",
            "cyan", "purple", "blue", "brown", "green", "red", "black", "checker", "chess", "ender", "astral", "nihil");

    /** Ob die Woerter {@code seq} in {@code words} in dieser Reihenfolge vorkommen. */
    private static boolean subsequence(List<String> seq, List<String> words) {
        int i = 0;
        for (String word : words) {
            if (i < seq.size() && seq.get(i).equals(word)) {
                i++;
            }
        }
        return i == seq.size();
    }

    /** Das Metall einer Id: das mit der laengsten passenden Wortfolge, bei Gleichstand das fruehere; sonst null. */
    static Metal metalOf(String path) {
        List<String> words = tokens(path);
        for (String word : words) {
            if (FOREIGN.contains(word)) {
                return null;
            }
        }
        Metal best = null;
        int bestLength = 0;
        for (Metal metal : METAL_LIST) {
            for (String alias : metal.words()) {
                List<String> seq = metal.seq(alias);
                if (seq.size() > bestLength && subsequence(seq, words)) {
                    best = metal;
                    bestLength = seq.size();
                }
            }
        }
        return best;
    }

    /**
     * Eine Metall-Reihe: feste Spalten bis {@link Slot#METAL}, weitere Objekte, und zurueckgestellte Items, die in einer
     * eigenen Stein-Familie stehen sollen (E3; fehlen sie dort, kommen sie doch zu den weiteren).
     */
    record MetalRow(Metal metal, Map<Slot, Item> slots, List<Item> others, List<Item> deferred) {
    }

    /** Kern- und Zusatzspalten nehmen nur Items ihrer Familienform (E1); Saeule/Sonder und die Metall-Spalten jedes. */
    private static boolean fits(Item item, Slot slot) {
        return slot.ordinal() >= Slot.FIXED || slot == Slot.PILLAR || slotOf(item) == slot;
    }

    /**
     * Die Metall-Reihen in {@link #METAL_LIST}-Folge: Kandidaten = Vanilla und Familien-Items mit dem Metall-Wort (Vanilla
     * zuerst), je Spalte das erste passende Muster; uebrige Items in die weiteren, ausser sie stehen in einer anderen
     * Familien-Reihe ({@code rowOf}) als {@code {p}{m}}/{@code {p}cut_{m}} (glatter Quarz, Quarzziegel ...).
     */
    static List<MetalRow> metalRows(Map<Item, String> rowOf) {
        Map<Metal, List<Item>> candidates = new LinkedHashMap<>();
        List<String> order = new ArrayList<>(List.of("minecraft"));
        order.addAll(namespaces());
        List<Item> all = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = TcContext.id(item);
            if (item == Items.AIR || item == Items.LEAD || TcContext.isSpacer(item) || placesOther(item) || !order.contains(id.getNamespace())
                    || !id.getNamespace().equals("minecraft") && !ModBlockGallery.shown(id)) {
                continue;
            }
            all.add(item);
        }
        all.sort(Comparator.comparingInt((Item item) -> order.indexOf(TcContext.id(item).getNamespace())));
        for (Item item : all) {
            Metal metal = metalOf(TcContext.id(item).getPath());
            if (metal != null) {
                candidates.computeIfAbsent(metal, key -> new ArrayList<>()).add(item);
            }
        }
        Set<Item> claimed = new HashSet<>();
        List<MetalRow> out = new ArrayList<>();
        for (Metal metal : METAL_LIST) {
            List<Item> pool = new ArrayList<>(candidates.getOrDefault(metal, List.of()));
            pool.removeIf(claimed::contains);
            Map<Slot, Item> slots = new EnumMap<>(Slot.class);
            Set<Item> used = new LinkedHashSet<>();
            for (Slot slot : Slot.values()) {
                if (slot == Slot.OTHER) {
                    break;
                }
                Item found = null;
                for (String extra : metal.extra().getOrDefault(slot, List.of())) {
                    Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(extra)).orElse(Items.AIR);
                    if (item != Items.AIR && !used.contains(item) && !claimed.contains(item) && fits(item, slot)) {
                        found = item;
                        break;
                    }
                }
                for (String pattern : slot.patterns) {
                    if (found != null) {
                        break;
                    }
                    boolean exact = pattern.startsWith("=");
                    for (String word : metal.words()) {
                        String path = (exact ? pattern.substring(1) : pattern).replace("{p}", metal.prefix()).replace("{m}", word);
                        for (Item item : pool) {
                            if (!used.contains(item) && TcContext.id(item).getPath().equals(path)
                                    && (!exact || (slot == Slot.STORAGE) == (item instanceof BlockItem)) && fits(item, slot)) {
                                found = item;
                                break;
                            }
                        }
                        if (found != null) {
                            break;
                        }
                    }
                }
                if (found != null) {
                    slots.put(slot, found);
                    used.add(found);
                }
            }
            if (slots.isEmpty()) {
                continue;
            }
            // Weitere Objekte: was das Metall im Namen traegt und keine Spalte hat (Aepfel, Eimer, Kopf, Oefen ...).
            Set<String> own = metal.ownRows();
            List<Item> others = new ArrayList<>();
            List<Item> deferred = new ArrayList<>();
            for (Item item : pool) {
                if (used.contains(item)) {
                    continue;
                }
                String row = rowOf.get(item);
                (row != null && !own.contains(row) ? deferred : others).add(item);
            }
            claimed.addAll(used);
            claimed.addAll(others);
            out.add(new MetalRow(metal, slots, others, deferred));
        }
        return out;
    }

    // ------------------------------------------------------------------ 3. Items je Mod (+ Besatz, Mobs)

    /** Familien-Mobs eines Namensraums (beschwoerbar, nicht MISC), nach Id. */
    public static List<EntityType<?>> mobs(String ns) {
        List<EntityType<?>> out = new ArrayList<>();
        for (EntityType<?> type : MobStations.modMobs()) {
            // Staender und Puppen sind keine Mobs (sie stehen als Staender bzw. in ihrer Station).
            if (BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals(ns)
                    && !net.minecraft.world.entity.decoration.ArmorStand.class.isAssignableFrom(type.getBaseClass())) {
                out.add(type);
            }
        }
        return out;
    }

    /** Bereich 3: Besatz-Staender (je Muster, je Material ein Eisen-Satz), dann je Mod eine Linie, dahinter ihre Mobs. */
    public static Area items(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        List<Cell> cells = new ArrayList<>();
        c.wallSign(0, 0, 1, TcText.bold(TcText.t("order.items", "Items per mod")),
                TcText.t("order.items.sub", "wiki order, placed or framed"), TcText.t("order.items.mobs", "mobs behind each mod"));
        int z = START_Z;
        // Besatz (Besitzer: "je Besatz ein Ruestungsstaender mit vollstaendiger Ruestung").
        List<Holder<TrimPattern>> patterns = TestCentreSections.patterns(ctx);
        List<Holder<TrimMaterial>> materials = TestCentreSections.trimMaterials(ctx);
        List<Item> armour = List.of(Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
        if (!patterns.isEmpty() && !materials.isEmpty()) {
            Holder<TrimMaterial> showMaterial = pick(materials, "gold");
            Holder<TrimPattern> showPattern = pick(patterns, "sentry");
            z = stands(c, z, patterns.size(), i -> trimmedSet(armour, showMaterial, patterns.get(i)),
                    i -> patterns.get(i).value().description());
            z = stands(c, z, materials.size(), i -> trimmedSet(armour, materials.get(i), showPattern),
                    i -> materials.get(i).value().description());
        }
        z += 1;
        for (String ns : namespaces()) {
            List<Item> list = items(ns);
            List<EntityType<?>> mobs = mobs(ns);
            if (list.isEmpty() && mobs.isEmpty()) {
                continue;
            }
            c.wallSign(0, 0, z, TcText.bold(TcText.lit(ModBlockGallery.modName(ns))), TcText.t("order.items.count", "%s items", list.size()),
                    TcText.t("order.items.mobcount", "%s mobs", mobs.size()));
            int perLine = MAX_WIDTH - START_X;
            for (int i = 0; i < list.size(); i++) {
                int x = START_X + i % perLine;
                int iz = z + 2 * (i / perLine);
                put(c, list.get(i), x, iz);
                cells.add(new Cell(ns, i, list.get(i), new BlockPos(x, 0, iz)));
            }
            z += 2 * Math.max(1, (list.size() + perLine - 1) / perLine);
            // Mobs: eigener Streifen hinter den Linien der Mod.
            if (!mobs.isEmpty()) {
                int x = START_X;
                int depth = 1;
                for (EntityType<?> type : mobs) {
                    int size = Math.max(1, (int) Math.ceil(type.getWidth()));
                    if (x > START_X && x + size > MAX_WIDTH) {
                        x = START_X;
                        z += depth + 1;
                        depth = 1;
                    }
                    c.mob(x + (size - 1) / 2, 0, z + (size - 1) / 2, 180F, BuiltInRegistries.ENTITY_TYPE.getKey(type));
                    x += size + 1;
                    depth = Math.max(depth, size);
                }
                z += depth + 1;
            }
            z += 1;
        }
        return new Area(c, cells, List.of());
    }

    /** Der Eintrag mit diesem Pfad, sonst der erste. */
    private static <T> Holder<T> pick(List<Holder<T>> list, String path) {
        for (Holder<T> holder : list) {
            if (holder.unwrapKey().map(k -> k.identifier().getPath().equals(path)).orElse(false)) {
                return holder;
            }
        }
        return list.getFirst();
    }

    private static List<ItemStack> trimmedSet(List<Item> armour, Holder<TrimMaterial> material, Holder<TrimPattern> pattern) {
        List<ItemStack> gear = new ArrayList<>();
        for (Item piece : armour) {
            gear.add(TestCentreSections.trimmed(piece, material, pattern));
        }
        gear.add(ItemStack.EMPTY);
        gear.add(ItemStack.EMPTY);
        return gear;
    }

    /** Eine Reihe Staender ab {@code z} (Abstand 2, umbrechend); liefert das naechste freie z. */
    private static int stands(TcCanvas c, int z, int count, java.util.function.IntFunction<List<ItemStack>> gear,
                              java.util.function.IntFunction<net.minecraft.network.chat.Component> name) {
        if (count == 0) {
            return z;
        }
        int perLine = Math.max(1, (MAX_WIDTH - START_X) / 2);
        for (int i = 0; i < count; i++) {
            c.stand(START_X + 2 * (i % perLine), 0, z + 2 * (i / perLine), 180F, gear.apply(i), name.apply(i));
        }
        return z + 2 * ((count + perLine - 1) / perLine) + 1;
    }

    // ------------------------------------------------------------------ 4. Kompakt je Mod

    /** Die Bloecke einer Mod: Block-Items und setzbare Items (Achtel). */
    public static List<Item> blocks(String ns) {
        List<Item> out = new ArrayList<>();
        for (Item item : items(ns)) {
            if (item instanceof BlockItem || !ModBlockGallery.states(item).isEmpty()) {
                out.add(item);
            }
        }
        return out;
    }

    /** Bereich 4: je Mod ein fast quadratischer, dichter Block (Feld 0 = Schildpfosten), ein Feld Abstand. */
    public static Area compact() {
        TcCanvas c = new TcCanvas();
        List<Cell> cells = new ArrayList<>();
        c.wallSign(0, 0, 1, TcText.bold(TcText.t("order.compact", "Compact per mod")),
                TcText.t("order.compact.sub", "all blocks of a mod together"));
        int x = START_X;
        int z = START_Z;
        int shelfDepth = 0;
        for (String ns : namespaces()) {
            List<Item> list = blocks(ns);
            if (list.isEmpty()) {
                continue;
            }
            int n = list.size() + 1;
            int cols = Math.min(MAX_WIDTH - START_X, Math.max(2, (int) Math.ceil(Math.sqrt(n))));
            int rows = (n + cols - 1) / cols;
            if (x > START_X && x + cols > MAX_WIDTH) {
                x = START_X;
                z += shelfDepth + 1;
                shelfDepth = 0;
            }
            c.wallSign(x, 0, z, TcText.bold(TcText.lit(ModBlockGallery.modName(ns))), TcText.t("order.compact.count", "%s blocks", list.size()));
            for (int i = 0; i < list.size(); i++) {
                int cell = i + 1;
                int bx = x + cell % cols;
                int bz = z + cell / cols;
                put(c, list.get(i), bx, bz);
                cells.add(new Cell(ns, i, list.get(i), new BlockPos(bx, 0, bz)));
            }
            x += cols + 1;
            shelfDepth = Math.max(shelfDepth, rows);
        }
        return new Area(c, cells, List.of());
    }

    // ------------------------------------------------------------------ alles

    /** Die drei Bereiche untereinander (fuer {@code /testcenter order build}), je mit Gang dazwischen. */
    public static TcCanvas all(TcContext ctx) {
        TcCanvas c = new TcCanvas();
        int z = 0;
        for (TcCanvas area : List.of(families().canvas(), items(ctx).canvas(), compact().canvas())) {
            if (area.ops().isEmpty()) {
                continue;
            }
            c.append(area, 0, z);
            z += area.depth() + TestCentreLayout.CORRIDOR;
        }
        return c;
    }
}
