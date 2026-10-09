package com.simplebuilding.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.simplebuilding.data.ChiselTables;
import com.simplebuilding.data.SledgehammerUpgradeData;
import com.simplebuilding.loot.LootInjection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Writes the mod's datapack defaults out of the code that defines them:
 * <ul>
 *   <li>{@code chisel_transformations/<tier>_<table>.json} from the built-in chisel chains
 *       ({@link ChiselTables#defaultFile});</li>
 *   <li>{@code sledgehammer_upgrades/<name>.json} from the built-in upgrade table
 *       ({@link SledgehammerUpgradeData#defaultFile});</li>
 *   <li>{@code loot_table/inject/<vanilla path>.json} - the loot the mod injects into a vanilla
 *       table ({@link LootInjection}), from {@code ModLootTableModifications#apply}.</li>
 * </ul>
 * Game tests compare the loaded files with the code, so a change to the code without a new
 * {@code runDatagen} turns red instead of shipping stale defaults.
 */
public class ModDataTablesProvider implements DataProvider {

    private final PackOutput output;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public ModDataTablesProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this.output = output;
        this.registries = registries;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return this.registries.thenCompose(lookup -> {
            List<CompletableFuture<?>> writes = new ArrayList<>();
            PackOutput.PathProvider chisel = this.output.createPathProvider(PackOutput.Target.DATA_PACK, ChiselTables.DIRECTORY);
            for (String tier : ChiselTables.TIERS) {
                for (String table : ChiselTables.TABLES) {
                    JsonObject file = ChiselTables.defaultFile(tier, table);
                    if (file.getAsJsonArray("chains").isEmpty()) {
                        continue;
                    }
                    writes.add(DataProvider.saveStable(cache, file, chisel.json(id(tier + "_" + table))));
                }
            }
            PackOutput.PathProvider upgrades = this.output.createPathProvider(PackOutput.Target.DATA_PACK, SledgehammerUpgradeData.DIRECTORY);
            for (String name : SledgehammerUpgradeData.DEFAULT_FILES) {
                writes.add(DataProvider.saveStable(cache, SledgehammerUpgradeData.defaultFile(name), upgrades.json(id(name))));
            }
            PackOutput.PathProvider loot = this.output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table");
            RegistryOps<JsonElement> ops = lookup.createSerializationContext(JsonOps.INSTANCE);
            for (ResourceKey<LootTable> key : injectedTables()) {
                List<LootPool> pools = LootInjection.defaultPools(key, lookup);
                if (pools.isEmpty()) {
                    continue;
                }
                JsonObject table = new JsonObject();
                table.addProperty("type", paramSet(key));
                JsonArray array = new JsonArray();
                for (LootPool pool : pools) {
                    array.add(LootPool.CODEC.encodeStart(ops, pool).getOrThrow());
                }
                table.add("pools", array);
                writes.add(DataProvider.saveStable(cache, table, loot.json(LootInjection.injectKey(key).identifier())));
            }
            return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
        });
    }

    /** Every vanilla table, plus the charged creeper table in case it is not in the list. */
    private static List<ResourceKey<LootTable>> injectedTables() {
        List<ResourceKey<LootTable>> keys = new ArrayList<>(BuiltInLootTables.all());
        if (!keys.contains(BuiltInLootTables.CHARGED_CREEPER)) {
            keys.add(BuiltInLootTables.CHARGED_CREEPER);
        }
        // Mob tables are not in BuiltInLootTables.all(): the warden's (warden tendril, queue N24).
        keys.add(com.simplebuilding.loot.ModLootTableModifications.wardenTable());
        return keys;
    }

    /** The loot context type of the vanilla table the inject table is rolled from. */
    private static String paramSet(ResourceKey<LootTable> key) {
        String path = key.identifier().getPath();
        if (path.startsWith("gameplay/fishing")) {
            return "minecraft:fishing";
        }
        if (path.startsWith("charged_creeper") || path.startsWith("entities/")) {
            return "minecraft:entity";
        }
        return "minecraft:chest";
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("simplebuilding", path);
    }

    @Override
    public String getName() {
        return "SimpleBuilding datapack tables and loot injection";
    }
}
