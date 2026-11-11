package com.simplelib.api.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The config screen of every mod, as a model (L13, owner 11.11. RELSB31 f2=A, docs/ai/PLAN-B-CFGSCREEN31-2026-11-11.md): the
 * options a module announced with {@link ConfigCommands} become categories (first part of the path, flat options in
 * {@link #GENERAL}) and fields with a widget, value, default, range and hints. No client classes, so the GameTests check it; the
 * Fabric client draws it with Cloth Config or with Vanilla widgets ({@code com.simplelib.config.screen.GenericConfigScreen}).
 * Saving goes through {@link ConfigCommands.Spec#set}, the same clamping and file writing as {@code /<modid> config set}.
 */
public final class ConfigScreenModel {
    /** Category of the options without a group. */
    public static final String GENERAL = "general";

    public enum Widget { TOGGLE, INTEGER, DECIMAL, TEXT, CHOICE, LIST, JSON }

    /** Whether the screen may change the options of a config or only show them. */
    public enum Access { EDIT, READ_ONLY }

    /** One option: {@code name} is the path inside its category ({@code depth} for {@code group.depth}). */
    public record Field(String path, String name, Widget widget, JsonElement value, JsonElement defaultValue, List<String> choices,
                        double[] range, boolean restart, boolean secret) {
        /** The value as the widget edits it ({@code ***} for a secret). */
        public String text() {
            return secret ? "***" : ConfigScreenModel.text(value, widget);
        }

        public String defaultText() {
            return secret ? "***" : ConfigScreenModel.text(defaultValue, widget);
        }
    }

    public record Category(String group, List<Field> fields) {}

    private ConfigScreenModel() {}

    /** The categories of {@code spec} in file order. */
    public static List<Category> categories(ConfigCommands.Spec spec) {
        Map<String, List<Field>> groups = new LinkedHashMap<>();
        for (ConfigCommands.Option o : spec.options()) {
            String path = o.path();
            int dot = path.indexOf('.');
            String group = dot > 0 ? path.substring(0, dot) : GENERAL;
            String name = dot > 0 ? path.substring(dot + 1) : path;
            JsonElement value = spec.value(path);
            List<String> choices = spec.choices(path);
            groups.computeIfAbsent(group, g -> new ArrayList<>()).add(new Field(path, name, widget(o, value, choices), value,
                    o.defaultValue(), choices, spec.range(path), spec.restart(path), spec.secret(path)));
        }
        List<Category> out = new ArrayList<>();
        groups.forEach((g, fields) -> out.add(new Category(g, List.copyOf(fields))));
        return out;
    }

    private static Widget widget(ConfigCommands.Option o, JsonElement value, List<String> choices) {
        if (!choices.isEmpty()) return Widget.CHOICE;
        return switch (o.kind()) {
            case BOOL -> Widget.TOGGLE;
            case INT -> Widget.INTEGER;
            case DECIMAL -> Widget.DECIMAL;
            case TEXT -> Widget.TEXT;
            case LIST -> flat(o.defaultValue()) && flat(value) ? Widget.LIST : Widget.JSON;
            case OBJECT -> Widget.JSON;
        };
    }

    /** A list of plain values (text, numbers, switches): one line per entry; anything nested stays JSON text. */
    private static boolean flat(JsonElement list) {
        if (list == null || !list.isJsonArray()) return list == null;
        for (JsonElement e : list.getAsJsonArray()) if (!e.isJsonPrimitive()) return false;
        return true;
    }

    /**
     * Who may change what: a client config always; a server config without a world (the own file, title screen); in an own
     * world (single player, LAN host) only with operator rights like {@code /<modid> config}; on a remote server never (the local
     * file is not the server's).
     */
    public static Access access(boolean clientConfig, boolean inWorld, boolean remoteServer, boolean operator) {
        if (clientConfig || !inWorld) return Access.EDIT;
        if (remoteServer) return Access.READ_ONLY;
        return operator ? Access.EDIT : Access.READ_ONLY;
    }

    /**
     * Writes the edited values ({@code path -> text} as {@link Field#text()} gives it) through {@link ConfigCommands.Spec#set};
     * values equal to the stored one and secrets are skipped, so a screen may hand in every field. One result per write.
     */
    public static List<ConfigCommands.Result> save(ConfigCommands.Spec spec, Map<String, String> edits) {
        List<ConfigCommands.Result> out = new ArrayList<>();
        for (var e : edits.entrySet()) {
            ConfigCommands.Option o = spec.option(e.getKey());
            if (o == null) {
                out.add(new ConfigCommands.Result(ConfigCommands.Status.UNKNOWN, e.getKey(), e.getValue()));
                continue;
            }
            if (spec.secret(o.path()) || e.getValue() == null) continue;
            String text = e.getValue().trim();
            JsonElement parsed = spec.choices(o.path()).isEmpty() ? ConfigCommands.parse(o.kind(), text, o.defaultValue()) : null;
            if (parsed != null && ConfigCommands.same(parsed, spec.value(o.path()))) continue;
            if (parsed == null && !spec.choices(o.path()).isEmpty() && text.equalsIgnoreCase(ConfigCommands.show(spec.value(o.path())))) continue;
            out.add(spec.set(o.path(), text));
        }
        return out;
    }

    /** The text a widget edits: JSON for lists and objects, the plain value otherwise. */
    public static String text(JsonElement v, Widget widget) {
        if (v == null || v.isJsonNull()) return "";
        if (widget == Widget.LIST || widget == Widget.JSON || !v.isJsonPrimitive()) return v.toString();
        return v.getAsString();
    }

    /** The entries of a plain list as text (one per line of a list widget). */
    public static List<String> items(JsonElement list) {
        List<String> out = new ArrayList<>();
        if (list != null && list.isJsonArray()) for (JsonElement e : list.getAsJsonArray()) out.add(e.isJsonPrimitive() ? e.getAsString() : e.toString());
        return out;
    }

    /** List entries back to the JSON text of {@link #save}: numbers and switches like the default's first entry, else text. */
    public static String listText(List<String> items, JsonElement like) {
        ConfigCommands.Kind kind = ConfigCommands.Kind.TEXT;
        if (like != null && like.isJsonArray() && !like.getAsJsonArray().isEmpty()) kind = ConfigCommands.kind(like.getAsJsonArray().get(0));
        JsonArray out = new JsonArray();
        for (String item : items) {
            String s = item == null ? "" : item.trim();
            if (s.isEmpty()) continue;
            JsonElement e = kind == ConfigCommands.Kind.INT || kind == ConfigCommands.Kind.DECIMAL || kind == ConfigCommands.Kind.BOOL
                    ? ConfigCommands.parse(kind, s, null) : null;
            out.add(e != null ? e : new JsonPrimitive(s));
        }
        return out.toString();
    }

    /** Lang key of an option's name ({@code .@Tooltip} appended: its tooltip), the schema of the Cloth screens. */
    public static String labelKey(String modId, String path) {
        return "text.autoconfig." + modId + ".option." + path;
    }

    public static String categoryKey(String modId, String group) {
        return "text.autoconfig." + modId + ".category." + group;
    }

    public static String titleKey(String modId) {
        return "text.autoconfig." + modId + ".title";
    }

    /** A readable name when the lang file has none: {@code upgradeSeconds} -> {@code Upgrade seconds}, the last path part. */
    public static String humanize(String path) {
        String last = path.substring(path.lastIndexOf('.') + 1).replace('_', ' ').replace('-', ' ');
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < last.length(); i++) {
            char c = last.charAt(i);
            boolean upper = Character.isUpperCase(c);
            char prev = i > 0 ? last.charAt(i - 1) : ' ';
            if (i > 0 && (upper && (Character.isLowerCase(prev) || Character.isDigit(prev)) || Character.isDigit(c) && Character.isLetter(prev))) b.append(' ');
            b.append(i == 0 ? Character.toUpperCase(c) : upper && i + 1 < last.length() && Character.isLowerCase(last.charAt(i + 1)) ? Character.toLowerCase(c) : c);
        }
        return b.toString();
    }

    /** Whether a tooltip text names the default already ("Default:"/"Standard:"), so the screen does not add it twice. */
    public static boolean namesDefault(String tooltip) {
        String t = tooltip.toLowerCase(Locale.ROOT);
        return t.contains("default:") || t.contains("standard:");
    }

    /** Whether a tooltip text names the range already. */
    public static boolean namesRange(String tooltip) {
        String t = tooltip.toLowerCase(Locale.ROOT);
        return t.contains("range") || t.contains("bereich");
    }

    /** {@code 1} for 1.0, {@code 0.5} for 0.5: a range bound as people write it. */
    public static String number(double d) {
        return d == Math.rint(d) && Math.abs(d) < 1e15 ? Long.toString((long) d) : Double.toString(d);
    }
}
