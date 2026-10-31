package com.simpleores;

import java.util.function.Consumer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/**
 * The loot way to Enderite on its own (IG31-13, formerly SimpleLib's {@code MaterialLoot}): End city treasure gets the scrap
 * (15 % per chest), the Enderite upgrade template (30 %) and one roll of raw Enderite (1-2) or Enderite nuggets (2-5) - the
 * numbers SimpleBuilding has always used. A partner that feeds the same loot itself (SimpleBuilding, with its own switches)
 * calls {@code SimpleLibApi.materialLootByPartner()}; then Simple Ores adds nothing. Loaders call {@link #apply} from their
 * loot table event.
 */
public final class OresLoot {
    private OresLoot() {}

    public static void apply(ResourceKey<LootTable> key, Consumer<LootPool.Builder> add) {
        if (!BuiltInLootTables.END_CITY_TREASURE.equals(key) || com.simplelib.api.SimpleLibApi.materialLootFedByPartner()) return;
        add.accept(LootPool.lootPool().setRolls(ContextIntProviders.binomial(1, 0.15F))
                .add(LootItem.lootTableItem(OresRegistry.item("enderite_scrap"))));
        add.accept(LootPool.lootPool().setRolls(ContextIntProviders.binomial(1, 0.3F))
                .add(LootItem.lootTableItem(OresRegistry.item("enderite_upgrade_template"))));
        // SimpleBuilding's pool has raw enderite 4, nuggets 6 of 36 weight (the rest are its own End materials).
        add.accept(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
                .add(LootItem.lootTableItem(OresRegistry.item("raw_enderite")).setWeight(4)
                        .apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 2))))
                .add(LootItem.lootTableItem(OresRegistry.item("enderite_nugget")).setWeight(6)
                        .apply(SetItemCountFunction.setCount(ContextIntProviders.between(2, 5))))
                .add(EmptyLootItem.emptyItem().setWeight(26)));
    }
}
