package com.simplebuilding.modules.simplemodels.editor;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Editor shortcut help (b-models14, concept §3 "Tastenkürzel ... Übersicht über F1 bzw. ?-Knopf"): the one list the help
 * overlay draws. Keys are tokens; tokens in {@link #NAMED} are translated ({@code simplemodels.editor.key.<token>}, e.g.
 * Ctrl / Strg), others are shown as they are in upper case. Each entry's text is {@code simplemodels.editor.help.<id>}.
 * Tested in {@code editor_snap_help} (every key in EN and DE).
 */
public final class EditorShortcuts {
    public record Shortcut(String group, String id, List<String> keys) {
        public String textKey() { return "simplemodels.editor.help." + id; }
    }

    /** Tokens with a translated label. */
    public static final Set<String> NAMED = Set.of("ctrl", "shift", "alt", "del", "esc", "wheel", "rmb", "mmb", "lmb", "home", "num");
    public static final List<String> GROUPS = List.of("view", "edit", "file");

    public static final List<Shortcut> ALL = List.of(
            new Shortcut("view", "turn", List.of("rmb")),
            new Shortcut("view", "pan", List.of("shift", "rmb")),
            new Shortcut("view", "zoom", List.of("wheel")),
            new Shortcut("view", "views", List.of("num", "1/3/7")),
            new Shortcut("view", "reset", List.of("num", "0")),
            new Shortcut("view", "pick_face", List.of("ctrl", "lmb")),
            new Shortcut("edit", "move", List.of("g")),
            new Shortcut("edit", "rotate", List.of("r")),
            new Shortcut("edit", "scale", List.of("s")),
            new Shortcut("edit", "free", List.of("alt")),
            new Shortcut("edit", "fine", List.of("shift")),
            new Shortcut("edit", "both_sides", List.of("ctrl")),
            new Shortcut("edit", "cancel", List.of("esc")),
            new Shortcut("edit", "add", List.of("n")),
            new Shortcut("edit", "duplicate", List.of("ctrl", "d")),
            new Shortcut("edit", "delete", List.of("del")),
            new Shortcut("edit", "hide", List.of("h")),
            new Shortcut("edit", "undo", List.of("ctrl", "z")),
            new Shortcut("edit", "redo", List.of("ctrl", "y")),
            new Shortcut("file", "file", List.of("ctrl", "s")),
            new Shortcut("file", "code_only", List.of("f4")),
            new Shortcut("file", "help", List.of("f1")));

    /** The key combination as shown, e.g. {@code Strg + D}; {@code translate} maps a lang key to its text. */
    public static String keys(Shortcut s, Function<String, String> translate) {
        var parts = new java.util.ArrayList<String>();
        for (String token : s.keys()) parts.add(NAMED.contains(token) ? translate.apply("simplemodels.editor.key." + token) : token.toUpperCase(java.util.Locale.ROOT));
        return String.join(" + ", parts);
    }

    /** All lang keys the help needs (entries, groups, named tokens). */
    public static List<String> langKeys() {
        var out = new java.util.ArrayList<String>();
        ALL.forEach(s -> out.add(s.textKey()));
        GROUPS.forEach(g -> out.add("simplemodels.editor.help.group." + g));
        NAMED.forEach(t -> out.add("simplemodels.editor.key." + t));
        return out;
    }

    private EditorShortcuts() {}
}
