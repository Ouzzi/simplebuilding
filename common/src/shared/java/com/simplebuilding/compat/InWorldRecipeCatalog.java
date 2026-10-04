package com.simplebuilding.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplebuilding.util.InWorldTransformations;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/**
 * The mod's in-world transformations as recipe entries for a recipe viewer (JEI).
 *
 * <p><b>Single source of truth.</b> Everything here is read from
 * {@link InWorldTransformations#describe()} - the very JSON the wiki's "In-world transformation"
 * category is generated from - plus {@link InWorldTransformations#reshapePairs}, which uses the
 * sledgehammer's own reshape rule. Nothing is copied, so JEI, wiki and game cannot drift apart.
 *
 * <p>Deliberately free of any JEI class: the JEI plugin (its own source tree, compiled only by the
 * loaders JEI exists for) turns these entries into categories, and a server game test
 * ({@code InWorldExportTests#jeiCatalogCoversEveryInWorldEntry}) checks without JEI that every
 * exported entry arrives here.
 */
public final class InWorldRecipeCatalog {

    private InWorldRecipeCatalog() {
    }

    /** One JEI category per section of {@link InWorldTransformations#describe()}. */
    public enum Kind {
        MACHINE_UPGRADE("sledgehammerUpgrade", "machine_upgrade"),
        RESHAPE("sledgehammerReshape", "reshape"),
        DIAMOND_CRUSH("diamondCrush", "diamond_crush"),
        CHISEL("chisel", "chisel"),
        SHEAR_WOOL("shearWool", "shear_wool"),
        TRIM_TEMPLATE("trimTemplate", "trim_template"),
        CAULDRON_WASH("cauldronWash", "cauldron_wash"),
        PISTON_REPAIR("pistonRepair", "piston_repair"),
        COPPER_PLATE("copperPressurePlate", "copper_plate"),
        ROTATE("rotator", "rotate"),
        CONSTRUCTORS_TOUCH("constructorsTouch", "constructors_touch"),
        CORE_ORE("coreOre", "core_ore"),
        SHELL_UPGRADE("shellUpgrade", "shell_upgrade"),
        DISC_FLIP("discFlip", "disc_flip");

        private final String section;
        private final String id;

        Kind(String section, String id) {
            this.section = section;
            this.id = id;
        }

        /** The key of this kind in {@link InWorldTransformations#describe()}. */
        public String section() {
            return section;
        }

        /** Lower-case id, used for the JEI recipe type and the translation keys. */
        public String id() {
            return id;
        }

        /** Translation key of the category title. */
        public String titleKey() {
            return "jei.simplebuilding.category." + id;
        }

        public static Kind bySection(String section) {
            for (Kind kind : values()) {
                if (kind.section.equals(section)) {
                    return kind;
                }
            }
            return null;
        }
    }

    /** One slot: alternatives (cycled by JEI) and a stack size. */
    public record Stack(List<Item> items, int count) {
        public static Stack of(Item item, int count) {
            return new Stack(List.of(item), count);
        }
    }

    /**
     * One recipe. {@code tools} are the items that perform it (none consumed except by
     * durability), {@code durationTicks} animates the arrow when above zero, {@code notes} are the
     * lines under the slots.
     */
    public record Entry(Kind kind, String id, List<Stack> inputs, List<Item> tools, Stack output,
                        int durationTicks, List<Component> notes) {
    }

    /** All entries plus everything that could not be turned into one (expected empty). */
    public record Catalog(List<Entry> entries, List<String> problems) {
        public List<Entry> of(Kind kind) {
            List<Entry> out = new ArrayList<>();
            for (Entry entry : entries) {
                if (entry.kind() == kind) {
                    out.add(entry);
                }
            }
            return out;
        }

        /** Every tool of one kind, in first-seen order - the JEI catalysts of that category. */
        public List<Item> toolsOf(Kind kind) {
            Set<Item> seen = new java.util.LinkedHashSet<>();
            for (Entry entry : of(kind)) {
                seen.addAll(entry.tools());
            }
            return new ArrayList<>(seen);
        }
    }

    /** The catalog as the game has it right now. */
    public static Catalog build() {
        return build(InWorldTransformations.describe(),
                InWorldTransformations.reshapePairs(false), InWorldTransformations.reshapePairs(true));
    }

    static Catalog build(JsonObject described, List<Block[]> reshapeForward, List<Block[]> reshapeReverse) {
        List<Entry> entries = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        for (String section : described.keySet()) {
            if (Kind.bySection(section) == null) {
                problems.add("in-world section '" + section + "' has no JEI category (add a Kind)");
            }
        }
        Map<Kind, JsonObject> sections = new EnumMap<>(Kind.class);
        for (Kind kind : Kind.values()) {
            JsonElement element = described.get(kind.section());
            if (element == null && kind == Kind.SHELL_UPGRADE && !com.simplebuilding.version.McVersion.RARE_STRUCTURE_FINDS) {
                continue;
            }
            if (element == null && kind == Kind.DISC_FLIP && !com.simplebuilding.version.McVersion.MUSIC_DISCS) {
                continue;
            }
            if (element == null || !element.isJsonObject()) {
                problems.add("in-world section '" + kind.section() + "' is missing from the export");
            } else {
                sections.put(kind, element.getAsJsonObject());
            }
        }
        Resolver resolver = new Resolver(problems);
        if (sections.containsKey(Kind.MACHINE_UPGRADE)) {
            machineUpgrades(sections.get(Kind.MACHINE_UPGRADE), resolver, entries);
        }
        if (sections.containsKey(Kind.RESHAPE)) {
            reshapes(sections.get(Kind.RESHAPE), reshapeForward, reshapeReverse, resolver, entries);
        }
        if (sections.containsKey(Kind.DIAMOND_CRUSH) && sections.containsKey(Kind.RESHAPE)) {
            diamondCrush(sections.get(Kind.DIAMOND_CRUSH), hammersOf(sections.get(Kind.RESHAPE), resolver), resolver, entries);
        }
        if (sections.containsKey(Kind.CHISEL)) {
            chisels(sections.get(Kind.CHISEL), resolver, entries);
        }
        if (sections.containsKey(Kind.SHEAR_WOOL)) {
            shearWool(sections.get(Kind.SHEAR_WOOL), resolver, entries);
        }
        if (sections.containsKey(Kind.TRIM_TEMPLATE)) {
            trimTemplates(sections.get(Kind.TRIM_TEMPLATE), resolver, entries);
        }
        if (sections.containsKey(Kind.CAULDRON_WASH)) {
            cauldronWash(sections.get(Kind.CAULDRON_WASH), resolver, entries);
        }
        if (sections.containsKey(Kind.PISTON_REPAIR)) {
            pistonRepair(sections.get(Kind.PISTON_REPAIR), resolver, entries);
        }
        if (sections.containsKey(Kind.COPPER_PLATE)) {
            copperPlate(sections.get(Kind.COPPER_PLATE), resolver, entries);
        }
        if (sections.containsKey(Kind.ROTATE)) {
            rotate(sections.get(Kind.ROTATE), resolver, entries);
        }
        if (sections.containsKey(Kind.CONSTRUCTORS_TOUCH)) {
            constructorsTouch(sections.get(Kind.CONSTRUCTORS_TOUCH), resolver, entries);
        }
        if (sections.containsKey(Kind.CORE_ORE)) {
            coreOre(sections.get(Kind.CORE_ORE), resolver, entries);
        }
        if (sections.containsKey(Kind.SHELL_UPGRADE)) {
            shellUpgrade(sections.get(Kind.SHELL_UPGRADE), resolver, entries);
        }
        if (sections.containsKey(Kind.DISC_FLIP)) {
            discFlip(sections.get(Kind.DISC_FLIP), resolver, entries);
        }
        return new Catalog(Collections.unmodifiableList(entries), Collections.unmodifiableList(problems));
    }

    // =====================================================================================

    private static void machineUpgrades(JsonObject up, Resolver resolver, List<Entry> out) {
        int hits = up.get("hits").getAsInt();
        int duration = up.get("durationTicks").getAsInt();
        Map<String, Integer> rank = new HashMap<>();
        for (JsonElement element : up.getAsJsonArray("hammers")) {
            JsonObject hammer = element.getAsJsonObject();
            rank.put(hammer.get("id").getAsString(), hammer.get("rank").getAsInt());
        }
        for (JsonElement element : up.getAsJsonArray("steps")) {
            JsonObject step = element.getAsJsonObject();
            String from = step.get("from").getAsString();
            JsonElement minimum = step.get("minimumHammer");
            if (minimum == null || minimum.isJsonNull()) {
                resolver.problems.add("machine upgrade from " + from + " names no hammer that can do it");
                continue;
            }
            int needed = rank.getOrDefault(minimum.getAsString(), Integer.MAX_VALUE);
            List<String> hammerIds = new ArrayList<>();
            rank.entrySet().stream()
                    .filter(e -> e.getValue() >= needed)
                    .sorted(Map.Entry.<String, Integer>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                    .forEach(e -> hammerIds.add(e.getKey()));
            Item fromItem = resolver.block(from);
            Item nugget = resolver.item(step.get("nugget").getAsString());
            Item to = resolver.block(step.get("to").getAsString());
            List<Item> tools = resolver.items(hammerIds);
            if (fromItem == null || nugget == null || to == null || tools.isEmpty()) {
                continue;
            }
            int perHit = step.get("damagePerHit").getAsInt();
            // Shulkerkisten haben eigene Schlaege und Dauer je Stufe; alte Exporte ohne sie gelten wie alle.
            int stepHits = step.has("hits") ? step.get("hits").getAsInt() : hits;
            int stepDuration = step.has("durationTicks") ? step.get("durationTicks").getAsInt() : duration;
            List<Component> notes = List.of(
                    Component.translatable("jei.simplebuilding.note.machine_upgrade.how"),
                    Component.translatable("jei.simplebuilding.note.machine_upgrade.time", stepHits, seconds(stepDuration), stepDuration),
                    Component.translatable("jei.simplebuilding.note.machine_upgrade.damage", perHit, step.get("totalDamage").getAsInt()));
            out.add(new Entry(Kind.MACHINE_UPGRADE, "machine_upgrade/" + from,
                    List.of(Stack.of(fromItem, 1), Stack.of(nugget, step.get("nuggetCount").getAsInt())),
                    tools, Stack.of(to, 1), stepDuration, notes));
        }
    }

    private static List<Item> hammersOf(JsonObject reshape, Resolver resolver) {
        List<String> ids = new ArrayList<>();
        for (JsonElement element : reshape.getAsJsonArray("hammers")) {
            ids.add(element.getAsJsonObject().get("id").getAsString());
        }
        return resolver.items(ids);
    }

    private static void reshapes(JsonObject reshape, List<Block[]> forward, List<Block[]> reverse,
                                 Resolver resolver, List<Entry> out) {
        List<Item> hammers = hammersOf(reshape, resolver);
        int minTicks = Integer.MAX_VALUE;
        int maxTicks = 0;
        for (JsonElement element : reshape.getAsJsonArray("hammers")) {
            int ticks = element.getAsJsonObject().get("ticks").getAsInt();
            minTicks = Math.min(minTicks, ticks);
            maxTicks = Math.max(maxTicks, ticks);
        }
        Component charge = Component.translatable("jei.simplebuilding.note.reshape.charge", minTicks, maxTicks);
        List<Component> forwardNotes = new ArrayList<>();
        forwardNotes.add(Component.translatable("jei.simplebuilding.note.reshape.forward"));
        if (com.simplebuilding.version.McVersion.TRANSFORM_HINTS_AND_CORNERS)
            forwardNotes.add(Component.translatable("jei.simplebuilding.note.reshape.corner"));
        forwardNotes.add(charge);
        forwardNotes.add(Component.translatable("jei.simplebuilding.note.damage", reshape.get("damage").getAsInt()));
        List<Component> reverseNotes = List.of(
                Component.translatable("jei.simplebuilding.note.reshape.reverse"),
                touchNote(), charge,
                Component.translatable("jei.simplebuilding.note.damage", reshape.get("reverseDamage").getAsInt()));
        for (boolean isReverse : new boolean[]{false, true}) {
            for (Block[] pair : isReverse ? reverse : forward) {
                Item from = resolver.block(InWorldTransformations.id(pair[0]));
                Item to = resolver.block(InWorldTransformations.id(pair[1]));
                if (from == null || to == null) {
                    continue;
                }
                out.add(new Entry(Kind.RESHAPE,
                        (isReverse ? "reshape_reverse/" : "reshape/") + InWorldTransformations.id(pair[0]),
                        List.of(Stack.of(from, 1)), hammers, Stack.of(to, 1), 0,
                        isReverse ? reverseNotes : forwardNotes));
            }
        }
    }

    private static void diamondCrush(JsonObject crush, List<Item> allHammers, Resolver resolver, List<Entry> out) {
        // Seit 2026-09-29 nennt der Export die Haemmer, die zerschlagen duerfen (ab Eisen); aeltere Exporte: alle.
        List<Item> hammers = allHammers;
        if (crush.has("hammers")) {
            List<String> ids = new ArrayList<>();
            for (JsonElement element : crush.getAsJsonArray("hammers")) {
                ids.add(element.getAsString());
            }
            hammers = resolver.items(ids);
        }
        String blockId = crush.get("block").getAsString();
        Item block = resolver.block(blockId);
        Item result = resolver.item(crush.get("result").getAsString());
        if (block == null || result == null || hammers.isEmpty()) {
            return;
        }
        out.add(new Entry(Kind.DIAMOND_CRUSH, "diamond_crush/" + blockId, List.of(Stack.of(block, 1)), hammers,
                Stack.of(result, crush.get("count").getAsInt()), 0,
                List.of(Component.translatable("jei.simplebuilding.note.diamond_crush.how", crush.has("strikes") ? crush.get("strikes").getAsInt() : 1),
                        Component.translatable("jei.simplebuilding.note.damage", crush.get("damage").getAsInt()))));
        if (crush.has("anvil")) {
            JsonObject anvil = crush.getAsJsonObject("anvil");
            List<String> ids = new ArrayList<>();
            for (JsonElement tool : anvil.getAsJsonArray("tools")) ids.add(tool.getAsString());
            out.add(new Entry(Kind.DIAMOND_CRUSH, "anvil_diamond_crush/" + blockId,
                    List.of(Stack.of(block, 1)), resolver.items(ids), Stack.of(result, anvil.get("count").getAsInt()), 0,
                    List.of(Component.translatable("jei.simplebuilding.note.anvil_crush.how",
                                    anvil.get("minimumFallBlocks").getAsInt()),
                            Component.translatable("jei.simplebuilding.note.anvil_crush.consumes"))));
        }
    }

    /**
     * Chisel pairs, grouped like the wiki groups them: one entry per pair, direction and
     * "needs Constructor's Touch", naming every chisel whose tier table contains it. The dedicated
     * spatulas are legacy items without a model (LegacySpatulaMigration) and are left out, as in the
     * wiki.
     */
    private static void chisels(JsonObject chisel, Resolver resolver, List<Entry> out) {
        JsonArray tables = chisel.getAsJsonArray("tables");
        List<JsonObject> tools = new ArrayList<>();
        for (JsonElement element : chisel.getAsJsonArray("tools")) {
            JsonObject tool = element.getAsJsonObject();
            if (!tool.get("spatula").getAsBoolean()) {
                tools.add(tool);
            }
        }
        // Weakest tier first: the smallest table is the lowest tier (every higher one contains it).
        tools.sort(Comparator.<JsonObject>comparingInt(t -> tableSize(tables.get(t.get("table").getAsInt()).getAsJsonObject()))
                .thenComparing(t -> t.get("id").getAsString()));

        record Key(boolean reverse, boolean touch, String from, String to) {
        }
        Map<Key, List<String>> grouped = new LinkedHashMap<>();
        for (JsonObject tool : tools) {
            JsonObject table = tables.get(tool.get("table").getAsInt()).getAsJsonObject();
            String toolId = tool.get("id").getAsString();
            for (boolean reverse : new boolean[]{false, true}) {
                Set<List<String>> normal = pairs(table.getAsJsonArray(reverse ? "backward" : "forward"));
                Set<List<String>> touch = pairs(table.getAsJsonArray(reverse ? "touchBackward" : "touchForward"));
                touch.removeAll(normal);
                for (List<String> pair : normal) {
                    grouped.computeIfAbsent(new Key(reverse, false, pair.get(0), pair.get(1)), k -> new ArrayList<>()).add(toolId);
                }
                for (List<String> pair : touch) {
                    grouped.computeIfAbsent(new Key(reverse, true, pair.get(0), pair.get(1)), k -> new ArrayList<>()).add(toolId);
                }
            }
        }
        int damage = chisel.get("damage").getAsInt();
        int reverseDamage = chisel.get("reverseDamage").getAsInt();
        List<Key> keys = new ArrayList<>(grouped.keySet());
        keys.sort(Comparator.comparing(Key::reverse).thenComparing(Key::touch).thenComparing(Key::from).thenComparing(Key::to));
        for (Key key : keys) {
            Item from = resolver.block(key.from());
            Item to = resolver.block(key.to());
            List<Item> chiselItems = resolver.items(grouped.get(key));
            if (from == null || to == null || chiselItems.isEmpty()) {
                continue;
            }
            List<Component> notes = new ArrayList<>();
            notes.add(Component.translatable(key.reverse() ? "jei.simplebuilding.note.chisel.reverse" : "jei.simplebuilding.note.chisel.forward"));
            if (key.touch()) {
                notes.add(touchNote());
            }
            notes.add(Component.translatable("jei.simplebuilding.note.damage", key.reverse() ? reverseDamage : damage));
            out.add(new Entry(Kind.CHISEL,
                    (key.reverse() ? "chisel_reverse/" : "chisel/") + key.from() + "/" + key.to() + (key.touch() ? "/touch" : ""),
                    List.of(Stack.of(from, 1)), chiselItems, Stack.of(to, 1), 0, List.copyOf(notes)));
        }
    }

    private static int tableSize(JsonObject table) {
        return table.getAsJsonArray("forward").size() + table.getAsJsonArray("touchForward").size();
    }

    /** The pairs of one exported table as a sorted set of [from, to]. */
    public static Set<List<String>> pairs(JsonArray array) {
        Set<List<String>> out = new TreeSet<>(Comparator.<List<String>, String>comparing(p -> p.get(0)).thenComparing(p -> p.get(1)));
        for (JsonElement element : array) {
            JsonArray pair = element.getAsJsonArray();
            out.add(List.of(pair.get(0).getAsString(), pair.get(1).getAsString()));
        }
        return out;
    }

    private static void shearWool(JsonObject shear, Resolver resolver, List<Entry> out) {
        List<String> blockIds = new ArrayList<>();
        for (JsonElement element : shear.getAsJsonArray("blocks")) {
            blockIds.add(element.getAsString());
        }
        List<Item> wool = new ArrayList<>();
        for (String id : blockIds) {
            Item item = resolver.block(id);
            if (item != null) {
                wool.add(item);
            }
        }
        Item tool = resolver.item(shear.get("tool").getAsString());
        Item result = resolver.item(shear.get("result").getAsString());
        if (wool.isEmpty() || tool == null || result == null) {
            return;
        }
        out.add(new Entry(Kind.SHEAR_WOOL, "shear_wool", List.of(new Stack(List.copyOf(wool), 1)), List.of(tool),
                Stack.of(result, shear.get("count").getAsInt()), 0,
                List.of(Component.translatable("jei.simplebuilding.note.shear_wool.how"),
                        Component.translatable("jei.simplebuilding.note.damage", shear.get("damage").getAsInt()))));
    }

    /** One entry per off-hand material: any trim template + the material -> the upgraded template. */
    private static void trimTemplates(JsonObject trim, Resolver resolver, List<Entry> out) {
        List<String> templateIds = new ArrayList<>();
        for (JsonElement element : trim.getAsJsonArray("templates")) {
            templateIds.add(element.getAsString());
        }
        List<String> hammerIds = new ArrayList<>();
        for (JsonElement element : trim.getAsJsonArray("hammers")) {
            hammerIds.add(element.getAsString());
        }
        List<Item> templates = resolver.items(templateIds);
        List<Item> hammers = resolver.items(hammerIds);
        if (templates.isEmpty() || hammers.isEmpty()) {
            resolver.problems.add("trim template upgrade names no template or no hammer");
            return;
        }
        List<Component> notes = List.of(
                Component.translatable("jei.simplebuilding.note.trim_template.placed",
                        trim.has("placedHits") ? trim.get("placedHits").getAsInt() : 1),
                Component.translatable("jei.simplebuilding.note.damage", trim.get("damage").getAsInt()));
        for (JsonElement element : trim.getAsJsonArray("upgrades")) {
            JsonObject upgrade = element.getAsJsonObject();
            String resultId = upgrade.get("result").getAsString();
            Item catalyst = resolver.item(upgrade.get("catalyst").getAsString());
            Item result = resolver.item(resultId);
            if (catalyst == null || result == null) {
                continue;
            }
            out.add(new Entry(Kind.TRIM_TEMPLATE, "trim_template/" + resultId,
                    List.of(new Stack(List.copyOf(templates), 1), Stack.of(catalyst, upgrade.get("catalystCount").getAsInt())),
                    hammers, Stack.of(result, 1), 0, notes));
        }
    }

    /** Any coloured octant on a water cauldron -> the plain octant. */
    private static void cauldronWash(JsonObject wash, Resolver resolver, List<Entry> out) {
        List<String> octantIds = new ArrayList<>();
        for (JsonElement element : wash.getAsJsonArray("octants")) {
            octantIds.add(element.getAsString());
        }
        List<Item> octants = resolver.items(octantIds);
        Item cauldron = resolver.item(wash.get("cauldron").getAsString());
        Item result = resolver.item(wash.get("result").getAsString());
        if (octants.isEmpty() || cauldron == null || result == null) {
            return;
        }
        out.add(new Entry(Kind.CAULDRON_WASH, "cauldron_wash", List.of(new Stack(List.copyOf(octants), 1)), List.of(cauldron),
                Stack.of(result, 1), 0,
                List.of(Component.translatable("jei.simplebuilding.note.cauldron_wash.how"),
                        Component.translatable("jei.simplebuilding.note.cauldron_wash.water", wash.get("waterLevels").getAsInt()),
                        Component.translatable("jei.simplebuilding.note.cauldron_wash.keeps"))));
    }

    /** A placed shulker shell + the nugget of the next tier -> the shell of that tier (one nugget per shell). */
    private static void shellUpgrade(JsonObject upgrade, Resolver resolver, List<Entry> out) {
        for (JsonElement element : upgrade.getAsJsonArray("steps")) {
            JsonObject step = element.getAsJsonObject();
            Item shell = resolver.item(step.get("shell").getAsString());
            Item nugget = resolver.item(step.get("nugget").getAsString());
            Item result = resolver.item(step.get("result").getAsString());
            if (shell == null || nugget == null || result == null) {
                continue;
            }
            out.add(new Entry(Kind.SHELL_UPGRADE, "shell_upgrade/" + step.get("result").getAsString(),
                    List.of(Stack.of(shell, 1), Stack.of(nugget, step.get("nuggetCount").getAsInt())), List.of(nugget),
                    Stack.of(result, 1), 0,
                    List.of(Component.translatable("jei.simplebuilding.note.shell_upgrade.how"),
                            Component.translatable("jei.simplebuilding.note.shell_upgrade.cost", step.get("nuggetCount").getAsInt()))));
        }
    }

    /** A placed music disc + a sledgehammer -> its other side, both ways (one durability per flip). */
    private static void discFlip(JsonObject flip, Resolver resolver, List<Entry> out) {
        List<String> ids = new ArrayList<>();
        for (JsonElement element : flip.getAsJsonArray("hammers")) {
            ids.add(element.getAsString());
        }
        List<Item> hammers = resolver.items(ids);
        int durability = flip.get("durabilityPerFlip").getAsInt();
        for (JsonElement element : flip.getAsJsonArray("pairs")) {
            JsonObject pair = element.getAsJsonObject();
            Item disc = resolver.item(pair.get("disc").getAsString());
            Item bSide = resolver.item(pair.get("bSide").getAsString());
            if (disc == null || bSide == null) {
                continue;
            }
            List<Component> notes = List.of(Component.translatable("jei.simplebuilding.note.disc_flip.how"),
                    Component.translatable("jei.simplebuilding.note.disc_flip.cost", durability));
            out.add(new Entry(Kind.DISC_FLIP, "disc_flip/" + pair.get("bSide").getAsString(), List.of(Stack.of(disc, 1)), hammers,
                    Stack.of(bSide, 1), 0, notes));
            out.add(new Entry(Kind.DISC_FLIP, "disc_flip/" + pair.get("disc").getAsString(), List.of(Stack.of(bSide, 1)), hammers,
                    Stack.of(disc, 1), 0, notes));
        }
    }

    /** A damaged breaker piston + the nugget of its tier -> the piston at full durability. */
    private static void pistonRepair(JsonObject repair, Resolver resolver, List<Entry> out) {
        for (JsonElement element : repair.getAsJsonArray("steps")) {
            JsonObject step = element.getAsJsonObject();
            String pistonId = step.get("piston").getAsString();
            Item piston = resolver.block(pistonId);
            Item nugget = resolver.item(step.get("nugget").getAsString());
            if (piston == null || nugget == null) {
                continue;
            }
            out.add(new Entry(Kind.PISTON_REPAIR, "piston_repair/" + pistonId, List.of(Stack.of(piston, 1)), List.of(nugget),
                    Stack.of(piston, 1), 0,
                    List.of(Component.translatable("jei.simplebuilding.note.piston_repair.how"),
                            Component.translatable("jei.simplebuilding.note.piston_repair.full",
                                    step.get("durability").getAsInt(), step.get("nuggetCount").getAsInt()))));
        }
    }

    /** Honeycomb waxes each stage; an axe scrapes the wax off, or else one oxidation stage. */
    private static void copperPlate(JsonObject plate, Resolver resolver, List<Entry> out) {
        Item honeycomb = resolver.item(plate.get("honeycomb").getAsString());
        List<String> axeIds = new ArrayList<>();
        for (JsonElement element : plate.getAsJsonArray("axes")) {
            axeIds.add(element.getAsString());
        }
        List<Item> axes = resolver.items(axeIds);
        Component damage = Component.translatable("jei.simplebuilding.note.damage", plate.get("axeDamage").getAsInt());
        for (String part : List.of("wax", "unwax", "scrape")) {
            boolean wax = part.equals("wax");
            List<Item> tools = wax ? (honeycomb == null ? List.of() : List.of(honeycomb)) : axes;
            for (List<String> pair : pairs(plate.getAsJsonArray(part))) {
                Item from = resolver.block(pair.get(0));
                Item to = resolver.block(pair.get(1));
                if (from == null || to == null || tools.isEmpty()) {
                    continue;
                }
                out.add(new Entry(Kind.COPPER_PLATE, "copper_plate/" + part + "/" + pair.get(0), List.of(Stack.of(from, 1)), tools,
                        Stack.of(to, 1), 0, wax ? List.of(Component.translatable("jei.simplebuilding.note.copper_plate.wax"))
                                : List.of(Component.translatable("jei.simplebuilding.note.copper_plate.scrape"), damage)));
            }
        }
    }

    /** The rotator turns a block in place; one example per kind of orientation. */
    private static void rotate(JsonObject rotator, Resolver resolver, List<Entry> out) {
        Item tool = resolver.item(rotator.get("tool").getAsString());
        if (tool == null) {
            return;
        }
        List<Component> notes = List.of(Component.translatable("jei.simplebuilding.note.rotate.how"),
                Component.translatable("jei.simplebuilding.note.rotate.charge", rotator.get("chargePerTurn").getAsInt(),
                        rotator.get("maxCharge").getAsInt()));
        for (JsonElement element : rotator.getAsJsonArray("examples")) {
            String blockId = element.getAsString();
            Item block = resolver.block(blockId);
            if (block != null) {
                out.add(new Entry(Kind.ROTATE, "rotate/" + blockId, List.of(Stack.of(block, 1)), List.of(tool), Stack.of(block, 1), 0, notes));
            }
        }
    }

    /** A stick with Constructor's Touch turns a block's orientation; one example per kind. */
    private static void constructorsTouch(JsonObject touch, Resolver resolver, List<Entry> out) {
        Item tool = resolver.item(touch.get("tool").getAsString());
        if (tool == null) {
            return;
        }
        List<Component> notes = List.of(Component.translatable("jei.simplebuilding.note.constructors_touch.how"), touchNote());
        for (JsonElement element : touch.getAsJsonArray("examples")) {
            String blockId = element.getAsString();
            Item block = resolver.block(blockId);
            if (block != null) {
                out.add(new Entry(Kind.CONSTRUCTORS_TOUCH, "constructors_touch/" + blockId, List.of(Stack.of(block, 1)), List.of(tool),
                        Stack.of(block, 1), 0, notes));
            }
        }
    }

    /** Every host block + any building core -> one of its ores, with the ore's share and the core chances. */
    private static void coreOre(JsonObject core, Resolver resolver, List<Entry> out) {
        List<String> coreIds = new ArrayList<>();
        int rarest = 0;
        int likeliest = Integer.MAX_VALUE;
        for (JsonElement element : core.getAsJsonArray("cores")) {
            JsonObject entry = element.getAsJsonObject();
            coreIds.add(entry.get("id").getAsString());
            rarest = Math.max(rarest, entry.get("oneIn").getAsInt());
            likeliest = Math.min(likeliest, entry.get("oneIn").getAsInt());
        }
        List<Item> cores = resolver.items(coreIds);
        if (cores.isEmpty()) {
            resolver.problems.add("core ore chance names no building core");
            return;
        }
        Component how = Component.translatable("jei.simplebuilding.note.core_ore.how");
        Component chance = Component.translatable("jei.simplebuilding.note.core_ore.chance", likeliest, rarest);
        for (JsonElement element : core.getAsJsonArray("hosts")) {
            JsonObject host = element.getAsJsonObject();
            List<String> blockIds = new ArrayList<>();
            for (JsonElement block : host.getAsJsonArray("blocks")) {
                blockIds.add(block.getAsString());
            }
            List<Item> blocks = new ArrayList<>();
            for (String id : blockIds) {
                Item item = resolver.block(id);
                if (item != null) {
                    blocks.add(item);
                }
            }
            if (blocks.isEmpty()) {
                continue;
            }
            for (JsonElement oreElement : host.getAsJsonArray("ores")) {
                JsonObject ore = oreElement.getAsJsonObject();
                String oreId = ore.get("id").getAsString();
                Item oreItem = resolver.block(oreId);
                if (oreItem == null) {
                    continue;
                }
                out.add(new Entry(Kind.CORE_ORE, "core_ore/" + host.get("host").getAsString() + "/" + oreId,
                        List.of(new Stack(List.copyOf(blocks), 1)), cores, Stack.of(oreItem, 1), 0,
                        List.of(how, chance, Component.translatable("jei.simplebuilding.note.core_ore.weight", ore.get("weight").getAsInt()))));
            }
        }
    }

    // =====================================================================================

    private static Component touchNote() {
        return Component.translatable("jei.simplebuilding.note.needs_touch",
                Component.translatable("enchantment.simplebuilding.constructors_touch"));
    }

    /** 100 ticks -> "5", 30 ticks -> "1.5". */
    private static String seconds(int ticks) {
        return ticks % 20 == 0 ? Integer.toString(ticks / 20) : Double.toString(ticks / 20.0);
    }

    /** Ids to items; anything that does not resolve becomes a problem instead of a silent gap. */
    private static final class Resolver {
        final List<String> problems;

        Resolver(List<String> problems) {
            this.problems = problems;
        }

        Item item(String id) {
            Identifier key = Identifier.tryParse(id);
            Item item = key == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(key);
            if (item == Items.AIR) {
                problems.add("no item " + id);
                return null;
            }
            return item;
        }

        /** The item form of a block; a block without one cannot be shown and is reported. */
        Item block(String id) {
            Identifier key = Identifier.tryParse(id);
            Block block = key == null ? null : BuiltInRegistries.BLOCK.getOptional(key).orElse(null);
            Item item = block == null ? Items.AIR : block.asItem();
            if (item == Items.AIR) {
                problems.add("no item form for block " + id);
                return null;
            }
            return item;
        }

        List<Item> items(List<String> ids) {
            List<Item> out = new ArrayList<>();
            for (String id : ids) {
                Item item = item(id);
                if (item != null) {
                    out.add(item);
                }
            }
            return out;
        }
    }
}
