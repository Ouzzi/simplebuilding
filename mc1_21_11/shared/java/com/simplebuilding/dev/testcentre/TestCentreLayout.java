package com.simplebuilding.dev.testcentre;

import com.simplebuilding.items.ModItems;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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
 * Neuer Abschnitt: Methode in {@link TestCentreSections}, Id in {@link #SECTION_IDS} und Eintrag in
 * {@link #build}. Neues Item: meist nichts zu tun, wenn es in einer Tab-Zeile steht, die ein Abschnitt
 * liest; sonst erscheint es unter "unsorted" und der Abdeckungstest nennt es.
 */
public final class TestCentreLayout {

    /** Reihenfolge der Abschnitte = Reihenfolge in der Welt. */
    public static final List<String> SECTION_IDS = List.of("controls", "armour", "books", "tools", "storage", "food",
            "materials", "chisel", "inworld", "blocks", "lightroom", "machines", "ores", "planning", "mining", "devices", "unsorted");

    /**
     * Mod-Items und -Bloecke, die bewusst NICHT in der Zentrale stehen, mit Grund. Jede Ausnahme muss
     * hier stehen - der Abdeckungstest liest genau diese Liste.
     */
    public static final Map<String, String> EXCLUDED = Map.of(
            "simplebuilding:creative_spacer", "Platzhalter der Kreativ-Tabs, kein Spielinhalt",
            "simplebuilding:netherite_piston_head", "technischer Block (Kopf des Netherit-Kolbens), kein Item");

    public static final int MAX_ROW_WIDTH = 140;
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
                       Set<Item> coveredItems, Set<Block> coveredBlocks, List<Item> leftovers) {

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
            StringBuilder out = new StringBuilder("test centre " + (bounds.getXSpan()) + "x" + bounds.getZSpan()
                    + " blocks, " + sections.size() + " sections:");
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

    public static Plan plan(HolderLookup.Provider lookup, BlockPos origin) {
        TcContext ctx = new TcContext(lookup);
        Map<String, Function<TcContext, TcCanvas>> builders = new LinkedHashMap<>();
        builders.put("armour", TestCentreSections::armour);
        builders.put("books", TestCentreSections::books);
        builders.put("tools", TestCentreSections::tools);
        builders.put("storage", TestCentreSections::storage);
        builders.put("food", TestCentreSections::food);
        builders.put("materials", TestCentreSections::materials);
        builders.put("chisel", TestCentreSections::chisel);
        builders.put("inworld", TestCentreSections::inWorld);
        builders.put("blocks", TestCentreSections::blocks);
        builders.put("lightroom", TestCentreSections::lightRoom);
        builders.put("machines", TestCentreSections::machines);
        builders.put("ores", TestCentreSections::ores);
        builders.put("planning", TestCentreSections::planning);
        builders.put("mining", TestCentreSections::mining);
        builders.put("devices", TestCentreSections::devices);

        Map<String, TcCanvas> canvases = new LinkedHashMap<>();
        for (Map.Entry<String, Function<TcContext, TcCanvas>> entry : builders.entrySet()) {
            canvases.put(entry.getKey(), entry.getValue().apply(ctx));
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

        // Erste Runde: Steuerwand mit Platzhaltern, nur fuer ihre Breite.
        canvases.put("controls", TestCentreSections.controls(controls(origin, Map.of(), null)));
        Map<String, BlockPos> offsets = pack(canvases);

        Map<String, BlockPos> anchors = new LinkedHashMap<>();
        for (Map.Entry<String, TcCanvas> entry : canvases.entrySet()) {
            BlockPos offset = offsets.get(entry.getKey());
            entry.getValue().anchors().forEach((name, pos) -> anchors.put(name, origin.offset(offset).offset(pos)));
        }
        BoundingBox bounds = bounds(origin, canvases, offsets);
        // Zweite Runde: jetzt mit den echten Koordinaten.
        canvases.put("controls", TestCentreSections.controls(controls(origin, anchors, bounds)));

        List<Section> sections = new ArrayList<>();
        for (String id : SECTION_IDS) {
            TcCanvas canvas = canvases.get(id);
            BlockPos offset = offsets.get(id);
            BlockPos absolute = origin.offset(offset);
            List<TcOp> ops = new ArrayList<>();
            for (TcOp op : canvas.ops()) {
                ops.add(op.moved(absolute));
            }
            sections.add(new Section(id, offset, canvas.width(), canvas.depth(), canvas.height(), ops));
        }
        return new Plan(origin, sections, anchors, bounds, coveredItems, coveredBlocks, leftovers);
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
                case TcOp.Stand stand -> stand.gear().forEach(stack -> items.add(stack.getItem()));
                case TcOp.Fill fill -> fill.contents().forEach(stack -> items.add(stack.getItem()));
                case TcOp.Sign sign -> {
                }
                case TcOp.Command command -> {
                }
            }
        }
        items.remove(Items.AIR);
    }

    /** Offsets je Abschnitt: Reihen in +x, neue Reihe (mit Gang davor) bei Ueberlauf. */
    private static Map<String, BlockPos> pack(Map<String, TcCanvas> canvases) {
        Map<String, BlockPos> offsets = new LinkedHashMap<>();
        int x = 0;
        int z = 0;
        int rowDepth = 0;
        for (String id : SECTION_IDS) {
            TcCanvas canvas = canvases.get(id);
            if (x > 0 && x + canvas.width() > MAX_ROW_WIDTH) {
                z += rowDepth + CORRIDOR;
                x = 0;
                rowDepth = 0;
            }
            offsets.put(id, new BlockPos(x, 0, z));
            x += canvas.width() + GAP;
            rowDepth = Math.max(rowDepth, canvas.depth());
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
    static List<TestCentreSections.Control> controls(BlockPos origin, Map<String, BlockPos> anchors, BoundingBox bounds) {
        String at = origin.getX() + " " + origin.getY() + " " + origin.getZ();
        List<TestCentreSections.Control> out = new ArrayList<>();
        out.add(new TestCentreSections.Control("sbtestcentre build " + at,
                TcText.t("controls.rebuild", "Rebuild all"), TcText.t("controls.rebuild.sub", "whole centre")));
        for (String id : SECTION_IDS) {
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
