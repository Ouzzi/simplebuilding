package com.simplebuilding.blueprint;

import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.util.OctantShape;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;

/**
 * Die Bausprache der Blaupause: Parser, Serialisierer und Syntax-Einfaerbung aus einer Hand.
 *
 * <p>Spezifikation mit Beispielen: {@code docs/BLUEPRINT.md}. Kurz:
 * <pre>
 * # Kommentar bis zum Zeilenende
 * $treppe = oak_stairs[facing=east,half=top]   Alias
 * $r = 5                                        Variable (ganze Zahl, auch $h = $r * 2 + 1)
 * stone 0..4,0,0..4                             Block + Bereiche (x,y,z; a..b je Achse)
 * glass 1,1,1..1,1,5                            Eckenform: von Ecke bis Ecke
 * oak_fence 0,1,0*5@2,0,0                       Wiederholung: 5 Stueck im Abstand 2,0,0
 * stone 0..$r,0,$r+1                            Variablen und + - ( * / % ) in Koordinaten
 * glass sphere(8,8,8,$r) hollow_box(0,0,0,5,4,5)  Formen, hohl mit hollow_
 * air 1..3,1..3,1..3                            air raeumt, was davor gesetzt wurde
 * </pre>
 * Spaetere Anweisungen ueberschreiben fruehere. Koordinaten liegen im Raster {@code 0..255}.
 */
public final class BlueprintCode {

    /** Kantenlaenge des lokalen Rasters (= Obergrenze der groessten Baustab-Stufe, Enderit). */
    public static final int GRID = 256;
    /** Laengste erlaubte Bausprache in Zeichen; passt in {@code ByteBufCodecs.STRING_UTF8}. */
    public static final int MAX_CODE_LENGTH = 32000;
    /** Laengster Titel einer signierten Blaupause (wie beim Buch). */
    public static final int MAX_TITLE_LENGTH = 32;
    /**
     * Budget fuer ausgerollte Stellen ueber alle Anweisungen (vor dem Ausrollen geprueft) und damit
     * auch die Hoechstzahl belegter Stellen eines Modells, beim Scan wie beim Schreiben: 4 194 304
     * (= 256 x 256 x 64). Ein 256er-Wuerfel darf also aufgespannt, aber nicht massiv gefuellt werden -
     * so bleiben Speicher (rund 60 MB fuer ein volles Modell) und Parse-Zeit beherrschbar.
     */
    public static final int MAX_EXPANDED_CELLS = 4_194_304;
    /**
     * Budget fuer das Rastern von Formen: Summe der Bounding-Box-Volumen aller Formen eines Codes
     * (jede Stelle der Box wird einmal gegen die Figur geprueft). Ein ganzes Raster (256^3), also
     * etwa eine hohle Kugel mit Radius 127 - mehr Pruefarbeit als das kann kein Code verlangen, egal
     * wie kurz er ist. Die gesetzten Stellen zaehlen zusaetzlich gegen {@link #MAX_EXPANDED_CELLS}.
     */
    public static final int MAX_SHAPE_VOLUME = GRID * GRID * GRID;
    /** Betrag, den keine Zahl, Variable oder Zwischenrechnung ueberschreiten darf (6 Ziffern). */
    public static final int MAX_NUMBER = 999_999;
    /** Tiefste Schachtelung von Klammern und Vorzeichen in einem Ausdruck. */
    public static final int MAX_EXPRESSION_DEPTH = 32;
    /** Hoechstzahl an Wiederholungen einer Region. */
    public static final int MAX_REPEAT = GRID;
    /** Zeilenlaenge, ab der der Serialisierer umbricht. */
    public static final int WRAP = 100;

    private BlueprintCode() {
    }

    // =====================================================================================
    // ERGEBNIS
    // =====================================================================================

    /** Ein Fehler im Code: absolute Zeichenpositionen {@code [start, end)} und Zeile (0-basiert). */
    public record Problem(int line, int start, int end, String key, List<String> args) {
        public Component message() {
            return Component.translatable("simplebuilding.blueprint.error." + key, args.toArray());
        }
    }

    public record ParseResult(BlueprintModel model, List<Problem> problems, byte[] styles) {
        public boolean ok() {
            return problems.isEmpty();
        }
    }

    /** Einfaerbe-Klassen, eine je Zeichen des Codes. */
    public static final byte STYLE_TEXT = 0;
    public static final byte STYLE_COMMENT = 1;
    public static final byte STYLE_BLOCK = 2;
    public static final byte STYLE_NAMESPACE = 3;
    /** Blockzustands-Eigenschaft, auch der Name einer Form ({@code sphere}, {@code hollow_box} ...). */
    public static final byte STYLE_PROPERTY = 4;
    public static final byte STYLE_VALUE = 5;
    /** Alias und Variable ({@code $name}). */
    public static final byte STYLE_ALIAS = 6;
    public static final byte STYLE_NUMBER = 7;
    public static final byte STYLE_OPERATOR = 8;
    public static final byte STYLE_ERROR = 9;
    public static final byte STYLE_AIR = 10;

    // =====================================================================================
    // FORMEN
    // =====================================================================================

    /**
     * Die Formen der Bausprache. Die Boxform ({@code sphere(0..8,0..8,0..8)}, auch in Eckenform)
     * fuellt dieselbe Figur wie ein Oktant mit dieser Auswahl ({@link OctantShape}, Ausrichtung
     * oben); die Zahlenform ({@code sphere(cx,cy,cz,r)}) ist nur eine Kurzschrift fuer eine Box um
     * den Mittelpunkt. {@code hollow_} davor laesst nur die Huelle stehen (wie "Hohl" am Oktanten:
     * Stellen mit mindestens einem Nachbarn ausserhalb der Figur).
     */
    enum Shape {
        /** {@code box(x,y,z,w,h,d)}: Ecke und Groesse. */
        BOX("box", 6, "box(x1..x2,y1..y2,z1..z2) | box(x,y,z,w,h,d)"),
        /** {@code sphere(cx,cy,cz,r)}: Box {@code c-r..c+r} je Achse. */
        SPHERE("sphere", 4, "sphere(x1..x2,y1..y2,z1..z2) | sphere(cx,cy,cz,r)"),
        /** {@code dome(cx,cy,cz,r)}: die obere Haelfte (ab {@code cy}) von {@code sphere(cx,cy,cz,r)}. */
        DOME("dome", 4, "dome(x1..x2,y1..y2,z1..z2) | dome(cx,cy,cz,r)"),
        /** {@code cylinder(cx,y,cz,r,h)}: stehend, Grundflaeche auf Hoehe {@code y}, {@code h} hoch. */
        CYLINDER("cylinder", 5, "cylinder(x1..x2,y1..y2,z1..z2) | cylinder(cx,y,cz,r,h)"),
        /** {@code pyramid(cx,y,cz,r,h)}: quadratische Grundflaeche {@code 2r+1}, Spitze oben. */
        PYRAMID("pyramid", 5, "pyramid(x1..x2,y1..y2,z1..z2) | pyramid(cx,y,cz,r,h)"),
        /** {@code line(x1,y1,z1..x2,y2,z2)}: gerade Linie von Ecke zu Ecke, nur Eckenform, nicht hohl. */
        LINE("line", 0, "line(x1,y1,z1..x2,y2,z2)");

        final String word;
        final int arity;
        final String usage;

        Shape(String word, int arity, String usage) {
            this.word = word;
            this.arity = arity;
            this.usage = usage;
        }

        static Shape byWord(String word) {
            for (Shape s : values()) {
                if (s.word.equals(word)) {
                    return s;
                }
            }
            return null;
        }
    }

    /** Das Wort, mit dem Formen hohl werden. */
    public static final String HOLLOW_PREFIX = "hollow_";

    /** Alle Formwoerter, fuer Hilfe und Tests: {@code box, sphere, ..., hollow_box, ...}. */
    public static List<String> shapeWords() {
        List<String> out = new ArrayList<>();
        for (Shape s : Shape.values()) {
            out.add(s.word);
        }
        for (Shape s : Shape.values()) {
            if (s != Shape.LINE) {
                out.add(HOLLOW_PREFIX + s.word);
            }
        }
        return out;
    }

    // =====================================================================================
    // PARSER
    // =====================================================================================

    private static final Map<String, ParseResult> CACHE = new LinkedHashMap<>(32, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ParseResult> eldest) {
            return size() > 24;
        }
    };

    /**
     * Wie {@link #parse}, aber mit einem kleinen LRU-Zwischenspeicher: Tooltip, Vorschau und
     * Baustab fragen denselben Code viele Male pro Sekunde. Das Modell darf nicht veraendert werden.
     */
    public static ParseResult parseCached(String code) {
        if (code == null) {
            code = "";
        }
        synchronized (CACHE) {
            ParseResult cached = CACHE.get(code);
            if (cached != null) {
                return cached;
            }
        }
        ParseResult result = parse(code);
        synchronized (CACHE) {
            CACHE.put(code, result);
        }
        return result;
    }

    public static ParseResult parse(String code) {
        Parser parser = new Parser(code == null ? "" : code);
        parser.run();
        return new ParseResult(parser.model, List.copyOf(parser.problems), parser.styles);
    }

    private static final class Word {
        final String text;
        final int start; // absolut

        Word(String text, int start) {
            this.text = text;
            this.start = start;
        }

        int end() {
            return start + text.length();
        }
    }

    /** Ausdruck ist keiner (falsche Zeichen, Klammer offen ...): der Aufrufer meldet ihn im Zusammenhang. */
    private static final class Syntax extends RuntimeException {
        Syntax() {
            super(null, null, false, false);
        }
    }

    /** Fehler ist schon gemeldet (unbekannte Variable, Division durch null ...): nur abbrechen. */
    private static final class Reported extends RuntimeException {
        Reported() {
            super(null, null, false, false);
        }
    }

    private static final Syntax SYNTAX = new Syntax();
    private static final Reported REPORTED = new Reported();

    private static final class Parser {
        final String code;
        final byte[] styles;
        final BlueprintModel model = new BlueprintModel();
        final List<Problem> problems = new ArrayList<>();
        final Map<String, BlockState> aliases = new HashMap<>();
        final Map<String, Long> variables = new HashMap<>();
        long expanded;
        long shapeVolume;
        boolean budgetExceeded;
        int line;
        // Zustand des Ausdrucksparsers: Leseposition, Ende, Schachtelung.
        int ep;
        int eEnd;
        int eDepth;

        Parser(String code) {
            this.code = code;
            this.styles = new byte[code.length()];
        }

        void problem(int start, int end, String key, String... args) {
            problems.add(new Problem(line, start, Math.max(end, start + 1), key, List.of(args)));
            style(start, end, STYLE_ERROR);
        }

        void style(int start, int end, byte style) {
            for (int i = Math.max(0, start); i < Math.min(end, styles.length); i++) {
                styles[i] = style;
            }
        }

        void run() {
            if (code.length() > MAX_CODE_LENGTH) {
                problems.add(new Problem(0, MAX_CODE_LENGTH, code.length(), "too_long",
                        List.of(String.valueOf(code.length()), String.valueOf(MAX_CODE_LENGTH))));
            }
            int lineStart = 0;
            line = 0;
            while (lineStart <= code.length()) {
                int lineEnd = code.indexOf('\n', lineStart);
                if (lineEnd < 0) {
                    lineEnd = code.length();
                }
                parseLine(lineStart, lineEnd);
                lineStart = lineEnd + 1;
                line++;
            }
        }

        void parseLine(int from, int to) {
            int comment = -1;
            for (int i = from; i < to; i++) {
                if (code.charAt(i) == '#') {
                    comment = i;
                    break;
                }
            }
            if (comment >= 0) {
                style(comment, to, STYLE_COMMENT);
                to = comment;
            }
            List<Word> words = splitWords(from, to);
            if (words.isEmpty()) {
                return;
            }
            Word first = words.get(0);
            if (first.text.startsWith("$")) {
                int eq = first.text.indexOf('=');
                if (eq >= 0) {
                    List<Word> rest = new ArrayList<>();
                    if (eq + 1 < first.text.length()) {
                        rest.add(new Word(first.text.substring(eq + 1), first.start + eq + 1));
                    }
                    rest.addAll(words.subList(1, words.size()));
                    define(new Word(first.text.substring(0, eq), first.start), first.start + eq, rest);
                    return;
                }
                if (words.size() > 1 && words.get(1).text.startsWith("=")) {
                    Word eqWord = words.get(1);
                    List<Word> rest = new ArrayList<>();
                    if (eqWord.text.length() > 1) {
                        rest.add(new Word(eqWord.text.substring(1), eqWord.start + 1));
                    }
                    rest.addAll(words.subList(2, words.size()));
                    define(first, eqWord.start, rest);
                    return;
                }
            }
            BlockState state = parseSpec(first);
            if (words.size() == 1) {
                problem(first.start, first.end(), "no_region");
                return;
            }
            for (int i = 1; i < words.size(); i++) {
                Region region = parseRegion(words.get(i));
                if (region != null && state != null) {
                    apply(region, state, words.get(i));
                }
            }
        }

        /**
         * {@code $name = ...}: eine Variable, wenn rechts ein Ausdruck steht (beginnt mit Ziffer,
         * Vorzeichen, Klammer oder {@code $}), sonst ein Alias. {@code $a = $b} mit einem Alias
         * {@code $b} bleibt der Fehler {@code alias_of_alias}. Aliase und Variablen teilen sich die
         * Namen: die juengste Definition gilt.
         */
        void define(Word name, int eqPos, List<Word> rest) {
            style(eqPos, eqPos + 1, STYLE_OPERATOR);
            String aliasName = name.text.substring(1);
            if (!isAliasName(aliasName)) {
                problem(name.start, name.end(), "bad_alias_name", name.text);
                return;
            }
            style(name.start, name.end(), STYLE_ALIAS);
            if (rest.isEmpty()) {
                problem(name.start, eqPos + 1, "alias_without_block", name.text);
                return;
            }
            Word spec = rest.get(0);
            if (isExpressionStart(spec, rest.size())) {
                int from = spec.start;
                int to = rest.get(rest.size() - 1).end();
                styleExpression(from, to);
                try {
                    long value = eval(from, to);
                    variables.put(aliasName, value);
                    aliases.remove(aliasName);
                } catch (Syntax e) {
                    problem(from, to, "bad_expression", code.substring(from, to));
                } catch (Reported ignored) {
                    // schon gemeldet
                }
                return;
            }
            if (rest.size() > 1) {
                Word extra = rest.get(1);
                problem(extra.start, rest.get(rest.size() - 1).end(), "alias_extra");
            }
            if (spec.text.startsWith("$")) {
                problem(spec.start, spec.end(), "alias_of_alias");
                return;
            }
            BlockState state = parseSpec(spec);
            if (state != null) {
                aliases.put(aliasName, state);
                variables.remove(aliasName);
            }
        }

        /** Rechts vom {@code =} ein Ausdruck statt eines Blocks? Ein einzelnes {@code $alias} bleibt Alias-Fehler. */
        boolean isExpressionStart(Word spec, int words) {
            char c = spec.text.charAt(0);
            if (Character.isDigit(c) || c == '-' || c == '+' || c == '(') {
                return true;
            }
            if (c != '$') {
                return false;
            }
            int n = 1;
            while (n < spec.text.length() && isNameChar(spec.text.charAt(n))) {
                n++;
            }
            String name = spec.text.substring(1, n);
            boolean singleAlias = words == 1 && n == spec.text.length() && aliases.containsKey(name);
            return !singleAlias;
        }

        List<Word> splitWords(int from, int to) {
            List<Word> words = new ArrayList<>();
            int i = from;
            while (i < to) {
                while (i < to && Character.isWhitespace(code.charAt(i))) {
                    i++;
                }
                if (i >= to) {
                    break;
                }
                int start = i;
                int depth = 0;
                int parens = 0;
                StringBuilder text = new StringBuilder();
                while (i < to) {
                    char c = code.charAt(i);
                    if (c == '[') depth++;
                    if (c == ']') depth = Math.max(0, depth - 1);
                    if (c == '(') parens++;
                    if (c == ')') parens = Math.max(0, parens - 1);
                    if (depth == 0 && parens == 0 && Character.isWhitespace(c)) {
                        break;
                    }
                    text.append(c);
                    i++;
                }
                words.add(new Word(text.toString(), start));
            }
            return words;
        }

        /** Blockangabe oder Alias; {@code null} bei Fehler. Luft liefert den Luftzustand (= raeumen). */
        BlockState parseSpec(Word word) {
            String text = word.text;
            if (text.startsWith("$")) {
                String name = text.substring(1);
                BlockState state = aliases.get(name);
                if (state == null) {
                    problem(word.start, word.end(), variables.containsKey(name) ? "variable_as_block" : "unknown_alias", text);
                    return null;
                }
                style(word.start, word.end(), STYLE_ALIAS);
                return state;
            }
            int bracket = text.indexOf('[');
            String idText = bracket >= 0 ? text.substring(0, bracket) : text;
            Identifier id = idText.isEmpty() ? null : Identifier.tryParse(idText);
            if (id == null) {
                problem(word.start, word.start + Math.max(1, idText.length()), "bad_block_id", idText);
                return null;
            }
            Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(id);
            if (block.isEmpty()) {
                problem(word.start, word.start + idText.length(), "unknown_block", idText);
                return null;
            }
            int colon = idText.indexOf(':');
            if (colon >= 0) {
                style(word.start, word.start + colon + 1, STYLE_NAMESPACE);
            }
            BlockState state = block.get().defaultBlockState();
            style(word.start + colon + 1, word.start + idText.length(), state.isAir() ? STYLE_AIR : STYLE_BLOCK);
            if (bracket < 0) {
                return state;
            }
            int propsStart = word.start + bracket;
            style(propsStart, propsStart + 1, STYLE_OPERATOR);
            if (!text.endsWith("]")) {
                problem(propsStart, word.end(), "unclosed_properties");
                return null;
            }
            style(word.end() - 1, word.end(), STYLE_OPERATOR);
            String inner = text.substring(bracket + 1, text.length() - 1);
            int offset = propsStart + 1;
            boolean failed = false;
            for (String part : inner.split(",", -1)) {
                int partStart = offset;
                offset += part.length() + 1;
                style(partStart + part.length(), partStart + part.length() + 1, STYLE_OPERATOR);
                String trimmed = part.trim();
                if (trimmed.isEmpty()) {
                    if (!inner.isBlank()) {
                        problem(partStart, partStart + 1, "empty_property");
                        failed = true;
                    }
                    continue;
                }
                int lead = part.indexOf(trimmed.charAt(0));
                int absPart = partStart + lead;
                int eq = trimmed.indexOf('=');
                if (eq <= 0 || eq == trimmed.length() - 1) {
                    problem(absPart, absPart + trimmed.length(), "bad_property", trimmed);
                    failed = true;
                    continue;
                }
                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                Property<?> property = state.getBlock().getStateDefinition().getProperty(key);
                if (property == null) {
                    problem(absPart, absPart + eq, "unknown_property", key, idText);
                    failed = true;
                    continue;
                }
                style(absPart, absPart + eq, STYLE_PROPERTY);
                style(absPart + eq, absPart + eq + 1, STYLE_OPERATOR);
                BlockState changed = withValue(state, property, value);
                if (changed == null) {
                    problem(absPart + eq + 1, absPart + trimmed.length(), "bad_value", value, key);
                    failed = true;
                    continue;
                }
                style(absPart + eq + 1, absPart + trimmed.length(), STYLE_VALUE);
                state = changed;
            }
            return failed ? null : state;
        }

        /**
         * Einfaerben eines Ausdrucks oder einer Region: Ziffern und Minus als Zahl, {@code $name} als
         * Alias/Variable, Formwoerter als Eigenschaft, alles andere als Operator.
         */
        void styleExpression(int from, int to) {
            int i = from;
            while (i < to) {
                char c = code.charAt(i);
                if (c == '$' || isLetter(c)) {
                    int j = i + 1;
                    while (j < to && isNameChar(code.charAt(j))) {
                        j++;
                    }
                    style(i, j, c == '$' ? STYLE_ALIAS : STYLE_PROPERTY);
                    i = j;
                    continue;
                }
                styles[i] = (Character.isDigit(c) || c == '-') ? STYLE_NUMBER : STYLE_OPERATOR;
                i++;
            }
        }

        Region parseRegion(Word word) {
            String text = word.text;
            styleExpression(word.start, word.end());
            int start = word.start;
            int end = word.end();
            int count = 1;
            int dx = 0, dy = 0, dz = 0;
            // Das erste '*' ausserhalb von Klammern leitet die Wiederholung ein; Multiplikation in
            // einer Koordinate steht darum in Klammern: ($w*2),0,0.
            int star = topLevel(start, end, '*');
            int baseEnd = star >= 0 ? star : end;
            if (star >= 0) {
                int at = code.indexOf('@', star + 1);
                if (at < 0 || at >= end) {
                    problem(star, end, "bad_repeat", code.substring(star, end));
                    return null;
                }
                long n;
                long[] step;
                try {
                    n = eval(star + 1, at);
                    step = triple(at + 1, end);
                } catch (Syntax e) {
                    problem(star, end, "bad_repeat", code.substring(star, end));
                    return null;
                } catch (Reported e) {
                    return null;
                }
                if (n < 1 || n > MAX_REPEAT) {
                    problem(star + 1, at, "repeat_range", String.valueOf(n), String.valueOf(MAX_REPEAT));
                    return null;
                }
                count = (int) n;
                dx = (int) step[0];
                dy = (int) step[1];
                dz = (int) step[2];
            }
            if (baseEnd > start && isLetter(code.charAt(start))) {
                return parseShape(word, start, baseEnd, count, dx, dy, dz);
            }
            long[] box;
            try {
                box = ordered(parseBox(start, baseEnd));
            } catch (Syntax e) {
                problem(start, baseEnd, "bad_region", code.substring(start, baseEnd));
                return null;
            } catch (Reported e) {
                return null;
            }
            int[] b = inGridOrReport(box, start, baseEnd);
            return b == null ? null : new Region(b, count, dx, dy, dz, null, volume(b));
        }

        /** {@code name(args)} an {@code [start, end)}: Form rastern, Grenzen pruefen. */
        Region parseShape(Word word, int start, int end, int count, int dx, int dy, int dz) {
            int open = code.indexOf('(', start);
            if (open < 0 || open >= end || code.charAt(end - 1) != ')' || !balanced(open, end)) {
                problem(start, end, "bad_region", code.substring(start, end));
                return null;
            }
            String name = code.substring(start, open);
            boolean hollow = name.startsWith(HOLLOW_PREFIX);
            Shape shape = Shape.byWord(hollow ? name.substring(HOLLOW_PREFIX.length()) : name);
            if (shape == null || (hollow && shape == Shape.LINE)) {
                problem(start, open, "unknown_shape", name);
                return null;
            }
            int from = open + 1;
            int to = end - 1;
            String usage = hollow ? HOLLOW_PREFIX + shape.usage.replace(" | ", " | " + HOLLOW_PREFIX) : shape.usage;
            long[] box;
            long[] line = null;
            try {
                int dots = topLevelDots(from, to);
                List<int[]> parts = splitTop(from, to);
                if (shape == Shape.LINE) {
                    if (dots < 0 || splitTop(from, dots).size() != 3 || splitTop(dots + 2, to).size() != 3) {
                        throw SYNTAX;
                    }
                    line = parseBox(from, to);
                    box = ordered(line);
                } else if (dots >= 0 || parts.size() == 3) {
                    box = ordered(parseBox(from, to));
                } else {
                    if (parts.size() != shape.arity) {
                        throw SYNTAX;
                    }
                    long[] v = new long[parts.size()];
                    for (int i = 0; i < v.length; i++) {
                        v[i] = eval(parts.get(i)[0], parts.get(i)[1]);
                    }
                    box = numericBox(shape, v, parts);
                }
            } catch (Syntax e) {
                problem(from - 1, end, "bad_shape_args", name, usage);
                return null;
            } catch (Reported e) {
                return null;
            }
            if (box == null) {
                return null; // Groesse schon gemeldet
            }
            int[] b = inGridOrReport(box, start, end);
            if (b == null) {
                return null;
            }
            long vol = volume(b);
            if (budgetExceeded) {
                return new Region(b, count, dx, dy, dz, new BitSet(), 0);
            }
            if (shape == Shape.BOX && !hollow) {
                return new Region(b, count, dx, dy, dz, null, vol);
            }
            if (shapeVolume + vol > MAX_SHAPE_VOLUME) {
                problem(start, end, "shape_volume", String.valueOf(MAX_SHAPE_VOLUME));
                return null;
            }
            shapeVolume += vol;
            BitSet cells = shape == Shape.LINE ? rasterLine(b, line) : raster(shape, b);
            if (hollow) {
                cells = shell(cells, b);
            }
            return new Region(b, count, dx, dy, dz, cells, cells.cardinality());
        }

        /** Box der Zahlenform, oder {@code null} nach gemeldetem Groessenfehler. */
        long[] numericBox(Shape shape, long[] v, List<int[]> parts) {
            return switch (shape) {
                case BOX -> {
                    for (int i = 3; i < 6; i++) {
                        if (!atLeast(v[i], 1, parts.get(i))) {
                            yield null;
                        }
                    }
                    yield new long[]{v[0], v[1], v[2], v[0] + v[3] - 1, v[1] + v[4] - 1, v[2] + v[5] - 1};
                }
                case SPHERE -> atLeast(v[3], 0, parts.get(3))
                        ? new long[]{v[0] - v[3], v[1] - v[3], v[2] - v[3], v[0] + v[3], v[1] + v[3], v[2] + v[3]} : null;
                case DOME -> atLeast(v[3], 0, parts.get(3))
                        ? new long[]{v[0] - v[3], v[1], v[2] - v[3], v[0] + v[3], v[1] + v[3], v[2] + v[3]} : null;
                case CYLINDER, PYRAMID -> atLeast(v[3], 0, parts.get(3)) && atLeast(v[4], 1, parts.get(4))
                        ? new long[]{v[0] - v[3], v[1], v[2] - v[3], v[0] + v[3], v[1] + v[4] - 1, v[2] + v[3]} : null;
                case LINE -> null;
            };
        }

        boolean atLeast(long value, int min, int[] span) {
            if (value >= min) {
                return true;
            }
            problem(span[0], span[1], "shape_size", String.valueOf(value), String.valueOf(min));
            return false;
        }

        /** Koordinaten im Raster, sonst {@code coordinate_range} ueber {@code [start, end)} und {@code null}. */
        int[] inGridOrReport(long[] box, int start, int end) {
            for (long v : box) {
                if (v < 0 || v >= GRID) {
                    problem(start, end, "coordinate_range", String.valueOf(v), String.valueOf(GRID - 1));
                    return null;
                }
            }
            return new int[]{(int) box[0], (int) box[1], (int) box[2], (int) box[3], (int) box[4], (int) box[5]};
        }

        // ---------------------------------------------------------------- Boxen und Ausdruecke

        /**
         * {@code x1,y1,z1,x2,y2,z2} wie geschrieben (nicht sortiert), aus Eckenform
         * ({@code a,b,c..d,e,f}) oder Achsenform ({@code a..b,c,d..e}). Die Eckenform gilt nur mit genau
         * einem {@code ..} und drei Teilen auf beiden Seiten - sonst ist es die Achsenform.
         */
        long[] parseBox(int from, int to) {
            int dots = topLevelDots(from, to);
            if (dots >= 0 && topLevelDots(dots + 2, to) < 0
                    && splitTop(from, dots).size() == 3 && splitTop(dots + 2, to).size() == 3) {
                long[] a = triple(from, dots);
                long[] b = triple(dots + 2, to);
                return new long[]{a[0], a[1], a[2], b[0], b[1], b[2]};
            }
            List<int[]> parts = splitTop(from, to);
            if (parts.size() != 3) {
                throw SYNTAX;
            }
            long[] out = new long[6];
            for (int i = 0; i < 3; i++) {
                int[] p = parts.get(i);
                int range = topLevelDots(p[0], p[1]);
                if (range >= 0) {
                    out[i] = eval(p[0], range);
                    out[i + 3] = eval(range + 2, p[1]);
                } else {
                    out[i] = eval(p[0], p[1]);
                    out[i + 3] = out[i];
                }
            }
            return out;
        }

        long[] triple(int from, int to) {
            List<int[]> parts = splitTop(from, to);
            if (parts.size() != 3) {
                throw SYNTAX;
            }
            long[] out = new long[3];
            for (int i = 0; i < 3; i++) {
                out[i] = eval(parts.get(i)[0], parts.get(i)[1]);
            }
            return out;
        }

        /** Stellen von {@code c} ausserhalb von Klammern in {@code [from, to)}, sonst -1. */
        int topLevel(int from, int to, char c) {
            int depth = 0;
            for (int i = from; i < to; i++) {
                char ch = code.charAt(i);
                if (ch == '(') depth++;
                else if (ch == ')') depth--;
                else if (ch == c && depth == 0) return i;
            }
            return -1;
        }

        int topLevelDots(int from, int to) {
            int depth = 0;
            for (int i = from; i + 1 < to; i++) {
                char ch = code.charAt(i);
                if (ch == '(') depth++;
                else if (ch == ')') depth--;
                else if (ch == '.' && code.charAt(i + 1) == '.' && depth == 0) return i;
            }
            return -1;
        }

        boolean balanced(int from, int to) {
            int depth = 0;
            for (int i = from; i < to; i++) {
                char ch = code.charAt(i);
                if (ch == '(') depth++;
                if (ch == ')' && --depth < 0) return false;
                if (depth == 0 && i < to - 1) return false; // die erste Klammer schliesst erst am Ende
            }
            return depth == 0;
        }

        /** Teile zwischen Kommas ausserhalb von Klammern, als {@code [start, end)}. */
        List<int[]> splitTop(int from, int to) {
            List<int[]> parts = new ArrayList<>();
            int depth = 0;
            int partStart = from;
            for (int i = from; i < to; i++) {
                char ch = code.charAt(i);
                if (ch == '(') depth++;
                else if (ch == ')') depth--;
                else if (ch == ',' && depth == 0) {
                    parts.add(new int[]{partStart, i});
                    partStart = i + 1;
                }
            }
            parts.add(new int[]{partStart, to});
            return parts;
        }

        /**
         * Ganzzahliger Ausdruck in {@code [from, to)}: Zahlen (bis 6 Ziffern), {@code $variable},
         * {@code + - * / %} (Division rundet ab), Klammern, Vorzeichen. Wirft {@link #SYNTAX} fuer
         * Nicht-Ausdruecke und {@link #REPORTED} nach einem gemeldeten Fehler.
         */
        long eval(int from, int to) {
            ep = from;
            eEnd = to;
            eDepth = 0;
            long v = sum();
            skipWs();
            if (ep != eEnd) {
                throw SYNTAX;
            }
            return v;
        }

        long sum() {
            skipWs();
            int start = ep;
            long v = product();
            while (true) {
                skipWs();
                if (ep >= eEnd) {
                    return v;
                }
                char c = code.charAt(ep);
                if (c != '+' && c != '-') {
                    return v;
                }
                ep++;
                long r = product();
                v = checked(c == '+' ? v + r : v - r, start);
            }
        }

        long product() {
            skipWs();
            int start = ep;
            long v = factor();
            while (true) {
                skipWs();
                if (ep >= eEnd) {
                    return v;
                }
                char c = code.charAt(ep);
                if (c != '*' && c != '/' && c != '%') {
                    return v;
                }
                int op = ep++;
                long r = factor();
                if (c != '*' && r == 0) {
                    problem(op, ep, "division_by_zero");
                    throw REPORTED;
                }
                v = checked(c == '*' ? v * r : c == '/' ? Math.floorDiv(v, r) : Math.floorMod(v, r), start);
            }
        }

        long factor() {
            skipWs();
            if (ep >= eEnd || ++eDepth > MAX_EXPRESSION_DEPTH) {
                throw SYNTAX;
            }
            int start = ep;
            char c = code.charAt(ep);
            try {
                if (c == '+' || c == '-') {
                    ep++;
                    long v = factor();
                    return c == '-' ? -v : v;
                }
                if (c == '(') {
                    ep++;
                    long v = sum();
                    skipWs();
                    if (ep >= eEnd || code.charAt(ep) != ')') {
                        throw SYNTAX;
                    }
                    ep++;
                    return v;
                }
                if (c == '$') {
                    ep++;
                    while (ep < eEnd && isNameChar(code.charAt(ep))) {
                        ep++;
                    }
                    String name = code.substring(start + 1, ep);
                    if (name.isEmpty()) {
                        throw SYNTAX;
                    }
                    Long v = variables.get(name);
                    if (v == null) {
                        problem(start, ep, aliases.containsKey(name) ? "alias_as_number" : "unknown_variable", "$" + name);
                        throw REPORTED;
                    }
                    return v;
                }
                if (Character.isDigit(c)) {
                    while (ep < eEnd && Character.isDigit(code.charAt(ep))) {
                        ep++;
                    }
                    if (ep - start > 6) {
                        throw SYNTAX;
                    }
                    return Integer.parseInt(code.substring(start, ep));
                }
                throw SYNTAX;
            } finally {
                eDepth--;
            }
        }

        long checked(long v, int start) {
            if (Math.abs(v) > MAX_NUMBER) {
                problem(start, ep, "number_range", String.valueOf(v), String.valueOf(MAX_NUMBER));
                throw REPORTED;
            }
            return v;
        }

        void skipWs() {
            while (ep < eEnd && Character.isWhitespace(code.charAt(ep))) {
                ep++;
            }
        }

        // ---------------------------------------------------------------- Ausrollen

        void apply(Region region, BlockState state, Word word) {
            if (budgetExceeded) {
                return;
            }
            int[] b = region.box;
            long cells = region.cellCount * region.count;
            if (expanded + cells > MAX_EXPANDED_CELLS) {
                budgetExceeded = true;
                problem(word.start, word.end(), "too_many_cells", String.valueOf(MAX_EXPANDED_CELLS));
                return;
            }
            // Alle Kopien muessen im Raster bleiben, sonst ist die Region als Ganzes falsch.
            for (int k = 0; k < region.count; k++) {
                int ox = region.dx * k, oy = region.dy * k, oz = region.dz * k;
                if (!BlueprintModel.inGrid(b[0] + ox, b[1] + oy, b[2] + oz) || !BlueprintModel.inGrid(b[3] + ox, b[4] + oy, b[5] + oz)) {
                    problem(word.start, word.end(), "repeat_outside", String.valueOf(k + 1), String.valueOf(GRID - 1));
                    return;
                }
            }
            expanded += cells;
            int sx = b[3] - b[0] + 1, sz = b[5] - b[2] + 1;
            for (int k = 0; k < region.count; k++) {
                int ox = region.dx * k, oy = region.dy * k, oz = region.dz * k;
                if (region.cells == null) {
                    for (int y = b[1]; y <= b[4]; y++) {
                        for (int z = b[2]; z <= b[5]; z++) {
                            for (int x = b[0]; x <= b[3]; x++) {
                                model.set(x + ox, y + oy, z + oz, state);
                            }
                        }
                    }
                    continue;
                }
                for (int i = region.cells.nextSetBit(0); i >= 0; i = region.cells.nextSetBit(i + 1)) {
                    int x = i % sx, z = (i / sx) % sz, y = i / (sx * sz);
                    model.set(b[0] + x + ox, b[1] + y + oy, b[2] + z + oz, state);
                }
            }
        }
    }

    /**
     * Eine Region: Box (sortiert, im Raster), Wiederholung und - fuer Formen - die belegten Stellen
     * der Box als Bits ({@code x + sx * (z + sz * y)}, lokal); {@code cells == null} heisst volle Box.
     */
    private record Region(int[] box, int count, int dx, int dy, int dz, BitSet cells, long cellCount) {
    }

    private static long volume(int[] b) {
        return (long) (b[3] - b[0] + 1) * (b[4] - b[1] + 1) * (b[5] - b[2] + 1);
    }

    private static long[] ordered(long[] r) {
        return new long[]{Math.min(r[0], r[3]), Math.min(r[1], r[4]), Math.min(r[2], r[5]),
                Math.max(r[0], r[3]), Math.max(r[1], r[4]), Math.max(r[2], r[5])};
    }

    /** Die Figur in der Box {@code b}: dieselbe Rechnung wie die Oktant-Auswahl ({@link OctantShape}). */
    static BitSet raster(Shape shape, int[] b) {
        int sx = b[3] - b[0] + 1, sy = b[4] - b[1] + 1, sz = b[5] - b[2] + 1;
        BitSet cells = new BitSet(sx * sy * sz);
        if (shape == Shape.BOX) {
            cells.set(0, sx * sy * sz);
            return cells;
        }
        AABB bounds = switch (shape) {
            // Kuppel: die obere Haelfte einer Kugel, deren Mittelschicht die Unterkante der Box ist.
            case DOME -> new AABB(b[0], b[1] - (b[4] - b[1]), b[2], b[3] + 1, b[4] + 1, b[5] + 1);
            default -> new AABB(b[0], b[1], b[2], b[3] + 1, b[4] + 1, b[5] + 1);
        };
        OctantItem.SelectionShape figure = switch (shape) {
            case CYLINDER -> OctantItem.SelectionShape.CYLINDER;
            case PYRAMID -> OctantItem.SelectionShape.PYRAMID;
            default -> OctantItem.SelectionShape.SPHERE;
        };
        Predicate<BlockPos> inside = OctantShape.predicate(figure, Direction.UP, bounds);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int i = 0;
        for (int y = 0; y < sy; y++) {
            for (int z = 0; z < sz; z++) {
                for (int x = 0; x < sx; x++, i++) {
                    if (inside.test(pos.set(b[0] + x, b[1] + y, b[2] + z))) {
                        cells.set(i);
                    }
                }
            }
        }
        return cells;
    }

    /**
     * Gerade Linie von Ecke {@code (r0,r1,r2)} zu {@code (r3,r4,r5)} in der Box {@code b}: so viele
     * Schritte wie die laengste Achse, jede Achse auf die naechste Stelle gerundet (halbe nach oben -
     * dadurch ist die Linie in beide Richtungen dieselbe).
     */
    static BitSet rasterLine(int[] b, long[] r) {
        int sx = b[3] - b[0] + 1, sz = b[5] - b[2] + 1;
        BitSet cells = new BitSet();
        long ddx = r[3] - r[0], ddy = r[4] - r[1], ddz = r[5] - r[2];
        long steps = Math.max(Math.abs(ddx), Math.max(Math.abs(ddy), Math.abs(ddz)));
        for (long i = 0; i <= steps; i++) {
            int x = (int) (r[0] + (steps == 0 ? 0 : Math.round((double) (ddx * i) / steps)));
            int y = (int) (r[1] + (steps == 0 ? 0 : Math.round((double) (ddy * i) / steps)));
            int z = (int) (r[2] + (steps == 0 ? 0 : Math.round((double) (ddz * i) / steps)));
            cells.set((x - b[0]) + sx * ((z - b[2]) + sz * (y - b[1])));
        }
        return cells;
    }

    /** Nur die Huelle: Stellen mit mindestens einem der sechs Nachbarn ausserhalb der Figur (oder der Box). */
    static BitSet shell(BitSet solid, int[] b) {
        int sx = b[3] - b[0] + 1, sy = b[4] - b[1] + 1, sz = b[5] - b[2] + 1;
        BitSet out = new BitSet(solid.length());
        int layer = sx * sz;
        for (int i = solid.nextSetBit(0); i >= 0; i = solid.nextSetBit(i + 1)) {
            int x = i % sx, z = (i / sx) % sz, y = i / layer;
            boolean interior = x > 0 && x < sx - 1 && y > 0 && y < sy - 1 && z > 0 && z < sz - 1
                    && solid.get(i - 1) && solid.get(i + 1) && solid.get(i - sx) && solid.get(i + sx)
                    && solid.get(i - layer) && solid.get(i + layer);
            if (!interior) {
                out.set(i);
            }
        }
        return out;
    }

    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isNameChar(char c) {
        return isLetter(c) || c == '_' || (c >= '0' && c <= '9');
    }

    private static boolean isAliasName(String name) {
        if (name.isEmpty() || name.length() > 24) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || (i > 0 && c >= '0' && c <= '9');
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private static <T extends Comparable<T>> BlockState withValue(BlockState state, Property<T> property, String value) {
        Optional<T> parsed = property.getValue(value);
        return parsed.map(v -> state.setValue(property, v)).orElse(null);
    }

    // =====================================================================================
    // SERIALISIERER
    // =====================================================================================

    /**
     * Schreibt das Modell als moeglichst kurzen, lesbaren Code: je Blockzustand eine Zeile (bei
     * Bedarf umgebrochen), die Stellen gierig zu Quadern zusammengefasst (erst entlang x, dann z,
     * dann y). Lange Zustaende, die mehr als eine Zeile brauchen, bekommen einen Alias.
     * {@code parse(serialize(m)).model()} ist wieder {@code m}.
     */
    public static String serialize(BlueprintModel model) {
        if (model.isEmpty()) {
            return "";
        }
        Map<BlockState, IntArrayList> byState = new HashMap<>();
        for (int k : model.sortedKeys()) {
            byState.computeIfAbsent(model.blocks().get(k), s -> new IntArrayList()).add(k);
        }
        List<Map.Entry<BlockState, IntArrayList>> groups = new ArrayList<>(byState.entrySet());
        groups.sort(Comparator.<Map.Entry<BlockState, IntArrayList>>comparingInt(e -> -e.getValue().size())
                .thenComparing(e -> spec(e.getKey())));

        StringBuilder header = new StringBuilder();
        header.append("# ").append(model.sizeX()).append('x').append(model.sizeY()).append('x').append(model.sizeZ())
                .append(", ").append(model.size()).append(" blocks\n");
        StringBuilder aliases = new StringBuilder();
        StringBuilder body = new StringBuilder();
        int aliasIndex = 0;
        for (Map.Entry<BlockState, IntArrayList> group : groups) {
            String spec = spec(group.getKey());
            List<String> regions = new ArrayList<>();
            for (int[] box : boxes(group.getValue())) {
                regions.add(region(box));
            }
            List<String> lines = wrap(spec, regions);
            if (lines.size() > 1 && spec.length() > 12) {
                String alias = "$" + aliasName(aliasIndex++);
                aliases.append(alias).append(" = ").append(spec).append('\n');
                lines = wrap(alias, regions);
            }
            for (String line : lines) {
                body.append(line).append('\n');
            }
        }
        return header.append(aliases).append(body).toString().stripTrailing();
    }

    private static List<String> wrap(String spec, List<String> regions) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder(spec);
        int onLine = 0;
        for (String region : regions) {
            if (onLine > 0 && line.length() + 1 + region.length() > WRAP) {
                lines.add(line.toString());
                line = new StringBuilder(spec);
                onLine = 0;
            }
            line.append(' ').append(region);
            onLine++;
        }
        lines.add(line.toString());
        return lines;
    }

    private static String aliasName(int index) {
        StringBuilder sb = new StringBuilder();
        int i = index;
        do {
            sb.insert(0, (char) ('a' + i % 26));
            i = i / 26 - 1;
        } while (i >= 0);
        return sb.toString();
    }

    private static String region(int[] b) {
        return axis(b[0], b[3]) + "," + axis(b[1], b[4]) + "," + axis(b[2], b[5]);
    }

    private static String axis(int lo, int hi) {
        return lo == hi ? Integer.toString(lo) : lo + ".." + hi;
    }

    /** Gierige Quader: Reihe entlang x, dann so weit wie moeglich in z, dann Schichten in y. */
    static List<int[]> boxes(IntArrayList sortedKeys) {
        IntOpenHashSet remaining = new IntOpenHashSet(sortedKeys);
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i < sortedKeys.size(); i++) {
            int k = sortedKeys.getInt(i);
            if (!remaining.contains(k)) {
                continue;
            }
            int x = BlueprintModel.keyX(k), y = BlueprintModel.keyY(k), z = BlueprintModel.keyZ(k);
            int x2 = x;
            while (x2 + 1 < GRID && remaining.contains(BlueprintModel.key(x2 + 1, y, z))) {
                x2++;
            }
            int z2 = z;
            while (z2 + 1 < GRID && rowFree(remaining, x, x2, y, z2 + 1)) {
                z2++;
            }
            int y2 = y;
            while (y2 + 1 < GRID && layerFree(remaining, x, x2, y2 + 1, z, z2)) {
                y2++;
            }
            for (int yy = y; yy <= y2; yy++) {
                for (int zz = z; zz <= z2; zz++) {
                    for (int xx = x; xx <= x2; xx++) {
                        remaining.remove(BlueprintModel.key(xx, yy, zz));
                    }
                }
            }
            out.add(new int[]{x, y, z, x2, y2, z2});
        }
        return out;
    }

    private static boolean rowFree(IntOpenHashSet set, int x1, int x2, int y, int z) {
        for (int x = x1; x <= x2; x++) {
            if (!set.contains(BlueprintModel.key(x, y, z))) {
                return false;
            }
        }
        return true;
    }

    private static boolean layerFree(IntOpenHashSet set, int x1, int x2, int y, int z1, int z2) {
        for (int z = z1; z <= z2; z++) {
            if (!rowFree(set, x1, x2, y, z)) {
                return false;
            }
        }
        return true;
    }

    /** Kurzform eines Zustands: Id ohne {@code minecraft:}, nur Eigenschaften abseits des Standards. */
    public static String spec(BlockState state) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String name = "minecraft".equals(id.getNamespace()) ? id.getPath() : id.toString();
        BlockState defaults = state.getBlock().defaultBlockState();
        StringBuilder props = new StringBuilder();
        for (Property<?> property : state.getProperties()) {
            if (!state.getValue(property).equals(defaults.getValue(property))) {
                if (!props.isEmpty()) {
                    props.append(',');
                }
                props.append(property.getName()).append('=').append(valueName(state, property));
            }
        }
        return props.isEmpty() ? name : name + "[" + props + "]";
    }

    private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    /** Fuer Tests und Werkzeuge: die gierigen Quader eines Modells, je Zustand. */
    public static int countBoxes(BlueprintModel model) {
        Map<BlockState, IntArrayList> byState = new HashMap<>();
        for (int k : model.sortedKeys()) {
            byState.computeIfAbsent(model.blocks().get(k), s -> new IntArrayList()).add(k);
        }
        int n = 0;
        for (IntArrayList keys : byState.values()) {
            n += boxes(keys).size();
        }
        return n;
    }

    /** Der Standardzustand fuer "nichts": Luft. */
    public static boolean isClearing(BlockState state) {
        return state.isAir() || state.is(Blocks.AIR);
    }
}
