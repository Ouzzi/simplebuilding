package com.simplebuilding.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ItemLike;

import java.util.Arrays;
import java.util.List;

/**
 * Zeilen-Layout fuer einen Kreativ-Tab: eine Kategorie je Zeile, der Rest der Zeile bleibt leer.
 *
 * <p>Das Kreativinventar ist 9 Plaetze breit und fuellt Zeile fuer Zeile. Damit eine Kategorie
 * links in einer neuen Zeile beginnt, wird jede Zeile mit unsichtbaren Platzhaltern
 * ({@link ModItems#CREATIVE_SPACER}) bis zum Zeilenende aufgefuellt. Eine Kategorie mit mehr als
 * neun Eintraegen (etwa 16 gefaerbte Varianten) laeuft einfach in die naechste Zeile weiter und wird
 * erst an ihrem Ende aufgefuellt. Nach der letzten Zeile kommt kein Fueller.
 *
 * <p>Platzhalter sind nur im Tab selbst sichtbar ({@link CreativeModeTab.TabVisibility#PARENT_TAB_ONLY}),
 * nie im Suchtab. Weil Vanilla denselben Stapel in einem Tab nur einmal annimmt, traegt jeder
 * Platzhalter seine laufende Nummer in {@code custom_data}.
 *
 * <p>Neue Tabs uebernehmen das Layout, indem sie ihre Kategorien als {@link Row}-Liste beschreiben
 * und {@link #emit} aufrufen.
 */
public final class CreativeTabLayout {
    /** Breite des Kreativinventars in Plaetzen. */
    public static final int ROW_WIDTH = 9;

    /** Schluessel der laufenden Nummer im {@code custom_data} eines Platzhalters. */
    public static final String SPACER_INDEX_KEY = "simplebuilding_spacer";

    private CreativeTabLayout() {
    }

    /**
     * Eine Kategorie: ein Name (nur fuer Tests und Fehlermeldungen) und ihre Stapel in Anzeigereihenfolge.
     * Vanilla zuerst, dann die Stufen aufsteigend.
     */
    public record Row(String name, List<ItemStack> stacks) {
        public static Row of(String name, ItemLike... items) {
            return new Row(name, Arrays.stream(items).map(ItemStack::new).toList());
        }
    }

    /** Gibt alle Zeilen aus, jede bis auf die letzte bis zum Zeilenende mit Platzhaltern aufgefuellt. */
    public static void emit(CreativeModeTab.Output entries, List<Row> rows) {
        int spacers = 0;
        for (int r = 0; r < rows.size(); r++) {
            List<ItemStack> stacks = rows.get(r).stacks();
            for (ItemStack stack : stacks) {
                entries.accept(stack.copy());
            }
            if (r == rows.size() - 1) {
                break;
            }
            int remainder = stacks.size() % ROW_WIDTH;
            int padding = remainder == 0 ? 0 : ROW_WIDTH - remainder;
            for (int i = 0; i < padding; i++) {
                entries.accept(spacer(spacers++), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
            }
        }
    }

    /** Ein Platzhalter mit eindeutiger Nummer. */
    public static ItemStack spacer(int index) {
        ItemStack stack = new ItemStack(ModItems.CREATIVE_SPACER);
        CompoundTag tag = new CompoundTag();
        tag.putInt(SPACER_INDEX_KEY, index);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }
}
