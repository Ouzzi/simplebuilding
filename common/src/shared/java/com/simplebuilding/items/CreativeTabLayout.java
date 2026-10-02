package com.simplebuilding.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
 * <p>Eine Zeile darf auch zwei Familien nebeneinander tragen: {@link #GAP} zwischen ihnen wird zu einem
 * Platzhalter, also einer leeren Zelle (Besitzer 2026-09-28, etwa "4 Trichter, Luecke, 4 Oefen").
 *
 * <p>Eine Zeile kann auch in der Zeile ihres Vorgaengers weiterlaufen ({@link Row#besides}): dann steht
 * zwischen beiden genau eine leere Zelle statt der Auffuellung bis zum Zeilenende - etwa die Bauplanung
 * (Blaupause, Kartografentisch) rechts neben den Baustaeben, eine eigene Kategorie, die aber keine
 * eigene Zeile braucht. Passt sie nicht mehr in die Zeile, beginnt sie wie jede andere links.
 *
 * <p>Eine Zeile kann auch nach genau einer leeren Zelle hinter ihrem Vorgaenger weiterfliessen
 * ({@link Row#flowing}), selbst wenn sie dort nicht ganz passt und in die naechste Zeile umbricht - etwa
 * die Handbuecher und verzauberten Buecher, damit keine Buecherzeile fast leer auslaeuft (Audit 2026-10-02).
 *
 * <p>Neue Tabs uebernehmen das Layout, indem sie ihre Kategorien als {@link Row}-Liste beschreiben
 * und {@link #emit} aufrufen.
 */
public final class CreativeTabLayout {
    /** Breite des Kreativinventars in Plaetzen. */
    public static final int ROW_WIDTH = 9;

    /** Schluessel der laufenden Nummer im {@code custom_data} eines Platzhalters. */
    public static final String SPACER_INDEX_KEY = "simplebuilding_spacer";

    /** Leere Zelle innerhalb einer Zeile ({@link Row#of} macht daraus einen leeren Stapel, {@link #emit} einen Platzhalter). */
    public static final ItemLike GAP = Items.AIR;

    private CreativeTabLayout() {
    }

    /**
     * Eine Kategorie: ein Name (nur fuer Tests und Fehlermeldungen) und ihre Stapel in Anzeigereihenfolge.
     * Vanilla zuerst, dann die Stufen aufsteigend.
     */
    public record Row(String name, List<ItemStack> stacks, boolean besidePrevious, boolean flowOn) {
        public Row(String name, List<ItemStack> stacks) {
            this(name, stacks, false);
        }

        public Row(String name, List<ItemStack> stacks, boolean besidePrevious) {
            this(name, stacks, besidePrevious, false);
        }

        /** Eine Kategorie, die nach einer leeren Zelle hinter der vorigen weiterlaeuft und bei Bedarf umbricht. */
        public static Row flowing(String name, List<ItemStack> stacks) {
            return new Row(name, stacks, false, true);
        }

        public static Row of(String name, ItemLike... items) {
            return new Row(name, Arrays.stream(items).map(ItemStack::new).toList());
        }

        /** Eine Kategorie, die nach einer leeren Zelle in der Zeile der vorigen weiterlaeuft, wenn sie dort passt. */
        public static Row besides(String name, ItemLike... items) {
            return new Row(name, Arrays.stream(items).map(ItemStack::new).toList(), true);
        }
    }

    /** Gibt alle Zeilen aus, jede bis auf die letzte bis zum Zeilenende mit Platzhaltern aufgefuellt. */
    public static void emit(CreativeModeTab.Output entries, List<Row> rows) {
        int spacers = 0;
        int column = 0;
        for (int r = 0; r < rows.size(); r++) {
            List<ItemStack> stacks = rows.get(r).stacks();
            column += stacks.size();
            for (ItemStack stack : stacks) {
                if (stack.isEmpty()) {
                    entries.accept(spacer(spacers++), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
                } else {
                    entries.accept(stack.copy());
                }
            }
            if (r == rows.size() - 1) {
                break;
            }
            int remainder = column % ROW_WIDTH;
            Row next = rows.get(r + 1);
            int padding = remainder == 0 ? 0 : ROW_WIDTH - remainder;
            if (next.besidePrevious() && remainder != 0 && remainder + 1 + next.stacks().size() <= ROW_WIDTH) {
                padding = 1;
            }
            if (next.flowOn() && remainder != 0) {
                padding = 1;
            }
            column += padding;
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
