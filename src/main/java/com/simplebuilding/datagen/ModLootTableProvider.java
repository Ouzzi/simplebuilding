package com.simplebuilding.datagen;

import com.simplebuilding.loot.ModLootTableModifications;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import java.util.concurrent.CompletableFuture;

// List all Loot Table types:
// 1. STRONGHOLD LIBRARY CHEST: (RANGE, QUIVER, MASTER_BUILDER, BRIDGE)
// 2. END CITY: (RANGE, QUIVER, MASTER_BUILDER, OVERRIDES, BRIDGE, DOUBLE_JUMP), (DIAMOND_CHISEL_ENCHANTED, DIAMOND_SPATULA_ENCHANTED,DIAMOND_BUILDING_WAND_ENCHANTED, DIAMOND_SLEDGEHAMMER_ENCHANTED, diamond_core, ENCHANTED_ENDERITE_APPLE)
// 4. ANCIENT CITY: (DEEP POCKETS, RADIUS), (OCTANT_ENCHANTED, DIAMOND_SLEDGEHAMMER, QUIVER_ENCHANTED, ENCHANTED_NETHERITE_APPLE)
// 5. BASTION: (FUNNEL, BREAK THROUGH), (GOLD_SLEDGEHAMMER, gold_core, NETHERITE_CORE, ENCHANTED_NETHERITE_APPLE)
// 6. NETHER BRIDGE: (FUNNEL, BREAK THROUGH, STRIP_MINER), (gold_core, OCTANT_ENCHANTED)
// 7. PILLAGER OUTPOST: (COLOR PALETTE, SURFACE PLACE, LINE PLACE), (OCTANT, QUIVER)
// 8. WOODLAND MANSION: (COLOR PALETTE, SURFACE PLACE, LINE PLACE, VEIN_MINER IV), (IRON_BUILDING_WAND, iron_core, QUIVER)
// 9. BURIED TREASURE: (CONSTRUCTORS TOUCH, FAST CHISEL), (GOLD_CHISEL, DIAMOND_SPATULA)
// 10. SIMPLE DUNGEON: (FAST CHISEL, FUNNEL, BREAK THROUGH, VEIN_MINER II), (REINFORCED_BUNDLE)
// 11. SHIPWRECK TREASURE: (FAST CHISEL), (REINFORCED_BUNDLE)
// 12. IGLOO: (CONSTRUCTORS TOUCH, FAST CHISEL), (DIAMOND_CHISEL, IRON_SPATULA)
// 13. ABANDONED MINESHAFT: (FAST CHISEL, STRIP_MINER I, VEIN_MINER III), (REINFORCED_BUNDLE_ENCHANTED)
// 14. VAULT: (CONSTRUCTORS TOUCH, FAST_CHISEL, DOUBLE_JUMP I), (diamond_core, ENCHANTED_NETHERITE_APPLE)

public class ModLootTableProvider extends FabricBlockLootSubProvider {
    public ModLootTableProvider(FabricPackOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {
        // Definiert, dass diese Blöcke sich selbst droppen, wenn sie abgebaut werden
        dropSelf(ModBlocks.CONSTRUCTION_LIGHT);
        dropSelf(ModBlocks.CRACKED_DIAMOND_BLOCK);

        dropSelf(ModBlocks.REINFORCED_HOPPER);
        dropSelf(ModBlocks.NETHERITE_HOPPER);

        dropSelf(ModBlocks.REINFORCED_PISTON);
        dropSelf(ModBlocks.NETHERITE_PISTON);

        dropSelf(ModBlocks.REINFORCED_BLAST_FURNACE);
        dropSelf(ModBlocks.NETHERITE_BLAST_FURNACE);

        dropSelf(ModBlocks.REINFORCED_FURNACE);
        dropSelf(ModBlocks.NETHERITE_FURNACE);

        dropSelf(ModBlocks.REINFORCED_SMOKER);
        dropSelf(ModBlocks.NETHERITE_SMOKER);

        // Nihilith Ore -> Droppt Shard
        add(ModBlocks.NIHILITH_ORE, createOreDrop(ModBlocks.NIHILITH_ORE, ModItems.NIHILITH_SHARD));

        // Astralit Ore -> Droppt Dust
        add(ModBlocks.ASTRALIT_ORE, createOreDrop(ModBlocks.ASTRALIT_ORE, ModItems.ASTRALIT_DUST));

        // Enderite Block -> Droppt sich selbst
        dropSelf(ModBlocks.ENDERITE_BLOCK);

        dropSelf(ModBlocks.POLISHED_END_STONE);
        dropSelf(ModBlocks.PURPUR_QUARTZ_CHECKER);
        dropSelf(ModBlocks.LAPIS_QUARTZ_CHECKER);
        dropSelf(ModBlocks.BLACKSTONE_QUARTZ_CHECKER);
        dropSelf(ModBlocks.RESIN_QUARTZ_CHECKER);

        dropSelf(ModBlocks.ASTRAL_PURPUR_BLOCK);
        dropSelf(ModBlocks.NIHIL_PURPUR_BLOCK);
        dropSelf(ModBlocks.ASTRAL_END_STONE);
        dropSelf(ModBlocks.NIHIL_END_STONE);

        dropSelf(ModBlocks.SUSPENDED_SAND);
        dropSelf(ModBlocks.SUSPENDED_GRAVEL);
        dropSelf(ModBlocks.LEVITATING_SAND);
        dropSelf(ModBlocks.LEVITATING_GRAVEL);

    }

    public static void modifyLootTables() {
        net.fabricmc.fabric.api.loot.v3.LootTableEvents.MODIFY.register((key, tableBuilder, source, registry) ->
                ModLootTableModifications.apply(key, new ModLootTableModifications.Editor() {
                    @Override
                    public void addPool(LootPool.Builder pool) {
                        tableBuilder.withPool(pool);
                    }

                    @Override
                    public void addBuiltPool(LootPool pool) {
                        tableBuilder.pool(pool);
                    }
                }, registry));
    }
}