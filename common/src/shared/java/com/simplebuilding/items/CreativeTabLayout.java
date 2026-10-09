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
 * Zeilen-Layout fuer einen Kreativ-Tab: eine Kategorie nacheinander, zwischen zwei Kategorien nur
 * eine leere Zelle, wenn die vorige Zeile nicht exakt aufhoerte (Besitzer tweaks P8). Die
 * Spacer-Ausgabe ist standardmaessig aus, damit die Kategorien wieder ohne kuenstliche Luecken
 * aufeinanderfolgen; die alte Logik bleibt per Config {@code creativeTabSpacers} einschaltbar.
 *
 * <p>Das Kreativinventar ist 9 Plaetze breit und fuellt Zeile fuer Zeile. Damit zwischen zwei
 * Kategorien eine leere Zelle sichtbar bleibt, werden Trennzellen als unsichtbare Platzhalter
 * ({@link ModItems#CREATIVE_SPACER}) abgesetzt; nur wenn die vorige Kategorie exakt an einer
 * Spaltengrenze aufhoerte, beginnt die naechste direkt links. Nach der letzten Zeile kommt kein
 * Fueller.
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
 * eigene Zeile braucht. Passt sie nicht mehr in die Zeile, wird bis zum Zeilenende aufgefuellt und sie
 * beginnt wie jede andere links (Besitzer tweaks P8: nur diese Kategorie darf zurueckspringen).
 *
 * <p>Eine Zeile kann auch nach genau einer leeren Zelle hinter ihrem Vorgaenger weiterfliessen
 * ({@link Row#flowing}): seit tweaks P8 fliesst jede nicht exakt endende Zeile so weiter, das Flag
 * bleibt nur als Vertrag erhalten.
 *
 * <p>Neue Tabs uebernehmen das Layout, indem sie ihre Kategorien als {@link Row}-Liste beschreiben
 * und {@link #emit} aufrufen.
 */
public final class CreativeTabLayout {
    /**
     * Ob {@link #emit} Trennzellen ausgibt: Config {@code creativeTabSpacers}, Standard aus (Besitzer N22: Tabs wieder
     * ohne Luecken, die Spacer-Logik bleibt abschaltbar erhalten).
     */
    public static boolean spacersEnabled() {
        com.simplebuilding.config.SimplebuildingConfig config = com.simplebuilding.Simplebuilding.getConfig();
        return config != null && config.creativeTabSpacers;
    }
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

    /**
     * Gibt alle Zeilen aus: zwischen zwei Kategorien eine leere Zelle, wenn die vorige nicht exakt an
     * einer Spaltengrenze aufhoerte. Nur eine Kategorie, die in der Zeile ihres Vorgaengers weiterlaufen
     * soll ({@link Row#besides}), darf ohne Passform zurueckspringen und bekommt dann den Rest der Zeile
     * als Fueller; alle anderen brechen mit der Trennzelle einfach um. Nach der letzten Zeile kommt kein
     * Fueller.
     */
    public static void emit(CreativeModeTab.Output entries, List<Row> rows) {
        if (!spacersEnabled()) {
            for (Row row : rows) {
                for (ItemStack stack : row.stacks()) {
                    if (!stack.isEmpty()) {
                        entries.accept(stack.copy());
                    }
                }
            }
            return;
        }
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
            if (remainder == 0) {
                continue;
            }
            Row next = rows.get(r + 1);
            int padding = 1;
            if (next.besidePrevious() && remainder + 1 + next.stacks().size() > ROW_WIDTH) {
                padding = ROW_WIDTH - remainder;
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
