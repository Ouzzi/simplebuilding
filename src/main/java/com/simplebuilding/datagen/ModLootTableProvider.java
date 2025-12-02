package com.simplebuilding.datagen;

import com.simplebuilding.items.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LeafEntry;
import net.minecraft.loot.function.ApplyBonusLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class ModLootTableProvider extends FabricBlockLootTableProvider {
    public ModLootTableProvider(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {
        RegistryWrapper.Impl<Enchantment> impl = this.registries.getOrThrow(RegistryKeys.ENCHANTMENT);
    }

    public static void modifyLootTables() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registry) -> {

            // TODO: add [RANGEFINDER, ENCHANTMENT BOOKS, CHISEL_TOOLS, BUILDING_WANDS, BUILDING_CORES]
            if (LootTables.END_CITY_TREASURE_CHEST.equals(key)) {
                LootPool.Builder poolBuilder = LootPool.builder()
                        .rolls(UniformLootNumberProvider.create(0, 1))
                        .conditionally(net.minecraft.loot.condition.RandomChanceLootCondition.builder(0.3f)) // 30% Chance
                        .with(ItemEntry.builder(ModItems.DIAMOND_BUILDING_WAND).weight(1))
                        .with(ItemEntry.builder(ModItems.REINFORCED_BUNDLE).weight(2))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 1.0f)));
                tableBuilder.pool(poolBuilder.build());
            }

            // 2. ANCIENT CITY / DEEP DARK (Deep Pockets, Radius)
            if (LootTables.ANCIENT_CITY_CHEST.equals(key)) {
                LootPool.Builder poolBuilder = LootPool.builder()
                        .rolls(UniformLootNumberProvider.create(0, 1))
                        .conditionally(net.minecraft.loot.condition.RandomChanceLootCondition.builder(0.2f))
                        .with(ItemEntry.builder(ModItems.NETHERITE_SLEDGEHAMMER).weight(1)) // Sehr selten
                        .with(ItemEntry.builder(ModItems.REINFORCED_BUNDLE).weight(3))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 1.0f)));
                tableBuilder.pool(poolBuilder.build());
            }

            // 3. BASTION / NETHER FORTRESS (Funnel, Break Through, Strip Miner)
            if (LootTables.BASTION_TREASURE_CHEST.equals(key) || LootTables.NETHER_BRIDGE_CHEST.equals(key)) {
                LootPool.Builder poolBuilder = LootPool.builder()
                        .rolls(UniformLootNumberProvider.create(0, 1))
                        .conditionally(net.minecraft.loot.condition.RandomChanceLootCondition.builder(0.25f))
                        .with(ItemEntry.builder(ModItems.GOLD_SLEDGEHAMMER).weight(2))
                        .with(ItemEntry.builder(ModItems.NETHERITE_BUILDING_CORE).weight(1))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 1.0f)));
                tableBuilder.pool(poolBuilder.build());
            }

            // 4. PILLAGER OUTPOST (Color Palette, Surface Place, Line Place)
            if (LootTables.PILLAGER_OUTPOST_CHEST.equals(key)) {
                LootPool.Builder poolBuilder = LootPool.builder()
                        .rolls(UniformLootNumberProvider.create(0, 1))
                        .conditionally(net.minecraft.loot.condition.RandomChanceLootCondition.builder(0.3f))
                        .with(ItemEntry.builder(ModItems.IRON_BUILDING_WAND).weight(5))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 1.0f)));
                tableBuilder.pool(poolBuilder.build());
            }

            // 5. BURIED TREASURE (Constructors Touch, Fast Chisel)
            if (LootTables.BURIED_TREASURE_CHEST.equals(key)) {
                LootPool.Builder poolBuilder = LootPool.builder()
                        .rolls(UniformLootNumberProvider.create(1, 1)) // Garantiert
                        .with(ItemEntry.builder(ModItems.GOLD_CHISEL).weight(5))
                        .with(ItemEntry.builder(ModItems.DIAMOND_SPATULA).weight(2))
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1.0f, 1.0f)));
                tableBuilder.pool(poolBuilder.build());
            }
        });
    }

    public LootTable.Builder multipleOreDrops(Block drop, Item item, float minDrops, float maxDrops) {
        RegistryWrapper.Impl<Enchantment> impl = this.registries.getOrThrow(RegistryKeys.ENCHANTMENT);
        return this.dropsWithSilkTouch(drop, this.applyExplosionDecay(drop, ((LeafEntry.Builder<?>)
                ItemEntry.builder(item).apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(minDrops, maxDrops))))
                .apply(ApplyBonusLootFunction.oreDrops(impl.getOrThrow(Enchantments.FORTUNE)))));
    }
}
