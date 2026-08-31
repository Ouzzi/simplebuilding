package com.simplebuilding.trade;

import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Gewichteter Verzauberungs-Pool für code-registrierte Trades — loader-neutral.
 *
 * <p>Entspricht 1:1 der Loot-Funktion {@code simplebuilding:weighted_enchant} der 26.2-Linie:
 * eine Verzauberung wird nach Gewicht gezogen, mit {@code secondChance} zusätzlich eine zweite,
 * andere. Die Auswahl selbst liegt in {@link WeightedPicker} und wird nicht dupliziert.
 *
 * <p>Anders als die Loot-Funktion hält dieser Pool {@link ResourceKey}s statt aufgelöster Holder,
 * weil Trades bereits bei der Mod-Initialisierung definiert werden — lange bevor eine
 * Enchantment-Registry existiert. Aufgelöst wird erst in {@link #apply}, mit der Registry der
 * Welt, in der das Angebot entsteht.
 */
public record EnchantmentPool(List<Entry> entries, float secondChance) {
    /** Ein Pool-Eintrag: Verzauberung mit fester Stufe und Ziehungsgewicht. */
    public record Entry(ResourceKey<Enchantment> enchantment, int level, int weight) {
    }

    /** Leerer Pool: {@link #apply} lässt den Stack unverändert. */
    public static final EnchantmentPool NONE = new EnchantmentPool(List.of(), 0.0F);

    public static Entry entry(ResourceKey<Enchantment> enchantment, int level, int weight) {
        return new Entry(enchantment, level, weight);
    }

    /** Pool ohne zweite Verzauberung. */
    public static EnchantmentPool of(Entry... entries) {
        return new EnchantmentPool(List.of(entries), 0.0F);
    }

    /** Pool mit Wahrscheinlichkeit {@code secondChance} (0..1) auf eine zweite Verzauberung. */
    public static EnchantmentPool of(float secondChance, List<Entry> entries) {
        return new EnchantmentPool(List.copyOf(entries), secondChance);
    }

    /**
     * Wendet den Pool auf {@code stack} an. Ein einfaches Buch wird — wie in der Loot-Funktion —
     * zum verzauberten Buch umgewandelt; {@code EnchantmentHelper.updateEnchantments} schreibt
     * bei verzauberten Büchern automatisch {@code stored_enchantments}, sonst {@code enchantments}.
     *
     * @param registries Registry-Zugriff der Welt (z. B. {@code serverLevel.registryAccess()}).
     * @return der verzauberte Stack (bei Buch-Umwandlung eine Kopie), sonst {@code stack} selbst.
     */
    public ItemStack apply(ItemStack stack, HolderLookup.Provider registries, RandomSource random) {
        if (this.entries.isEmpty()) {
            return stack;
        }
        List<Entry> picks = WeightedPicker.pickOneOrTwo(
                this.entries, Entry::weight, Entry::enchantment, this.secondChance, random);
        if (picks.isEmpty()) {
            return stack;
        }

        ItemStack result = stack.is(Items.BOOK) ? stack.transmuteCopy(Items.ENCHANTED_BOOK) : stack;
        HolderLookup.RegistryLookup<Enchantment> lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        EnchantmentHelper.updateEnchantments(result, enchantments -> {
            for (Entry pick : picks) {
                enchantments.set(lookup.getOrThrow(pick.enchantment()), pick.level());
            }
        });
        return result;
    }
}
