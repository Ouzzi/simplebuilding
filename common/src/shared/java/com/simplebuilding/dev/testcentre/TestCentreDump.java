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

    private static String item(ItemStack stack) {
        return stack.isEmpty() ? "-" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
