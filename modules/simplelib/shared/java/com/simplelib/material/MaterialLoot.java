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

    /** Bastion chests (owner N71): warped wart, like nether wart is found in nether fortress chests. */
    private static final java.util.Set<ResourceKey<LootTable>> BASTION_CHESTS = java.util.Set.of(BuiltInLootTables.BASTION_BRIDGE,
            BuiltInLootTables.BASTION_HOGLIN_STABLE, BuiltInLootTables.BASTION_OTHER, BuiltInLootTables.BASTION_TREASURE);

    /** One pool per bastion chest: 35 % chance for 2-5 warped wart (treasure chests: 60 %, 3-7). */
    public static void applyWarpedWart(ResourceKey<LootTable> key, Consumer<LootPool.Builder> add) {
        if (!BASTION_CHESTS.contains(key)) return;
        boolean treasure = BuiltInLootTables.BASTION_TREASURE.equals(key);
        add.accept(LootPool.lootPool().setRolls(ContextIntProviders.binomial(1, treasure ? 0.6F : 0.35F))
                .add(LootItem.lootTableItem(com.simplelib.registry.LibItems.WARPED_WART)
                        .apply(SetItemCountFunction.setCount(ContextIntProviders.between(treasure ? 3 : 2, treasure ? 7 : 5)))));
    }

    public static void apply(ResourceKey<LootTable> key, Consumer<LootPool.Builder> add, HolderGetter.Provider registries) {
        applyWarpedWart(key, add);
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
