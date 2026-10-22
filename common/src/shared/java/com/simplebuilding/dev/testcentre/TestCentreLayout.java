package com.simplebuilding.dev.testcentre;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.tweaks.block.PadBlock;
import com.simplebuilding.tweaks.block.TweaksBlocks;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Plant die ganze Testzentrale: welche Abschnitte, wo sie liegen, was darin steht und welche
 * Mod-Items keinen Platz gefunden haben.
 *
 * <p>Die Abschnitte liegen nebeneinander in +x, eine Reihe hoechstens {@link #MAX_ROW_WIDTH} breit; vor
 * jeder Reihe verlaeuft ein Gang in -z. Der Ursprung ist die Standhoehe am Anfang des ersten Gangs.
 *
 * <h2>Erweitern</h2>
 * Neuer Abschnitt: Methode in {@link TestCentreSections} oder {@link FeatureStations}, Id in
 * {@link #SECTION_IDS} und Eintrag in {@link #plan}. Neues Item: meist nichts zu tun, wenn es in einer Tab-Zeile steht, die ein Abschnitt
 * liest; sonst erscheint es unter "unsorted" und der Abdeckungstest nennt es.
 */
public final class TestCentreLayout {

    /** Abschnitt mit der Modul-Ausstellung (Stationen und Register aller Module der Familie), nur wenn es Module gibt. */
    public static final String MODULES_SECTION = "modules";

    /**
     * Bezirke (TZ22-5, Besitzer 22.10.: "kompakt und uebersichtlich wie beim Holzbereich"): jede Themengruppe beginnt eine
     * eigene Reihe, eine Reihe ist hoechstens {@link #MAX_ROW_WIDTH} breit. Die Reihenfolge ist die in der Welt. Ein spaeterer
     * Bezirk "Strukturen" (TZ22-3) kommt als eigener Eintrag vor den Katalog.
     */
    public static final List<List<String>> DISTRICTS = List.of(
            // Eingang: Steuerwand, Werkzeuge und Bauen.
            List.of("controls", "tools", "chisel", "planning", "mining", "enchants"),
            // Zustaende, Umwandlungen, Ablegen, Buecher.
            List.of("states", "inworld", "templates", "placeables", "books"),
            // Baubloecke und Material.
            List.of("blocks", "woodwork", "simpleend", "chess", "astral", "lightroom", "ores"),
            // Technik und Lager.
            List.of("storage", "devices", "machines", "crucible", "music", "vehicles"),
            // Kampf, Bewegung, Vorraete.
            List.of("armour", "archery", "arrows", "sinkdamper", "tweaks", "deathvault", "respawn", "food", "materials"),
            // Die Module der Familie (Modulaufteilung 3c, TZ22-1); nur mit Modulen.
            List.of(MODULES_SECTION),
            // Mobs of every loaded mod (Queue N43); empty without such mods.
            List.of("mobs"),
            // Item-orientiert: je Kreativ-Tab eine Wand (TabBrowser), zaehlt nicht fuer die Abdeckung.
            List.of("tab_tools", "tab_combat", "tab_building_blocks", "tab_materials", "tab_food", "tab_functional", "tab_pads", "tab_arrows"),
            // Was noch keinen Abschnitt hat.
            List.of("gallery", "unsorted"));

    /** Reihenfolge der Abschnitte = Reihenfolge in der Welt (ohne {@link #MODULES_SECTION}, siehe {@link #sectionIds()}). */
    public static final List<String> SECTION_IDS = DISTRICTS.stream().flatMap(List::stream)
            .filter(id -> !id.equals(MODULES_SECTION)).toList();

    /**
     * Die geplanten Abschnitte: {@link #SECTION_IDS}, und vor "mobs" der Abschnitt {@link #MODULES_SECTION}, sobald ein
     * Modul der Familie Inhalte hat oder Stationen angemeldet sind (26.2: keine Module, unveraendert).
     */
    public static List<String> sectionIds() {
        if (!hasModules()) {
            return SECTION_IDS;
        }
        List<String> ids = new ArrayList<>(SECTION_IDS);
        ids.add(ids.indexOf("mobs"), MODULES_SECTION);
        return List.copyOf(ids);
    }

    /** Ob es Modul-Inhalte gibt: angemeldete Stationen oder Items eines Familien-Namensraums ausser SimpleBuilding. */
    static boolean hasModules() {
        if (!ModuleStations.all().isEmpty()) {
            return true;
        }
        for (net.minecraft.resources.Identifier id : BuiltInRegistries.ITEM.keySet()) {
            if (TcContext.isFamily(id) && !TcContext.isMod(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Mod-Items und -Bloecke, die bewusst NICHT in der Zentrale stehen, mit Grund. Jede Ausnahme muss
     * hier stehen - der Abdeckungstest liest genau diese Liste.
     */
    public static final Map<String, String> EXCLUDED;

    static {
        Map<String, String> excluded = new LinkedHashMap<>(Map.of(
            "simplebuilding:creative_spacer", "Platzhalter der Kreativ-Tabs, kein Spielinhalt",
            "simplebuilding:reinforced_piston_head", "technischer Block (Kopf der verstaerkten Kolben), kein Item",
            "simplebuilding:netherite_piston_head", "technischer Block (Kopf des Netherit-Kolbens), kein Item",
            "simplebuilding:enderite_piston_head", "technischer Block (Kopf des Enderit-Kolbens), kein Item",
            "simplebuilding:funny_stick", "Easter Egg, bewusst versteckt (Spoiler in docs/SIMPLETWEAKS-UEBERNAHME.md)"));
        if (com.simplebuilding.version.McVersion.FIRES) {
            excluded.put("simplebuilding:sulfur_fire", "Feuer ohne Item: entsteht mit Feuerzeug auf Schwefelbloecken");
            excluded.put("simplebuilding:copper_fire", "Feuer ohne Item: entsteht mit Feuerzeug auf Kupferbloecken");
            excluded.put("simplebuilding:sulfur_wall_torch", "Wandvariante der Schwefelfackel (N58), gleiches Item");
        }
        EXCLUDED = Map.copyOf(excluded);
    }

    /**
     * Technische Bloecke ohne Item, die die Regel erkennt (TZ22-1): Wandvarianten ({@code x_wall_torch} zu {@code x_torch},
     * Schilder, Koepfe, Banner) und Blumentoepfe ({@code potted_x}); jeweils steht das Item der Grundform in der Zentrale.
     * Sonst null - dann braucht der Block einen Eintrag in {@link #EXCLUDED}.
     */
    public static String technical(Block block) {
        if (block.asItem() != Items.AIR) {
            return null;
        }
        net.minecraft.resources.Identifier id = TcContext.id(block);
        String path = id.getPath();
        if (path.contains("wall_")) {
            net.minecraft.resources.Identifier base = net.minecraft.resources.Identifier.fromNamespaceAndPath(id.getNamespace(), path.replace("wall_", ""));
            if (BuiltInRegistries.BLOCK.containsKey(base) && BuiltInRegistries.BLOCK.getValue(base).asItem() != Items.AIR) {
                return "Wandvariante von " + base + " (gleiches Item)";
            }
        }
        if (path.startsWith("potted_")) {
            return "Blumentopf mit Pflanze (Topf + Pflanze, kein eigenes Item)";
        }
        return null;
    }

    /**
     * Mod-Bloecke, die nur im Rahmen stehen und nirgends gesetzt werden: Pads mit Wirkung auf die
     * Umgebung (Elytra-Pad III gibt auf 128 x 128 Bloecken Elytren, Teleporter versetzen, Chunk-Loader
     * erzwingen Chunks, Kupferplatten altern) und die alten Stufen (werden beim ersten Tick umgewandelt).
     * Die Tweaks-Station setzt je Familie ein Vorfuehrstueck von Hand.
     */
    public static boolean frameOnly(Block block) {
        return block instanceof PadBlock || TweaksBlocks.legacy().contains(block);
    }

    /** Breite des Ausgabe-Knopfs vorn links an jeder Station (Befehlsblock + eine Spalte Luft). */
    public static final int KIOSK_WIDTH = 2;
    /** Tiefe des Befehlsblocks am Ausgabe-Knopf; der Knopf selbst sitzt davor zum Gang hin. */
    public static final int KIOSK_Z = 1;
    /** Erhoehen, wenn sich der Bau-Code (nicht die Planung) aendert: dann bauen alte Welten neu. */
    static final int BUILDER_VERSION = 2;

    public static final int MAX_ROW_WIDTH = 120;
    public static final int GAP = 4;
    public static final int CORRIDOR = 7;

    /** Ein geplanter Abschnitt: Lage relativ zum Ursprung, Groesse, Schritte in absoluten Koordinaten. */
    public record Section(String id, BlockPos offset, int width, int depth, int height, List<TcOp> ops) {
        public BoundingBox box(BlockPos origin) {
            BlockPos min = origin.offset(offset);
            return new BoundingBox(min.getX(), min.getY() - 1, min.getZ(),
                    min.getX() + width - 1, min.getY() + height + 2, min.getZ() + depth - 1);
        }
    }

    /** Die fertige Planung. */
    public record Plan(BlockPos origin, List<Section> sections, Map<String, BlockPos> anchors, BoundingBox bounds,
                       Set<Item> coveredItems, Set<Block> coveredBlocks, List<Item> leftovers,
                       Map<String, TestCentreKits.Kit> kits) {

        /**
         * Fingerabdruck der Planung (Bau-Version, jeder Schritt mit Lage, Block, Items, Befehl). Er steht
         * neben dem Ursprung in der Markierungsdatei; weicht er beim Betreten ab, ist die Welt aelter als
         * der Code und die Zentrale wird neu gebaut. Nur stabile Texte gehen ein (Ids, Zahlen, Befehle,
         * Uebersetzungsschluessel) - nichts, was von Objekt-Identitaeten abhaengt.
         */
        public String fingerprint() {
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
            StringBuilder text = new StringBuilder("v").append(BUILDER_VERSION).append('\n');
            for (Section section : sections) {
                text.append(section.id()).append('@').append(section.offset().toShortString()).append('\n');
                for (TcOp op : section.ops()) {
                    text.append(describe(op)).append('\n');
                }
            }
            byte[] hash = digest.digest(text.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 12; i++) {
                hex.append(String.format("%02x", hash[i]));
            }
            return hex.toString();
        }

        public Section section(String id) {
            for (Section section : sections) {
                if (section.id().equals(id)) {
                    return section;
                }
            }
            throw new IllegalArgumentException("unknown test centre section " + id);
        }

        /** Eine Zeile je Abschnitt: Lage, Groesse, Rahmen/Staender - fuer Log und Doku. */
        public String summary() {
            long allFrames = sections.stream().flatMap(section -> section.ops().stream()).filter(TcOp::spawnsFrame).count();
            long allStands = sections.stream().flatMap(section -> section.ops().stream()).filter(op -> op instanceof TcOp.Stand).count();
            StringBuilder out = new StringBuilder("test centre " + (bounds.getXSpan()) + "x" + bounds.getZSpan()
                    + " blocks, " + sections.size() + " sections, " + allFrames + " frames, " + allStands + " stands:");
            for (Section section : sections) {
                long frames = section.ops().stream().filter(TcOp::spawnsFrame).count();
                long stands = section.ops().stream().filter(op -> op instanceof TcOp.Stand).count();
                out.append(String.format("%n  %-10s at +%d/+%d, %dx%dx%d, %d frames, %d stands", section.id(),
                        section.offset().getX(), section.offset().getZ(), section.width(), section.depth(), section.height(),
                        frames, stands));
            }
            return out.toString();
        }

        /** Wo ein Spieler nach dem Bau abgesetzt wird: im ersten Gang, Blick auf die Abschnitte. */
        public BlockPos entrance() {
            return origin.offset(2, 0, -3);
        }
    }

    private TestCentreLayout() {
    }

    /** Eine Zeile je Schritt fuer den Fingerabdruck. */
    static String describe(TcOp op) {
        String at = op.getClass().getSimpleName() + " " + op.pos().toShortString() + " ";
        return at + switch (op) {
            case TcOp.Place place -> place.state().toString();
            case TcOp.Frame frame -> frame.facing() + " " + stackText(frame.stack());
            case TcOp.OctantFrame frame -> frame.facing() + " " + frame.cornerA().toShortString() + " "
                    + frame.cornerB().toShortString() + " " + frame.shape();
            case TcOp.BlueprintFrame frame -> frame.facing() + " " + frame.cornerA().toShortString() + " "
                    + frame.cornerB().toShortString() + " " + frame.table().toShortString();
            case TcOp.Stand stand -> stand.yaw() + " " + stand.gear().stream().map(TestCentreLayout::stackText).toList()
                    + (stand.dummy() ? " dummy" : "");
            case TcOp.Sign sign -> sign.facing() + " " + sign.lines().stream().map(TestCentreLayout::componentText).toList();
            case TcOp.Fill fill -> fill.contents().stream().map(TestCentreLayout::stackText).toList().toString();
            case TcOp.Command command -> command.facing() + " " + command.command() + " "
                    + command.label().stream().map(TestCentreLayout::componentText).toList();
            case TcOp.Hammock hammock -> hammock.dx() + "," + hammock.dz();
        };
    }

    private static String stackText(ItemStack stack) {
        return stack.getCount() + "x" + BuiltInRegistries.ITEM.getKey(stack.getItem()) + "/" + stack.getComponentsPatch().size();
    }

    private static String componentText(Component component) {
        return component.getContents() instanceof TranslatableContents translatable
                ? translatable.getKey() + "/" + translatable.getArgs().length : component.getString();
    }

    public static Plan plan(HolderLookup.Provider lookup, BlockPos origin) {
        TcContext ctx = new TcContext(lookup);
        Map<String, Function<TcContext, TcCanvas>> builders = new LinkedHashMap<>();
        builders.put("armour", TestCentreSections::armour);
        builders.put("books", TestCentreSections::books);
        builders.put("tools", TestCentreSections::tools);
        builders.put("states", FeatureStations::states);
        builders.put("storage", TestCentreSections::storage);
        builders.put("food", TestCentreSections::food);
        builders.put("materials", TestCentreSections::materials);
        builders.put("placeables", FeatureStations::placeables);
        // Auf Linien ohne Befiederungstisch (26.2) ein leerer Abschnitt ohne Breite.
        builders.put("arrows", TestCentreSections::arrows);
        // Pfeil-Station mit Trainingspuppen (2026-10-02); auf 26.2 ebenfalls leer.
        builders.put("archery", FeatureStations::archery);
        // Musik-Station (2026-10-03); auf 26.2 leer.
        builders.put("music", FeatureStations::music);
        // Schmelztiegel-Station (Crucible P6); auf 26.2 leer.
        builders.put("crucible", FeatureStations::crucible);
        // Schach-Station (2026-10-06); auf 26.2 leer.
        builders.put("chess", FeatureStations::chess);
        // Astral-Verzauberung (Queue N27); auf 26.2 leer.
        builders.put("astral", FeatureStations::astralEnchanting);
        // Holzwerk-Station (2026-10-09); auf 26.2 leer.
        builders.put("woodwork", FeatureStations::woodwork);
        // Fahrzeug-Station (Queue N19/N23); auf 26.2 leer.
        // Simple-End-Station (2026-10-12); leer ohne das Modul.
        builders.put("simpleend", FeatureStations::simpleEnd);
        builders.put("vehicles", FeatureStations::vehicles);
        builders.put("chisel", TestCentreSections::chisel);
        builders.put("inworld", TestCentreSections::inWorld);
        builders.put("templates", TestCentreSections::templates);
        builders.put("blocks", TestCentreSections::blocks);
        builders.put("lightroom", TestCentreSections::lightRoom);
        builders.put("machines", TestCentreSections::machines);
        builders.put("ores", TestCentreSections::ores);
        builders.put("planning", TestCentreSections::planning);
        builders.put("mining", TestCentreSections::mining);
        builders.put("enchants", FeatureStations::enchants);
        builders.put("sinkdamper", FeatureStations::sinkDamper);
        builders.put("tweaks", TweaksStation::build);
        // Death Vault + Soulbound (Simple Tools, 2026-10-12); leer ohne Simple Tools.
        builders.put("deathvault", FeatureStations::deathVault);
        builders.put("respawn", FeatureStations::respawn);
        builders.put("devices", TestCentreSections::devices);
        builders.put("mobs", MobStations::build);
        List<String> sectionIds = sectionIds();

        Map<String, TcCanvas> canvases = new LinkedHashMap<>();
        for (Map.Entry<String, Function<TcContext, TcCanvas>> entry : builders.entrySet()) {
            TcCanvas canvas = entry.getValue().apply(ctx);
            dropPlacedFrames(canvas);
            canvases.put(entry.getKey(), canvas);
        }

        // Modul-Ausstellung (TZ22-1): je Modul der Familie seine Stationen und alles Weitere aus seinem Register, ohne was
        // eine Station schon sichtbar zeigt (TZ22-2: nichts doppelt).
        if (sectionIds.contains(MODULES_SECTION)) {
            Set<Item> visible = new java.util.HashSet<>();
            for (TcCanvas canvas : canvases.values()) {
                visible(canvas.ops(), visible);
            }
            canvases.put(MODULES_SECTION, TestCentreSections.modules(ModuleStations.all(), visible));
        }

        // Abdeckung ohne "unsorted" und ohne die Steuerwand (die zeigt keine Mod-Inhalte).
        Set<Item> coveredItems = new LinkedHashSet<>();
        Set<Block> coveredBlocks = new LinkedHashSet<>();
        for (TcCanvas canvas : canvases.values()) {
            collect(canvas.ops(), coveredItems, coveredBlocks);
        }
        List<Item> leftovers = new ArrayList<>();
        for (Item item : TestCentreSections.sortedModItems()) {
            if (!coveredItems.contains(item) && !EXCLUDED.containsKey(TcContext.id(item).toString())) {
                leftovers.add(item);
            }
        }
        canvases.put("unsorted", TestCentreSections.unsorted(leftovers));

        // Galerie: Mod-Bloecke, die bisher nur als Rahmen (oder gar nicht) gesetzt sind.
        List<Block> unplaced = new ArrayList<>();
        for (Block block : modBlocks()) {
            if (block.asItem() != Items.AIR && !coveredBlocks.contains(block) && !frameOnly(block)
                    && !EXCLUDED.containsKey(TcContext.id(block).toString())) {
                unplaced.add(block);
            }
        }
        TcCanvas gallery = TestCentreSections.gallery(unplaced);
        collect(gallery.ops(), coveredItems, coveredBlocks);
        canvases.put("gallery", gallery);

        // Item-orientierter Rundgang: je Tab eine Wand. Erst nach der Abdeckung, damit jedes Item
        // weiterhin eine Station braucht, in der man es ausprobieren kann.
        for (com.simplebuilding.items.ModItemGroupsContent.Tab tab : com.simplebuilding.items.ModItemGroupsContent.Tab.values()) {
            canvases.put(TabBrowser.sectionId(tab), TabBrowser.build(ctx, tab));
        }

        // Ausgabe-Knopf je Station: Kit aus dem, was die Station zeigt; die Station rueckt dafuer nach rechts.
        Map<String, TestCentreKits.Kit> kits = new LinkedHashMap<>();
        for (Map.Entry<String, TcCanvas> entry : canvases.entrySet()) {
            TestCentreKits.Kit kit = TestCentreKits.of(ctx, entry.getKey(), entry.getValue().ops());
            if (kit.isEmpty()) {
                continue;
            }
            kits.put(entry.getKey(), kit);
            entry.setValue(withKiosk(entry.getKey(), entry.getValue()));
        }

        // Erste Runde: Steuerwand mit Platzhaltern, nur fuer ihre Breite.
        canvases.put("controls", TestCentreSections.controls(controls(origin, Map.of(), null, sectionIds)));
        Map<String, BlockPos> offsets = pack(canvases, sectionIds);

        Map<String, BlockPos> anchors = new LinkedHashMap<>();
        for (Map.Entry<String, TcCanvas> entry : canvases.entrySet()) {
            BlockPos offset = offsets.get(entry.getKey());
            entry.getValue().anchors().forEach((name, pos) -> anchors.put(name, origin.offset(offset).offset(pos)));
        }
        BoundingBox bounds = bounds(origin, canvases, offsets);
        // Zweite Runde: jetzt mit den echten Koordinaten.
        canvases.put("controls", TestCentreSections.controls(controls(origin, anchors, bounds, sectionIds)));

        List<Section> sections = new ArrayList<>();
        for (String id : sectionIds) {
            TcCanvas canvas = canvases.get(id);
            BlockPos offset = offsets.get(id);
            BlockPos absolute = origin.offset(offset);
            List<TcOp> ops = new ArrayList<>();
            for (TcOp op : canvas.ops()) {
                ops.add(op.moved(absolute));
            }
            sections.add(new Section(id, offset, canvas.width(), canvas.depth(), canvas.height(), ops));
        }
        return new Plan(origin, sections, anchors, bounds, coveredItems, coveredBlocks, leftovers, kits);
    }

    /** Befehl des Ausgabe-Knopfs einer Station. */
    public static String giveCommand(String section) {
        return "sbtestcentre give " + section + " @p";
    }

    /** Die Station mit Ausgabe-Knopf vorn links: Befehlsblock auf Glas, Knopf zum Gang, Schild darueber. */
    private static TcCanvas withKiosk(String id, TcCanvas station) {
        TcCanvas c = new TcCanvas();
        c.command(0, 1, KIOSK_Z, Direction.NORTH, giveCommand(id),
                TcText.t("kiosk.give", "Give items"), TcText.t("section." + id, TestCentreSections.pretty(id)));
        c.append(station, KIOSK_WIDTH);
        return c;
    }

    /**
     * TZ22-2 (Besitzer 22.10.: "schon gesetzte Dinge bekommen keinen zusaetzlichen Rahmen"): Rahmen eines Stapels ohne
     * Komponenten, dessen Item dieselbe Station schon gesetzt zeigt - als Block oder abgelegt (Fuellen eines Blocks, der kein
     * Behaelter ist: Vorlage, Kleinteile, Hornhalter, Buendel) -, fallen weg.
     */
    static void dropPlacedFrames(TcCanvas canvas) {
        Set<Item> placed = shownPlaced(canvas.ops());
        canvas.ops().removeIf(op -> op instanceof TcOp.Frame frame && frame.stack().getComponentsPatch().isEmpty()
                && placed.contains(frame.stack().getItem()));
    }

    /** Was eine Schrittliste gesetzt zeigt: Bloecke (ohne das Geruest) und der Inhalt abgelegter Stuecke. */
    public static Set<Item> shownPlaced(List<TcOp> ops) {
        Map<BlockPos, net.minecraft.world.level.block.state.BlockState> at = new java.util.HashMap<>();
        Set<Item> placed = new java.util.HashSet<>();
        for (TcOp op : ops) {
            if (op instanceof TcOp.Place place) {
                at.put(place.pos(), place.state());
                Block block = place.state().getBlock();
                if (block.asItem() != Items.AIR && !TestCentreKits.INFRASTRUCTURE.contains(block)) {
                    placed.add(block.asItem());
                }
            }
        }
        for (TcOp op : ops) {
            if (op instanceof TcOp.Fill fill && at.containsKey(fill.pos()) && !Exhibits.container(at.get(fill.pos()))) {
                fill.contents().forEach(stack -> placed.add(stack.getItem()));
            }
        }
        placed.remove(Items.AIR);
        return placed;
    }

    /** Was eine Schrittliste sichtbar zeigt: gesetzt, abgelegt, im Rahmen, am Staender (Behaelterinhalte nicht). */
    static void visible(List<TcOp> ops, Set<Item> into) {
        into.addAll(shownPlaced(ops));
        for (TcOp op : ops) {
            switch (op) {
                case TcOp.Frame frame -> into.add(frame.stack().getItem());
                case TcOp.OctantFrame frame -> into.add(ModItems.OCTANT);
                case TcOp.BlueprintFrame frame -> into.add(ModItems.BLUEPRINT);
                case TcOp.Stand stand -> stand.gear().forEach(stack -> into.add(stack.getItem()));
                default -> {
                }
            }
        }
        into.remove(Items.AIR);
    }

    /** Welche Items und Bloecke eine Schrittliste zeigt. */
    static void collect(List<TcOp> ops, Set<Item> items, Set<Block> blocks) {
        for (TcOp op : ops) {
            switch (op) {
                case TcOp.Place place -> {
                    blocks.add(place.state().getBlock());
                    Item item = place.state().getBlock().asItem();
                    if (item != Items.AIR) {
                        items.add(item);
                    }
                }
                case TcOp.Frame frame -> items.add(frame.stack().getItem());
                case TcOp.OctantFrame frame -> items.add(ModItems.OCTANT);
                case TcOp.BlueprintFrame frame -> items.add(ModItems.BLUEPRINT);
                case TcOp.Stand stand -> {
                    stand.gear().forEach(stack -> items.add(stack.getItem()));
                    if (stand.dummy()) {
                        items.add(ModItems.STRAW_ARMOR_STAND);
                        items.add(ModItems.TRAINING_DUMMY);
                    }
                }
                case TcOp.Fill fill -> fill.contents().forEach(stack -> items.add(stack.getItem()));
                case TcOp.Sign sign -> {
                }
                case TcOp.Command command -> {
                }
                case TcOp.Hammock hammock -> {
                }
            }
        }
        items.remove(Items.AIR);
    }

    /**
     * Offsets je Abschnitt: Reihen in +x, eine neue Reihe (mit Gang davor) fuer jeden Bezirk ({@link #DISTRICTS}) und bei
     * Ueberlauf. Leere Abschnitte (ohne Schritte) nehmen keinen Platz ein.
     */
    private static Map<String, BlockPos> pack(Map<String, TcCanvas> canvases, List<String> sectionIds) {
        Map<String, BlockPos> offsets = new LinkedHashMap<>();
        int x = 0;
        int z = 0;
        int rowDepth = 0;
        for (List<String> district : DISTRICTS) {
            if (x > 0) {
                z += rowDepth + CORRIDOR;
                x = 0;
                rowDepth = 0;
            }
            for (String id : district) {
                if (!sectionIds.contains(id)) {
                    continue;
                }
                TcCanvas canvas = canvases.get(id);
                if (x > 0 && x + canvas.width() > MAX_ROW_WIDTH) {
                    z += rowDepth + CORRIDOR;
                    x = 0;
                    rowDepth = 0;
                }
                offsets.put(id, new BlockPos(x, 0, z));
                if (canvas.ops().isEmpty()) {
                    continue;
                }
                x += canvas.width() + GAP;
                rowDepth = Math.max(rowDepth, canvas.depth());
            }
        }
        return offsets;
    }

    private static BoundingBox bounds(BlockPos origin, Map<String, TcCanvas> canvases, Map<String, BlockPos> offsets) {
        int maxX = 0;
        int maxZ = 0;
        int maxY = 0;
        for (Map.Entry<String, TcCanvas> entry : canvases.entrySet()) {
            BlockPos offset = offsets.get(entry.getKey());
            maxX = Math.max(maxX, offset.getX() + entry.getValue().width());
            maxZ = Math.max(maxZ, offset.getZ() + entry.getValue().depth());
            maxY = Math.max(maxY, entry.getValue().height());
        }
        return new BoundingBox(origin.getX() - 3, origin.getY() - 1, origin.getZ() - CORRIDOR - 1,
                origin.getX() + maxX + 3, origin.getY() + maxY + 4, origin.getZ() + maxZ + 3);
    }

    /** Die Knoepfe der Steuerwand; Koordinaten sind absolut, damit die Befehlsbloecke ohne Zustand auskommen. */
    static List<TestCentreSections.Control> controls(BlockPos origin, Map<String, BlockPos> anchors, BoundingBox bounds,
                                                     List<String> sectionIds) {
        String at = origin.getX() + " " + origin.getY() + " " + origin.getZ();
        List<TestCentreSections.Control> out = new ArrayList<>();
        out.add(new TestCentreSections.Control("sbtestcentre build " + at,
                TcText.t("controls.rebuild", "Rebuild all"), TcText.t("controls.rebuild.sub", "whole centre")));
        for (String id : sectionIds) {
            if (id.equals("controls")) {
                continue;
            }
            out.add(new TestCentreSections.Control("sbtestcentre section " + id + " " + at,
                    TcText.t("controls.section", "Rebuild"), TcText.t("section." + id, TestCentreSections.pretty(id))));
        }
        out.add(new TestCentreSections.Control("sbtestcentre kit @p", TcText.t("controls.kit", "Give kit"), Component.empty()));
        out.add(new TestCentreSections.Control("time set day", TcText.t("controls.day", "Day"), Component.empty()));
        out.add(new TestCentreSections.Control("time set midnight", TcText.t("controls.night", "Night"), Component.empty()));
        out.add(new TestCentreSections.Control("weather clear", TcText.t("controls.clear", "Clear weather"), Component.empty()));
        out.add(new TestCentreSections.Control("weather rain", TcText.t("controls.rain", "Rain"), Component.empty()));
        out.add(new TestCentreSections.Control("weather thunder", TcText.t("controls.thunder", "Thunder"),
                TcText.t("controls.thunder.sub", "iron pole test")));
        out.add(new TestCentreSections.Control("gamemode creative @p", TcText.t("controls.creative", "Creative"), Component.empty()));
        out.add(new TestCentreSections.Control("gamemode survival @p", TcText.t("controls.survival", "Survival"), Component.empty()));
        BlockPos spawn = anchors.getOrDefault("mob_spawn", origin);
        String spawnAt = spawn.getX() + " " + spawn.getY() + " " + spawn.getZ();
        for (String mob : List.of("zombie", "skeleton", "creeper")) {
            out.add(new TestCentreSections.Control("summon minecraft:" + mob + " " + spawnAt,
                    TcText.t("controls.spawn", "Spawn"), TcText.t("controls.spawn." + mob, TestCentreSections.pretty(mob))));
        }
        String area = bounds == null ? "distance=..1" : "x=" + bounds.minX() + ",y=" + bounds.minY() + ",z=" + bounds.minZ()
                + ",dx=" + (bounds.maxX() - bounds.minX()) + ",dy=" + (bounds.maxY() - bounds.minY()) + ",dz=" + (bounds.maxZ() - bounds.minZ());
        out.add(new TestCentreSections.Control("kill @e[type=!minecraft:player,type=!minecraft:armor_stand,"
                + "type=!minecraft:item_frame,type=!minecraft:glow_item_frame," + area + "]",
                TcText.t("controls.kill", "Remove mobs"), TcText.t("controls.kill.sub", "and dropped items")));
        return out;
    }

    /** Alle Mod-Bloecke im Register (fuer den Abdeckungstest). */
    public static List<Block> modBlocks() {
        List<Block> out = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (TcContext.isMod(TcContext.id(block))) {
                out.add(block);
            }
        }
        return out;
    }
}
