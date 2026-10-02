package com.simplebuilding.dev.testcentre;

import com.simplebuilding.items.ModItemGroupsContent;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * Item-orientierter Rundgang (B12, Besitzer 2026-10-01: "einmal item-, einmal testorientiert"): je
 * Kreativ-Tab der Mod eine Wand mit dem ganzen Tab in Anzeigereihenfolge, spaltenweise von oben nach
 * unten. Die Stationen davor sind der testorientierte Rundgang. Die Waende zaehlen nicht fuer den
 * Abdeckungstest - dort muss jedes Item eine Station haben, in der man es ausprobieren kann.
 */
public final class TabBrowser {

    /** Reihen je Wand. */
    public static final int ROWS = 6;
    /** Praefix der Abschnitts-Ids, dahinter {@link ModItemGroupsContent.Tab#id}. */
    public static final String PREFIX = "tab_";

    private TabBrowser() {
    }

    public static String sectionId(ModItemGroupsContent.Tab tab) {
        return PREFIX + tab.id;
    }

    public static TcCanvas build(TcContext ctx, ModItemGroupsContent.Tab tab) {
        TcCanvas c = new TcCanvas();
        List<ItemStack> items = ctx.tab(tab);
        if (items.isEmpty()) {
            // Tab ohne Inhalt auf dieser Linie (Pfeile auf 26.2): ein leerer Abschnitt ohne Breite.
            return c;
        }
        int wallZ = 2;
        String id = sectionId(tab);
        c.title(0, ROWS, wallZ, TcText.t("section." + id, TestCentreSections.pretty(id)),
                TcText.t("section.tab.sub", "%s items, tab order", items.size()));
        int end = c.frameGrid(1, 0, wallZ, items, null, ROWS);
        c.backWall(0, end, wallZ, ROWS + 2);
        return c;
    }
}
