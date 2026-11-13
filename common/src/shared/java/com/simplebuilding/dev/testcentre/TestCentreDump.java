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
                        extra = String.join(" | ", sign.lines().stream().map(line -> line.getString()).toList());
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
        List<String> missing = OrderAreas.missingList(lines);
        com.simplebuilding.Simplebuilding.LOGGER.info("test centre lines: {} rows, {} placeholders, {} red (missing)", lines.rows().size(),
                lines.placeholders().size(), missing.size());
        if (wanted()) {
            writeLines(lines, "lines-rows");
        }
        return problems;
    }

    /** Die Reihen der Linien als Tabelle (Vorschau, Fehlliste): Reihe, Art, Seite, x, Spalte, Slot, Item oder Platzhalter. */
    static void writeLines(OrderAreas.Area lines, String name) {
        List<String> out = new ArrayList<>();
        out.add("#lineZ\t" + lines.lineZ());
        for (OrderAreas.Row row : lines.rows()) {
            for (int col = 0; col < row.length(); col++) {
                OrderAreas.Slot slot = col < row.columns().size() ? row.columns().get(col) : OrderAreas.Slot.OTHER;
                net.minecraft.world.item.Item item = col < row.columns().size() ? row.fixed().get(slot) : row.others().get(col - row.columns().size());
                net.minecraft.world.item.Item top = col < row.columns().size() ? row.tops().get(slot) : null;
                String what = item != null ? BuiltInRegistries.ITEM.getKey(item).toString()
                        : row.expected().contains(slot) ? "MISSING" : "NONE";
                out.add(row.group() + "\t" + row.kind() + "\t" + row.family() + "\t" + (row.step() < 0 ? "A" : "B") + "\t" + row.x()
                        + "\t" + row.z(col) + "\t" + col + "\t" + slot + "\t" + what + "\t" + (top == null ? "" : BuiltInRegistries.ITEM.getKey(top)));
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

    private static String item(ItemStack stack) {
        return stack.isEmpty() ? "-" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
