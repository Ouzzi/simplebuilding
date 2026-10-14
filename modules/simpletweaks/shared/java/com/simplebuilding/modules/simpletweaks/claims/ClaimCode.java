package com.simplebuilding.modules.simpletweaks.claims;

import com.simplebuilding.modules.simpletweaks.claims.ClaimRights.Rules;
import java.util.*;

/**
 * Parser and generator of the rights code (server authority; the client may only preview). Hard limits are
 * checked before any parsing; no regular expressions, no network lookups (names resolve through {@link Names}).
 * <pre>
 * # comment
 * *: U               every player
 * Steve: BDU         name or UUID, then letters or a role (@guest @helper @member @manager)
 * [12,-4]            a section: replaces the rules for that chunk ([nether 3,7] names the dimension)
 * Steve: BDUC
 * </pre>
 */
public final class ClaimCode {
    public static final int MAX_CHARS = 4096, MAX_LINES = 200, MAX_LINE = 96, MAX_SECTIONS = 32, MAX_ERRORS = 16;
    /** World border is 30 million blocks; far beyond is a typo, and it keeps the arithmetic safe. */
    static final int MAX_COORD = 2_000_000;

    /** Resolves a name to a UUID without any blocking lookup; null = unknown. */
    @FunctionalInterface public interface Names { UUID resolve(String name); }
    /** {@code key} is a stable id ({@code syntax}, {@code unknown_letter}, ...), {@code arg} the offending text. Line 0 = whole code. */
    public record Error(int line, String key, String arg) {}
    public record At(String dimension, int x, int z) {}
    /** {@code notes} are hints that do not block saving. {@code sectionLines} maps a section to its source line. */
    public record Parsed(boolean ok, List<Error> errors, List<Error> notes, Rules top, Map<At,Rules> sections, Map<At,Integer> sectionLines) {}

    private static final class Scope {
        int everyone; final Map<UUID,Integer> members = new LinkedHashMap<>(); boolean hasEveryone;
        Rules rules() { return new Rules(everyone, members); }
    }

    /**
     * @param owner the land owner (cannot appear as a person)
     * @param defaultDimension dimension for sections without one
     * @param maxPeople persons allowed per scope (config {@code maxTrustedPlayers})
     */
    public static Parsed parse(String code, UUID owner, String defaultDimension, Names names, int maxPeople) {
        var errors = new ArrayList<Error>(); var notes = new ArrayList<Error>();
        var empty = new Parsed(false, errors, notes, Rules.EMPTY, Map.of(), Map.of());
        if (code == null) { errors.add(new Error(0, "syntax", "")); return empty; }
        if (code.length() > MAX_CHARS) { errors.add(new Error(0, "too_long", Integer.toString(MAX_CHARS))); return empty; }
        String[] lines = code.split("\n", -1);
        if (lines.length > MAX_LINES) { errors.add(new Error(MAX_LINES + 1, "too_many_lines", Integer.toString(MAX_LINES))); return empty; }
        var top = new Scope(); var sections = new LinkedHashMap<At,Scope>(); var sectionLines = new LinkedHashMap<At,Integer>();
        Scope current = top; boolean discard = false;
        for (int i = 0; i < lines.length && errors.size() < MAX_ERRORS; i++) {
            int no = i + 1;
            String line = lines[i];
            if (line.length() > MAX_LINE) { errors.add(new Error(no, "line_too_long", Integer.toString(MAX_LINE))); continue; }
            int hash = line.indexOf('#');
            if (hash >= 0) line = line.substring(0, hash);
            line = line.strip();
            if (line.isEmpty()) continue;
            if (hasControl(line)) { errors.add(new Error(no, "bad_char", "")); continue; }
            if (line.charAt(0) == '[') {
                At at = section(line, defaultDimension);
                if (at == null) { errors.add(new Error(no, "bad_section", line)); discard = true; current = new Scope(); continue; }
                if (sections.containsKey(at)) { errors.add(new Error(no, "duplicate_section", line)); discard = true; current = new Scope(); continue; }
                if (sections.size() >= MAX_SECTIONS) { errors.add(new Error(no, "too_many_sections", Integer.toString(MAX_SECTIONS))); discard = true; current = new Scope(); continue; }
                current = new Scope(); sections.put(at, current); sectionLines.put(at, no); discard = false;
                continue;
            }
            person(line, no, current, owner, names, maxPeople, errors, notes);
        }
        var out = new LinkedHashMap<At,Rules>();
        sections.forEach((k, v) -> out.put(k, v.rules()));
        return new Parsed(errors.isEmpty(), List.copyOf(errors), List.copyOf(notes), top.rules(), Collections.unmodifiableMap(out), Collections.unmodifiableMap(sectionLines));
    }

    private static boolean hasControl(String s) {
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) < 32 || s.charAt(i) == 127) return true;
        return false;
    }

    private static void person(String line, int no, Scope scope, UUID owner, Names names, int maxPeople, List<Error> errors, List<Error> notes) {
        int colon = line.indexOf(':');
        if (colon <= 0) { errors.add(new Error(no, "syntax", line)); return; }
        String who = line.substring(0, colon).strip(), rhs = line.substring(colon + 1).strip();
        if (who.isEmpty()) { errors.add(new Error(no, "syntax", line)); return; }
        int bits = 0;
        if (rhs.startsWith("@")) {
            String role = rhs.substring(1);
            if (role.isEmpty() || role.chars().anyMatch(Character::isWhitespace)) { errors.add(new Error(no, "syntax", rhs)); return; }
            bits = ClaimRights.role(role);
            if (bits < 0) { errors.add(new Error(no, "unknown_role", role)); return; }
        } else {
            for (int i = 0; i < rhs.length(); i++) {
                char c = rhs.charAt(i);
                if (Character.isWhitespace(c)) continue;
                int b = ClaimRights.bit(c);
                if (b < 0) { errors.add(new Error(no, "unknown_letter", String.valueOf(c))); return; }
                if ((bits & b) != 0) notes.add(new Error(no, "duplicate_letter", String.valueOf(Character.toUpperCase(c))));
                bits |= b;
            }
        }
        if (who.equals("*")) {
            if (bits == 0) { notes.add(new Error(no, "no_rights", who)); return; }
            if ((bits & ClaimRights.MANAGE) != 0) { errors.add(new Error(no, "everyone_manage", who)); return; }
            if (scope.hasEveryone) { errors.add(new Error(no, "duplicate", who)); return; }
            scope.hasEveryone = true; scope.everyone = bits; return;
        }
        UUID id = identity(who, names);
        if (id == null) { errors.add(new Error(no, who.length() == 36 && who.indexOf('-') >= 0 ? "bad_uuid" : validName(who) ? "unknown_name" : "syntax", who)); return; }
        if (id.equals(new UUID(0, 0))) { errors.add(new Error(no, "null_uuid", who)); return; }
        if (id.equals(owner)) { errors.add(new Error(no, "owner", who)); return; }
        if (scope.members.containsKey(id)) { errors.add(new Error(no, "duplicate", who)); return; }
        if (bits == 0) { notes.add(new Error(no, "no_rights", who)); return; }
        if (scope.members.size() >= Math.min(maxPeople, ClaimRights.MAX_MEMBERS)) { errors.add(new Error(no, "too_many", Integer.toString(maxPeople))); return; }
        scope.members.put(id, bits);
    }

    private static UUID identity(String who, Names names) {
        if (who.length() == 36) {
            for (int i = 0; i < 36; i++) {
                char c = who.charAt(i);
                boolean dash = i == 8 || i == 13 || i == 18 || i == 23;
                if (dash ? c != '-' : Character.digit(c, 16) < 0 || c > 127) return null;
            }
            return UUID.fromString(who);
        }
        return validName(who) ? names.resolve(who) : null;
    }
    private static boolean validName(String s) {
        if (s.isEmpty() || s.length() > 16) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_')) return false;
        }
        return true;
    }

    /** {@code [x,z]} or {@code [dimension x,z]}; null if malformed. */
    private static At section(String line, String defaultDimension) {
        if (!line.endsWith("]")) return null;
        String inner = line.substring(1, line.length() - 1).strip();
        String dimension = defaultDimension, coords = inner;
        int space = inner.lastIndexOf(' ');
        if (space >= 0) {
            dimension = dimensionId(inner.substring(0, space).strip());
            coords = inner.substring(space + 1).strip();
            if (dimension == null) return null;
        }
        int comma = coords.indexOf(',');
        if (comma <= 0 || coords.indexOf(',', comma + 1) >= 0) return null;
        Integer x = coord(coords.substring(0, comma).strip()), z = coord(coords.substring(comma + 1).strip());
        return x == null || z == null ? null : new At(dimension, x, z);
    }
    private static Integer coord(String s) {
        if (s.isEmpty() || s.length() > 8) return null;
        int start = s.charAt(0) == '-' ? 1 : 0;
        if (start == s.length()) return null;
        for (int i = start; i < s.length(); i++) if (s.charAt(i) < '0' || s.charAt(i) > '9') return null;
        int v = Integer.parseInt(s);
        return Math.abs(v) > MAX_COORD ? null : v;
    }
    static String dimensionId(String name) {
        switch (name.toLowerCase(Locale.ROOT)) {
            case "overworld": return "minecraft:overworld";
            case "nether": return "minecraft:the_nether";
            case "end": return "minecraft:the_end";
            default:
        }
        if (name.length() > 128 || name.indexOf(':') <= 0) return null;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '-' || c == '.' || c == ':' || c == '/')) return null;
        }
        return name;
    }
    private static String shortDimension(String id) {
        return switch (id) { case "minecraft:overworld" -> "overworld"; case "minecraft:the_nether" -> "nether"; case "minecraft:the_end" -> "end"; default -> id; };
    }

    private static final String HEADER = """
            # Claim rights. One line per person: name or UUID, a colon, then letters or a role.
            # B Build  D Destroy  U Use  C Container  R Redstone  A Animals/vehicles  M Manage
            # Roles: @guest (U)  @helper (UCA)  @member (BDUCRA)  @manager (all)
            # *: U gives every player a right. [x,z] starts a section replacing the rules of that chunk.
            """;
    /** Canonical code for a rule set (header comment, UUIDs). Parsing it yields the same rules. Null if it exceeds the code limits. */
    public static String generate(Rules top, Map<At,Rules> sections) {
        var out = new StringBuilder(HEADER);
        lines(out, top);
        sections.entrySet().stream().sorted(Comparator.comparing((Map.Entry<At,Rules> e) -> e.getKey().dimension())
                .thenComparingInt(e -> e.getKey().x()).thenComparingInt(e -> e.getKey().z())).forEach(e -> {
            out.append('[').append(shortDimension(e.getKey().dimension())).append(' ').append(e.getKey().x()).append(',').append(e.getKey().z()).append("]\n");
            lines(out, e.getValue());
        });
        String text = out.toString();
        return text.length() > MAX_CHARS || text.split("\n", -1).length > MAX_LINES || sections.size() > MAX_SECTIONS ? null : text;
    }
    private static void lines(StringBuilder out, Rules rules) {
        if (rules.everyone() != 0) out.append("*: ").append(ClaimRights.describe(rules.everyone())).append('\n');
        rules.members().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(e -> out.append(e.getKey()).append(": ").append(ClaimRights.describe(e.getValue())).append('\n'));
    }
    private ClaimCode() {}
}
