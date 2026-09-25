package com.simplebuilding.items;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.platform.ModEnvironment;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Entwickler-Tab "SimpleEnchants (Dev)": vorverzauberte Gegenstaende auf Hoechststufe.
 *
 * <p><b>Sichtbarkeit.</b> Der Tab ist auf jedem Loader immer registriert, wird aber nur gefuellt,
 * wenn {@link #isShown()} gilt - in einer Entwicklungsumgebung oder mit der Konfigoption
 * {@code showDevEnchantedTab}. Ein leerer Kategorie-Tab wird von Vanilla
 * ({@code CreativeModeTab#shouldDisplay}) gar nicht angezeigt. Die Einträge sind
 * {@code PARENT_TAB_ONLY}, erscheinen also nie im Suchtab.
 *
 * <p><b>Keine Luecke auf Fabrics Tab-Seiten</b> (geprueft 2026-09-25 am Bytecode von
 * fabric-creative-tab-api-v1 5.0.21 fuer 26.2 und fabric-item-group-api-v1 4.2.36 fuer 1.21.11):
 * Fabric verteilt die Mod-Tabs nach jedem {@code CreativeModeTabs#buildAllTabContents} neu auf Seiten
 * ({@code CreativeModeTabsMixin#paginateTabs}) und sortiert dabei die angezeigten Tabs vor die
 * leeren. Ein leerer Dev-Tab landet also hinter allen sichtbaren Tabs statt zwischen ihnen, und weil
 * die Seitenzahl ({@code FabricCreativeGuiComponents#getPageCount}) nur {@code CreativeModeTabs#tabs()}
 * zaehlt - die angezeigten -, macht er auch keine Seite auf. Ihn ausserhalb der Entwicklung gar nicht
 * zu registrieren braucht es deshalb nicht (sie muesste sonst auf allen drei
 * Loadern eigens an Entwicklungsumgebung und Konfig gebunden werden).
 *
 * <p><b>Inhalt, vollstaendig aus Registern abgeleitet.</b>
 * <ul>
 *   <li>Traeger: jedes Mod-Item aus den vier normalen Tabs, das irgendeine (nicht verfluchte)
 *       Verzauberung unterstuetzt - je Familie nur die hoechste Stufe ({@link #TIERS}; gefaerbte
 *       Varianten zaehlen zur Grundform). Dazu Vanilla-Items, die eine Mod-Verzauberung unterstuetzen,
 *       wieder nur die hoechste Stufe je Familie und nur, wenn sie nicht bloss eine schwaechere Ausgabe
 *       eines Mod-Traegers sind (haltbar und mit einer Teilmenge von dessen Verzauberungen) - uebrig
 *       bleiben Stock und Shulkerkiste, denen Constructor's Touch eine eigene Funktion gibt.</li>
 *   <li>Verzauberungen: jede, deren {@code supportedItems} das Item enthaelt, auf {@code maxLevel};
 *       Flueche nicht. Unvertraegliche Verzauberungen ({@code exclusiveSet}, in beide Richtungen
 *       gelesen) bilden Gruppen; je Gruppe gibt es Optionen (je Mitglied eine maximale vertraegliche
 *       Auswahl). Ist das Produkt der Optionszahlen hoechstens {@link #MAX_CROSS_PRODUCT}, wird jede
 *       Kombination ausgegeben, sonst eine Grundvariante (je Gruppe die Vorgabe) plus je Gruppe und
 *       Nicht-Vorgabe-Option eine Variante.</li>
 * </ul>
 * Neue Verzauberungen und neue Items erscheinen damit ohne Aenderung hier.
 */
public final class DevEnchantedTab {
    public static final String ID = "enchanted_dev";

    /** Hoechstens so viele Varianten je Item werden als volles Kreuzprodukt ausgegeben. */
    public static final int MAX_CROSS_PRODUCT = 6;

    /** Stufen-Praefixe, aufsteigend; kein Praefix ist die Grundform (Stufe -1). */
    private static final List<String> TIERS = List.of(
            "leather", "wooden", "chainmail", "stone", "copper", "golden", "gold", "iron",
            "reinforced", "diamond", "netherite", "enderite");

    private DevEnchantedTab() {
    }

    public static String translationKey() {
        return "itemgroup." + Simplebuilding.MOD_ID + "." + ID;
    }

    public static ItemStack icon() {
        ItemStack icon = new ItemStack(ModItems.ENDERITE_PICKAXE);
        icon.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return icon;
    }

    /** Entwicklungsumgebung oder Konfigoption {@code showDevEnchantedTab}. */
    public static boolean isShown() {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        return ModEnvironment.isDevelopmentEnvironment() || (config != null && config.showDevEnchantedTab);
    }

    /** Was jeder Loader dem registrierten Tab als Inhalt gibt. */
    public static void populateIfShown(CreativeModeTab.Output entries, HolderLookup.Provider lookup) {
        if (isShown()) {
            populate(entries, lookup);
        }
    }

    /** Der Inhalt, unabhaengig von der Sichtbarkeit. */
    public static void populate(CreativeModeTab.Output entries, HolderLookup.Provider lookup) {
        for (ItemStack stack : variants(lookup)) {
            entries.accept(stack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
        }
    }

    public static List<ItemStack> variants(HolderLookup.Provider lookup) {
        List<Holder<Enchantment>> enchantments = enchantments(lookup);
        List<ItemStack> out = new ArrayList<>();
        for (Item item : carriers(lookup, enchantments)) {
            for (List<Holder<Enchantment>> set : enchantmentSets(new ItemStack(item), enchantments)) {
                ItemStack stack = new ItemStack(item);
                ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                for (Holder<Enchantment> enchantment : set) {
                    builder.set(enchantment, enchantment.value().getMaxLevel());
                }
                stack.set(DataComponents.ENCHANTMENTS, builder.toImmutable());
                out.add(stack);
            }
        }
        return out;
    }

    /** Alle Verzauberungen ausser Fluechen, in Registerreihenfolge. */
    public static List<Holder<Enchantment>> enchantments(HolderLookup.Provider lookup) {
        List<Holder<Enchantment>> out = new ArrayList<>();
        lookup.lookupOrThrow(Registries.ENCHANTMENT).listElements()
                .filter(holder -> !holder.is(EnchantmentTags.CURSE))
                .forEach(out::add);
        return out;
    }

    private static List<Holder<Enchantment>> supporting(ItemStack stack, List<Holder<Enchantment>> enchantments) {
        return enchantments.stream().filter(e -> e.value().isSupportedItem(stack)).toList();
    }

    private static boolean isMod(Holder<Enchantment> enchantment) {
        return enchantment.unwrapKey().map(k -> Simplebuilding.MOD_ID.equals(k.identifier().getNamespace())).orElse(false);
    }

    /** Die Items des Tabs in Anzeigereihenfolge: erst die Mod-Traeger, dann die Vanilla-Traeger. */
    public static List<Item> carriers(HolderLookup.Provider lookup) {
        return carriers(lookup, enchantments(lookup));
    }

    private static List<Item> carriers(HolderLookup.Provider lookup, List<Holder<Enchantment>> enchantments) {
        Set<Item> modItems = new LinkedHashSet<>();
        ModItemGroupsContent.populate((CreativeModeTab.Output) (stack, visibility) -> {
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (Simplebuilding.MOD_ID.equals(id.getNamespace()) && stack.getItem() != ModItems.CREATIVE_SPACER) {
                modItems.add(stack.getItem());
            }
        }, lookup);

        List<Item> mod = topTiers(modItems.stream()
                .filter(item -> !supporting(new ItemStack(item), enchantments).isEmpty()).toList());

        List<Set<Holder<Enchantment>>> modSets = mod.stream()
                .map(item -> (Set<Holder<Enchantment>>) new LinkedHashSet<>(supporting(new ItemStack(item), enchantments)))
                .toList();
        List<Item> vanillaCandidates = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (Simplebuilding.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            List<Holder<Enchantment>> supported = supporting(stack, enchantments);
            if (supported.stream().noneMatch(DevEnchantedTab::isMod)) {
                continue;
            }
            boolean dominated = stack.isDamageableItem()
                    && modSets.stream().anyMatch(set -> set.containsAll(supported));
            if (!dominated) {
                vanillaCandidates.add(item);
            }
        }

        List<Item> out = new ArrayList<>(mod);
        out.addAll(topTiers(vanillaCandidates));
        return out;
    }

    /** Je Familie (Pfad ohne Stufen-Praefix und ohne Farb-Suffix) das Item mit der hoechsten Stufe. */
    private static List<Item> topTiers(List<Item> items) {
        Map<String, Item> best = new LinkedHashMap<>();
        for (Item item : items) {
            String path = BuiltInRegistries.ITEM.getKey(item).getPath();
            String family = family(path);
            Item current = best.get(family);
            if (current == null || tier(path) > tier(BuiltInRegistries.ITEM.getKey(current).getPath())) {
                best.put(family, item);
            }
        }
        return new ArrayList<>(best.values());
    }

    private static int tier(String path) {
        for (int i = TIERS.size() - 1; i >= 0; i--) {
            if (path.startsWith(TIERS.get(i) + "_")) {
                return i;
            }
        }
        return -1;
    }

    private static String family(String path) {
        int tier = tier(path);
        String family = tier < 0 ? path : path.substring(TIERS.get(tier).length() + 1);
        // Laengste Farbnamen zuerst, sonst endet "light_gray" schon auf "gray".
        List<DyeColor> colors = new ArrayList<>(List.of(DyeColor.values()));
        colors.sort(Comparator.comparingInt((DyeColor c) -> -c.getName().length()));
        for (DyeColor color : colors) {
            if (family.endsWith("_" + color.getName())) {
                return family.substring(0, family.length() - color.getName().length() - 1);
            }
        }
        return family;
    }

    /**
     * Die Verzauberungs-Auswahlen fuer ein Item. Jede Auswahl ist vertraeglich und maximal, und jede
     * Verzauberung, die das Item unterstuetzt, steckt in mindestens einer.
     */
    public static List<List<Holder<Enchantment>>> enchantmentSets(ItemStack stack, List<Holder<Enchantment>> enchantments) {
        List<Holder<Enchantment>> supported = supporting(stack, enchantments);
        if (supported.isEmpty()) {
            return List.of();
        }
        // Zusammenhangskomponenten des Unvertraeglichkeitsgraphen.
        List<List<Holder<Enchantment>>> groups = new ArrayList<>();
        Set<Holder<Enchantment>> seen = new LinkedHashSet<>();
        for (Holder<Enchantment> start : supported) {
            if (!seen.add(start)) {
                continue;
            }
            List<Holder<Enchantment>> group = new ArrayList<>(List.of(start));
            for (int i = 0; i < group.size(); i++) {
                for (Holder<Enchantment> other : supported) {
                    if (!seen.contains(other) && !Enchantment.areCompatible(group.get(i), other)) {
                        seen.add(other);
                        group.add(other);
                    }
                }
            }
            groups.add(group);
        }

        List<Holder<Enchantment>> fixed = new ArrayList<>();
        List<List<List<Holder<Enchantment>>>> choices = new ArrayList<>();
        for (List<Holder<Enchantment>> group : groups) {
            if (group.size() == 1) {
                fixed.add(group.getFirst());
            } else {
                choices.add(options(group));
            }
        }

        long product = 1;
        for (List<List<Holder<Enchantment>>> options : choices) {
            product *= options.size();
        }
        List<List<Holder<Enchantment>>> result = new ArrayList<>();
        if (product <= MAX_CROSS_PRODUCT) {
            List<List<Holder<Enchantment>>> partial = new ArrayList<>();
            partial.add(new ArrayList<>(fixed));
            for (List<List<Holder<Enchantment>>> options : choices) {
                List<List<Holder<Enchantment>>> next = new ArrayList<>();
                for (List<Holder<Enchantment>> base : partial) {
                    for (List<Holder<Enchantment>> option : options) {
                        List<Holder<Enchantment>> combined = new ArrayList<>(base);
                        combined.addAll(option);
                        next.add(combined);
                    }
                }
                partial = next;
            }
            result.addAll(partial);
        } else {
            List<Holder<Enchantment>> base = new ArrayList<>(fixed);
            for (List<List<Holder<Enchantment>>> options : choices) {
                base.addAll(options.getFirst());
            }
            result.add(base);
            for (int g = 0; g < choices.size(); g++) {
                List<List<Holder<Enchantment>>> options = choices.get(g);
                for (int o = 1; o < options.size(); o++) {
                    List<Holder<Enchantment>> variant = new ArrayList<>(fixed);
                    for (int h = 0; h < choices.size(); h++) {
                        variant.addAll(h == g ? options.get(o) : choices.get(h).getFirst());
                    }
                    result.add(variant);
                }
            }
        }
        return result;
    }

    /**
     * Die Optionen einer Unvertraeglichkeitsgruppe: je Mitglied eine maximale vertraegliche Auswahl,
     * die es enthaelt (gierig in Bevorzugungsreihenfolge aufgefuellt), ohne Dubletten. Die erste Option
     * ist die Vorgabe: die groesste, bei Gleichstand die mit dem hoechsten Gewicht - also die
     * gebraeuchlichste Verzauberung (Schutz vor Feuerschutz, Schaerfe vor Bann, Glueck vor Behutsamkeit).
     */
    private static List<List<Holder<Enchantment>>> options(List<Holder<Enchantment>> group) {
        List<Holder<Enchantment>> preferred = new ArrayList<>(group);
        preferred.sort(Comparator.<Holder<Enchantment>>comparingInt(e -> -e.value().getWeight())
                .thenComparing(e -> e.unwrapKey().map(k -> k.identifier().toString()).orElse("")));
        List<List<Holder<Enchantment>>> options = new ArrayList<>();
        Set<Set<Holder<Enchantment>>> distinct = new LinkedHashSet<>();
        for (Holder<Enchantment> member : preferred) {
            List<Holder<Enchantment>> option = new ArrayList<>(List.of(member));
            for (Holder<Enchantment> other : preferred) {
                if (option.contains(other)) {
                    continue;
                }
                if (option.stream().allMatch(chosen -> Enchantment.areCompatible(chosen, other))) {
                    option.add(other);
                }
            }
            if (distinct.add(new LinkedHashSet<>(option))) {
                options.add(option);
            }
        }
        options.sort(Comparator.<List<Holder<Enchantment>>>comparingInt(o -> -o.size())
                .thenComparingInt(o -> -o.stream().mapToInt(e -> e.value().getWeight()).sum()));
        return options;
    }
}
