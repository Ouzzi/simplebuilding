package com.simplestorage.rare;

import com.simplestorage.SimpleStorage;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/**
 * PLAN40 simplestorage-1 (module independence, rule 1): the storage enchantments Deep Pockets, Drawer and Funnel are in no Vanilla
 * enchanting or trade tag; with SimpleBuilding they come from its loot injections and its librarian. On its own Simple Storage adds
 * the books to Vanilla chests itself - one pool per table, a book against an empty entry, about the odds SimpleBuilding gives them.
 * With SimpleBuilding loaded nothing is added (its injections already carry the same books). Loaders call {@link #apply} from their
 * loot table event.
 */
public final class StorageLoot {
    /** A Vanilla chest table, the storage enchantment and level of its book, and the book's weight against the empty entry. */
    public record Source(ResourceKey<LootTable> table, String enchantment, int level, int weight, int empty) {
        /** The chance per chest, for the wiki and the tests. */
        public double chance() {
            return this.weight / (double) (this.weight + this.empty);
        }
    }

    private static volatile Boolean simpleBuilding;

    private StorageLoot() {
    }

    /** The sources (a fixed list, in the wiki): about 4-9 % per chest. */
    public static List<Source> sources() {
        return List.of(
                new Source(BuiltInLootTables.SIMPLE_DUNGEON, "funnel", 1, 2, 23),
                new Source(BuiltInLootTables.SIMPLE_DUNGEON, "deep_pockets", 1, 1, 19),
                new Source(BuiltInLootTables.NETHER_BRIDGE, "funnel", 1, 1, 24),
                new Source(BuiltInLootTables.BASTION_OTHER, "funnel", 1, 1, 15),
                new Source(BuiltInLootTables.BASTION_TREASURE, "funnel", 1, 1, 11),
                new Source(BuiltInLootTables.ANCIENT_CITY, "deep_pockets", 2, 1, 10),
                new Source(BuiltInLootTables.WOODLAND_MANSION, "drawer", 1, 1, 15),
                new Source(BuiltInLootTables.STRONGHOLD_LIBRARY, "drawer", 1, 1, 12));
    }

    /** Whether SimpleBuilding is installed (any item of its namespace; read once, after registration). */
    public static boolean simpleBuildingLoaded() {
        Boolean known = simpleBuilding;
        if (known == null) {
            known = BuiltInRegistries.ITEM.keySet().stream().anyMatch(id -> id.getNamespace().equals(SimpleStorage.OLD_NAMESPACE));
            simpleBuilding = known;
        }
        return known;
    }

    /** Adds this table's book pools (only without SimpleBuilding and while the module is switched on). */
    public static void apply(ResourceKey<LootTable> key, Consumer<LootPool.Builder> add, HolderGetter.Provider registries) {
        if (!SimpleStorage.on() || simpleBuildingLoaded()) return;
        for (Source source : sources()) {
            if (!source.table().equals(key)) continue;
            var enchantment = registries.lookupOrThrow(Registries.ENCHANTMENT)
                    .get(ResourceKey.create(Registries.ENCHANTMENT, SimpleStorage.id(source.enchantment())));
            if (enchantment.isEmpty()) continue;
            ItemEnchantments.Mutable book = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            book.upgrade(enchantment.get(), source.level());
            add.accept(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
                    .add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(source.weight())
                            .apply(SetComponentsFunction.setComponent(DataComponents.STORED_ENCHANTMENTS, book.toImmutable())))
                    .add(EmptyLootItem.emptyItem().setWeight(source.empty())));
        }
    }

    /** The enchantment key of a source (tests). */
    public static ResourceKey<Enchantment> key(Source source) {
        return ResourceKey.create(Registries.ENCHANTMENT, SimpleStorage.id(source.enchantment()));
    }
}
