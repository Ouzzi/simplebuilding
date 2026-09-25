package com.simplebuilding.blueprint;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Die Bausprache der Blaupause: Parser, Serialisierer und Syntax-Einfaerbung aus einer Hand.
 *
 * <p>Spezifikation mit Beispielen: {@code docs/BLUEPRINT.md}. Kurz:
 * <pre>
 * # Kommentar bis zum Zeilenende
 * $treppe = oak_stairs[facing=east,half=top]   Alias (spaeter: Variablen)
 * stone 0..4,0,0..4                             Block + Bereiche (x,y,z; a..b je Achse)
 * glass 1,1,1..1,1,5                            Eckenform: von Ecke bis Ecke
 * oak_fence 0,1,0*5@2,0,0                       Wiederholung: 5 Stueck im Abstand 2,0,0
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
    public static final byte STYLE_PROPERTY = 4;
    public static final byte STYLE_VALUE = 5;
    public static final byte STYLE_ALIAS = 6;
    public static final byte STYLE_NUMBER = 7;
    public static final byte STYLE_OPERATOR = 8;
    public static final byte STYLE_ERROR = 9;
    public static final byte STYLE_AIR = 10;

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

    private static final class Parser {
        final String code;
        final byte[] styles;
        final BlueprintModel model = new BlueprintModel();
        final List<Problem> problems = new ArrayList<>();
        final Map<String, BlockState> aliases = new HashMap<>();
        long expanded;
        boolean budgetExceeded;
        int line;

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
                    defineAlias(new Word(first.text.substring(0, eq), first.start), first.start + eq, rest);
                    return;
                }
                if (words.size() > 1 && words.get(1).text.startsWith("=")) {
                    Word eqWord = words.get(1);
                    List<Word> rest = new ArrayList<>();
                    if (eqWord.text.length() > 1) {
                        rest.add(new Word(eqWord.text.substring(1), eqWord.start + 1));
                    }
                    rest.addAll(words.subList(2, words.size()));
                    defineAlias(first, eqWord.start, rest);
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

        void defineAlias(Word name, int eqPos, List<Word> rest) {
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
            if (rest.size() > 1) {
                Word extra = rest.get(1);
                problem(extra.start, rest.get(rest.size() - 1).end(), "alias_extra");
            }
            Word spec = rest.get(0);
            if (spec.text.startsWith("$")) {
                problem(spec.start, spec.end(), "alias_of_alias");
                return;
            }
            BlockState state = parseSpec(spec);
            if (state != null) {
                aliases.put(aliasName, state);
            }
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
                StringBuilder text = new StringBuilder();
                while (i < to) {
                    char c = code.charAt(i);
                    if (c == '[') depth++;
                    if (c == ']') depth = Math.max(0, depth - 1);
                    if (depth == 0 && Character.isWhitespace(c)) {
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
                BlockState state = aliases.get(text.substring(1));
                if (state == null) {
                    problem(word.start, word.end(), "unknown_alias", text);
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

        Region parseRegion(Word word) {
            String text = word.text;
            // Einfaerben: Ziffern und Minus als Zahl, alles andere als Operator.
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                styles[word.start + i] = (Character.isDigit(c) || c == '-') ? STYLE_NUMBER : STYLE_OPERATOR;
            }
            String base = text;
            int count = 1;
            int dx = 0, dy = 0, dz = 0;
            int star = text.indexOf('*');
            if (star >= 0) {
                base = text.substring(0, star);
                String repeat = text.substring(star + 1);
                int at = repeat.indexOf('@');
                if (at < 0) {
                    problem(word.start + star, word.end(), "bad_repeat", text.substring(star));
                    return null;
                }
                Integer n = parseInt(repeat.substring(0, at));
                int[] step = parseTriple(repeat.substring(at + 1));
                if (n == null || step == null) {
                    problem(word.start + star, word.end(), "bad_repeat", text.substring(star));
                    return null;
                }
                if (n < 1 || n > MAX_REPEAT) {
                    problem(word.start + star + 1, word.start + star + 1 + at, "repeat_range", String.valueOf(n), String.valueOf(MAX_REPEAT));
                    return null;
                }
                count = n;
                dx = step[0];
                dy = step[1];
                dz = step[2];
            }
            int[] box = parseBox(base);
            if (box == null) {
                problem(word.start, word.start + base.length(), "bad_region", base);
                return null;
            }
            for (int v : box) {
                if (v < 0 || v >= GRID) {
                    problem(word.start, word.start + base.length(), "coordinate_range", String.valueOf(v), String.valueOf(GRID - 1));
                    return null;
                }
            }
            return new Region(box, count, dx, dy, dz);
        }

        void apply(Region region, BlockState state, Word word) {
            if (budgetExceeded) {
                return;
            }
            int[] b = region.box;
            long cells = (long) (b[3] - b[0] + 1) * (b[4] - b[1] + 1) * (b[5] - b[2] + 1) * region.count;
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
            for (int k = 0; k < region.count; k++) {
                int ox = region.dx * k, oy = region.dy * k, oz = region.dz * k;
                for (int y = b[1]; y <= b[4]; y++) {
                    for (int z = b[2]; z <= b[5]; z++) {
                        for (int x = b[0]; x <= b[3]; x++) {
                            model.set(x + ox, y + oy, z + oz, state);
                        }
                    }
                }
            }
        }
    }

    private record Region(int[] box, int count, int dx, int dy, int dz) {
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

    private static Integer parseInt(String s) {
        if (s.isEmpty() || s.length() > 6) {
            return null;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int[] parseTriple(String s) {
        String[] parts = s.split(",", -1);
        if (parts.length != 3) {
            return null;
        }
        int[] out = new int[3];
        for (int i = 0; i < 3; i++) {
            Integer v = parseInt(parts[i]);
            if (v == null) {
                return null;
            }
            out[i] = v;
        }
        return out;
    }

    /** {@code x1,y1,z1,x2,y2,z2} mit x1<=x2 usw., aus Achsenform oder Eckenform. */
    private static int[] parseBox(String s) {
        int dots = s.indexOf("..");
        if (dots >= 0 && s.indexOf("..", dots + 2) < 0) {
            int[] a = parseTriple(s.substring(0, dots));
            int[] b = parseTriple(s.substring(dots + 2));
            if (a != null && b != null) {
                return order(a[0], a[1], a[2], b[0], b[1], b[2]);
            }
        }
        String[] parts = s.split(",", -1);
        if (parts.length != 3) {
            return null;
        }
        int[] lo = new int[3];
        int[] hi = new int[3];
        for (int i = 0; i < 3; i++) {
            String p = parts[i];
            int range = p.indexOf("..");
            if (range >= 0) {
                Integer a = parseInt(p.substring(0, range));
                Integer b = parseInt(p.substring(range + 2));
                if (a == null || b == null) {
                    return null;
                }
                lo[i] = a;
                hi[i] = b;
            } else {
                Integer a = parseInt(p);
                if (a == null) {
                    return null;
                }
                lo[i] = a;
                hi[i] = a;
            }
        }
        return order(lo[0], lo[1], lo[2], hi[0], hi[1], hi[2]);
    }

    private static int[] order(int x1, int y1, int z1, int x2, int y2, int z2) {
        return new int[]{Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2)};
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
