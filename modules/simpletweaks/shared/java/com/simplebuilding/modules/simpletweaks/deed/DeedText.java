package com.simplebuilding.modules.simpletweaks.deed;

import com.simplebuilding.modules.simpletweaks.claims.ClaimCode;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Pure helpers of the deed editor (no Minecraft classes, JUnit-tested): inserting a person, finding a source line,
 * the client-side preview parse and the one-line status. The server never uses the preview; it parses again.
 */
public final class DeedText {
    /** What the status line says about a parse. {@code firstLine} 0 = no error or a whole-code error. */
    public record Status(boolean ok, int errors, int firstLine, String firstKey, String firstArg, int people, int sections, int notes) {}

    private DeedText() {}

    public static int lineCount(String code) {
        int n = 1;
        for (int i = 0; i < code.length(); i++) if (code.charAt(i) == '\n') n++;
        return n;
    }

    /** Offset where source line {@code line} (1-based) starts, or -1 if there is no such line. */
    public static int lineStart(String code, int line) {
        if (line < 1) return -1;
        int at = 0;
        for (int l = 1; l < line; l++) {
            int nl = code.indexOf('\n', at);
            if (nl < 0) return -1;
            at = nl + 1;
        }
        return at;
    }

    /** True if a person line for {@code name} (case-insensitive) already exists. */
    public static boolean hasPerson(String code, String name) {
        for (String raw : code.split("\n", -1)) {
            int hash = raw.indexOf('#');
            String body = (hash >= 0 ? raw.substring(0, hash) : raw).strip();
            int colon = body.indexOf(':');
            if (colon > 0 && body.substring(0, colon).strip().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    /**
     * Adds {@code name: letters} to the top part: before the first {@code [x,z]} section, otherwise at the end (a
     * person appended after a section would end up in that section). Unchanged if the person exists or the result
     * would exceed the code limits.
     */
    public static String addPerson(String code, String name, String letters) {
        if (name == null || name.isEmpty() || hasPerson(code, name)) return code;
        String line = name + ": " + letters + "\n";
        int at = code.length();
        int offset = 0;
        for (String raw : code.split("\n", -1)) {
            int hash = raw.indexOf('#');
            String body = (hash >= 0 ? raw.substring(0, hash) : raw).strip();
            if (!body.isEmpty() && body.charAt(0) == '[') { at = offset; break; }
            offset += raw.length() + 1;
        }
        String out;
        if (at == code.length()) out = code + (code.isEmpty() || code.endsWith("\n") ? "" : "\n") + line;
        else out = code.substring(0, at) + line + code.substring(at);
        return out.length() > ClaimCode.MAX_CHARS || lineCount(out) > ClaimCode.MAX_LINES ? code : out;
    }

    /**
     * Resolver of the preview: the owner's name resolves to the owner, every other known name to a stable fake id
     * (the preview only needs "known or not" and "same person twice"; the server resolves for real).
     */
    public static ClaimCode.Names previewNames(UUID owner, String ownerName, Collection<String> known) {
        return name -> {
            String key = name.toLowerCase(Locale.ROOT);
            if (key.equals(ownerName.toLowerCase(Locale.ROOT))) return owner;
            for (String k : known) if (k.equalsIgnoreCase(name)) return UUID.nameUUIDFromBytes(("deed:" + key).getBytes(StandardCharsets.UTF_8));
            return null;
        };
    }

    public static ClaimCode.Parsed preview(String code, UUID owner, String ownerName, String dimension, List<String> known, int maxPeople) {
        return ClaimCode.parse(code, owner, dimension, previewNames(owner, ownerName, known), maxPeople);
    }

    public static Status status(ClaimCode.Parsed parsed) {
        var first = parsed.errors().isEmpty() ? null : parsed.errors().get(0);
        return new Status(parsed.ok(), parsed.errors().size(), first == null ? 0 : first.line(), first == null ? "" : first.key(),
                first == null ? "" : first.arg(), parsed.top().members().size(), parsed.sections().size(), parsed.notes().size());
    }
}
