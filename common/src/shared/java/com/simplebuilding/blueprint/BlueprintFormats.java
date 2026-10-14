package com.simplebuilding.blueprint;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Text-Austausch der Blaupause (Queue N55): Export und Import als Text in zwei Formaten.
 * <ul>
 *   <li>{@link Format#CODE}: der eigene Bau-Code, unveraendert (Kommentare, Variablen, Formen bleiben).</li>
 *   <li>{@link Format#COMMANDS}: Vanilla-Befehlsliste {@code setblock}/{@code fill} mit relativen
 *       Koordinaten ({@code ~}), wie eine {@code .mcfunction}-Datei. Beim Import auch absolute
 *       Koordinaten (sie werden so verschoben, dass die kleinste Ecke bei 0,0,0 liegt).</li>
 * </ul>
 * Der Import liefert nur Text/Modell: er platziert nichts und gibt nichts aus (kein Item-Duplizieren);
 * der Code geht danach wie jede Eingabe durch den Editor und {@code handleBlueprintEdit}. Alle Grenzen
 * kommen aus {@link BlueprintCode} (Codelaenge, Raster 256, Summe der Stellen); dazu
 * {@link #MAX_IMPORT_CHARS} und {@link BlueprintCommandText#MAX_COMMANDS}. Befehle werden dafuer in
 * Bau-Code umgesetzt und durch {@link BlueprintCode#parse} geprueft, damit unbekannte Bloecke,
 * Eigenschaften und Groessen genau wie im Editor gemeldet werden. Es gilt alles oder nichts.
 */
public final class BlueprintFormats {
    /** Laengster Text, den der Import annimmt (Zeichen). */
    public static final int MAX_IMPORT_CHARS = 262_144;
    /** Mehr Befehlszeilen schreibt der Export nicht (der Bau ist dann zu zerklueftet fuer Text). */
    public static final int MAX_EXPORT_LINES = 50_000;

    public enum Format {
        CODE("code"), COMMANDS("commands");

        public final String key;

        Format(String key) {
            this.key = key;
        }

        public Component label() {
            return Component.translatable("simplebuilding.blueprint.io.format." + key);
        }
    }

    /** Ein Problem im Importtext: Zeile (1-basiert, 0 = ganzer Text) und ein Sprachschluessel mit Werten. */
    public record Issue(int line, String langKey, List<String> args) {
        public Component message() {
            return Component.translatable(langKey, args.toArray());
        }

        static Issue of(int line, String key, String... args) {
            return new Issue(line, "simplebuilding.blueprint.import.error." + key, List.of(args));
        }
    }

    /** Ergebnis des Imports: {@code code} ist nur bei {@link #ok()} brauchbar. */
    public record Imported(Format format, String code, BlueprintModel model, List<Issue> issues) {
        public boolean ok() {
            return issues.isEmpty();
        }
    }

    /** Ergebnis des Exports; {@code tooLarge}: Bau braucht mehr als {@link #MAX_EXPORT_LINES} Zeilen. */
    public record Exported(String text, boolean tooLarge) {
    }

    private BlueprintFormats() {
    }

    /** Das Format eines Texts: Befehlsliste, wenn die erste Zeile {@code setblock}/{@code fill} ist, sonst Bau-Code. */
    public static Format detect(String text) {
        return BlueprintCommandText.firstCommand(text.replace("\r", "")) != null ? Format.COMMANDS : Format.CODE;
    }

    // =====================================================================================
    // IMPORT
    // =====================================================================================

    /** {@code format == null}: Format erkennen. */
    public static Imported importText(String text, Format format) {
        String t = text == null ? "" : text.replace("\r", "");
        Format f = format != null ? format : detect(t);
        BlueprintModel empty = new BlueprintModel();
        if (t.isBlank()) {
            return new Imported(f, "", empty, List.of(Issue.of(0, "empty")));
        }
        if (t.length() > MAX_IMPORT_CHARS) {
            return new Imported(f, "", empty, List.of(Issue.of(0, "too_long", String.valueOf(MAX_IMPORT_CHARS))));
        }
        return f == Format.CODE ? fromCode(t) : fromCommands(t);
    }

    private static Imported fromCode(String text) {
        BlueprintCode.ParseResult parsed = BlueprintCode.parse(text);
        List<Issue> issues = new ArrayList<>();
        for (BlueprintCode.Problem p : parsed.problems()) {
            issues.add(codeIssue(p.line() + 1, p));
        }
        if (issues.isEmpty() && parsed.model().isEmpty()) {
            issues.add(Issue.of(0, "no_blocks"));
        }
        if (!issues.isEmpty()) {
            return new Imported(Format.CODE, "", new BlueprintModel(), List.copyOf(issues));
        }
        return new Imported(Format.CODE, text.stripTrailing(), parsed.model(), List.of());
    }

    private static Issue codeIssue(int line, BlueprintCode.Problem p) {
        return new Issue(line, "simplebuilding.blueprint.error." + p.key(), p.args());
    }

    private static Imported fromCommands(String text) {
        BlueprintCommandText.Result commands = BlueprintCommandText.parse(text);
        List<Issue> issues = new ArrayList<>();
        for (BlueprintCommandText.Issue i : commands.issues()) {
            issues.add(new Issue(i.line(), "simplebuilding.blueprint.import.error." + i.key(), i.args()));
        }
        BlueprintModel empty = new BlueprintModel();
        if (!issues.isEmpty()) {
            return new Imported(Format.COMMANDS, "", empty, List.copyOf(issues));
        }
        List<BlueprintCommandText.Op> ops = commands.ops();
        if (ops.isEmpty()) {
            return new Imported(Format.COMMANDS, "", empty, List.of(Issue.of(0, "no_blocks")));
        }
        // Kleinste Ecke nach 0,0,0 schieben; die Spanne muss ins Raster passen.
        long[] min = {Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE};
        long[] max = {Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE};
        for (BlueprintCommandText.Op op : ops) {
            for (int a = 0; a < 3; a++) {
                min[a] = Math.min(min[a], Math.min(op.box()[a], op.box()[a + 3]));
                max[a] = Math.max(max[a], Math.max(op.box()[a], op.box()[a + 3]));
                if (max[a] - min[a] >= BlueprintCode.GRID) {
                    issues.add(Issue.of(op.line(), "span", String.valueOf(BlueprintCode.GRID)));
                    return new Imported(Format.COMMANDS, "", empty, List.copyOf(issues));
                }
            }
        }
        StringBuilder code = new StringBuilder();
        List<Integer> source = new ArrayList<>();
        for (BlueprintCommandText.Op op : ops) {
            int[] b = new int[6];
            for (int a = 0; a < 3; a++) {
                long lo = Math.min(op.box()[a], op.box()[a + 3]) - min[a];
                long hi = Math.max(op.box()[a], op.box()[a + 3]) - min[a];
                b[a] = (int) lo;
                b[a + 3] = (int) hi;
            }
            for (String line : lines(op, b)) {
                code.append(line).append('\n');
                source.add(op.line());
            }
        }
        BlueprintCode.ParseResult parsed = BlueprintCode.parse(code.toString());
        for (BlueprintCode.Problem p : parsed.problems()) {
            int src = p.line() >= 0 && p.line() < source.size() ? source.get(p.line()) : 0;
            issues.add(codeIssue(src, p));
        }
        if (!issues.isEmpty()) {
            return new Imported(Format.COMMANDS, "", empty, List.copyOf(issues));
        }
        if (parsed.model().isEmpty()) {
            return new Imported(Format.COMMANDS, "", empty, List.of(Issue.of(0, "no_blocks")));
        }
        String result = BlueprintCode.serialize(parsed.model());
        if (result.length() > BlueprintCode.MAX_CODE_LENGTH) {
            return new Imported(Format.COMMANDS, "", empty,
                    List.of(Issue.of(0, "code_too_long", String.valueOf(result.length()), String.valueOf(BlueprintCode.MAX_CODE_LENGTH))));
        }
        return new Imported(Format.COMMANDS, result, parsed.model(), List.of());
    }

    /** Bau-Code-Zeilen fuer einen Befehl auf dem verschobenen Quader {@code b} (x1,y1,z1,x2,y2,z2, aufsteigend). */
    private static List<String> lines(BlueprintCommandText.Op op, int[] b) {
        List<String> out = new ArrayList<>();
        boolean hollow = op.mode() != BlueprintCommandText.Mode.SET;
        boolean hasInterior = b[3] - b[0] >= 2 && b[4] - b[1] >= 2 && b[5] - b[2] >= 2;
        if (!hollow || !hasInterior) {
            out.add(op.block() + " " + box(b[0], b[1], b[2], b[3], b[4], b[5]));
            return out;
        }
        int x1 = b[0], y1 = b[1], z1 = b[2], x2 = b[3], y2 = b[4], z2 = b[5];
        if (op.mode() == BlueprintCommandText.Mode.HOLLOW) {
            out.add(op.block() + " " + box(x1, y1, z1, x2, y2, z2));
            out.add("air " + box(x1 + 1, y1 + 1, z1 + 1, x2 - 1, y2 - 1, z2 - 1));
            return out;
        }
        // outline: six faces, the inside stays as it is
        String block = op.block();
        out.add(block + " " + box(x1, y1, z1, x1, y2, z2) + " " + box(x2, y1, z1, x2, y2, z2));
        out.add(block + " " + box(x1 + 1, y1, z1, x2 - 1, y1, z2) + " " + box(x1 + 1, y2, z1, x2 - 1, y2, z2));
        out.add(block + " " + box(x1 + 1, y1 + 1, z1, x2 - 1, y2 - 1, z1) + " " + box(x1 + 1, y1 + 1, z2, x2 - 1, y2 - 1, z2));
        return out;
    }

    private static String box(int x1, int y1, int z1, int x2, int y2, int z2) {
        return axis(x1, x2) + "," + axis(y1, y2) + "," + axis(z1, z2);
    }

    private static String axis(int lo, int hi) {
        return lo == hi ? Integer.toString(lo) : lo + ".." + hi;
    }

    // =====================================================================================
    // EXPORT
    // =====================================================================================

    /**
     * Der Bau als Text. {@link Format#CODE}: der Editor-Text selbst. {@link Format#COMMANDS}: je
     * Quader ein {@code fill} (ein einzelner Block: {@code setblock}), relativ zur kleinsten Ecke
     * ({@code ~0 ~0 ~0}); nur der fehlerfreie Teil des Codes.
     */
    public static Exported export(Format format, String code) {
        String c = code == null ? "" : code.replace("\r", "");
        if (format == Format.CODE) {
            return new Exported(c, false);
        }
        return toCommands(BlueprintCode.parse(c).model());
    }

    public static Exported toCommands(BlueprintModel model) {
        if (model.isEmpty()) {
            return new Exported("", false);
        }
        BlueprintModel m = model.normalized();
        Map<BlockState, IntArrayList> byState = new HashMap<>();
        for (int k : m.sortedKeys()) {
            byState.computeIfAbsent(m.blocks().get(k), s -> new IntArrayList()).add(k);
        }
        List<Map.Entry<BlockState, IntArrayList>> groups = new ArrayList<>(byState.entrySet());
        groups.sort(java.util.Comparator.<Map.Entry<BlockState, IntArrayList>>comparingInt(e -> -e.getValue().size())
                .thenComparing(e -> BlueprintCode.spec(e.getKey())));
        StringBuilder out = new StringBuilder();
        out.append("# SimpleBuilding blueprint ").append(m.sizeX()).append('x').append(m.sizeY()).append('x').append(m.sizeZ())
                .append(", ").append(m.size()).append(" blocks; run at the lowest corner\n");
        int lines = 0;
        for (Map.Entry<BlockState, IntArrayList> group : groups) {
            String spec = BlueprintCode.spec(group.getKey());
            for (int[] b : BlueprintCode.boxes(group.getValue())) {
                if (++lines > MAX_EXPORT_LINES) {
                    return new Exported("", true);
                }
                if (b[0] == b[3] && b[1] == b[4] && b[2] == b[5]) {
                    out.append("setblock ~").append(b[0]).append(" ~").append(b[1]).append(" ~").append(b[2]);
                } else {
                    out.append("fill ~").append(b[0]).append(" ~").append(b[1]).append(" ~").append(b[2])
                            .append(" ~").append(b[3]).append(" ~").append(b[4]).append(" ~").append(b[5]);
                }
                out.append(' ').append(spec).append('\n');
            }
        }
        return new Exported(out.toString().stripTrailing(), false);
    }
}
