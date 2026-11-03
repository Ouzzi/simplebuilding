package com.simplelib.api.client.keys;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simplelib.api.client.keys.KeyboardLayout.Cell;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Rules of the keyboard editor (b-keys34) as pure data, so game tests can check them: a layout is valid when no key is twice,
 * no two keys overlap, every cap is inside the grid and an L-shaped cap is one connected piece. Edits that would break a rule
 * are refused ({@link #replace}), a pressed key that is missing is added on the first free place ({@link #add}). Layouts are
 * saved with Minecraft key names ({@code key.keyboard.*}), never numbers, so they survive a change of key numbering.
 */
public final class KeyboardEdit {
    /** Grid limits in quarter keys and the key count (a full 100 % board has 104). */
    public static final int MAX_X = 160, MAX_Y = 64, MAX_KEYS = 160, MAX_SIZE = 40, MAX_LABEL = 5, MAX_NAME = 16;
    /** First row (quarter keys) for added keys: just below the standard board (26 high); a taller layout pushes them down. */
    public static final int EXTRA_ROW = 28;

    private KeyboardEdit() {}

    /** A broken rule: {@code kind} is overlap / duplicate / outside / shape / empty / count, {@code a} and {@code b} the captions involved. */
    public record Problem(String kind, String a, String b) {}

    /** The first broken rule of {@code cells}, or empty when the layout is valid. */
    public static Optional<Problem> problem(List<Cell> cells) {
        if (cells.isEmpty()) return Optional.of(new Problem("empty", "", ""));
        if (cells.size() > MAX_KEYS) return Optional.of(new Problem("count", String.valueOf(cells.size()), ""));
        Set<Integer> codes = new HashSet<>();
        for (int i = 0; i < cells.size(); i++) {
            Cell c = cells.get(i);
            if (!codes.add(c.code())) return Optional.of(new Problem("duplicate", c.label(), ""));
            if (c.w() < 1 || c.h() < 1 || c.w() > MAX_SIZE || c.h() > MAX_SIZE || c.left() < 0 || c.top() < 0 || c.right() > MAX_X
                    || c.bottom() > MAX_Y || c.l() && (c.ew() > MAX_SIZE || c.eh() > MAX_SIZE)) {
                return Optional.of(new Problem("outside", c.label(), ""));
            }
            if (c.l() && (!touches(c) || rectsOverlap(c.x(), c.y(), c.w(), c.h(), c.ex(), c.ey(), c.ew(), c.eh()))) {
                return Optional.of(new Problem("shape", c.label(), ""));
            }
            for (int j = i + 1; j < cells.size(); j++) {
                if (c.overlaps(cells.get(j))) return Optional.of(new Problem("overlap", c.label(), cells.get(j).label()));
            }
        }
        return Optional.empty();
    }

    /** True when the two rectangles of an L-shaped cap share an edge of positive length. */
    private static boolean touches(Cell c) {
        boolean beside = (c.x() + c.w() == c.ex() || c.ex() + c.ew() == c.x()) && c.y() < c.ey() + c.eh() && c.ey() < c.y() + c.h();
        boolean above = (c.y() + c.h() == c.ey() || c.ey() + c.eh() == c.y()) && c.x() < c.ex() + c.ew() && c.ex() < c.x() + c.w();
        return beside || above;
    }

    private static boolean rectsOverlap(int ax, int ay, int aw, int ah, int bx, int by, int bw, int bh) {
        return ax < bx + bw && bx < ax + aw && ay < by + bh && by < ay + ah;
    }

    /** {@code cells} with the cap of {@code updated.code()} replaced; empty when the result would be invalid (the edit is refused). */
    public static Optional<List<Cell>> replace(List<Cell> cells, Cell updated) {
        List<Cell> out = new ArrayList<>(cells);
        for (int i = 0; i < out.size(); i++) {
            if (out.get(i).code() == updated.code()) {
                out.set(i, updated);
                return problem(out).isPresent() ? Optional.empty() : Optional.of(out);
            }
        }
        return Optional.empty();
    }

    /** {@code cells} without the key {@code code}. */
    public static List<Cell> remove(List<Cell> cells, int code) {
        List<Cell> out = new ArrayList<>(cells);
        out.removeIf(c -> c.code() == code);
        return out;
    }

    /**
     * {@code cells} plus a normal key (4 x 4) for {@code code} on the first free place: from {@link #EXTRA_ROW} down, row by row
     * from the left. Unchanged when the key is already there or no place is left.
     */
    public static List<Cell> add(List<Cell> cells, int code, String label) {
        for (Cell c : cells) if (c.code() == code) return cells;
        for (int y = EXTRA_ROW; y + KeyboardLayout.UNIT <= MAX_Y; y += KeyboardLayout.UNIT) {
            for (int x = 0; x + KeyboardLayout.UNIT <= MAX_X; x += KeyboardLayout.UNIT) {
                Cell fresh = new Cell(code, label, x, y, KeyboardLayout.UNIT, KeyboardLayout.UNIT);
                boolean free = true;
                for (Cell c : cells) free &= !c.overlaps(fresh);
                if (free) {
                    List<Cell> out = new ArrayList<>(cells);
                    out.add(fresh);
                    return problem(out).isPresent() ? cells : out;
                }
            }
        }
        return cells;
    }

    /** A layout name usable for saving: 1-16 visible characters, not a built-in id. */
    public static boolean validName(String name) {
        if (name == null || name.isBlank() || name.length() > MAX_NAME || !name.equals(name.strip())) return false;
        if (KeyboardLayout.PRESETS.contains(name.toLowerCase(java.util.Locale.ROOT))) return false;
        for (char ch : name.toCharArray()) if (Character.isISOControl(ch)) return false;
        return true;
    }

    // --- JSON ---------------------------------------------------------------------------------------------------------

    /** The layout as JSON: a list of {@code {key,label,x,y,w,h[,ex,ey,ew,eh]}}. */
    public static JsonArray encode(List<Cell> cells) {
        JsonArray out = new JsonArray();
        for (Cell c : cells) {
            JsonObject o = new JsonObject();
            o.addProperty("key", KeyboardLayout.keyName(c.code()));
            o.addProperty("label", c.label());
            o.addProperty("x", c.x());
            o.addProperty("y", c.y());
            o.addProperty("w", c.w());
            o.addProperty("h", c.h());
            if (c.l()) {
                o.addProperty("ex", c.ex());
                o.addProperty("ey", c.ey());
                o.addProperty("ew", c.ew());
                o.addProperty("eh", c.eh());
            }
            out.add(o);
        }
        return out;
    }

    /** Reads {@link #encode}; keys with an unknown name or broken numbers are skipped, an invalid result gives empty. */
    public static Optional<List<Cell>> decode(JsonElement json) {
        if (json == null || !json.isJsonArray()) return Optional.empty();
        List<Cell> out = new ArrayList<>();
        try {
            for (JsonElement e : json.getAsJsonArray()) {
                JsonObject o = e.getAsJsonObject();
                int code = KeyboardLayout.codeOf(o.get("key").getAsString());
                if (code < 0) continue;
                String label = o.get("label").getAsString();
                if (label.length() > MAX_LABEL) label = label.substring(0, MAX_LABEL);
                out.add(new Cell(code, label, o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("w").getAsInt(), o.get("h").getAsInt(),
                        o.has("ew") ? o.get("ex").getAsInt() : 0, o.has("ew") ? o.get("ey").getAsInt() : 0,
                        o.has("ew") ? o.get("ew").getAsInt() : 0, o.has("ew") ? o.get("eh").getAsInt() : 0));
            }
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
        return problem(out).isPresent() ? Optional.empty() : Optional.of(out);
    }
}
