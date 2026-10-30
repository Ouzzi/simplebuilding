package com.simplebuilding.modules.simpleinterfaces;

import com.simplelib.api.client.keys.KeyConflicts;
import com.simplelib.api.client.keys.KeyConflicts.Binding;
import com.simplelib.api.client.keys.KeyboardLayout;
import com.simplelib.api.client.keys.KeyboardLayout.Cell;
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
            for (boolean german : new boolean[] {false, true}) {
                List<Cell> cells = KeyboardLayout.cells(size, german);
                h.assertTrue(cells.size() == counts[i], size + " has " + counts[i] + " keys, found " + cells.size());
                Set<Integer> codes = new HashSet<>();
                for (Cell c : cells) {
                    h.assertTrue(codes.add(c.code()), size + ": key " + c.label() + " twice");
                    h.assertTrue(c.w() > 0 && c.h() > 0 && c.x() >= 0 && c.y() >= 0, size + ": key " + c.label() + " outside");
                    for (Cell o : cells) {
                        if (o == c) continue;
                        boolean apart = c.right() <= o.x() || o.right() <= c.x() || c.bottom() <= o.y() || o.bottom() <= c.y();
                        h.assertTrue(apart, size + ": " + c.label() + " overlaps " + o.label());
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
}
