package com.simplebuilding.modules.simpleinterfaces;

import com.simplelib.api.client.keys.KeyConflicts;
import com.simplelib.api.client.keys.KeyConflicts.Binding;
import com.simplelib.api.client.keys.KeyboardEdit;
import com.simplelib.api.client.keys.KeyboardLayout;
import com.simplelib.api.client.keys.KeyboardLayout.Cell;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;

/** Logic of the key binding page (UI28-1 / WP-25): conflict detection and the keyboard layouts (the drawing itself needs a client). */
public final class KeyPageTests {
    public static final java.util.Map<String, Consumer<GameTestHelper>> ALL = new java.util.LinkedHashMap<>();

    static {
        ALL.put("key_page_layouts", KeyPageTests::layouts);
        ALL.put("key_page_arrows", KeyPageTests::arrows);
        ALL.put("key_page_conflicts", KeyPageTests::conflicts);
        ALL.put("key_page_filter", KeyPageTests::filter);
        ALL.put("key_page_suggest", KeyPageTests::suggest);
        ALL.put("key_page_key_names", KeyPageTests::keyNames);
        ALL.put("key_page_iso_layouts", KeyPageTests::isoLayouts);
        ALL.put("key_page_editor", KeyPageTests::editor);
    }

    private KeyPageTests() {}

    private static Binding b(String id, String key) {
        return new Binding(id, "Action " + id, "Group", "mod" + id.charAt(0), key, "key.keyboard.h");
    }

    private static Cell cell(List<Cell> cells, int code) {
        return cells.stream().filter(c -> c.code() == code).findFirst().orElse(null);
    }

    /** 60 / 75 / 100 %: 62 / 81 / 104 keys, no key twice, none overlapping, all inside the picture. */
    public static void layouts(GameTestHelper h) {
        int[] counts = {62, 81, 104};
        int i = 0;
        for (KeyboardLayout.Size size : KeyboardLayout.Size.values()) {
            for (String layout : KeyboardLayout.PRESETS) {
                List<Cell> cells = KeyboardLayout.cells(size, layout);
                int expected = counts[i] + (layout.equals("us") ? 0 : 1); // ISO: one more key left of Y/Z
                h.assertTrue(cells.size() == expected, size + "/" + layout + " has " + expected + " keys, found " + cells.size());
                h.assertTrue(KeyboardEdit.problem(cells).isEmpty(), size + "/" + layout + ": " + KeyboardEdit.problem(cells));
                Set<Integer> codes = new HashSet<>();
                for (Cell c : cells) {
                    h.assertTrue(codes.add(c.code()), size + ": key " + c.label() + " twice");
                    h.assertTrue(c.w() > 0 && c.h() > 0 && c.x() >= 0 && c.y() >= 0, size + ": key " + c.label() + " outside");
                    for (Cell o : cells) {
                        if (o == c) continue;
                        h.assertTrue(!c.overlaps(o), size + ": " + c.label() + " overlaps " + o.label());
                    }
                }
            }
            i++;
        }
        List<Cell> full = KeyboardLayout.cells(KeyboardLayout.Size.FULL, false);
        List<Cell> compact = KeyboardLayout.cells(KeyboardLayout.Size.COMPACT, false);
        h.assertTrue(KeyboardLayout.width(compact) == 60 && KeyboardLayout.width(full) == 92, "widths 15 and 23 keys: " + KeyboardLayout.width(compact) + "/" + KeyboardLayout.width(full));
        h.assertTrue(KeyboardLayout.label('Y', true).equals("Z") && KeyboardLayout.label('Z', true).equals("Y") && KeyboardLayout.label('Y', false).equals("Y"), "QWERTZ swaps Y and Z");
        h.assertTrue(KeyboardLayout.scale(full, 10, 10) == 2 && KeyboardLayout.scale(compact, 1000, 1000) == 5, "scale clamps to 2..5");
        Cell space = cell(full, KeyboardLayout.SPACE);
        h.assertTrue(KeyboardLayout.at(full, 10, 20, 4, 10 + space.x() * 4 + 1, 20 + space.y() * 4 + 1).map(Cell::code).orElse(-1) == KeyboardLayout.SPACE
                && KeyboardLayout.at(full, 10, 20, 4, 5, 5).isEmpty(), "hit test finds the space bar, nothing outside");
        h.succeed();
    }

    /** The arrow keys are the inverted T of the owner's picture: up above down, left and right beside down. */
    public static void arrows(GameTestHelper h) {
        h.assertTrue(cell(KeyboardLayout.cells(KeyboardLayout.Size.COMPACT, false), KeyboardLayout.UP) == null, "60 % has no arrow keys");
        for (KeyboardLayout.Size size : new KeyboardLayout.Size[] {KeyboardLayout.Size.TENKEYLESS, KeyboardLayout.Size.FULL}) {
            List<Cell> cells = KeyboardLayout.cells(size, false);
            Cell up = cell(cells, KeyboardLayout.UP), down = cell(cells, KeyboardLayout.DOWN), left = cell(cells, KeyboardLayout.LEFT),
                    right = cell(cells, KeyboardLayout.RIGHT);
            h.assertTrue(up != null && down != null && left != null && right != null, size + ": all four arrows");
            h.assertTrue(up.x() == down.x() && up.bottom() == down.y(), size + ": up sits right above down");
            Cell shift = cell(cells, KeyboardLayout.RIGHT_SHIFT), enter = cell(cells, KeyboardLayout.ENTER);
            h.assertTrue(left.x() >= shift.right() && up.x() >= shift.right() && up.x() >= enter.right(), size + ": arrow block stands right of the main block, no overlap");
            h.assertTrue(up.y() == shift.y() && left.y() > up.y(), size + ": up on the shift row, left/down/right one row below");
            h.assertTrue(up.x() + up.w() / 2 == down.x() + down.w() / 2, size + ": up is centred above down");
            h.assertTrue(left.right() == down.x() && right.x() == down.right() && left.y() == down.y() && right.y() == down.y(), size + ": left and right beside down");
        }
        h.succeed();
    }

    /** Free / bound / shared, shared keys counted once, "shares with" lists the others, unbound actions count nowhere. */
    public static void conflicts(GameTestHelper h) {
        List<Binding> all = List.of(b("a1", "key.keyboard.h"), b("b1", "key.keyboard.h"), b("c1", "key.keyboard.k"), b("d1", KeyConflicts.UNBOUND),
                b("e1", "key.keyboard.k"), b("f1", "key.keyboard.j"));
        var map = KeyConflicts.byKey(all);
        h.assertTrue(KeyConflicts.state(map, "key.keyboard.h") == KeyConflicts.State.SHARED, "two on H = shared");
        h.assertTrue(KeyConflicts.state(map, "key.keyboard.j") == KeyConflicts.State.BOUND, "one on J = bound");
        h.assertTrue(KeyConflicts.state(map, "key.keyboard.x") == KeyConflicts.State.FREE && KeyConflicts.state(map, KeyConflicts.UNBOUND) == KeyConflicts.State.FREE, "free keys");
        h.assertTrue(KeyConflicts.sharedKeys(map) == 2, "H and K are shared: " + KeyConflicts.sharedKeys(map));
        h.assertTrue(KeyConflicts.sharedWith(map, all.get(0)).size() == 1 && KeyConflicts.sharedWith(map, all.get(0)).get(0).id().equals("b1"), "a1 shares with b1");
        h.assertTrue(!KeyConflicts.conflicts(map, all.get(3)) && !KeyConflicts.conflicts(map, all.get(5)), "unbound and single are no conflict");
        var moved = KeyConflicts.assign(all, "b1", "key.keyboard.x");
        h.assertTrue(KeyConflicts.sharedKeys(KeyConflicts.byKey(moved)) == 1, "moving b1 to a free key resolves H");
        var reset = KeyConflicts.reset(all, Set.of("c1", "e1"));
        h.assertTrue(reset.get(2).key().equals("key.keyboard.h") && reset.get(2).isDefault() && reset.get(0).key().equals("key.keyboard.h"), "reset puts the default key back");
        h.assertTrue(KeyConflicts.sharedKeys(KeyConflicts.byKey(reset)) == 1 && KeyConflicts.byKey(reset).get("key.keyboard.h").size() == 4, "reset to H shares it four ways");
        h.succeed();
    }

    /** Search words (all must match action, group, mod or key text), conflict and unbound filters. */
    public static void filter(GameTestHelper h) {
        List<Binding> all = List.of(b("a1", "key.keyboard.h"), b("b1", "key.keyboard.h"), b("c1", "key.keyboard.k"), b("d1", KeyConflicts.UNBOUND));
        java.util.function.Function<Binding, String> shown = x -> x.unbound() ? "None" : x.key().substring(x.key().lastIndexOf('.') + 1).toUpperCase();
        h.assertTrue(KeyConflicts.filter(all, "", false, false, shown).size() == 4, "empty query lists all");
        h.assertTrue(KeyConflicts.filter(all, "action b1", false, false, shown).size() == 1, "words in the action");
        h.assertTrue(KeyConflicts.filter(all, "moda", false, false, shown).size() == 1, "mod name");
        h.assertTrue(KeyConflicts.filter(all, "key k", false, false, shown).isEmpty() && KeyConflicts.filter(all, "K", false, false, shown).size() >= 1, "words must all match");
        h.assertTrue(KeyConflicts.filter(all, "", true, false, shown).size() == 2, "only conflicts: a1 and b1");
        h.assertTrue(KeyConflicts.filter(all, "", false, true, shown).size() == 1 && KeyConflicts.filter(all, "", false, true, shown).get(0).id().equals("d1"), "only unbound: d1");
        h.assertTrue(KeyConflicts.filter(all, "zzz", false, false, shown).isEmpty(), "no match");
        h.succeed();
    }

    /** The suggestion for a shared key is a free plain key nearby - never a used one, never a modifier. */
    public static void suggest(GameTestHelper h) {
        List<Cell> cells = KeyboardLayout.cells(KeyboardLayout.Size.FULL, false);
        Set<Integer> used = new HashSet<>(List.of((int) 'H', (int) 'J'));
        Cell near = KeyboardLayout.nearestFree(cells, used, 'H').orElse(null);
        h.assertTrue(near != null && near.code() == 'G', "next to H with J taken: G, found " + (near == null ? "none" : near.label()));
        for (char c = 'A'; c <= 'Z'; c++) used.add((int) c);
        Cell other = KeyboardLayout.nearestFree(cells, used, 'H').orElse(null);
        h.assertTrue(other != null && !used.contains(other.code()) && KeyboardLayout.plain(other.code()), "letters used up: a plain, unused key");
        h.assertTrue(!KeyboardLayout.plain(KeyboardLayout.LEFT_SHIFT) && !KeyboardLayout.plain(KeyboardLayout.ESC) && KeyboardLayout.plain('Q'), "plain keys");
        for (Cell c : cells) if (KeyboardLayout.plain(c.code())) used.add(c.code());
        h.assertTrue(KeyboardLayout.nearestFree(cells, used, 'H').isEmpty(), "none free: empty");
        h.succeed();
    }

    /**
     * IG31-31: every key of the picture has Minecraft's key name (the picture's GLFW codes never go to 26.3's SDL-numbered
     * InputConstants as numbers): letters, digits, F-keys, keypad, the special keys; names unique.
     */
    public static void keyNames(GameTestHelper h) {
        h.assertTrue("key.keyboard.a".equals(KeyboardLayout.keyName('A')) && "key.keyboard.7".equals(KeyboardLayout.keyName('7'))
                && "key.keyboard.f9".equals(KeyboardLayout.keyName(KeyboardLayout.F1 + 8)) && "key.keyboard.keypad.3".equals(KeyboardLayout.keyName(KeyboardLayout.KP_0 + 3))
                && "key.keyboard.left.shift".equals(KeyboardLayout.keyName(KeyboardLayout.LEFT_SHIFT)) && "key.keyboard.space".equals(KeyboardLayout.keyName(KeyboardLayout.SPACE)),
                "key names");
        Set<String> names = new HashSet<>();
        for (Cell c : KeyboardLayout.cells(KeyboardLayout.Size.FULL, false)) {
            String n = KeyboardLayout.keyName(c.code());
            h.assertTrue(!com.simplelib.api.client.keys.KeyConflicts.UNBOUND.equals(n), "cell " + c.label() + " (" + c.code() + ") has no key name");
            h.assertTrue(names.add(n), "two cells with the key name " + n);
        }
        h.succeed();
    }

    /** DE / UK / FR: L-shaped Enter, extra key left of Enter and left of Y/Z, captions per layout, the codes stay physical. */
    public static void isoLayouts(GameTestHelper h) {
        for (String layout : new String[] {"de", "uk", "fr"}) {
            List<Cell> cells = KeyboardLayout.cells(KeyboardLayout.Size.FULL, layout);
            Cell enter = cell(cells, KeyboardLayout.ENTER), hash = cell(cells, KeyboardLayout.BACKSLASH), world = cell(cells, KeyboardLayout.WORLD_1);
            Cell lshift = cell(cells, KeyboardLayout.LEFT_SHIFT), z = cell(cells, 'Z'), bracket = cell(cells, KeyboardLayout.RIGHT_BRACKET);
            h.assertTrue(enter.l() && enter.ew() < enter.w() + 1 && enter.ey() == enter.y() + enter.h(), layout + ": Enter is L-shaped");
            h.assertTrue(hash.right() == enter.ex() && hash.y() == enter.ey(), layout + ": the extra key sits left of the Enter foot");
            h.assertTrue(world.x() == lshift.right() && world.y() == lshift.y() && z.x() == world.right(), layout + ": the extra key sits between Shift and the letters");
            h.assertTrue(bracket.right() == enter.x(), layout + ": Enter's top starts right of the last bracket key");
            h.assertTrue(KeyboardLayout.at(cells, 0, 0, 4, (enter.ex() + 1) * 4, (enter.ey() + 1) * 4).map(Cell::code).orElse(-1) == KeyboardLayout.ENTER,
                    layout + ": the foot of the L is hit");
            h.assertTrue(KeyboardLayout.at(cells, 0, 0, 4, (enter.x() - 1) * 4, (enter.ey() + 1) * 4 - 2).map(Cell::code).orElse(-1) != KeyboardLayout.ENTER,
                    layout + ": the notch of the L is not hit");
        }
        h.assertTrue(KeyboardLayout.label('Y', "de").equals("Z") && KeyboardLayout.label('Z', "de").equals("Y") && KeyboardLayout.label('Q', "fr").equals("A")
                && KeyboardLayout.label('A', "fr").equals("Q") && KeyboardLayout.label(KeyboardLayout.WORLD_1, "uk").equals("\\"), "captions per layout");
        h.assertTrue(KeyboardLayout.keyName(KeyboardLayout.WORLD_1).equals("key.keyboard.world.1") && KeyboardLayout.codeOf("key.keyboard.world.1") == KeyboardLayout.WORLD_1
                && KeyboardLayout.codeOf("key.keyboard.a") == 'A' && KeyboardLayout.codeOf("key.keyboard.nope") == -1, "key names both ways");
        h.succeed();
    }

    /** Editor rules: no overlap, no duplicate, auto-add of a missing key on a free place, refused edits, JSON round trip. */
    public static void editor(GameTestHelper h) {
        List<Cell> de = KeyboardLayout.cells(KeyboardLayout.Size.FULL, "de");
        Cell a = cell(de, 'A');
        h.assertTrue(KeyboardEdit.replace(de, a.moved(1, 0)).isEmpty(), "moving A into S is refused");
        h.assertTrue(KeyboardEdit.replace(de, a.moved(0, 20)).isPresent(), "moving A to a free place works");
        h.assertTrue(KeyboardEdit.replace(de, cell(de, KeyboardLayout.ESC).moved(-1, 0)).isEmpty(), "Esc left of column 0 is outside");
        List<Cell> twice = new ArrayList<>(de);
        twice.add(new Cell('A', "A", 0, 40, 4, 4));
        h.assertTrue(KeyboardEdit.problem(twice).map(KeyboardEdit.Problem::kind).orElse("").equals("duplicate"), "a key twice is refused");
        List<Cell> overlap = new ArrayList<>(de);
        overlap.add(new Cell(KeyboardLayout.F1 + 12, "F13", a.x(), a.y(), 4, 4));
        h.assertTrue(KeyboardEdit.problem(overlap).map(KeyboardEdit.Problem::kind).orElse("").equals("overlap"), "overlap is found");
        List<Cell> without = KeyboardEdit.remove(de, 'A');
        h.assertTrue(without.size() == de.size() - 1 && cell(without, 'A') == null, "remove takes the key out");
        List<Cell> grown = KeyboardEdit.add(without, 'A', "A");
        Cell fresh = cell(grown, 'A');
        h.assertTrue(grown.size() == de.size() && fresh != null && fresh.y() == KeyboardEdit.EXTRA_ROW && KeyboardEdit.problem(grown).isEmpty(),
                "a missing key is added below the picture without overlap");
        h.assertTrue(KeyboardEdit.add(grown, 'A', "A") == grown, "adding a present key changes nothing");
        List<Cell> second = KeyboardEdit.add(grown, KeyboardLayout.F1 + 12, "F13");
        h.assertTrue(second.size() == grown.size() + 1 && KeyboardEdit.problem(second).isEmpty() && cell(second, KeyboardLayout.F1 + 12).x() == fresh.right() && cell(second, KeyboardLayout.F1 + 12).y() == fresh.y(),
                "the next added key takes the next free place in the row");
        h.assertTrue(KeyboardEdit.problem(List.of()).isPresent(), "an empty layout is invalid");
        Cell badL = new Cell(KeyboardLayout.ENTER, "Enter", 0, 0, 4, 4, 10, 10, 4, 4);
        h.assertTrue(KeyboardEdit.problem(List.of(badL)).map(KeyboardEdit.Problem::kind).orElse("").equals("shape"), "L parts must touch");
        h.assertTrue(KeyboardEdit.decode(KeyboardEdit.encode(de)).map(de::equals).orElse(false), "JSON round trip keeps every cap, also the L");
        com.google.gson.JsonArray broken = KeyboardEdit.encode(de);
        broken.get(0).getAsJsonObject().addProperty("key", "key.keyboard.nope");
        h.assertTrue(KeyboardEdit.decode(broken).map(l -> l.size() == de.size() - 1).orElse(false), "an unknown key name is skipped");
        broken.get(1).getAsJsonObject().addProperty("x", cell(de, 'A').x());
        broken.get(1).getAsJsonObject().addProperty("y", cell(de, 'A').y());
        h.assertTrue(KeyboardEdit.decode(broken).isEmpty(), "a hand-edited overlap makes the file layout invalid");
        h.assertTrue(KeyboardEdit.validName("My DE") && !KeyboardEdit.validName("de") && !KeyboardEdit.validName("") && !KeyboardEdit.validName(" x")
                && !KeyboardEdit.validName("12345678901234567"), "names");
        h.succeed();
    }
}
