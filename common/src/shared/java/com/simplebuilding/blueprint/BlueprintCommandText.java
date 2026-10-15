package com.simplebuilding.blueprint;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Liest eine Liste von Vanilla-Befehlen ({@code setblock} und {@code fill}, wie in einer
 * {@code .mcfunction}-Datei oder mit fuehrendem Schraegstrich) in Quader. Rein textuell, ohne
 * Minecraft-Abhaengigkeit ({@code BlueprintCommandTextTest}): ob ein Block existiert, prueft erst
 * {@link BlueprintCode} beim Umsetzen ({@link BlueprintFormats}).
 *
 * <p>Unterstuetzt: absolute und relative ({@code ~}, {@code ~3}) ganzzahlige Koordinaten,
 * {@code fill ... [replace|destroy|hollow|outline]}, Blockzustand in eckigen Klammern. Alles andere
 * wird mit Zeile und Schluessel gemeldet statt still uebergangen: {@code keep}, Filter bei
 * {@code replace}, Blockdaten in geschweiften Klammern, lokale Koordinaten {@code ^}, Kommazahlen,
 * fremde Befehle. Leerzeilen und Zeilen mit {@code #} sind Kommentare.
 */
public final class BlueprintCommandText {
    /** Mehr Befehle als das nimmt der Import nicht (Schutz vor riesigen Einfuegungen). */
    public static final int MAX_COMMANDS = 20_000;
    /** Betrag einer Koordinate (9 Ziffern); danach kommt {@code bad_coordinate}. */
    private static final long MAX_COORD = 999_999_999L;
    /** Blockangabe: Id und optional Zustand; ohne {@code $ # " { } ( )} und Zeilenumbrueche. */
    private static final Pattern BLOCK = Pattern.compile("[a-z0-9_.\\-/:]+(\\[[^\\[\\]#$\"{}()\\n]*])?");
    private static final Pattern COORD = Pattern.compile("~?-?[0-9]+|~");

    public enum Mode { SET, HOLLOW, OUTLINE }

    /** Ein Befehl als Quader {@code x1,y1,z1 .. x2,y2,z2} (roh, noch nicht verschoben). */
    public record Op(int line, String block, long[] box, Mode mode) {
    }

    /** Ein Problem: Zeile (1-basiert, 0 = ganzer Text), Schluessel (Lang: simplebuilding.blueprint.import.error.KEY), Werte. */
    public record Issue(int line, String key, List<String> args) {
    }

    public record Result(List<Op> ops, List<Issue> issues) {
    }

    private BlueprintCommandText() {
    }

    /** Der erste Befehl des Texts ({@code setblock}/{@code fill}), sonst null - fuer die Formaterkennung. */
    public static String firstCommand(String text) {
        for (String raw : text.split("\n", -1)) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("/")) {
                line = line.substring(1).stripLeading();
            }
            int end = 0;
            while (end < line.length() && !Character.isWhitespace(line.charAt(end))) {
                end++;
            }
            String word = line.substring(0, end);
            return word.equals("setblock") || word.equals("fill") ? word : null;
        }
        return null;
    }

    public static Result parse(String text) {
        List<Op> ops = new ArrayList<>();
        List<Issue> issues = new ArrayList<>();
        String[] lines = text.replace("\r", "").split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (ops.size() >= MAX_COMMANDS) {
                issues.add(new Issue(i + 1, "too_many_commands", List.of(String.valueOf(MAX_COMMANDS))));
                break;
            }
            if (line.startsWith("/")) {
                line = line.substring(1).stripLeading();
            }
            int before = issues.size();
            Op op = parseLine(i + 1, tokens(line), issues);
            if (op != null && issues.size() == before) {
                ops.add(op);
            }
        }
        return new Result(ops, issues);
    }

    /** Woerter durch Leerraum getrennt; innerhalb von {@code [...]} und {@code {...}} gehoert Leerraum zum Wort. */
    private static List<String> tokens(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '[' || c == '{') {
                depth++;
            } else if ((c == ']' || c == '}') && depth > 0) {
                depth--;
            }
            if (depth == 0 && Character.isWhitespace(c)) {
                if (!word.isEmpty()) {
                    out.add(word.toString());
                    word.setLength(0);
                }
            } else {
                word.append(c);
            }
        }
        if (!word.isEmpty()) {
            out.add(word.toString());
        }
        return out;
    }

    private static Op parseLine(int line, List<String> t, List<Issue> issues) {
        String command = t.get(0);
        boolean fill = command.equals("fill");
        if (!fill && !command.equals("setblock")) {
            issues.add(new Issue(line, "unknown_command", List.of(command)));
            return null;
        }
        int coords = fill ? 6 : 3;
        // fill x1 y1 z1 x2 y2 z2 block [mode [filter]]   |   setblock x y z block [mode]
        if (t.size() < coords + 2 || t.size() > coords + (fill ? 4 : 3)) {
            issues.add(new Issue(line, "bad_syntax", List.of(command)));
            return null;
        }
        long[] c = new long[6];
        for (int i = 0; i < coords; i++) {
            Long value = coordinate(t.get(1 + i), line, issues);
            if (value == null) {
                return null;
            }
            c[i] = value;
        }
        if (!fill) {
            c[3] = c[0];
            c[4] = c[1];
            c[5] = c[2];
        }
        String block = t.get(1 + coords);
        if (block.indexOf('{') >= 0) {
            issues.add(new Issue(line, "block_data", List.of(block)));
            return null;
        }
        if (!BLOCK.matcher(block).matches()) {
            issues.add(new Issue(line, "bad_block", List.of(block)));
            return null;
        }
        Mode mode = Mode.SET;
        if (t.size() > coords + 2) {
            String m = t.get(coords + 2);
            switch (m) {
                case "replace" -> {
                    if (t.size() > coords + 3) {
                        issues.add(new Issue(line, "unsupported_filter", List.of(t.get(coords + 3))));
                        return null;
                    }
                }
                case "destroy" -> { }
                case "hollow" -> mode = Mode.HOLLOW;
                case "outline" -> mode = Mode.OUTLINE;
                case "keep" -> {
                    issues.add(new Issue(line, "unsupported_mode", List.of(m)));
                    return null;
                }
                default -> {
                    issues.add(new Issue(line, "bad_syntax", List.of(command)));
                    return null;
                }
            }
            if (t.size() > coords + 3 && !m.equals("replace")) {
                issues.add(new Issue(line, "bad_syntax", List.of(command)));
                return null;
            }
        }
        if (!fill && mode != Mode.SET) {
            issues.add(new Issue(line, "bad_syntax", List.of(command)));
            return null;
        }
        return new Op(line, block, c, mode);
    }

    private static Long coordinate(String token, int line, List<Issue> issues) {
        if (token.startsWith("^")) {
            issues.add(new Issue(line, "local_coordinate", List.of(token)));
            return null;
        }
        if (!COORD.matcher(token).matches() || token.length() > 11) {
            issues.add(new Issue(line, "bad_coordinate", List.of(token)));
            return null;
        }
        String number = token.startsWith("~") ? token.substring(1) : token;
        long value = number.isEmpty() ? 0 : Long.parseLong(number);
        if (Math.abs(value) > MAX_COORD) {
            issues.add(new Issue(line, "bad_coordinate", List.of(token)));
            return null;
        }
        return value;
    }
}
