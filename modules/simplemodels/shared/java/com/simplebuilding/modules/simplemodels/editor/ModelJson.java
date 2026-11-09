package com.simplebuilding.modules.simplemodels.editor;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.simplebuilding.modules.simplemodels.editor.EditableModel.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Editor stage 1 (b-models12): vanilla 26.3 model JSON to {@link EditableModel} and back. Vanilla JSON is the storage and
 * export format, so the bounds are the vanilla ones ({@code CuboidModelElement}: from/to -16..32, light 0..15, face
 * rotation 0/90/180/270; {@code ItemTransform}: translation ±80 px, scale ±4) plus editor limits (256 elements, 65 536
 * characters, nesting 16, uv 0..16). All or nothing: any problem means no model. Problems carry a JSON path such as
 * {@code elements[2].from} and a translation key with arguments; line/column come with the code editor (stage 4).
 */
public final class ModelJson {
    public static final int MAX_ELEMENTS = 256, MAX_CHARS = 65536, MAX_DEPTH = 16, MAX_TEXTURES = 64, MAX_NAME = 48;
    public static final float MIN_EXTENT = -16, MAX_EXTENT = 32, MAX_TRANSLATION = 80, MAX_SCALE = 4;
    private static final Pattern ID = Pattern.compile("([a-z0-9_.-]{1,64}:)?[a-z0-9_./-]{1,128}");
    private static final Pattern VARIABLE = Pattern.compile("[A-Za-z0-9_]{1,32}");

    /** One problem: {@code key} is a translation key under {@code simplemodels.editor.problem.}, {@code args} fill it. */
    public record Problem(String path, String key, List<Object> args) {
        @Override public String toString() { return path + ": " + key + (args.isEmpty() ? "" : " " + args); }
    }
    /** {@code model} is {@code null} exactly when {@code problems} is not empty. */
    public record Result(EditableModel model, List<Problem> problems) {
        public boolean ok() { return model != null; }
    }

    // ---------------------------------------------------------------- read

    public static Result read(String json) {
        var problems = new ArrayList<Problem>();
        if (json == null || json.length() > MAX_CHARS) return fail("", "too_long", MAX_CHARS);
        if (depth(json) > MAX_DEPTH) return fail("", "too_deep", MAX_DEPTH);
        JsonObject root;
        try {
            var parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) return fail("", "not_object");
            root = parsed.getAsJsonObject();
        } catch (RuntimeException e) { return fail("", "syntax", String.valueOf(e.getMessage())); }
        var reader = new Reader(problems);
        var model = reader.model(root);
        return problems.isEmpty() ? new Result(model, List.of()) : new Result(null, List.copyOf(problems));
    }

    private static Result fail(String path, String key, Object... args) {
        return new Result(null, List.of(new Problem(path, key, List.of(args))));
    }

    private static int depth(String json) {
        int depth = 0, max = 0; boolean quoted = false, escaped = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (quoted) { if (escaped) escaped = false; else if (c == '\\') escaped = true; else if (c == '"') quoted = false; }
            else if (c == '"') quoted = true;
            else if (c == '{' || c == '[') max = Math.max(max, ++depth);
            else if (c == '}' || c == ']') depth--;
        }
        return max;
    }

    private static final class Reader {
        final List<Problem> problems;
        Reader(List<Problem> problems) { this.problems = problems; }

        void problem(String path, String key, Object... args) { problems.add(new Problem(path, key, List.of(args))); }

        EditableModel model(JsonObject root) {
            String parent = root.has("parent") ? string(root, "parent", "parent") : null;
            if (parent != null && !parent.isEmpty() && !id(parent)) problem("parent", "bad_id", parent);
            if ("".equals(parent)) parent = null;
            var textures = new LinkedHashMap<String, String>();
            if (root.has("textures")) {
                var obj = object(root, "textures", "textures");
                if (obj != null) {
                    if (obj.size() > MAX_TEXTURES) problem("textures", "too_many_textures", MAX_TEXTURES);
                    for (var e : obj.entrySet()) {
                        String path = "textures." + e.getKey();
                        if (!VARIABLE.matcher(e.getKey()).matches()) problem(path, "bad_variable", e.getKey());
                        String value = primitiveString(e.getValue(), path);
                        if (value != null && !texture(value)) problem(path, "bad_texture", value);
                        if (value != null) textures.put(e.getKey(), value);
                    }
                }
            }
            var elements = new ArrayList<Element>();
            if (root.has("elements")) {
                var array = array(root, "elements", "elements");
                if (array != null) {
                    if (array.size() > MAX_ELEMENTS) problem("elements", "too_many_elements", MAX_ELEMENTS);
                    else for (int i = 0; i < array.size(); i++) {
                        String path = "elements[" + i + "]";
                        if (!array.get(i).isJsonObject()) { problem(path, "not_object"); continue; }
                        var element = element(array.get(i).getAsJsonObject(), path);
                        if (element != null) elements.add(element);
                    }
                }
            }
            var display = new LinkedHashMap<String, Transform>();
            if (root.has("display")) {
                var obj = object(root, "display", "display");
                if (obj != null) for (var e : obj.entrySet()) {
                    String path = "display." + e.getKey();
                    if (!EditableModel.DISPLAY_SLOTS.contains(e.getKey())) { problem(path, "bad_display_slot", e.getKey()); continue; }
                    if (!e.getValue().isJsonObject()) { problem(path, "not_object"); continue; }
                    display.put(e.getKey(), transform(e.getValue().getAsJsonObject(), path));
                }
            }
            Boolean ao = null;
            if (root.has("ambientocclusion")) {
                var v = root.get("ambientocclusion");
                if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean()) ao = v.getAsBoolean(); else problem("ambientocclusion", "not_boolean");
            }
            String guiLight = root.has("gui_light") ? string(root, "gui_light", "gui_light") : null;
            if (guiLight != null && !guiLight.equals("front") && !guiLight.equals("side")) problem("gui_light", "bad_gui_light", guiLight);
            return new EditableModel(parent, textures, elements, display, ao, guiLight);
        }

        Element element(JsonObject obj, String path) {
            String name = null;
            if (obj.has("name")) {
                name = string(obj, "name", path + ".name");
                if (name != null && (name.length() > MAX_NAME || name.chars().anyMatch(Character::isISOControl))) problem(path + ".name", "bad_name");
            }
            Vec from = position(obj, "from", path), to = position(obj, "to", path);
            Rotation rotation = obj.has("rotation") ? rotation(obj, path + ".rotation") : null;
            String shade = null;
            if (obj.has("shade_direction_override")) {
                shade = string(obj, "shade_direction_override", path + ".shade_direction_override");
                if (shade != null && !EditableModel.DIRECTIONS.contains(shade)) problem(path + ".shade_direction_override", "bad_direction", shade);
            }
            int light = 0;
            if (obj.has("light_emission")) {
                var v = obj.get("light_emission");
                if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isNumber() || v.getAsDouble() != Math.rint(v.getAsDouble())
                        || v.getAsInt() < 0 || v.getAsInt() > 15) problem(path + ".light_emission", "bad_light");
                else light = v.getAsInt();
            }
            var faces = new LinkedHashMap<String, Face>();
            var faceObj = object(obj, "faces", path + ".faces");
            if (faceObj != null) {
                for (var e : faceObj.entrySet()) {
                    String fp = path + ".faces." + e.getKey();
                    if (!EditableModel.DIRECTIONS.contains(e.getKey())) { problem(fp, "bad_direction", e.getKey()); continue; }
                    if (!e.getValue().isJsonObject()) { problem(fp, "not_object"); continue; }
                    var face = face(e.getValue().getAsJsonObject(), fp);
                    if (face != null) faces.put(e.getKey(), face);
                }
                if (faceObj.isEmpty()) problem(path + ".faces", "no_faces");
            }
            if (from == null || to == null) return null;
            return new Element(name, from, to, rotation, shade, light, faces);
        }

        Rotation rotation(JsonObject parentObj, String path) {
            var obj = object(parentObj, "rotation", path);
            if (obj == null) return null;
            Vec origin = vec(obj, "origin", path + ".origin", true);
            boolean single = obj.has("axis") || obj.has("angle"), euler = obj.has("x") || obj.has("y") || obj.has("z");
            boolean rescale = false;
            if (obj.has("rescale")) {
                var v = obj.get("rescale");
                if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean()) rescale = v.getAsBoolean(); else problem(path + ".rescale", "not_boolean");
            }
            if (single) {
                if (euler) problem(path, "rotation_mixed");
                String axis = string(obj, "axis", path + ".axis");
                if (axis != null && !EditableModel.AXES.contains(axis)) problem(path + ".axis", "bad_axis", axis);
                Float angle = number(obj, "angle", path + ".angle", true);
                return origin == null || axis == null || angle == null ? null : Rotation.single(origin, axis, angle, rescale);
            }
            if (!euler) { problem(path, "rotation_missing"); return null; }
            Float x = number(obj, "x", path + ".x", false), y = number(obj, "y", path + ".y", false), z = number(obj, "z", path + ".z", false);
            return origin == null ? null : Rotation.euler(origin, new Vec(x == null ? 0 : x, y == null ? 0 : y, z == null ? 0 : z), rescale);
        }

        Face face(JsonObject obj, String path) {
            String texture = string(obj, "texture", path + ".texture");
            if (texture != null && !texture(texture)) problem(path + ".texture", "bad_texture", texture);
            Uv uv = null;
            if (obj.has("uv")) {
                var a = array(obj, "uv", path + ".uv");
                if (a != null) {
                    if (a.size() != 4) problem(path + ".uv", "bad_count", 4, a.size());
                    else {
                        float[] f = new float[4];
                        boolean good = true;
                        for (int i = 0; i < 4; i++) {
                            Float n = primitiveNumber(a.get(i), path + ".uv[" + i + "]");
                            if (n == null) { good = false; continue; }
                            if (n < 0 || n > 16) { problem(path + ".uv[" + i + "]", "out_of_range", 0, 16); good = false; }
                            f[i] = n;
                        }
                        if (good) uv = new Uv(f[0], f[1], f[2], f[3]);
                    }
                }
            }
            int rotation = 0;
            if (obj.has("rotation")) {
                Float r = number(obj, "rotation", path + ".rotation", true);
                if (r != null && (r != Math.rint(r) || ((int) (float) r) % 90 != 0 || r < 0 || r > 270)) problem(path + ".rotation", "bad_face_rotation");
                else if (r != null) rotation = (int) (float) r;
            }
            String cull = null;
            if (obj.has("cullface")) {
                cull = string(obj, "cullface", path + ".cullface");
                if (cull != null && !EditableModel.DIRECTIONS.contains(cull)) problem(path + ".cullface", "bad_direction", cull);
            }
            int tint = -1;
            if (obj.has("tintindex")) {
                Float t = number(obj, "tintindex", path + ".tintindex", true);
                if (t != null && (t != Math.rint(t) || t < -1 || t > 255)) problem(path + ".tintindex", "bad_tint");
                else if (t != null) tint = (int) (float) t;
            }
            return texture == null ? null : new Face(texture, uv, rotation, cull, tint);
        }

        Transform transform(JsonObject obj, String path) {
            Vec rotation = obj.has("rotation") ? vec(obj, "rotation", path + ".rotation", false) : Vec.ZERO;
            Vec translation = obj.has("translation") ? vec(obj, "translation", path + ".translation", false) : Vec.ZERO;
            Vec scale = obj.has("scale") ? vec(obj, "scale", path + ".scale", false) : Vec.ONE;
            Vec right = obj.has("right_rotation") ? vec(obj, "right_rotation", path + ".right_rotation", false) : Vec.ZERO;
            if (translation != null) for (int i = 0; i < 3; i++)
                if (Math.abs(translation.get(i)) > MAX_TRANSLATION) { problem(path + ".translation", "out_of_range", -MAX_TRANSLATION, MAX_TRANSLATION); break; }
            if (scale != null) for (int i = 0; i < 3; i++)
                if (Math.abs(scale.get(i)) > MAX_SCALE) { problem(path + ".scale", "out_of_range", -MAX_SCALE, MAX_SCALE); break; }
            return new Transform(rotation == null ? Vec.ZERO : rotation, translation == null ? Vec.ZERO : translation,
                    scale == null ? Vec.ONE : scale, right == null ? Vec.ZERO : right);
        }

        Vec position(JsonObject obj, String key, String path) {
            Vec v = vec(obj, key, path + "." + key, true);
            if (v != null) for (int i = 0; i < 3; i++)
                if (v.get(i) < MIN_EXTENT || v.get(i) > MAX_EXTENT) { problem(path + "." + key, "out_of_range", MIN_EXTENT, MAX_EXTENT); return null; }
            return v;
        }

        Vec vec(JsonObject obj, String key, String path, boolean required) {
            if (!obj.has(key)) { if (required) problem(path, "missing"); return null; }
            var a = array(obj, key, path);
            if (a == null) return null;
            if (a.size() != 3) { problem(path, "bad_count", 3, a.size()); return null; }
            Float x = primitiveNumber(a.get(0), path + "[0]"), y = primitiveNumber(a.get(1), path + "[1]"), z = primitiveNumber(a.get(2), path + "[2]");
            return x == null || y == null || z == null ? null : new Vec(x, y, z);
        }

        Float number(JsonObject obj, String key, String path, boolean required) {
            if (!obj.has(key)) { if (required) problem(path, "missing"); return null; }
            return primitiveNumber(obj.get(key), path);
        }

        Float primitiveNumber(JsonElement e, String path) {
            if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) { problem(path, "not_number"); return null; }
            float f = e.getAsFloat();
            if (!Float.isFinite(f)) { problem(path, "not_number"); return null; }
            return f;
        }

        String string(JsonObject obj, String key, String path) {
            if (!obj.has(key)) { problem(path, "missing"); return null; }
            return primitiveString(obj.get(key), path);
        }

        String primitiveString(JsonElement e, String path) {
            if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) { problem(path, "not_string"); return null; }
            return e.getAsString();
        }

        JsonObject object(JsonObject obj, String key, String path) {
            if (!obj.has(key)) { problem(path, "missing"); return null; }
            if (!obj.get(key).isJsonObject()) { problem(path, "not_object"); return null; }
            return obj.getAsJsonObject(key);
        }

        JsonArray array(JsonObject obj, String key, String path) {
            if (!obj.has(key)) { problem(path, "missing"); return null; }
            if (!obj.get(key).isJsonArray()) { problem(path, "not_array"); return null; }
            return obj.getAsJsonArray(key);
        }
    }

    /** A texture value: a {@code #variable} reference or a resource id like {@code minecraft:block/oak_planks}. */
    static boolean texture(String value) {
        return value.startsWith("#") ? VARIABLE.matcher(value.substring(1)).matches() : id(value);
    }

    /** A resource id as vanilla accepts it, without {@code ..} or leading/trailing slashes (same rule as ModelDefinition.safeId). */
    static boolean id(String value) {
        String path = value.substring(value.indexOf(':') + 1);
        return ID.matcher(value).matches() && !value.contains("..") && !path.startsWith("/") && !path.endsWith("/");
    }

    // ---------------------------------------------------------------- problem location

    private static final Pattern LINE_COLUMN = Pattern.compile("line (\\d+) column (\\d+)");

    /**
     * Where a problem sits in {@code text} (b-models14, for the code view's underline): {@code [start, end)} of the
     * value at the problem's JSON path ({@code elements[2].faces.up.uv[1]}); a missing field marks the opening brace of
     * its object; a syntax error marks the character at Gson's line/column. {@code null} when nothing can be found.
     */
    public static int[] locate(String text, Problem problem) {
        if (text == null || problem == null) return null;
        if (problem.key().equals("syntax")) {
            var m = LINE_COLUMN.matcher(problem.args().isEmpty() ? "" : String.valueOf(problem.args().getFirst()));
            if (!m.find()) return null;
            int line = Integer.parseInt(m.group(1)), column = Integer.parseInt(m.group(2)), offset = 0;
            for (int l = 1; l < line; l++) {
                int newline = text.indexOf('\n', offset);
                if (newline < 0) return null;
                offset = newline + 1;
            }
            int at = Math.min(text.length(), offset + Math.max(0, column - 2));
            int end = Math.min(text.length(), at + 1);
            return end > at ? new int[]{at, end} : at > 0 ? new int[]{at - 1, at} : null;
        }
        var segments = new ArrayList<Object>();
        if (!problem.path().isEmpty()) {
            for (String part : problem.path().split("\\.")) {
                int bracket = part.indexOf('[');
                if (bracket != 0) segments.add(bracket < 0 ? part : part.substring(0, bracket));
                for (var m = Pattern.compile("\\[(\\d+)]").matcher(part); m.find(); ) segments.add(Integer.parseInt(m.group(1)));
            }
        }
        try {
            var locator = new Locator(text);
            locator.space();
            return locator.find(segments, 0);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** A forgiving scanner over JSON text that follows a path and returns the span of the value it ends on. */
    private static final class Locator {
        final String s;
        int i;
        Locator(String s) { this.s = s; }

        int[] find(List<Object> path, int depth) {
            int start = i;
            if (depth == path.size() || i >= s.length()) { skipValue(); return new int[]{start, Math.max(i, Math.min(s.length(), start + 1))}; }
            Object seg = path.get(depth);
            char c = s.charAt(i);
            if (seg instanceof String key && c == '{') {
                i++;
                while (true) {
                    space();
                    if (i >= s.length() || s.charAt(i) == '}') return new int[]{start, start + 1};
                    String name = string();
                    space();
                    if (i < s.length() && s.charAt(i) == ':') i++;
                    space();
                    if (key.equals(name)) return find(path, depth + 1);
                    skipValue();
                    space();
                    if (i < s.length() && s.charAt(i) == ',') i++; else return new int[]{start, start + 1};
                }
            }
            if (seg instanceof Integer index && c == '[') {
                i++;
                for (int n = 0; ; n++) {
                    space();
                    if (i >= s.length() || s.charAt(i) == ']') return new int[]{start, start + 1};
                    if (n == index) return find(path, depth + 1);
                    skipValue();
                    space();
                    if (i < s.length() && s.charAt(i) == ',') i++; else return new int[]{start, start + 1};
                }
            }
            skipValue(); // the path expects another shape here: mark this value
            return new int[]{start, Math.max(i, start + 1)};
        }

        void space() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

        String string() {
            if (i >= s.length() || s.charAt(i) != '"') { int a = i; skipValue(); return s.substring(a, i); }
            var out = new StringBuilder();
            i++;
            while (i < s.length() && s.charAt(i) != '"') {
                if (s.charAt(i) == '\\' && i + 1 < s.length()) i++;
                out.append(s.charAt(i++));
            }
            i = Math.min(s.length(), i + 1);
            return out.toString();
        }

        void skipValue() {
            if (i >= s.length()) return;
            char c = s.charAt(i);
            if (c == '"') { string(); return; }
            if (c == '{' || c == '[') {
                int depth = 0;
                while (i < s.length()) {
                    char d = s.charAt(i);
                    if (d == '"') { string(); continue; }
                    if (d == '{' || d == '[') depth++;
                    else if (d == '}' || d == ']') { depth--; if (depth == 0) { i++; return; } }
                    i++;
                }
                return;
            }
            while (i < s.length() && ",}]\n".indexOf(s.charAt(i)) < 0 && !Character.isWhitespace(s.charAt(i))) i++;
        }
    }

    // ---------------------------------------------------------------- write

    /** Vanilla model JSON; defaults are left out (no rotation, light 0, tint -1, face rotation 0, auto-UV). */
    public static JsonObject toJson(EditableModel model) {
        var root = new JsonObject();
        if (model.parent() != null) root.addProperty("parent", model.parent());
        if (model.ambientOcclusion() != null) root.addProperty("ambientocclusion", model.ambientOcclusion());
        if (model.guiLight() != null) root.addProperty("gui_light", model.guiLight());
        if (!model.textures().isEmpty()) {
            var t = new JsonObject();
            model.textures().forEach(t::addProperty);
            root.add("textures", t);
        }
        if (!model.elements().isEmpty()) {
            var array = new JsonArray();
            for (var e : model.elements()) array.add(element(e));
            root.add("elements", array);
        }
        if (!model.display().isEmpty()) {
            var d = new JsonObject();
            model.display().forEach((slot, t) -> {
                var o = new JsonObject();
                if (!t.rotation().equals(Vec.ZERO)) o.add("rotation", vec(t.rotation()));
                if (!t.translation().equals(Vec.ZERO)) o.add("translation", vec(t.translation()));
                if (!t.scale().equals(Vec.ONE)) o.add("scale", vec(t.scale()));
                if (!t.rightRotation().equals(Vec.ZERO)) o.add("right_rotation", vec(t.rightRotation()));
                d.add(slot, o);
            });
            root.add("display", d);
        }
        return root;
    }

    public static String write(EditableModel model) {
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(toJson(model));
    }

    /**
     * The same JSON for the editor's code view (b-models13): an object or array goes on one line when that line stays
     * within {@link #COMPACT_WIDTH} characters ({@code "from": [2, 10, 2]}, one face per line), otherwise it opens
     * with two-space indentation. Reads back to the same model as {@link #write}.
     */
    public static String writeCompact(EditableModel model) {
        return writeCompact(model, COMPACT_WIDTH);
    }

    /**
     * {@link #writeCompact(EditableModel)} for a code column of {@code width} characters (b-models14): narrower columns
     * open objects earlier so lines do not wrap at GUI scale 2. Never below {@link #MIN_COMPACT_WIDTH}.
     */
    public static String writeCompact(EditableModel model, int width) {
        var out = new StringBuilder();
        compact(toJson(model), 0, 0, Math.max(MIN_COMPACT_WIDTH, width), out);
        return out.toString();
    }

    public static final int COMPACT_WIDTH = 64, MIN_COMPACT_WIDTH = 24;
    private static final com.google.gson.Gson PLAIN = new GsonBuilder().disableHtmlEscaping().create();

    private static String inline(JsonElement e) {
        if (e.isJsonObject()) {
            var parts = new ArrayList<String>();
            e.getAsJsonObject().entrySet().forEach(en -> parts.add(PLAIN.toJson(en.getKey()) + ": " + inline(en.getValue())));
            return "{" + String.join(", ", parts) + "}";
        }
        if (e.isJsonArray()) {
            var parts = new ArrayList<String>();
            e.getAsJsonArray().forEach(v -> parts.add(inline(v)));
            return "[" + String.join(", ", parts) + "]";
        }
        return PLAIN.toJson(e);
    }

    /** {@code prefix}: characters before the value on its line ({@code "key": }), so whole lines fit {@code width}. */
    private static void compact(JsonElement e, int indent, int prefix, int width, StringBuilder out) {
        String one = inline(e);
        if (indent * 2 + prefix + one.length() + 1 <= width || !(e.isJsonObject() || e.isJsonArray())) { out.append(one); return; }
        String pad = "  ".repeat(indent + 1);
        boolean object = e.isJsonObject();
        out.append(object ? "{\n" : "[\n");
        var entries = object ? new ArrayList<>(e.getAsJsonObject().entrySet()) : null;
        int size = object ? entries.size() : e.getAsJsonArray().size();
        for (int i = 0; i < size; i++) {
            out.append(pad);
            if (object) {
                String key = PLAIN.toJson(entries.get(i).getKey()) + ": ";
                out.append(key);
                compact(entries.get(i).getValue(), indent + 1, key.length(), width, out);
            } else compact(e.getAsJsonArray().get(i), indent + 1, 0, width, out);
            out.append(i + 1 < size ? ",\n" : "\n");
        }
        out.append("  ".repeat(indent)).append(object ? '}' : ']');
    }

    private static JsonObject element(Element e) {
        var o = new JsonObject();
        if (e.name() != null) o.addProperty("name", e.name());
        o.add("from", vec(e.from()));
        o.add("to", vec(e.to()));
        if (e.rotation() != null) {
            var r = new JsonObject();
            r.add("origin", vec(e.rotation().origin()));
            if (e.rotation().isEuler()) {
                r.add("x", num(e.rotation().euler().x()));
                r.add("y", num(e.rotation().euler().y()));
                r.add("z", num(e.rotation().euler().z()));
            } else {
                r.addProperty("axis", e.rotation().axis());
                r.add("angle", num(e.rotation().angle()));
            }
            if (e.rotation().rescale()) r.addProperty("rescale", true);
            o.add("rotation", r);
        }
        if (e.shadeDirection() != null) o.addProperty("shade_direction_override", e.shadeDirection());
        if (e.lightEmission() != 0) o.addProperty("light_emission", e.lightEmission());
        var faces = new JsonObject();
        e.faces().forEach((dir, f) -> {
            var fo = new JsonObject();
            if (f.uv() != null) {
                var uv = new JsonArray();
                uv.add(num(f.uv().u1())); uv.add(num(f.uv().v1())); uv.add(num(f.uv().u2())); uv.add(num(f.uv().v2()));
                fo.add("uv", uv);
            }
            fo.addProperty("texture", f.texture());
            if (f.rotation() != 0) fo.addProperty("rotation", f.rotation());
            if (f.cullface() != null) fo.addProperty("cullface", f.cullface());
            if (f.tintIndex() != -1) fo.addProperty("tintindex", f.tintIndex());
            faces.add(dir, fo);
        });
        o.add("faces", faces);
        return o;
    }

    private static JsonArray vec(Vec v) {
        var a = new JsonArray();
        a.add(num(v.x())); a.add(num(v.y())); a.add(num(v.z()));
        return a;
    }

    /** Whole numbers without {@code .0} (vanilla files look like that), others as the shortest float text (7.9, not 7.900000095). */
    static JsonPrimitive num(float f) {
        return f == Math.rint(f) && Math.abs(f) < 1e6 ? new JsonPrimitive((int) f) : new JsonPrimitive(new java.math.BigDecimal(Float.toString(f)));
    }

    private ModelJson() {}
}
