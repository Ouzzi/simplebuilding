package com.simplebuilding.dev.testcentre;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.items.CreativeTabLayout;
import com.simplebuilding.items.DevEnchantedTab;
import com.simplebuilding.items.ModItemGroupsContent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;

/**
 * Was die Abschnitte zum Planen brauchen: die Register (Verzauberungen, Besatzmuster) und die
 * Inhaltslisten der Kreativ-Tabs. Die Tabs sind die kuratierte Ordnung der Mod - wer dort ein neues
 * Werkzeug in eine Zeile setzt, bekommt es damit auch in der Testzentrale.
 */
public record TcContext(HolderLookup.Provider lookup) {

    public HolderLookup.RegistryLookup<Enchantment> enchantmentLookup() {
        return lookup.lookupOrThrow(Registries.ENCHANTMENT);
    }

    /** Alle Verzauberungen ausser Fluechen (wie der Entwickler-Tab). */
    public List<Holder<Enchantment>> enchantments() {
        return DevEnchantedTab.enchantments(lookup);
    }

    public Optional<Holder<Enchantment>> enchantment(ResourceKey<Enchantment> key) {
        return enchantmentLookup().get(key).map(ref -> (Holder<Enchantment>) ref);
    }

    /** Zeile {@code name} aus SimpleTools oder Maschinen &amp; Lager; leer, wenn es sie nicht gibt. */
    public List<ItemStack> row(String name) {
        for (CreativeTabLayout.Row row : ModItemGroupsContent.toolsRows(enchantmentLookup())) {
            if (row.name().equals(name)) {
                return row.stacks();
            }
        }
        for (CreativeTabLayout.Row row : ModItemGroupsContent.functionalRows()) {
            if (row.name().equals(name)) {
                return row.stacks();
            }
        }
        return List.of();
    }

    public List<Item> rowItems(String name) {
        return row(name).stream().map(ItemStack::getItem).toList();
    }

    /** Der Inhalt eines Mod-Tabs in Anzeigereihenfolge, ohne Platzhalter. */
    public List<ItemStack> tab(ModItemGroupsContent.Tab tab) {
        List<ItemStack> out = new ArrayList<>();
        ModItemGroupsContent.populate(tab, (CreativeModeTab.Output) (stack, visibility) -> {
            if (!stack.isEmpty() && !isSpacer(stack.getItem())) {
                out.add(stack);
            }
        }, lookup);
        return out;
    }

    static boolean isSpacer(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath().equals("creative_spacer");
    }

    /** Stapel mit den gegebenen Verzauberungen auf Hoechststufe. */
    public static ItemStack enchanted(ItemStack base, List<Holder<Enchantment>> set) {
        ItemStack stack = base.copy();
        if (set.isEmpty()) {
            return stack;
        }
        ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (Holder<Enchantment> enchantment : set) {
            builder.set(enchantment, enchantment.value().getMaxLevel());
        }
        stack.set(DataComponents.ENCHANTMENTS, builder.toImmutable());
        return stack;
    }

    /** Die erste maximale Verzauberungs-Auswahl des Entwickler-Tabs, oder der Stapel unverzaubert. */
    public ItemStack maxEnchanted(ItemStack base) {
        List<List<Holder<Enchantment>>> sets = DevEnchantedTab.enchantmentSets(base, enchantments());
        return sets.isEmpty() ? base.copy() : enchanted(base, sets.getFirst());
    }

    /** Alle Auswahlen (exklusive Varianten) eines Stapels. */
    public List<ItemStack> allEnchantedVariants(ItemStack base) {
        List<ItemStack> out = new ArrayList<>();
        for (List<Holder<Enchantment>> set : DevEnchantedTab.enchantmentSets(base, enchantments())) {
            out.add(enchanted(base, set));
        }
        return out;
    }

    /** Ein verzaubertes Buch mit einer Verzauberung auf Hoechststufe. */
    public static ItemStack book(Holder<Enchantment> enchantment) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        builder.set(enchantment, enchantment.value().getMaxLevel());
        book.set(DataComponents.STORED_ENCHANTMENTS, builder.toImmutable());
        return book;
    }

    public static boolean isMod(Identifier id) {
        return Simplebuilding.MOD_ID.equals(id.getNamespace());
    }

    public static Identifier id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    public static Identifier id(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    /** Sortierung: erst Mod, dann Vanilla, jeweils nach Id. */
    public static final Comparator<Identifier> MOD_FIRST = Comparator
            .comparing((Identifier id) -> !isMod(id))
            .thenComparing(Identifier::toString);
}
