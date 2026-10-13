package com.simplelib.material;

import com.simplelib.registry.LibMaterials;
import java.util.function.Consumer;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/**
 * The loot way to Enderite once a mod requested it (split plan §2.5, "Bezug nur auf Anfrage"): End city treasure gets
 * the scrap (15 % per chest), the Enderite upgrade template (30 %, when the templates are requested) and one roll of
 * raw Enderite (1-2) or Enderite nuggets (2-5) - the numbers SimpleBuilding has always used. A partner that feeds the
 * same loot itself (SimpleBuilding, with its own switches) calls {@code SimpleLibApi.materialLootByPartner()}; then
 * SimpleLib adds nothing. Loaders call {@link #apply} from their loot table event.
 */
public final class MaterialLoot {
    private static volatile boolean partnerFeeds;

    private MaterialLoot() {}

    public static void byPartner() {
        partnerFeeds = true;
    }

    public static boolean isByPartner() {
        return partnerFeeds;
    }

    public static void apply(ResourceKey<LootTable> key, Consumer<LootPool.Builder> add, HolderGetter.Provider registries) {
        if (partnerFeeds || !BuiltInLootTables.END_CITY_TREASURE.equals(key) || !LibMaterials.requested("enderite")) return;
        add.accept(LootPool.lootPool().setRolls(ContextIntProviders.binomial(1, 0.15F))
                .add(LootItem.lootTableItem(LibMaterials.item("enderite_scrap"))));
        if (LibMaterials.requested("upgrade_templates")) {
            add.accept(LootPool.lootPool().setRolls(ContextIntProviders.binomial(1, 0.3F))
                    .add(LootItem.lootTableItem(LibMaterials.item("enderite_upgrade_template"))));
        }
        // SimpleBuilding's pool has raw enderite 4, nuggets 6 of 36 weight (the rest are its own End materials).
        add.accept(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
                .add(LootItem.lootTableItem(LibMaterials.item("raw_enderite")).setWeight(4)
                        .apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 2))))
                .add(LootItem.lootTableItem(LibMaterials.item("enderite_nugget")).setWeight(6)
                        .apply(SetItemCountFunction.setCount(ContextIntProviders.between(2, 5))))
                .add(EmptyLootItem.emptyItem().setWeight(26)));
    }
}
