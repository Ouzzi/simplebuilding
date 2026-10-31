package com.simplelib.material;

import java.util.function.Consumer;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/**
 * Material loot of SimpleLib: warped wart in bastion chests. Until IG31-13 also the loot way to Enderite (End city
 * treasure); that moved to Simple Ores, which still honours {@code SimpleLibApi.materialLootByPartner()} (SimpleBuilding
 * feeds the same loot with its own switches). Loaders call {@link #apply} from their loot table event.
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

    /**
     * IG31-13: the Enderite pools left with Enderite (Simple Ores, {@code OresLoot}); only the warped wart stays. The flag
     * {@link #byPartner} is still read by Simple Ores.
     */
    public static void apply(ResourceKey<LootTable> key, Consumer<LootPool.Builder> add, HolderGetter.Provider registries) {
        applyWarpedWart(key, add);
    }
}
