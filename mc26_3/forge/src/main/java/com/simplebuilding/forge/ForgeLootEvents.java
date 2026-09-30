package com.simplebuilding.forge;

import com.simplebuilding.loot.LootInjection;
import com.simplebuilding.loot.ModLootTableModifications;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.LootPool;

/** 26.3 loot references need the newly loaded loot registry, unavailable in LootTableLoadEvent. */
public final class ForgeLootEvents {
    private ForgeLootEvents() {}

    public static void injectLoadedTables(HolderLookup.Provider context, RegistryAccess.Frozen loaded) {
        HolderLookup.Provider lookup = HolderLookup.Provider.create(Stream.concat(context.listRegistries(), loaded.listRegistries()));
        loaded.lookupOrThrow(Registries.LOOT_TABLE).listElements().forEach(holder -> {
            var table = holder.value();
            LootInjection.apply(holder.key(), new ModLootTableModifications.Editor() {
                @Override public void addPool(LootPool.Builder pool) { table.addPool(pool.build()); }
                @Override public void addBuiltPool(LootPool pool) { table.addPool(pool); }
            }, lookup);
        });
    }
}
