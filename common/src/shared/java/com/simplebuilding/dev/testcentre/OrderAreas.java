package com.simplebuilding.dev.testcentre;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
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
 * TZ38 (Besitzer 12.11.: "Testzentrale neu ordnen", Plan {@code docs/ai/PLAN-A-TZ38-2026-11-12.md}), TZ38D (Besitzer 13.11.
 * N42 + Erweiterung, Plan {@code docs/ai/PLAN-A-TZ38D-2026-11-13.md}): die geordneten Bereiche vor den Teststationen, in zwei
 * waehlbaren Varianten ({@link Variant}); jedes Glied steht in einer Variante genau einmal:
 * <ol>
 *   <li>{@link #FAMILIES} (Variante "lines"): graue Mittellinie, Seite A Metalle/Rohstoffe, Holz, Rest, gegenueber Seite B
 *       alles Steinige; je Variante/Material eine Reihe, die neben der Linie mit Knopf, Druckplatte ... beginnt (gemeinsamer
 *       Spaltenplan {@link #CORE}, Holz und Metalle mit eigenen Zusatzspalten); fehlende Glieder als Platzhalter (rot = sollte
 *       existieren, hellgrau = soll nicht).</li>
 *   <li>{@link #COMPACT} (Variante "blocks"): alle Bloecke je Mod dicht beieinander, ein Feld Abstand zur naechsten Mod.</li>
 *   <li>{@link #ITEMS} (beide): Besatz-Staender (je Muster, je Material ein voller Satz), dann je Mod eine Linie in
 *       Wiki-Reihenfolge mit allem, was der Block-Bereich der Variante nicht zeigt (setzbar = gesetzt, sonst Rahmen); dahinter
 *       die Mobs der Mod (ohne KI, unverwundbar, still).</li>
 * </ol>
 */
public final class OrderAreas {

    public static final String FAMILIES = "order_families";
    public static final String ITEMS = "order_items";
    public static final String COMPACT = "order_compact";
    /** Reihenfolge in der Welt: Block-Bereich der Variante, dann Items je Mod. */
    public static final List<String> SECTIONS = List.of(FAMILIES, COMPACT, ITEMS);

    /**
     * Die zwei Varianten der Zentrale (N42 Erweiterung, {@code /sbtestcentre build [lines|blocks]}): "lines" ordnet alle
     * Bloecke in Reihen an der grauen Mittellinie ({@link #FAMILIES}), "blocks" je Mod kompakt ({@link #COMPACT}). Alle
     * uebrigen Bereiche (Stationen, Testflaechen, Strukturen, Mobs, Items je Mod) bauen beide gleich.
     */
    public enum Variant {
        LINES, BLOCKS;

        /** Befehlswort. */
        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        /** Der Block-Bereich der Variante. */
        public String blockSection() {
            return this == LINES ? FAMILIES : COMPACT;
        }

        /** Variante zu einem Befehlswort, sonst null. */
        public static Variant of(String word) {
            for (Variant variant : values()) {
                if (variant.id().equals(word)) {
                    return variant;
                }
            }
            return null;
        }
    }

    /** Standard ohne Angabe: die Linien (N42 ist der Wunsch, "blocks" die bisherige kompakte Form). */
    public static final Variant DEFAULT = Variant.LINES;

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

    // ------------------------------------------------------------------ 1. Linien: Block-Familien, Metalle, Holz (TZ38D)

    /**
     * Gemeinsamer Spaltenplan aller Linien-Reihen (TZ38D, Besitzer 13.11. N42): jede Reihe beginnt ein Feld neben der grauen
     * Mittellinie mit den kleinen Gliedern und laeuft nach aussen. Die Kernspalten {@link #CORE} sind auf beiden Seiten gleich:
     * Knopf | Druckplatte | Blech | Zaun (Metall: Stange) | Zauntor (Metall: Kette) | Falltuer | Tuer | Octet | Mauer (Metall:
     * Gitter, Glas: Scheibe) | Stufe | Treppe | Block (Metall: geschnitten, Holz: Bretter) | rissig (Metall: Rost) | gemeisselt |
     * Saeule (Holz: Stamm, Sonderblock). Holz und Metalle haengen ihre eigenen Spalten an ({@link #WOOD_EXTRA},
     * {@link #METAL_EXTRA}), danach "weitere" ({@link #OTHER}).
     * <p>Muster (Holz/Metall): {@code {p}} Praefix der Stufe ({@code waxed_exposed_} ...), {@code {m}} Metall- bzw. Holz-Wort;
     * {@code =} am Anfang: genau diese Id, bei {@link #STORAGE} nur Bloecke, sonst nur Nicht-Bloecke.
     */
    public enum Slot {
        // Kern - alle Reihen; Metalle setzen in die Zaun-/Zauntor-Spalten Stange und Kette (Holz hat keine Kette/Stange).
        BUTTON("{p}{m}_button"), PLATE("{p}{m}_pressure_plate"), SHEET("{p}{m}_sheet", "{p}{m}_plate"),
        FENCE("{p}{m}_fence"), FENCE_GATE("{p}{m}_fence_gate"), POLE("{p}{m}_pole", "{p}{m}_rod"), CHAIN("{p}{m}_chain"),
        TRAPDOOR("{p}{m}_trapdoor"), DOOR("{p}{m}_door"),
        OCTET("{p}cut_{m}_octet", "{p}cut_{m}_block_octet"),
        WALL("{p}{m}_bars", "{p}cut_{m}_wall", "{p}{m}_wall"),
        SLAB("{p}cut_{m}_slab", "{p}{m}_slab"), STAIRS("{p}cut_{m}_stairs", "{p}{m}_stairs"),
        BLOCK("{p}cut_{m}", "{p}cut_{m}_block"),
        CRACKED("{p}rusty_{m}", "{p}rusted_{m}", "{p}{m}_rust", "{p}cracked_{m}", "{p}cracked_{m}_block"),
        CHISELED("{p}chiseled_{m}", "{p}chiseled_{m}_block"),
        PILLAR("{p}{m}_pillar"),
        // Nur Holz (Vanilla-Folge des Kreativ-Tabs).
        STRIPPED_LOG("stripped_{m}_log", "stripped_{m}_stem", "stripped_{m}_block"), WOOD("{m}_wood", "{m}_hyphae"),
        STRIPPED_WOOD("stripped_{m}_wood", "stripped_{m}_hyphae"), LEAVES("{m}_leaves", "{m}_wart_block"),
        SAPLING("{m}_sapling", "{m}_propagule", "{m}_fungus", "{m}"), SIGN("{m}_sign"), HANGING_SIGN("{m}_hanging_sign"),
        SHELF("{m}_shelf"), BOOKSHELF("{m}_bookshelf"), BOAT("{m}_boat", "{m}_raft"), CHEST_BOAT("{m}_chest_boat", "{m}_chest_raft"),
        // Nur Metalle: weitere Bauteile.
        TILES("{p}{m}_tiles"), GRATE("{p}{m}_grate"), LANTERN("{p}{m}_lantern"), LAMP("{p}{m}_bulb", "{p}{m}_lamp"),
        CHEST("{p}{m}_chest"), SPECIAL("{p}{m}_golem_statue", "{m}_mirror"), HOPPER("{m}_hopper"), PIPE("{m}_pipe"),
        // Nur Metalle: Material und Fundort (Achtel stehen auf ihrem Block, siehe TOPS).
        STORAGE("{p}{m}_block", "={p}{m}"), RAW_BLOCK("raw_{m}_block", "{m}_scrap_block", "layered_raw_{m}"), ORE("{m}_ore"),
        DEEPSLATE_ORE("deepslate_{m}_ore"), NETHER_ORE("nether_{m}_ore"), END_ORE("end_{m}_ore"),
        LOOSE_ORE("{m}_bearing_gravel", "{m}_bearing_sand"),
        // Nur Metalle: Material-Items (Roh -> Barren -> Nugget).
        RAW("raw_{m}"), INGOT("{m}_ingot", "={m}", "{m}_gem", "{m}_shard", "{m}_crystal", "{m}_lazuli", "{m}_dust"),
        NUGGET("{m}_nugget"), SCRAP("{m}_scrap"), CORE("{m}_core"),
        // Nur Metalle: Ausruestung (keine Spachtel-Spalte: Spachteln sind seit 09/2026 Meissel).
        HORSE_ARMOR("{m}_horse_armor"), NAUTILUS_ARMOR("{m}_nautilus_armor"), ANIMAL_ARMOR("trex_armor_{m}", "{m}_wolf_armor"),
        HORSESHOE("{m}_horseshoe"), SWORD("{m}_sword"), SPEAR("{m}_spear"), PICKAXE("{m}_pickaxe"), AXE("{m}_axe"),
        SHOVEL("{m}_shovel"), HOE("{m}_hoe"), CHISEL("{m}_chisel"), SLEDGEHAMMER("{m}_sledgehammer"),
        BUILDING_WAND("{m}_building_wand"), HELMET("{m}_helmet"), CHESTPLATE("{m}_chestplate"), LEGGINGS("{m}_leggings"),
        BOOTS("{m}_boots"),
        /** "weitere": alles Uebrige einer Reihe, hinter ihren festen Spalten. */
        OTHER;

        final List<String> patterns;

        Slot(String... patterns) {
            this.patterns = List.of(patterns);
        }
    }

    /** Kernspalten (Feld 0 = neben dem Abstand zur Mittellinie), fuer Stein-, Rest- und Holz-Reihen. */
    public static final List<Slot> CORE = List.of(Slot.BUTTON, Slot.PLATE, Slot.SHEET, Slot.FENCE, Slot.FENCE_GATE, Slot.TRAPDOOR,
            Slot.DOOR, Slot.OCTET, Slot.WALL, Slot.SLAB, Slot.STAIRS, Slot.BLOCK, Slot.CRACKED, Slot.CHISELED, Slot.PILLAR);
    /** Holz nach dem Kern. */
    public static final List<Slot> WOOD_EXTRA = List.of(Slot.STRIPPED_LOG, Slot.WOOD, Slot.STRIPPED_WOOD, Slot.LEAVES, Slot.SAPLING,
            Slot.SIGN, Slot.HANGING_SIGN, Slot.SHELF, Slot.BOOKSHELF, Slot.BOAT, Slot.CHEST_BOAT);
    /** Metalle nach dem Kern (TZ38b F4: Bauteile, Material und Fundort, Material-Items, Ausruestung). */
    public static final List<Slot> METAL_EXTRA;
    /** Achtel, die auf ihrem Block stehen (N42-7: "Rohblock-Achtel beim Rohblock"; Speicher- und Stamm-Achtel ebenso). */
    static final Map<Slot, List<String>> TOPS = Map.of(
            Slot.STORAGE, List.of("{p}{m}_block_octet", "{p}{m}_octet"),
            Slot.RAW_BLOCK, List.of("raw_{m}_block_octet", "raw_{m}_octet"),
            Slot.PILLAR, List.of("{m}_log_octet", "{m}_stem_octet"),
            Slot.STRIPPED_LOG, List.of("stripped_{m}_log_octet", "stripped_{m}_stem_octet"));
    /** Holz-Muster der Kernspalten, die anders heissen als bei Metallen (Bretter, Stamm, Mosaik). */
    static final Map<Slot, List<String>> WOOD_PATTERNS = Map.of(
            Slot.OCTET, List.of("{m}_octet", "{m}_planks_octet"),
            Slot.BLOCK, List.of("{m}_planks"),
            Slot.CRACKED, List.of("cracked_{m}_planks"),
            Slot.CHISELED, List.of("chiseled_{m}_planks", "{m}_mosaic"),
            Slot.PILLAR, List.of("{m}_log", "{m}_stem", "{m}_block"),
            Slot.WALL, List.of("{m}_wall"),
            Slot.SLAB, List.of("{m}_slab"),
            Slot.STAIRS, List.of("{m}_stairs"));

    /** Muster einer Spalte fuer eine Material-Art. */
    static List<String> patterns(Kind kind, Slot slot) {
        return kind == Kind.WOOD ? WOOD_PATTERNS.getOrDefault(slot, slot.patterns) : slot.patterns;
    }

    static {
        List<Slot> metal = new ArrayList<>();
        for (Slot slot : Slot.values()) {
            if (slot.ordinal() >= Slot.TILES.ordinal() && slot != Slot.OTHER) {
                metal.add(slot);
            }
        }
        METAL_EXTRA = List.copyOf(metal);
    }

    /** Art einer Reihe: Seite A Metalle, Holz, Rest; Seite B Stein (alles Steinige, Nicht-Hoelzerne). */
    public enum Kind {
        METAL, WOOD, REST, STONE;

        /** Seite A (Metalle, Holz, Rest) oder B (gegenueber: Stein). */
        public boolean sideA() {
            return this != STONE;
        }
    }

    /** Feste Spalten einer Reihe dieser Art. */
    public static List<Slot> columns(Kind kind) {
        List<Slot> out = new ArrayList<>(CORE);
        if (kind == Kind.METAL) {
            out.set(CORE.indexOf(Slot.FENCE), Slot.POLE);
            out.set(CORE.indexOf(Slot.FENCE_GATE), Slot.CHAIN);
            out.addAll(METAL_EXTRA);
        } else if (kind == Kind.WOOD) {
            out.addAll(WOOD_EXTRA);
        }
        return List.copyOf(out);
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

    /** Spalte eines Items in seiner Familien-Reihe nach Form, null = keine Familien-Form (Maschinen, Lichter, Items ...). */
    static Slot slot(Item item, List<BlockState> states) {
        String path = TcContext.id(item).getPath();
        if (item instanceof BlockItem && isSheet(path)) {
            return Slot.SHEET;
        }
        Block block = item instanceof BlockItem blockItem ? blockItem.getBlock() : null;
        // Druckplatten und Knoepfe auch nach Namen (Simple Pads: Kupfer-Druckplatte ist ein PadBlock) und ungesetzt (Signalquellen).
        if (block instanceof BasePressurePlateBlock || block != null && path.endsWith("_pressure_plate")) {
            return Slot.PLATE;
        }
        if (block instanceof ButtonBlock || block != null && path.endsWith("_button")) {
            return Slot.BUTTON;
        }
        if (!states.isEmpty()) {
            ModBlockGallery.Form form = ModBlockGallery.formOf(states.getFirst());
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
        if (block == null) {
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
        bySuffix.put("_trapdoor", Slot.TRAPDOOR);
        bySuffix.put("_door", Slot.DOOR);
        for (Map.Entry<String, Slot> entry : bySuffix.entrySet()) {
            if (path.endsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        BlockState state = block.defaultBlockState();
        try {
            return state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) && !state.hasBlockEntity()
                    ? blockSlot(path) : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Spalte eines Items nach seiner Form (ohne Sonderblock-Zuordnung), null = keine Familien-Form. */
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
     * Alle Familien-Reihen vor Abzug der Metall- und Holz-Reihen: Reihen-Schluessel -> Glieder (Familien-Mod-Items, Vanilla als
     * Bezug in vorhandenen Reihen, Sonderbloecke in der Saeulen-Spalte); nur Reihen mit mindestens einem Familien-Mod-Glied.
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
     * Die Familien-Reihen ohne die Items der Metall- und Holz-Reihen (keine Doppel), je Familie gruppiert (Familien nach Farbe,
     * Reihen nach Variante).
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
        BlockState state = familyState(family);
        return state == null ? 3.0 : hue(state);
    }

    /** Der Zustand des ersten gesetzten Vollblocks (sonst des ersten gesetzten Glieds) einer Familie. */
    private static BlockState familyState(Map<String, List<Member>> family) {
        BlockState any = null;
        for (List<Member> row : family.values()) {
            for (Member m : row) {
                List<BlockState> states = ModBlockGallery.states(m.item());
                if (states.isEmpty()) {
                    continue;
                }
                if (m.slot() == Slot.BLOCK) {
                    return states.getFirst();
                }
                if (any == null) {
                    any = states.getFirst();
                }
            }
        }
        return any;
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
     * Steinig (Seite B, N42-3: "alle steinigen/nicht-hoelzernen Bloecke: Stein, Ziegel, Sand, Wolle, Eis, Beton, Terrakotta"):
     * nach dem Klang des Grundblocks oder weil er mit der Spitzhacke abgebaut wird (Mod-Gestein mit eigenem Klang). Alles andere
     * ohne Holz und Metall (Kuerbisse, Melonen, Heu, Moos, Schleim ...) ist "Rest" (Seite A).
     */
    static boolean stony(BlockState state) {
        if (state == null) {
            return true;
        }
        net.minecraft.world.level.block.SoundType sound;
        try {
            sound = state.getSoundType();
        } catch (RuntimeException e) {
            return true;
        }
        return STONY_SOUNDS.contains(sound) || state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE);
    }

    private static final Set<net.minecraft.world.level.block.SoundType> STONY_SOUNDS = Set.of(
            net.minecraft.world.level.block.SoundType.STONE, net.minecraft.world.level.block.SoundType.DEEPSLATE,
            net.minecraft.world.level.block.SoundType.DEEPSLATE_BRICKS, net.minecraft.world.level.block.SoundType.DEEPSLATE_TILES,
            net.minecraft.world.level.block.SoundType.POLISHED_DEEPSLATE, net.minecraft.world.level.block.SoundType.TUFF,
            net.minecraft.world.level.block.SoundType.TUFF_BRICKS, net.minecraft.world.level.block.SoundType.POLISHED_TUFF,
            net.minecraft.world.level.block.SoundType.NETHER_BRICKS, net.minecraft.world.level.block.SoundType.NETHERRACK,
            net.minecraft.world.level.block.SoundType.BASALT, net.minecraft.world.level.block.SoundType.CALCITE,
            net.minecraft.world.level.block.SoundType.DRIPSTONE_BLOCK, net.minecraft.world.level.block.SoundType.MUD_BRICKS,
            net.minecraft.world.level.block.SoundType.PACKED_MUD, net.minecraft.world.level.block.SoundType.SAND,
            net.minecraft.world.level.block.SoundType.GRAVEL, net.minecraft.world.level.block.SoundType.WOOL,
            net.minecraft.world.level.block.SoundType.SNOW, net.minecraft.world.level.block.SoundType.GLASS,
            net.minecraft.world.level.block.SoundType.BONE_BLOCK, net.minecraft.world.level.block.SoundType.CORAL_BLOCK,
            net.minecraft.world.level.block.SoundType.AMETHYST, net.minecraft.world.level.block.SoundType.LODESTONE,
            net.minecraft.world.level.block.SoundType.GILDED_BLACKSTONE, net.minecraft.world.level.block.SoundType.NETHER_ORE,
            net.minecraft.world.level.block.SoundType.NETHER_GOLD_ORE, net.minecraft.world.level.block.SoundType.ANCIENT_DEBRIS,
            net.minecraft.world.level.block.SoundType.RESIN_BRICKS, net.minecraft.world.level.block.SoundType.SOUL_SOIL,
            net.minecraft.world.level.block.SoundType.SOUL_SAND, net.minecraft.world.level.block.SoundType.NYLIUM);

    /** Graue Betonstufe der Mittellinie (26.3 {@code gray_concrete_slab}), sonst Beton-Achtel, sonst grauer Beton. */
    public static BlockState lineState() {
        return concrete("gray");
    }

    /** Platzhalter "soll nicht existieren": hellgraue Betonstufe (N42-2). */
    public static BlockState absentState() {
        return concrete("light_gray");
    }

    /** Platzhalter "sollte existieren, fehlt": rote Betonstufe (N42-2, Fehlliste). */
    public static BlockState missingState() {
        return concrete("red");
    }

    /** Betonstufe der Farbe: Vanilla-Stufe (26.3), sonst Beton-Achtel mit voller unterer Lage (Simple Octets), sonst Beton. */
    private static BlockState concrete(String colour) {
        Block slab = BuiltInRegistries.BLOCK.getOptional(Identifier.withDefaultNamespace(colour + "_concrete_slab")).orElse(null);
        if (slab != null) {
            return slab.defaultBlockState();
        }
        Block octet = BuiltInRegistries.BLOCK.getOptional(Identifier.fromNamespaceAndPath("simpleoctets", colour + "_concrete_octet"))
                .orElse(null);
        if (octet == null) {
            return BuiltInRegistries.BLOCK.getOptional(Identifier.withDefaultNamespace(colour + "_concrete")).orElse(Blocks.STONE)
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

    // ------------------------------------------------------------------ Material-Reihen (Metalle, Holz)

    /**
     * Ein Material mit eigenen Spalten: Anzeige-Schluessel, Art, Stufen-Praefix ({@code exposed_} ...), Praefixe in
     * Vorzugsfolge (Kupfer: gewachst zuerst, N42-6), Woerter (erstes = Name, weitere = Aliasse wie {@code golden}), Gruppe
     * (Metalle: 0 Vanilla/SimpleBuilding, 1 Mod-Erze, 2 Mineralien), feste Extras je Spalte (Vanilla-Ids ohne das Wort).
     */
    public record Material(String key, Kind kind, String prefix, List<String> prefixes, List<String> words, int group,
                           Map<Slot, List<String>> extra) {
        Material(String key, int group, String... words) {
            this(key, Kind.METAL, "", List.of(""), List.of(words), group, Map.of());
        }

        /** Wortfolge, an der Items erkannt werden ({@code exposed}+{@code copper}; gewachst zaehlt nicht mit). */
        List<String> seq(String word) {
            List<String> out = new ArrayList<>(prefix.isEmpty() ? List.of() : tokens(prefix.substring(0, prefix.length() - 1)));
            out.addAll(tokens(word));
            return out;
        }

        /** Reihen-Schluessel, die ganz zu dieser Reihe gehoeren ({@code {p}{m}}, {@code {p}cut_{m}}). */
        Set<String> ownRows() {
            Set<String> out = new HashSet<>();
            for (String p : prefixes) {
                for (String word : words) {
                    out.add(p + word);
                    out.add(p + "cut_" + word);
                }
            }
            return out;
        }

        /** Eine Kupfer-Stufe (nicht die Grundstufe): nur Bauformen, keine Material-/Ausruestungs-Erwartungen. */
        boolean stage() {
            return !prefix.isEmpty();
        }
    }

    private static Material copperStage(String prefix) {
        return new Material(prefix + "copper", Kind.METAL, prefix, List.of("waxed_" + prefix, prefix), List.of("copper"), 0,
                Map.of(Slot.POLE, List.of("minecraft:waxed_" + prefix + "lightning_rod", "minecraft:" + prefix + "lightning_rod")));
    }

    /**
     * Reihenfolge des Besitzers: Vanilla/SB-Metalle bis Astralit/Nihilith (Kupfer nur mit den vier gewachsten Stufen, N42-6),
     * dann die Mod-Erze, dann die Mineralien - alle auf Seite A, danach Holz und Rest.
     */
    public static final List<Material> METAL_LIST = List.of(
            copperStage(""), copperStage("exposed_"), copperStage("weathered_"), copperStage("oxidized_"),
            new Material("iron", Kind.METAL, "", List.of(""), List.of("iron"), 0, Map.of(Slot.PLATE,
                    List.of("minecraft:heavy_weighted_pressure_plate"), Slot.LANTERN, List.of("minecraft:lantern"),
                    Slot.HOPPER, List.of("minecraft:hopper"), Slot.CHAIN, List.of("minecraft:iron_chain", "minecraft:chain"))),
            new Material("gold", Kind.METAL, "", List.of(""), List.of("gold", "golden"), 0,
                    Map.of(Slot.PLATE, List.of("minecraft:light_weighted_pressure_plate"))),
            new Material("rose_gold", 0, "rose_gold"),
            new Material("rose_bronze", 0, "rose_bronze"),
            new Material("diamond", 0, "diamond"),
            new Material("emerald", 0, "emerald"),
            new Material("netherite", Kind.METAL, "", List.of(""), List.of("netherite"), 0,
                    Map.of(Slot.NETHER_ORE, List.of("minecraft:ancient_debris"))),
            new Material("enderite", 0, "enderite"),
            new Material("dimensional", 0, "dimensional"),
            new Material("terranite", 0, "terranite"),
            new Material("zenithite", 0, "zenithite"),
            new Material("infernite", 0, "infernite"),
            new Material("rift", 0, "rift"),
            new Material("sage", 0, "sage"),
            new Material("astralit", 0, "astralit"),
            new Material("nihilith", 0, "nihilith"),
            new Material("silver", 1, "silver"),
            new Material("lead", 1, "lead"),
            new Material("zinc", 1, "zinc"),
            new Material("nickel", 1, "nickel"),
            new Material("tin", 1, "tin"),
            new Material("brass", 1, "brass"),
            new Material("bronze", 1, "bronze"),
            new Material("stainless_steel", 1, "stainless_steel"),
            new Material("redstone", 2, "redstone"),
            new Material("lapis", 2, "lapis"),
            new Material("quartz", 2, "quartz"),
            new Material("coal", 2, "coal"),
            new Material("amethyst", 2, "amethyst"));

    /** Vanilla-Hoelzer in der Folge des Kreativ-Tabs; Familien-Hoelzer ({@code x_planks}) folgen nach Mod und Id. */
    static final List<String> VANILLA_WOODS = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry",
            "pale_oak", "bamboo", "crimson", "warped");

    /** Alle Hoelzer: jedes {@code x_planks} (Vanilla und Familie) ist ein Holz {@code x}. */
    static List<Material> woods() {
        List<String> namespaces = new ArrayList<>(List.of("minecraft"));
        namespaces.addAll(namespaces());
        List<String> keys = new ArrayList<>();
        List<String> mod = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = TcContext.id(item);
            String path = id.getPath();
            if (!path.endsWith("_planks") || !namespaces.contains(id.getNamespace()) || foreign(path)
                    || !id.getNamespace().equals("minecraft") && !ModBlockGallery.shown(id)) {
                continue;
            }
            String key = path.substring(0, path.length() - "_planks".length());
            if (VANILLA_WOODS.contains(key) || keys.contains(key) || mod.contains(key)) {
                continue;
            }
            mod.add(key);
        }
        for (String key : VANILLA_WOODS) {
            if (BuiltInRegistries.ITEM.containsKey(Identifier.withDefaultNamespace(key + "_planks"))) {
                keys.add(key);
            }
        }
        keys.addAll(mod);
        List<Material> out = new ArrayList<>();
        for (String key : keys) {
            // Vanilla nennt das Karmesin-"Laub" Netherwarzenblock.
            Map<Slot, List<String>> extra = key.equals("crimson") ? Map.of(Slot.LEAVES, List.of("minecraft:nether_wart_block")) : Map.of();
            out.add(new Material(key, Kind.WOOD, "", List.of(""), List.of(key), 0, extra));
        }
        return out;
    }

    /** Woerter, die ein Item einem anderen Thema zuordnen (Farben, Schach, Lichtscheiben, Ender-/Astral-/Nihil-Bloecke). */
    private static final Set<String> FOREIGN = Set.of("white", "orange", "magenta", "light", "yellow", "lime", "pink", "gray",
            "cyan", "purple", "blue", "brown", "green", "red", "black", "checker", "chess", "ender", "astral", "nihil");

    private static boolean foreign(String path) {
        for (String word : tokens(path)) {
            if (FOREIGN.contains(word)) {
                return true;
            }
        }
        return path.endsWith("_spawn_egg");
    }

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

    /** Das Material einer Id: das mit der laengsten passenden Wortfolge, bei Gleichstand das fruehere; sonst null. */
    static Material materialOf(String path, List<Material> materials) {
        if (foreign(path)) {
            return null;
        }
        List<String> words = tokens(path);
        Material best = null;
        int bestLength = 0;
        for (Material material : materials) {
            for (String alias : material.words()) {
                List<String> seq = material.seq(alias);
                if (seq.size() > bestLength && subsequence(seq, words)) {
                    best = material;
                    bestLength = seq.size();
                }
            }
        }
        return best;
    }

    /** Das Metall einer Id (nur Metalle), sonst null. */
    static Material metalOf(String path) {
        return materialOf(path, METAL_LIST);
    }

    /**
     * Eine Material-Reihe: feste Spalten, Achtel auf ihrem Block ({@link #TOPS}), weitere Objekte, zurueckgestellte Items
     * (stehen in einer eigenen Stein-Familie, E3) und ausgelassene ungewachste Kupfer-Zwillinge (N42-6).
     */
    record MaterialRow(Material material, Map<Slot, Item> slots, Map<Slot, Item> tops, List<Item> others, List<Item> deferred,
                       List<Item> hidden) {
    }

    /** Kern- und Zusatzformen nehmen nur Items ihrer Familienform; Blech, Saeule und die Material-Spalten jedes. */
    private static boolean fits(Item item, Slot slot) {
        return slot == Slot.SHEET || slot == Slot.PILLAR || !CORE.contains(slot) || slotOf(item) == slot;
    }

    private static Item find(List<Item> pool, Set<Item> used, Material material, Slot slot, List<String> patterns) {
        for (String pattern : patterns) {
            boolean exact = pattern.startsWith("=");
            for (String prefix : material.prefixes()) {
                for (String word : material.words()) {
                    String path = (exact ? pattern.substring(1) : pattern).replace("{p}", prefix).replace("{m}", word);
                    for (Item item : pool) {
                        if (!used.contains(item) && TcContext.id(item).getPath().equals(path) && fits(item, slot)
                                && (!exact || slot != Slot.STORAGE || item instanceof BlockItem)) {
                            return item;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Die Metall- und Holz-Reihen: Kandidaten = Vanilla und Familien-Items mit dem Wort (Vanilla zuerst), je Spalte das erste
     * passende Muster (Kupfer gewachst vor ungewachst); uebrige Items in die weiteren, ausser sie stehen in einer anderen
     * Familien-Reihe ({@code rowOf}, nur Metalle: glatter Quarz, Quarzziegel ...).
     */
    static List<MaterialRow> materialRows(Map<Item, String> rowOf) {
        List<Material> materials = new ArrayList<>(METAL_LIST);
        materials.addAll(woods());
        Map<Material, List<Item>> candidates = new LinkedHashMap<>();
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
            Material material = materialOf(TcContext.id(item).getPath(), materials);
            if (material != null) {
                candidates.computeIfAbsent(material, key -> new ArrayList<>()).add(item);
            }
        }
        List<MaterialRow> out = new ArrayList<>();
        for (Material material : materials) {
            List<Item> pool = new ArrayList<>(candidates.getOrDefault(material, List.of()));
            Map<Slot, Item> slots = new EnumMap<>(Slot.class);
            Map<Slot, Item> tops = new EnumMap<>(Slot.class);
            Set<Item> used = new LinkedHashSet<>();
            for (Slot slot : columns(material.kind())) {
                Item found = null;
                for (String extra : material.extra().getOrDefault(slot, List.of())) {
                    Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(extra)).orElse(Items.AIR);
                    if (item != Items.AIR && !used.contains(item) && fits(item, slot)) {
                        found = item;
                        break;
                    }
                }
                if (found == null) {
                    found = find(pool, used, material, slot, patterns(material.kind(), slot));
                }
                if (found != null) {
                    slots.put(slot, found);
                    used.add(found);
                    if (TOPS.containsKey(slot) && (material.kind() == Kind.WOOD) == (slot == Slot.PILLAR || slot == Slot.STRIPPED_LOG)) {
                        Item top = find(pool, used, material, Slot.OTHER, TOPS.get(slot));
                        if (top != null && !ModBlockGallery.states(top).isEmpty()) {
                            tops.put(slot, top);
                            used.add(top);
                        }
                    }
                }
            }
            if (slots.isEmpty()) {
                continue;
            }
            // Gewachstes Kupfer steht, die ungewachsten Zwillinge nicht (N42-6: sieht gleich aus, wuerde altern).
            Set<String> shownPaths = new HashSet<>();
            used.forEach(item -> shownPaths.add(TcContext.id(item).getPath()));
            List<Item> hidden = new ArrayList<>();
            List<Item> rest = new ArrayList<>();
            for (Item item : pool) {
                if (used.contains(item)) {
                    continue;
                }
                rest.add(item);
            }
            rest.forEach(item -> {
                String path = TcContext.id(item).getPath();
                if (path.startsWith("waxed_")) {
                    shownPaths.add(path);
                }
            });
            List<Item> others = new ArrayList<>();
            List<Item> deferred = new ArrayList<>();
            Set<String> own = material.ownRows();
            for (Item item : rest) {
                String path = TcContext.id(item).getPath();
                if (material.kind() == Kind.METAL && !path.startsWith("waxed_") && shownPaths.contains("waxed_" + path)) {
                    hidden.add(item);
                    continue;
                }
                String row = rowOf.get(item);
                boolean defer = material.kind() == Kind.METAL && row != null && !own.contains(row);
                (defer ? deferred : others).add(item);
            }
            // Ungewachste Stufen-Bloecke, deren gewachster Zwilling in einer Spalte steht, sind ebenso ausgelassen.
            out.add(new MaterialRow(material, slots, tops, others, deferred, hidden));
        }
        return out;
    }

    // ------------------------------------------------------------------ Linien-Aufbau

    /** Eine Reihe der Linien (fuer Tests und Vorschau): Lage, Spalten, Inhalt, erwartete Glieder. */
    public record Row(String group, Kind kind, String family, int x, int nearZ, int step, List<Slot> columns, Map<Slot, Item> fixed,
                      Map<Slot, Item> tops, List<Item> others, Set<Slot> expected) {
        /** z des Felds in Spalte {@code column}. */
        public int z(int column) {
            return nearZ + step * column;
        }

        /** Laenge der Reihe: feste Spalten plus weitere. */
        public int length() {
            return columns.size() + others.size();
        }
    }

    /** Ein Platzhalter-Feld: {@code missing} = rot (sollte existieren), sonst hellgrau (soll nicht existieren). */
    public record Placeholder(BlockPos pos, String group, Slot slot, boolean missing) {
    }

    /** Ergebnis eines Bereichs: Schritte, gesetzte/gerahmte Items, Reihen und Platzhalter (Linien), ausgelassene Zwillinge. */
    public record Area(TcCanvas canvas, List<Cell> cells, List<Row> rows, List<Placeholder> placeholders, List<Item> hidden,
                       int lineZ) {
        Area(TcCanvas canvas, List<Cell> cells) {
            this(canvas, cells, List.of(), List.of(), List.of(), -1);
        }
    }

    /** Geplante Reihe vor der Lage. */
    private record RowPlan(String group, Kind kind, String family, Map<Slot, Item> fixed, Map<Slot, Item> tops, List<Item> others,
                        Set<Slot> expected, Component label) {
        int length() {
            return columns(kind).size() + others.size();
        }
    }

    /** Naturstufen bekommen keine Achtel (Octet-Regel, Ausnahme Besitzer 17.10.). */
    private static boolean natural(String row) {
        List<String> words = tokens(row);
        return words.contains("dirt") || words.contains("grass") || words.contains("sand") || words.contains("gravel");
    }

    /** Glieder-Saetze: hat eine Reihe eines, sollen alle da sein (rot, wenn eins fehlt). */
    private static final List<List<Slot>> PAIRS = List.of(List.of(Slot.BUTTON, Slot.PLATE), List.of(Slot.DOOR, Slot.TRAPDOOR),
            List.of(Slot.FENCE, Slot.FENCE_GATE));
    private static final List<List<Slot>> METAL_SETS = List.of(List.of(Slot.ORE, Slot.DEEPSLATE_ORE),
            List.of(Slot.STORAGE, Slot.INGOT), List.of(Slot.SWORD, Slot.SPEAR, Slot.PICKAXE, Slot.AXE, Slot.SHOVEL, Slot.HOE),
            List.of(Slot.HELMET, Slot.CHESTPLATE, Slot.LEGGINGS, Slot.BOOTS), List.of(Slot.HORSE_ARMOR, Slot.NAUTILUS_ARMOR),
            List.of(Slot.CHISEL, Slot.SLEDGEHAMMER), List.of(Slot.DOOR, Slot.TRAPDOOR));
    /** Holz-Spalten eines Baums (nur Hoelzer mit Stamm erwarten sie). */
    private static final Set<Slot> TREE = EnumSet.of(Slot.PILLAR, Slot.STRIPPED_LOG, Slot.WOOD, Slot.STRIPPED_WOOD, Slot.LEAVES, Slot.SAPLING);
    /** Holz-Spalten, die Vanilla fuer seine Hoelzer festlegt (dort nie rot: Vanilla entscheidet, etwa keine Nether-Boote). */
    private static final Set<Slot> VANILLA_DECIDES = EnumSet.of(Slot.PILLAR, Slot.STRIPPED_LOG, Slot.WOOD, Slot.STRIPPED_WOOD, Slot.LEAVES,
            Slot.SAPLING, Slot.SIGN, Slot.HANGING_SIGN, Slot.SHELF, Slot.BOOKSHELF, Slot.BOAT, Slot.CHEST_BOAT, Slot.BUTTON, Slot.PLATE,
            Slot.FENCE, Slot.FENCE_GATE, Slot.DOOR, Slot.TRAPDOOR, Slot.SLAB, Slot.STAIRS, Slot.BLOCK);
    /** Kein Mauer-Wunsch fuer weiche/lose Bloecke (Wolle, Sand, Kies, Schnee, Glas). */
    private static final Set<net.minecraft.world.level.block.SoundType> SOFT_SOUNDS = Set.of(net.minecraft.world.level.block.SoundType.WOOL,
            net.minecraft.world.level.block.SoundType.SAND, net.minecraft.world.level.block.SoundType.GRAVEL,
            net.minecraft.world.level.block.SoundType.SNOW, net.minecraft.world.level.block.SoundType.GLASS,
            net.minecraft.world.level.block.SoundType.SOUL_SAND, net.minecraft.world.level.block.SoundType.SOUL_SOIL);

    /** Was eine Reihe ueber ihre Art hinaus weiss (fuer die Erwartung). */
    private record Context(Material material, BlockState base, boolean special, boolean derived, Map<Slot, Integer> woodShare,
                           int woods) {
        static final Context NONE = new Context(null, null, false, false, Map.of(), 0);
    }

    /**
     * Welche Glieder eine Reihe haben sollte (rote Platzhalter, wenn sie fehlen; N42-2), begruendet in
     * {@code docs/ai/PLAN-A-TZ38D-2026-11-13.md} E2: Paare (Knopf+Druckplatte, Tuer+Falltuer, Zaun+Zauntor); Bauform-Familie
     * (Stufe oder Treppe vorhanden: Stufe, Treppe, Block, Achtel nach der Octet-Regel - Naturstufen nicht; Stein mit Stufe und
     * Treppe auch eine Mauer, ausser weich/lose); Metalle der Grundstufe: Erz+Tiefenschiefer-Erz, Roh+Rohblock, Speicher+Barren
     * (+Nugget bei {@code _ingot}), Werkzeuge, Ruestung, Pferde-+Nautilus-Ruestung, Meissel+Hammer, Tuer+Falltuer; Holz: was
     * mindestens die Haelfte der Hoelzer hat (Baumteile nur bei Hoelzern mit Stamm, bei Vanilla-Hoelzern nur die Zusaetze der
     * Familie; abgeleitete Hoelzer wie Lohenholz/Nihilit-Bretter wie Stein-Familien).
     */
    private static Set<Slot> expected(Kind kind, String row, Map<Slot, Item> fixed, Map<Slot, Item> tops, Context ctx) {
        Set<Slot> out = EnumSet.noneOf(Slot.class);
        if (kind == Kind.WOOD && !ctx.derived()) {
            boolean vanilla = ctx.material() != null && VANILLA_WOODS.contains(ctx.material().key());
            ctx.woodShare().forEach((slot, n) -> {
                if (n * 2 >= ctx.woods() && !(vanilla && VANILLA_DECIDES.contains(slot))
                        && !(TREE.contains(slot) && !fixed.containsKey(Slot.PILLAR))) {
                    out.add(slot);
                }
            });
            return out;
        }
        if (kind != Kind.METAL) {
            for (List<Slot> pair : PAIRS) {
                if (pair.stream().anyMatch(fixed::containsKey)) {
                    out.addAll(pair);
                }
            }
        }
        boolean shapes = fixed.containsKey(Slot.SLAB) || fixed.containsKey(Slot.STAIRS);
        if (shapes && !natural(row)) {
            out.addAll(List.of(Slot.SLAB, Slot.STAIRS));
            boolean octet = fixed.containsKey(Slot.OCTET) || !tops.isEmpty();
            if (!octet) {
                out.add(Slot.OCTET);
            }
            if (kind != Kind.METAL && !ctx.special()) {
                out.add(Slot.BLOCK);
            }
            if (kind == Kind.METAL && java.util.stream.Stream.of(fixed.get(Slot.SLAB), fixed.get(Slot.STAIRS))
                    .anyMatch(item -> item != null && TcContext.id(item).getPath().contains("cut_"))) {
                out.add(Slot.BLOCK); // geschnittene Stufe/Treppe: der geschnittene Block gehoert dazu
            }
            if (kind == Kind.STONE && fixed.containsKey(Slot.SLAB) && fixed.containsKey(Slot.STAIRS)
                    && (ctx.base() == null || !SOFT_SOUNDS.contains(soundOf(ctx.base())))) {
                out.add(Slot.WALL);
            }
        }
        if (kind == Kind.METAL && ctx.material() != null && !ctx.material().stage()) {
            for (List<Slot> set : METAL_SETS) {
                if (set.stream().anyMatch(fixed::containsKey)) {
                    out.addAll(set);
                }
            }
            Item rawBlock = fixed.get(Slot.RAW_BLOCK);
            if (fixed.containsKey(Slot.RAW) || rawBlock != null && TcContext.id(rawBlock).getPath().startsWith("raw_")) {
                out.addAll(List.of(Slot.RAW, Slot.RAW_BLOCK)); // ein Schrott-Block ist kein Rohblock (Netherit)
            }
            Item ingot = fixed.get(Slot.INGOT);
            if (ingot != null && TcContext.id(ingot).getPath().endsWith("_ingot")) {
                out.add(Slot.NUGGET);
            }
        }
        out.retainAll(columns(kind));
        return out;
    }

    private static net.minecraft.world.level.block.SoundType soundOf(BlockState state) {
        try {
            return state.getSoundType();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Familien-Reihe: je Kernspalte das erste Glied (echte Mauer vor Scheibe), Ueberzaehlige nach Form in "weitere". */
    private static RowPlan familyPlan(String key, List<Member> members, Kind kind, String family, Component label, Context context) {
        List<Member> ordered = new ArrayList<>(members);
        ordered.sort(Comparator.comparing(Member::weak));
        Map<Slot, Item> fixed = new EnumMap<>(Slot.class);
        List<Member> extra = new ArrayList<>();
        for (Member m : ordered) {
            if (CORE.contains(m.slot()) && !fixed.containsKey(m.slot())) {
                fixed.put(m.slot(), m.item());
            } else {
                extra.add(m);
            }
        }
        extra.sort(Comparator.comparingInt(m -> m.slot().ordinal()));
        List<Item> others = new ArrayList<>();
        extra.forEach(m -> others.add(m.item()));
        return new RowPlan(key, kind, family, fixed, Map.of(), others, expected(kind, key, fixed, Map.of(), context), label);
    }

    /** Name einer Reihe auf dem Schild: Barren/Speicherblock (Metall), Bretter (Holz). */
    private static Component label(Map<Slot, Item> slots, Slot... preferred) {
        for (Slot slot : preferred) {
            if (slots.containsKey(slot)) {
                return TcText.bold(new ItemStack(slots.get(slot)).getHoverName());
            }
        }
        return TcText.bold(new ItemStack(slots.values().iterator().next()).getHoverName());
    }

    /**
     * Bereich 1 "Linien" (TZ38D, N42): eine graue Mittellinie aus Betonstufen; Seite A (-z) Metalle und Rohstoffe, anschliessend
     * Holz, danach Rest; gegenueber Seite B (+z) alles Steinige. Jede Reihe beginnt ein Feld neben der Linie mit Knopf,
     * Druckplatte ... (kleine Glieder am Weg) und laeuft nach aussen; Reihen direkt nebeneinander, Felder ohne Luecke; fehlende
     * Glieder als Platzhalter (rot = sollte existieren, hellgrau = soll nicht). Laenger als {@link #MAX_WIDTH}: weitere
     * Linien-Paare dahinter.
     */
    public static Area families() {
        Map<String, List<Member>> raw = rawRows();
        Map<Item, String> rowOf = new HashMap<>();
        raw.forEach((key, members) -> members.forEach(m -> rowOf.putIfAbsent(m.item(), key)));
        List<MaterialRow> materialRows = materialRows(rowOf);
        Set<Item> claimed = new HashSet<>();
        List<Item> hidden = new ArrayList<>();
        for (MaterialRow row : materialRows) {
            claimed.addAll(row.slots().values());
            claimed.addAll(row.tops().values());
            claimed.addAll(row.others());
            claimed.addAll(row.hidden());
            hidden.addAll(row.hidden());
        }
        List<Map<String, List<Member>>> families = familyRows(raw, claimed);
        Set<Item> inFamilies = new HashSet<>();
        families.forEach(family -> family.values().forEach(members -> members.forEach(m -> inFamilies.add(m.item()))));

        // Holz: abgeleitete Hoelzer (Lohenholz aus Karmesin, Nihilit-Bretter aus Eschenholz) stehen direkt hinter ihrem Grundholz;
        // Anteil der eigenstaendigen Hoelzer je Spalte (fuer die Erwartung "was die Mehrheit hat").
        List<String> woodKeys = new ArrayList<>();
        materialRows.forEach(row -> {
            if (row.material().kind() == Kind.WOOD) {
                woodKeys.add(row.material().key());
            }
        });
        Map<String, String> baseOf = new HashMap<>();
        for (String key : woodKeys) {
            for (String other : woodKeys) {
                if (!other.equals(key) && subsequence(tokens(other), tokens(key))
                        && (!baseOf.containsKey(key) || tokens(other).size() > tokens(baseOf.get(key)).size())) {
                    baseOf.put(key, other);
                }
            }
        }
        Map<Slot, Integer> woodShare = new EnumMap<>(Slot.class);
        int woods = 0;
        for (MaterialRow row : materialRows) {
            if (row.material().kind() == Kind.WOOD && !baseOf.containsKey(row.material().key())) {
                woods++;
                row.slots().keySet().forEach(slot -> woodShare.merge(slot, 1, Integer::sum));
            }
        }
        List<RowPlan> sideA = new ArrayList<>();
        List<RowPlan> wood = new ArrayList<>();
        Map<String, List<RowPlan>> derived = new HashMap<>();
        for (MaterialRow row : materialRows) {
            List<Item> others = new ArrayList<>(row.others());
            for (Item item : row.deferred()) {
                if (!inFamilies.contains(item)) {
                    others.add(item); // E3: hat keine eigene Familien-Reihe mehr - dann doch zu den weiteren
                }
            }
            Material m = row.material();
            boolean isDerived = baseOf.containsKey(m.key());
            RowPlan plan = new RowPlan(m.key(), m.kind(), m.kind() == Kind.METAL ? m.words().getFirst() : isDerived ? baseOf.get(m.key()) : m.key(),
                    row.slots(), row.tops(), others,
                    expected(m.kind(), m.key(), row.slots(), row.tops(), new Context(m, null, false, isDerived, woodShare, woods)),
                    m.kind() == Kind.METAL ? label(row.slots(), Slot.INGOT, Slot.STORAGE) : label(row.slots(), Slot.BLOCK, Slot.PILLAR));
            if (m.kind() == Kind.METAL) {
                sideA.add(plan);
            } else if (isDerived) {
                derived.computeIfAbsent(baseOf.get(m.key()), key -> new ArrayList<>()).add(plan);
            } else {
                wood.add(plan);
            }
        }
        for (RowPlan plan : wood) {
            sideA.add(plan);
            sideA.addAll(derived.getOrDefault(plan.group(), List.of()));
        }
        // Sonderbloecke (Besitzer-Beispiele) fehlen ihrer eigenen Reihe nicht als "rot".
        Set<String> specialSources = new HashSet<>();
        SPECIAL.values().forEach(id -> specialSources.add(rowKey(Identifier.parse(id).getPath())));
        List<RowPlan> rest = new ArrayList<>();
        List<RowPlan> stone = new ArrayList<>();
        for (Map<String, List<Member>> family : families) {
            BlockState state = familyState(family);
            Kind kind = stony(state) ? Kind.STONE : Kind.REST;
            boolean first = true;
            for (Map.Entry<String, List<Member>> entry : family.entrySet()) {
                String familyKey = entry.getValue().getFirst().family();
                Component label = null;
                if (first && state != null) {
                    label = TcText.bold(new ItemStack(state.getBlock().asItem()).getHoverName());
                }
                Context context = new Context(null, state, specialSources.contains(entry.getKey()), false, Map.of(), 0);
                (kind == Kind.STONE ? stone : rest).add(familyPlan(entry.getKey(), entry.getValue(), kind, familyKey, label, context));
                first = false;
            }
        }
        sideA.addAll(rest);

        TcCanvas c = new TcCanvas();
        List<Cell> cells = new ArrayList<>();
        List<Row> rows = new ArrayList<>();
        List<Placeholder> placeholders = new ArrayList<>();
        int perPair = MAX_WIDTH - START_X;
        int pairs = Math.max(1, Math.max((sideA.size() + perPair - 1) / perPair, (stone.size() + perPair - 1) / perPair));
        int z0 = 0;
        int firstLine = -1;
        BlockState line = lineState();
        for (int p = 0; p < pairs; p++) {
            List<RowPlan> a = sideA.subList(Math.min(sideA.size(), p * perPair), Math.min(sideA.size(), (p + 1) * perPair));
            List<RowPlan> b = stone.subList(Math.min(stone.size(), p * perPair), Math.min(stone.size(), (p + 1) * perPair));
            int depthA = a.stream().mapToInt(RowPlan::length).max().orElse(0);
            int depthB = b.stream().mapToInt(RowPlan::length).max().orElse(1);
            // z0: Schildreihe A | A-Reihen (Feld 0 bei nearA) | Abstand | Linie | Abstand | B-Reihen | Schildreihe B.
            int nearA = z0 + depthA;
            int lineZ = nearA + 2;
            int nearB = lineZ + 2;
            if (firstLine < 0) {
                firstLine = lineZ;
            }
            int maxX = START_X;
            for (int side = 0; side < 2; side++) {
                List<RowPlan> plans = side == 0 ? a : b;
                int near = side == 0 ? nearA : nearB;
                int step = side == 0 ? -1 : 1;
                int x = START_X;
                for (RowPlan plan : plans) {
                    List<Slot> columns = columns(plan.kind());
                    for (int col = 0; col < columns.size(); col++) {
                        Slot slot = columns.get(col);
                        int z = near + step * col;
                        Item item = plan.fixed().get(slot);
                        if (item == null) {
                            boolean missing = plan.expected().contains(slot);
                            c.place(x, 0, z, missing ? missingState() : absentState());
                            placeholders.add(new Placeholder(new BlockPos(x, 0, z), plan.group(), slot, missing));
                            continue;
                        }
                        linePut(c, item, x, z);
                        cells.add(new Cell(plan.group(), col, item, new BlockPos(x, 0, z)));
                        Item top = plan.tops().get(slot);
                        if (top != null) {
                            List<BlockState> states = ModBlockGallery.states(top);
                            c.place(x, 1, z, states.getFirst());
                            cells.add(new Cell(plan.group(), col, top, new BlockPos(x, 1, z)));
                        }
                    }
                    for (int i = 0; i < plan.others().size(); i++) {
                        int col = columns.size() + i;
                        int z = near + step * col;
                        linePut(c, plan.others().get(i), x, z);
                        cells.add(new Cell(plan.group(), col, plan.others().get(i), new BlockPos(x, 0, z)));
                    }
                    if (plan.label() != null) {
                        // Am Reihenende, zur Linie gewandt (N42: das Feld neben der Linie bleibt frei).
                        c.sign(x, 0, near + step * plan.length(), side == 0 ? Direction.SOUTH : Direction.NORTH, plan.label());
                    }
                    rows.add(new Row(plan.group(), plan.kind(), plan.family(), x, near, step, columns, plan.fixed(), plan.tops(),
                            plan.others(), plan.expected()));
                    maxX = Math.max(maxX, x);
                    x++;
                }
            }
            for (int lx = 0; lx <= maxX + 1; lx++) {
                c.place(lx, 0, lineZ, line);
            }
            // Titel am Anfang der Linie (x 0, vor der ersten Reihe), je Seite nur, wenn sie Reihen hat.
            if (!a.isEmpty()) {
                c.place(0, 0, nearA, TcCanvas.WALL);
                c.sign(0, 0, nearA - 1, Direction.NORTH, TcText.bold(TcText.t("order.lines.a", "Metals, wood, rest")),
                        TcText.t("order.lines.a.sub", "small parts next to the line"));
            }
            if (!b.isEmpty()) {
                c.place(0, 0, nearB, TcCanvas.WALL);
                c.sign(0, 0, nearB + 1, Direction.SOUTH, TcText.bold(TcText.t("order.lines.b", "Stone & co.")),
                        TcText.t("order.lines.key", "red = missing, light gray = none"));
            }
            z0 = nearB + depthB + 1 + AISLE;
        }
        return new Area(c, cells, rows, placeholders, hidden, firstLine);
    }

    /**
     * Setzt ein Item in eine Linien-Reihe (N42-4: gesetzt statt Rahmen): Stangen und Blitzableiter aufrecht, Druckplatten und
     * Knoepfe flach auf dem Boden, Ketten senkrecht; sonst wie die Ausstellungen ({@link #put}); nur Nicht-Setzbares im Rahmen.
     */
    static void linePut(TcCanvas c, Item item, int x, int z) {
        BlockState state = lineState(item);
        if (state != null) {
            c.place(x, 0, z, state);
            return;
        }
        put(c, item, x, z);
    }

    /** Der Zustand, mit dem ein Item nur in den Linien steht (sonst null = wie ueberall): siehe {@link #linePut}. */
    static BlockState lineState(Item item) {
        if (!(item instanceof BlockItem blockItem)) {
            return null;
        }
        Block block = blockItem.getBlock();
        if (TestCentreLayout.frameOnly(block) && !(block instanceof BasePressurePlateBlock || TcContext.id(block).getPath().endsWith("_pressure_plate"))) {
            return null;
        }
        BlockState state = block.defaultBlockState();
        try {
            if (block instanceof net.minecraft.world.level.block.RodBlock || block instanceof net.minecraft.world.level.block.LightningRodBlock) {
                return withValue(state, net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, Direction.UP);
            }
            if (block instanceof ButtonBlock) {
                return withValue(withValue(state, ButtonBlock.FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR),
                        ButtonBlock.FACING, Direction.NORTH);
            }
            if (block instanceof BasePressurePlateBlock || TcContext.id(block).getPath().endsWith("_pressure_plate")
                    || block instanceof net.minecraft.world.level.block.ChainBlock) {
                return state.isRandomlyTicking() && !(block instanceof net.minecraft.world.level.block.WeatheringCopper) ? null : state;
            }
        } catch (RuntimeException e) {
            return null;
        }
        return null;
    }

    private static <T extends Comparable<T>> BlockState withValue(BlockState state,
                                                                  net.minecraft.world.level.block.state.properties.Property<T> property, T value) {
        return state.hasProperty(property) && property.getPossibleValues().contains(value) ? state.setValue(property, value) : state;
    }

    /**
     * Die Pruefung der Linien (Gametest SimpleBuilding und Integration mit allen Modulen): dicht (jedes feste Feld gefuellt,
     * Reihen direkt nebeneinander), kein Item doppelt, Platzhalterfarben passend zur Erwartung, Mittellinie und Abstand frei,
     * Stangen aufrecht, Druckplatten/Knoepfe/Ketten/Stangen gesetzt statt im Rahmen, Kupfer nur gewachst, Achtel auf ihrem Block,
     * Seite A Metalle | Holz | Rest, Seite B Stein. Liefert die Probleme (leer = gut).
     */
    public static List<String> check(Area area) {
        List<String> problems = new ArrayList<>();
        Map<BlockPos, List<TcOp>> at = new HashMap<>();
        for (TcOp op : area.canvas().ops()) {
            at.computeIfAbsent(op.pos(), pos -> new ArrayList<>()).add(op);
        }
        Map<Item, Integer> count = new HashMap<>();
        area.cells().forEach(cell -> count.merge(cell.item(), 1, Integer::sum));
        count.forEach((item, n) -> {
            if (n != 1) {
                problems.add("lines: " + TcContext.id(item) + " stands " + n + " times");
            }
        });
        Map<BlockPos, Placeholder> holders = new HashMap<>();
        area.placeholders().forEach(p -> holders.put(p.pos(), p));
        Map<String, Integer> lastX = new HashMap<>();
        String lastKind = "";
        for (Row row : area.rows()) {
            String side = row.step() < 0 ? "A" : "B";
            if (row.kind().sideA() != side.equals("A")) {
                problems.add("lines: " + row.group() + " (" + row.kind() + ") stands on side " + side);
            }
            Integer before = lastX.get(side + row.nearZ());
            if (before != null && before + 1 != row.x()) {
                problems.add("lines: gap before row " + row.group() + " (x " + before + " -> " + row.x() + ")");
            }
            lastX.put(side + row.nearZ(), row.x());
            if (side.equals("A")) {
                String kind = row.kind().name();
                if (lastKind.equals("WOOD") && kind.equals("METAL") || lastKind.equals("REST") && !kind.equals("REST")) {
                    problems.add("lines: side A is not metals | wood | rest at " + row.group());
                }
                lastKind = kind;
            }
            for (int col = 0; col < row.length(); col++) {
                BlockPos pos = new BlockPos(row.x(), 0, row.z(col));
                List<TcOp> ops = at.getOrDefault(pos, List.of());
                if (ops.isEmpty()) {
                    problems.add("lines: row " + row.group() + " has an empty field in column " + col);
                    continue;
                }
                if (col < row.columns().size()) {
                    Slot slot = row.columns().get(col);
                    Placeholder holder = holders.get(pos);
                    if (row.fixed().containsKey(slot) == (holder != null)) {
                        problems.add("lines: row " + row.group() + " " + slot + " is " + (holder == null ? "empty" : "filled and a placeholder"));
                    } else if (holder != null) {
                        boolean red = ops.stream().anyMatch(op -> op instanceof TcOp.Place place && place.state().equals(missingState()));
                        if (holder.missing() != row.expected().contains(slot) || red != holder.missing()) {
                            problems.add("lines: placeholder " + row.group() + " " + slot + " has the wrong colour");
                        }
                    }
                }
            }
            // Abstand zur Linie frei, Linie grau.
            BlockPos gap = new BlockPos(row.x(), 0, row.nearZ() - row.step());
            if (at.containsKey(gap)) {
                problems.add("lines: the field between line and row " + row.group() + " is not free");
            }
            BlockPos lineAt = new BlockPos(row.x(), 0, row.nearZ() - 2 * row.step());
            if (at.getOrDefault(lineAt, List.of()).stream().noneMatch(op -> op instanceof TcOp.Place place && place.state().equals(lineState()))) {
                problems.add("lines: no grey line next to row " + row.group());
            }
        }
        for (TcOp op : area.canvas().ops()) {
            if (op instanceof TcOp.Place place) {
                BlockState state = place.state();
                Block block = state.getBlock();
                boolean rod = block instanceof net.minecraft.world.level.block.RodBlock || block instanceof net.minecraft.world.level.block.LightningRodBlock;
                if (rod && state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)
                        && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING) != Direction.UP) {
                    problems.add("lines: the pole " + TcContext.id(block) + " is not upright");
                }
                String path = TcContext.id(block).getPath();
                if (block instanceof net.minecraft.world.level.block.WeatheringCopper && !path.startsWith("waxed_")
                        && BuiltInRegistries.BLOCK.containsKey(Identifier.fromNamespaceAndPath(TcContext.id(block).getNamespace(), "waxed_" + path))) {
                    problems.add("lines: unwaxed " + TcContext.id(block) + " stands although its waxed twin exists");
                }
            } else if (op instanceof TcOp.Frame frame && frame.stack().getItem() instanceof BlockItem blockItem
                    && lineState(frame.stack().getItem()) != null) {
                problems.add("lines: " + TcContext.id(blockItem) + " hangs in a frame although it can be placed");
            }
        }
        // Achtel auf ihrem Block (N42-7).
        for (Row row : area.rows()) {
            row.tops().forEach((slot, top) -> {
                if (!row.fixed().containsKey(slot)) {
                    problems.add("lines: octet " + TcContext.id(top) + " floats without its block");
                }
            });
        }
        return problems;
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

    /** Was der Block-Bereich einer Variante zeigt (gesetzt, gerahmt oder als gewachster Zwilling vertreten). */
    public static Set<Item> blockAreaItems(Variant variant) {
        return blockAreaItems(variant == Variant.LINES ? families() : null);
    }

    /** Wie oben mit schon geplanten Linien ({@code null} = Variante "blocks"). */
    public static Set<Item> blockAreaItems(Area lines) {
        Set<Item> out = new HashSet<>();
        if (lines != null) {
            // Ausgelassene ungewachste Kupfer-Zwillinge der Familie stehen im Rahmen bei Items je Mod (TZ22-1: jeder Registereintrag
            // ist zu sehen; N42-6: nicht gesetzt, "ungewachste optional in Rahmen").
            lines.cells().forEach(cell -> out.add(cell.item()));
        } else {
            for (String ns : namespaces()) {
                out.addAll(blocks(ns));
            }
        }
        return out;
    }

    /** Items je Mod, die {@code skip} (der Block-Bereich der Variante) nicht schon zeigt. */
    public static List<Item> items(String ns, Set<Item> skip) {
        List<Item> out = new ArrayList<>(items(ns));
        out.removeIf(skip::contains);
        return out;
    }

    /** Bereich "Items je Mod" der Standard-Variante. */
    public static Area items(TcContext ctx) {
        return items(ctx, blockAreaItems(DEFAULT));
    }

    /**
     * Bereich Items je Mod: Besatz-Staender (je Muster, je Material ein Eisen-Satz), dann je Mod eine Linie mit allem, was
     * {@code skip} (der Block-Bereich der Variante) nicht zeigt, dahinter ihre Mobs.
     */
    public static Area items(TcContext ctx, Set<Item> skip) {
        return items(ctx, skip, new HashSet<>(families().hidden()));
    }

    /** Wie oben; {@code framed} = in den Linien ausgelassene Kupfer-Zwillinge, die hier im Rahmen haengen statt zu stehen. */
    public static Area items(TcContext ctx, Set<Item> skip, Set<Item> framed) {
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
        // TZ38D ("kaum Luecken"): je Mod eine Reihe, die Mods wie Regalbretter nebeneinander (Wiki-Reihenfolge von links nach
        // rechts, dann das naechste Brett), ein Feld Abstand; die Mobs der Mod direkt dahinter in ihrem Abschnitt (TZ38-7).
        int perLine = MAX_WIDTH - START_X - 1;
        int x0 = START_X;
        int shelfDepth = 0;
        z += 1; // Schildreihe vor dem ersten Brett
        for (String ns : namespaces()) {
            List<Item> list = items(ns, skip);
            List<EntityType<?>> mobs = mobs(ns);
            if (list.isEmpty() && mobs.isEmpty()) {
                continue;
            }
            int lines = Math.max(1, (list.size() + perLine - 1) / perLine);
            int itemWidth = Math.min(perLine, Math.max(1, list.size()));
            int mobWidth = 0;
            int mobDepth = 0;
            for (EntityType<?> type : mobs) {
                int size = Math.max(1, (int) Math.ceil(type.getWidth()));
                mobWidth += size + 1;
                mobDepth = Math.max(mobDepth, size);
            }
            int width = 1 + Math.max(itemWidth, Math.min(perLine, mobWidth));
            int depth = lines + (mobs.isEmpty() ? 0 : 1 + mobDepth * Math.max(1, (mobWidth + perLine - 1) / perLine));
            if (x0 > START_X && x0 + width > MAX_WIDTH) {
                x0 = START_X;
                z += shelfDepth + 2;
                shelfDepth = 0;
            }
            c.wallSign(x0, 0, z, TcText.bold(TcText.lit(ModBlockGallery.modName(ns))), TcText.t("order.items.count", "%s items", list.size()),
                    TcText.t("order.items.mobcount", "%s mobs", mobs.size()));
            for (int i = 0; i < list.size(); i++) {
                int x = x0 + 1 + i % perLine;
                int iz = z + i / perLine;
                if (framed.contains(list.get(i))) {
                    c.frame(x, 0, iz, Direction.UP, new ItemStack(list.get(i)));
                } else {
                    put(c, list.get(i), x, iz);
                }
                cells.add(new Cell(ns, i, list.get(i), new BlockPos(x, 0, iz)));
            }
            // Mobs: eigener Streifen hinter der Reihe der Mod.
            int mx = x0 + 1;
            int mz = z + lines + 1;
            int depthHere = 1;
            for (EntityType<?> type : mobs) {
                int size = Math.max(1, (int) Math.ceil(type.getWidth()));
                if (mx > x0 + 1 && mx + size > x0 + width) {
                    mx = x0 + 1;
                    mz += depthHere + 1;
                    depthHere = 1;
                }
                c.mob(mx + (size - 1) / 2, 0, mz + (size - 1) / 2, 180F, BuiltInRegistries.ENTITY_TYPE.getKey(type));
                mx += size + 1;
                depthHere = Math.max(depthHere, size);
            }
            if (!mobs.isEmpty()) {
                depth = Math.max(depth, mz + depthHere - z);
            }
            shelfDepth = Math.max(shelfDepth, depth);
            x0 += width + 1;
        }
        return new Area(c, cells);
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
        return new Area(c, cells);
    }

    // ------------------------------------------------------------------ alles

    /**
     * Pruefung beider Varianten (Gametest SimpleBuilding, Integration mit allen Modulen): die Linien ({@link #check}); je
     * Variante steht jedes Item jeder Familien-Mod genau einmal im Block-Bereich oder in Items je Mod (ein in den Linien
     * ausgelassener ungewachster Kupfer-Zwilling der Familie haengt dort im Rahmen); kompakt: jeder Block einmal, Mods mit Abstand.
     */
    public static List<String> problems(TcContext ctx) {
        Area lines = families();
        List<String> problems = new ArrayList<>(check(lines));
        Area compact = compact();
        for (Variant variant : Variant.values()) {
            Area blocks = variant == Variant.LINES ? lines : compact;
            Set<Item> skip = blockAreaItems(variant == Variant.LINES ? lines : null);
            Area items = items(ctx, skip, new HashSet<>(lines.hidden()));
            Map<Item, Integer> shown = new HashMap<>();
            blocks.cells().forEach(cell -> shown.merge(cell.item(), 1, Integer::sum));
            items.cells().forEach(cell -> shown.merge(cell.item(), 1, Integer::sum));
            for (String ns : namespaces()) {
                for (Item item : items(ns)) {
                    int n = shown.getOrDefault(item, 0);
                    if (n != 1) {
                        problems.add(variant.id() + ": " + TcContext.id(item) + " stands " + n + " times in the order areas");
                    }
                }
            }
        }
        Map<String, int[]> boxes = new LinkedHashMap<>();
        for (Cell cell : compact.cells()) {
            boxes.merge(cell.group(), new int[] {cell.pos().getX(), cell.pos().getZ(), cell.pos().getX(), cell.pos().getZ()},
                    (a, b) -> new int[] {Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.max(a[2], b[2]), Math.max(a[3], b[3])});
        }
        List<Map.Entry<String, int[]>> list = new ArrayList<>(boxes.entrySet());
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                int[] a = list.get(i).getValue();
                int[] b = list.get(j).getValue();
                if (a[0] <= b[2] + 1 && b[0] <= a[2] + 1 && a[1] <= b[3] + 1 && b[1] <= a[3] + 1) {
                    problems.add("compact: " + list.get(i).getKey() + " touches " + list.get(j).getKey());
                }
            }
        }
        for (TcCanvas canvas : List.of(lines.canvas(), compact.canvas())) {
            if (canvas.width() > MAX_WIDTH + 1) {
                problems.add("an order area is " + canvas.width() + " wide");
            }
        }
        return problems;
    }

    /** Die Fehlliste (rote Platzhalter) der Linien: Reihe, Art, Spalte - fuer Log, Tabelle und Vorschau. */
    public static List<String> missingList(Area lines) {
        List<String> out = new ArrayList<>();
        for (Placeholder p : lines.placeholders()) {
            if (p.missing()) {
                out.add(p.group() + "\t" + p.slot().name().toLowerCase(java.util.Locale.ROOT));
            }
        }
        return out;
    }

    /** Die Bereiche einer Variante untereinander (fuer {@code /testcenter order build [lines|blocks]}), mit Gang dazwischen. */
    public static TcCanvas all(TcContext ctx, Variant variant) {
        TcCanvas c = new TcCanvas();
        int z = 0;
        Area lines = families();
        TcCanvas blocks = variant == Variant.LINES ? lines.canvas() : compact().canvas();
        Set<Item> skip = blockAreaItems(variant == Variant.LINES ? lines : null);
        for (TcCanvas area : List.of(blocks, items(ctx, skip, new HashSet<>(lines.hidden())).canvas())) {
            if (area.ops().isEmpty()) {
                continue;
            }
            c.append(area, 0, z);
            z += area.depth() + TestCentreLayout.CORRIDOR;
        }
        return c;
    }
}
