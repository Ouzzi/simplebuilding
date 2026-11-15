package com.simplebuilding.dev.testcentre;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * TZ38D (Ist-Aufnahme und Vorschau): schreibt eine Planung als Tabelle, eine Zeile je Schritt
 * ({@code Abschnitt, x, y, z, Art, Id, Zusatz}, Koordinaten relativ zum Ursprung), davor je Abschnitt eine Kopfzeile
 * ({@code #section id x z breite tiefe hoehe}). Nur fuer Entwickler-Laeufe: der Bau-Test ruft es, wenn die
 * Umgebungsvariable {@code SB_TC_DUMP} gesetzt ist; die Vorschau-Werkzeuge lesen die Datei.
 */
public final class TestCentreDump {

    private TestCentreDump() {
    }

    /** Ob ein Lauf die Tabellen schreiben soll ({@code SB_TC_DUMP} gesetzt). */
    public static boolean wanted() {
        String value = System.getenv("SB_TC_DUMP");
        return value != null && !value.isBlank();
    }

    /** Schreibt {@code plan} nach {@code testcentre-dump/<name>.tsv} im Arbeitsordner; Fehler nur ins Log. */
    public static void write(TestCentreLayout.Plan plan, String name) {
        List<String> lines = new ArrayList<>();
        net.minecraft.core.BlockPos origin = plan.origin();
        for (TestCentreLayout.Section section : plan.sections()) {
            lines.add("#section\t" + section.id() + "\t" + section.offset().getX() + "\t" + section.offset().getZ() + "\t"
                    + section.width() + "\t" + section.depth() + "\t" + section.height());
            for (TcOp op : section.ops()) {
                net.minecraft.core.BlockPos p = op.pos().subtract(origin);
                String kind = op.getClass().getSimpleName();
                String id;
                String extra = "";
                switch (op) {
                    case TcOp.Place place -> {
                        id = BuiltInRegistries.BLOCK.getKey(place.state().getBlock()).toString();
                        extra = place.state().toString();
                    }
                    case TcOp.Frame frame -> {
                        id = item(frame.stack());
                        extra = frame.facing() + (frame.stack().getComponentsPatch().isEmpty() ? "" : " +components");
                    }
                    case TcOp.Stand stand -> {
                        id = stand.dummy() ? "dummy" : "stand";
                        extra = String.join(",", stand.gear().stream().map(TestCentreDump::item).toList());
                    }
                    case TcOp.Mob mob -> id = mob.entity().toString();
                    case TcOp.Fill fill -> {
                        id = "fill";
                        extra = String.join(",", fill.contents().stream().map(TestCentreDump::item).toList());
                    }
                    case TcOp.Sign sign -> {
                        id = "sign";
                        extra = (sign.standing() ? "standing " + sign.facing() + ": " : "")
                                + String.join(" | ", sign.lines().stream().map(line -> line.getString()).toList());
                    }
                    case TcOp.Command command -> {
                        id = "command";
                        extra = command.command();
                    }
                    case TcOp.Structure structure -> {
                        id = structure.template().toString();
                        extra = structure.size().toShortString();
                    }
                    default -> id = "-";
                }
                lines.add(section.id() + "\t" + p.getX() + "\t" + p.getY() + "\t" + p.getZ() + "\t" + kind + "\t" + id + "\t"
                        + extra.replace('\t', ' ').replace('\n', ' '));
            }
        }
        try {
            Path dir = Path.of("testcentre-dump");
            Files.createDirectories(dir);
            Files.write(dir.resolve(name + ".tsv"), lines, StandardCharsets.UTF_8);
            com.simplebuilding.Simplebuilding.LOGGER.info("test centre dump {}: {} lines in {}", name, lines.size(),
                    dir.toAbsolutePath());
        } catch (IOException e) {
            com.simplebuilding.Simplebuilding.LOGGER.warn("test centre dump {} failed", name, e);
        }
    }

    /**
     * Integration (alle Module geladen, {@code sbintegration} ruft per Reflexion): plant die Zentrale wie die Welt des
     * Besitzers und liefert die Probleme der Planung (leer = gut); schreibt die Tabellen, wenn {@link #wanted()}.
     */
    public static List<String> allModules(net.minecraft.server.level.ServerLevel level) {
        List<String> problems = new ArrayList<>(OrderAreas.problems(new TcContext(level.registryAccess())));
        for (OrderAreas.Variant variant : OrderAreas.Variant.values()) {
            TestCentreLayout.Plan plan = TestCentreLayout.plan(level, net.minecraft.core.BlockPos.ZERO,
                    TestCentreLayout.Options.ALL.with(variant));
            if (!plan.leftovers().isEmpty()) {
                problems.add(variant.id() + ": " + plan.leftovers().size() + " items without a place, e.g. " + TcContext.id(plan.leftovers().getFirst()));
            }
            if (wanted()) {
                write(plan, variant.id());
            }
            com.simplebuilding.Simplebuilding.LOGGER.info("test centre with all modules ({}): {}", variant.id(),
                    plan.summary().lines().findFirst().orElse(""));
        }
        OrderAreas.Area lines = OrderAreas.families();
        // N50 TZEDIT50: mit allen Modulen steht die eingecheckte Config genau so in der Welt, und nichts faellt ins Band "Neu".
        problems.addAll(OrderAreas.configProblems(LayoutConfig.current(), lines));
        OrderAreas.configWarnings().forEach(w -> problems.add("layout config: " + w));
        lines.rows().stream().filter(row -> row.group().startsWith(OrderAreas.NEW_PREFIX)).limit(10)
                .forEach(row -> problems.add("layout config: row " + row.group() + " has items without a config cell"));
        List<String> missing = OrderAreas.missingList(lines);
        com.simplebuilding.Simplebuilding.LOGGER.info("test centre lines: {} rows, {} placeholders, {} red (missing)", lines.rows().size(),
                lines.placeholders().size(), missing.size());
        if (wanted()) {
            writeLines(lines, "lines-rows");
            // N50 TZEDIT50: the other order areas cell by cell (tools/tzedit/export_layout.py turns them into layout configs).
            TcContext ctx = new TcContext(level.registryAccess());
            java.util.Set<net.minecraft.world.item.Item> framed = new java.util.HashSet<>(lines.hidden());
            writeCells(OrderAreas.compact(), "compact-cells");
            writeCells(OrderAreas.items(ctx, OrderAreas.blockAreaItems(lines), framed), "items-lines-cells");
            writeCells(OrderAreas.items(ctx, OrderAreas.blockAreaItems((OrderAreas.Area) null), framed), "items-blocks-cells");
        }
        return problems;
    }

    /** Die Reihen der Linien als Tabelle (Vorschau, Fehlliste): Reihe, Art, Seite, x, Spalte, Slot, Item oder Platzhalter. */
    static void writeLines(OrderAreas.Area lines, String name) {
        List<String> out = new ArrayList<>();
        out.add("#lineZ\t" + lines.lineZ());
        for (OrderAreas.Band band : lines.bands()) {
            out.add("#band\t" + band.x0() + "\t" + band.width() + "\t" + band.zA() + "\t" + band.nearA() + "\t" + band.lineZ() + "\t"
                    + band.nearB() + "\t" + band.depthB());
        }
        for (OrderAreas.Row row : lines.rows()) {
            for (int col = 0; col < row.depth(); col++) {
                OrderAreas.Slot slot = col < row.columns().size() ? row.columns().get(col) : OrderAreas.Slot.OTHER;
                int other = col - row.columns().size();
                net.minecraft.world.item.Item item = col < row.columns().size() ? row.fixed().get(slot)
                        : other < row.others().size() ? row.others().get(other) : null;
                net.minecraft.world.item.Item top = col < row.columns().size() ? row.tops().get(slot) : null;
                String key = col < row.columns().size() ? slot.name() : other < row.otherKeys().size() ? row.otherKeys().get(other) : "-";
                String what = item != null ? BuiltInRegistries.ITEM.getKey(item).toString()
                        : col < row.columns().size() && row.expected().contains(slot) ? "MISSING" : "NONE";
                out.add(row.group() + "\t" + row.kind() + "\t" + row.family() + "\t" + (row.step() < 0 ? "A" : "B") + "\t" + row.x()
                        + "\t" + row.z(col) + "\t" + col + "\t" + key + "\t" + what + "\t" + (top == null ? "" : BuiltInRegistries.ITEM.getKey(top)));
            }
        }
        // N50 TZEDIT50: the sign text of each row (standing sign on the path field, z(-1)).
        java.util.Map<net.minecraft.core.BlockPos, String> signs = new java.util.HashMap<>();
        for (TcOp op : lines.canvas().ops()) {
            if (op instanceof TcOp.Sign sign) {
                signs.put(sign.pos(), String.join(" ", sign.lines().stream().map(line -> line.getString()).toList()));
            }
        }
        for (OrderAreas.Row row : lines.rows()) {
            String text = signs.get(new net.minecraft.core.BlockPos(row.x(), 0, row.z(-1)));
            if (text != null) {
                out.add("#label\t" + row.group() + "\t" + row.x() + "\t" + row.nearZ() + "\t" + text.replace('\t', ' '));
            }
        }
        lines.hidden().forEach(item -> out.add("#hidden\t" + BuiltInRegistries.ITEM.getKey(item)));
        try {
            Path dir = Path.of("testcentre-dump");
            Files.createDirectories(dir);
            Files.write(dir.resolve(name + ".tsv"), out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            com.simplebuilding.Simplebuilding.LOGGER.warn("test centre dump {} failed", name, e);
        }
    }

    /** N50 TZEDIT50: the cells of an area (group, column, item, x, y, z, framed) as a table. */
    static void writeCells(OrderAreas.Area area, String name) {
        java.util.Set<net.minecraft.core.BlockPos> frames = new java.util.HashSet<>();
        for (TcOp op : area.canvas().ops()) {
            if (op instanceof TcOp.Frame frame) {
                frames.add(frame.pos());
            }
        }
        List<String> out = new ArrayList<>();
        for (OrderAreas.Cell cell : area.cells()) {
            out.add(cell.group() + "\t" + cell.column() + "\t" + BuiltInRegistries.ITEM.getKey(cell.item()) + "\t" + cell.pos().getX()
                    + "\t" + cell.pos().getY() + "\t" + cell.pos().getZ() + "\t" + (frames.contains(cell.pos()) ? "frame" : "place"));
        }
        try {
            Path dir = Path.of("testcentre-dump");
            Files.createDirectories(dir);
            Files.write(dir.resolve(name + ".tsv"), out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            com.simplebuilding.Simplebuilding.LOGGER.warn("test centre dump {} failed", name, e);
        }
    }

    private static String item(ItemStack stack) {
        return stack.isEmpty() ? "-" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
