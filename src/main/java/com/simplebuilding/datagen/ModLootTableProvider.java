package com.simplebuilding.datagen;

import com.simplebuilding.version.LootNumbers;

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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import java.util.List;
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
        if (com.simplebuilding.version.McVersion.SILENT_DANDELION) {
            dropSelf(ModBlocks.SILENT_DANDELION);
            dropPottedContents(ModBlocks.POTTED_SILENT_DANDELION);
        }
        if (com.simplebuilding.version.McVersion.END_SYSTEMS) {
            dropSelf(ModBlocks.ASTRAL_PISTON);
            dropSelf(ModBlocks.NIHIL_PISTON);
        }
        if (com.simplebuilding.version.McVersion.END_RAILS) {
            dropSelf(ModBlocks.ASTRAL_RAIL);
            dropSelf(ModBlocks.NIHIL_RAIL);
        }
        // Aus Simple Tweaks: jede Platte droppt sich selbst (wie dort).
        com.simplebuilding.tweaks.block.TweaksBlocks.all().forEach(this::dropSelf);
        // Mob-Koepfe: der Wandkopf teilt die Tabelle des stehenden (wie Vanillas wallVariant).
        for (com.simplebuilding.tweaks.block.BlazeHeadType type : com.simplebuilding.tweaks.block.BlazeHeadType.values()) {
            dropSelf(com.simplebuilding.tweaks.block.TweaksBlocks.head(type));
        }

        // Definiert, dass diese Blöcke sich selbst droppen, wenn sie abgebaut werden
        dropSelf(ModBlocks.CONSTRUCTION_LIGHT);
        if (ModBlocks.IRON_ROD != null) dropSelf(ModBlocks.IRON_ROD);
        if (com.simplebuilding.crucible.CrucibleCompat.enderiteCrucible() != null) {
            // Crucible P5: der Inhalt droppt ueber die Block-Entity (SimpleLib), der Block selbst hier.
            dropSelf(com.simplebuilding.crucible.CrucibleCompat.enderiteCrucible());
            dropSelf(com.simplebuilding.crucible.CrucibleCompat.enderiteBarrel());
        }
        if (ModBlocks.AUTO_SMITHER != null) dropSelf(ModBlocks.AUTO_SMITHER);
        if (ModBlocks.AUTONOMOUS_CRAFTER != null) dropSelf(ModBlocks.AUTONOMOUS_CRAFTER);
        if (ModBlocks.ASTRAL_ENCHANTING_TABLE != null) {
            // Astral-Verzauberung (Queue N27): der Tisch gibt den Vanilla-Tisch und das eingesetzte Enderit-Nugget zurueck
            // (gelagerter Lapis/Lohenstaub faellt als Inhalt heraus), Lohen-Regale drei Lohenbuecher wie Vanilla-Regale.
            add(ModBlocks.ASTRAL_ENCHANTING_TABLE, LootTable.lootTable()
                    .withPool(applyExplosionCondition(Items.ENCHANTING_TABLE, LootPool.lootPool().setRolls(LootNumbers.exactly(1))
                            .add(LootItem.lootTableItem(Items.ENCHANTING_TABLE))))
                    .withPool(applyExplosionCondition(ModItems.ENDERITE_NUGGET, LootPool.lootPool().setRolls(LootNumbers.exactly(1))
                            .add(LootItem.lootTableItem(ModItems.ENDERITE_NUGGET)))));
            for (Block planks : ModBlocks.BLAZEWOOD_PLANKS) dropSelf(planks);
            for (Block shelf : ModBlocks.BLAZEWOOD_BOOKSHELVES) {
                add(shelf, createSingleItemTableWithSilkTouch(shelf, ModItems.BLAZE_BOOK, LootNumbers.exactly(3)));
            }
            dropSelf(ModBlocks.BLAZING_OBSIDIAN);
        }
        if (ModBlocks.STORAGE_CRAFTING_TABLE != null) dropSelf(ModBlocks.STORAGE_CRAFTING_TABLE);
        if (ModBlocks.JUKEBOX_AMPLIFIER != null) dropSelf(ModBlocks.JUKEBOX_AMPLIFIER);
        if (ModBlocks.NOTE_AMPLIFIER != null) dropSelf(ModBlocks.NOTE_AMPLIFIER);
        if (ModBlocks.GOLD_ROD != null) dropSelf(ModBlocks.GOLD_ROD);
        if (ModBlocks.NETHERITE_ROD != null) dropSelf(ModBlocks.NETHERITE_ROD);
        if (ModBlocks.ENDERITE_ROD != null) dropSelf(ModBlocks.ENDERITE_ROD);
        // Haengematten wie Betten: nur das Kopfteil des Tuchs gibt das Item (die anderen Teile fallen mit).
        for (net.minecraft.world.level.block.Block hammock : ModBlocks.HAMMOCKS) {
            add(hammock, createSinglePropConditionTable(hammock, com.simplebuilding.blocks.custom.HammockBlock.PART,
                    net.minecraft.world.level.block.state.properties.BedPart.HEAD));
        }
        dropSelf(ModBlocks.CRACKED_DIAMOND_BLOCK);

        dropSelf(ModBlocks.REINFORCED_HOPPER);
        dropSelf(ModBlocks.NETHERITE_HOPPER);
        dropSelf(ModBlocks.ENDERITE_HOPPER);

        // Truhen wie Vanillas Truhe: sich selbst, mit dem Namen aus dem Amboss (der Inhalt faellt heraus).
        for (Block chest : ModBlocks.tieredChests()) {
            add(chest, createNameableBlockEntityTable(chest));
        }
        for (Block chest : ModBlocks.trappedCopperChests()) {
            add(chest, createNameableBlockEntityTable(chest));
        }

        // Gestufte Shulkerkisten wie Vanillas Shulkerkiste: sich selbst mit Inhalt, Name und Schloss -
        // dazu die Farbe und die echten Anzahlen der Plaetze ueber 99. Ohne survives_explosion.
        add(ModBlocks.REINFORCED_SHULKER_BOX, tieredShulkerBoxDrop(ModBlocks.REINFORCED_SHULKER_BOX));
        add(ModBlocks.NETHERITE_SHULKER_BOX, tieredShulkerBoxDrop(ModBlocks.NETHERITE_SHULKER_BOX));
        add(ModBlocks.ENDERITE_SHULKER_BOX, tieredShulkerBoxDrop(ModBlocks.ENDERITE_SHULKER_BOX));

        dropSelf(ModBlocks.REINFORCED_PISTON);
        dropSelf(ModBlocks.REINFORCED_STICKY_PISTON);
        dropSelf(ModBlocks.NETHERITE_PISTON);
        dropSelf(ModBlocks.ENDERITE_PISTON);

        dropSelf(ModBlocks.REINFORCED_BLAST_FURNACE);
        dropSelf(ModBlocks.NETHERITE_BLAST_FURNACE);
        dropSelf(ModBlocks.ENDERITE_BLAST_FURNACE);

        dropSelf(ModBlocks.REINFORCED_FURNACE);
        dropSelf(ModBlocks.NETHERITE_FURNACE);
        dropSelf(ModBlocks.ENDERITE_FURNACE);

        dropSelf(ModBlocks.REINFORCED_SMOKER);
        dropSelf(ModBlocks.NETHERITE_SMOKER);
        dropSelf(ModBlocks.ENDERITE_SMOKER);

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
        dropSelf(ModBlocks.NETHER_BRICK_QUARTZ_CHECKER);
        dropSelf(ModBlocks.RED_NETHER_BRICK_QUARTZ_CHECKER);
        dropSelf(ModBlocks.NIHILITH_QUARTZ_CHECKER);
        dropSelf(ModBlocks.ASTRALIT_QUARTZ_CHECKER);
        dropSelf(ModBlocks.ENDER_QUARTZ_CHECKER);
        dropSelf(ModBlocks.POLISHED_ASTRALIT_CHECKER);
        dropSelf(ModBlocks.POLISHED_NIHILITH_CHECKER);
        dropSelf(ModBlocks.POLISHED_ENDER_QUARTZ_CHECKER);
        // Treppen und Stufen der Schachbretter (Schach); Achtel und Figuren droppen ohne Loot-Tabelle.
        for (ModBlocks.CheckerShapes shapes : ModBlocks.CHECKER_SHAPES) {
            dropSelf(shapes.stairs());
            add(shapes.slab(), createSlabItemTable(shapes.slab()));
        }

        // Holzwerk: alles droppt sich selbst, Schnitzholz mit seinem Motiv (copy_state), Kisten zusaetzlich ihren Inhalt.
        for (com.simplebuilding.woodwork.WoodBlocks.Family family : ModBlocks.WOOD_FAMILIES) {
            for (net.minecraft.world.level.block.Block block : family.blocks()) {
                if (block == family.carved()) {
                    add(block, net.minecraft.world.level.storage.loot.LootTable.lootTable().withPool(applyExplosionCondition(block,
                            net.minecraft.world.level.storage.loot.LootPool.lootPool()
                                    .setRolls(com.simplebuilding.version.LootNumbers.exactly(1))
                                    .add(net.minecraft.world.level.storage.loot.entries.LootItem.lootTableItem(block)
                                            .apply(net.minecraft.world.level.storage.loot.functions.CopyBlockState.copyState(block)
                                                    .copy(com.simplebuilding.woodwork.CarvedLogBlock.MOTIF))))));
                } else {
                    dropSelf(block);
                }
            }
        }

        dropSelf(ModBlocks.ASTRAL_PURPUR_BLOCK);
        dropSelf(ModBlocks.NIHIL_PURPUR_BLOCK);
        dropSelf(ModBlocks.ASTRAL_END_STONE);
        dropSelf(ModBlocks.NIHIL_END_STONE);

        // End-Paletten (Astralit, Nihilith, Enderquarz); Stufen droppen doppelt, wenn sie als Doppelstufe stehen
        for (ModBlocks.EndPalette palette : ModBlocks.END_PALETTES) {
            for (Block block : palette.blocks()) {
                if (palette.slabs().contains(block)) {
                    add(block, createSlabItemTable(block));
                } else {
                    dropSelf(block);
                }
            }
        }
        // Alternativbloecke (2026-10-03) droppen sich selbst wie ihr Grundblock.
        for (ModBlocks.EndAlternates alternates : ModBlocks.END_ALTERNATES) {
            alternates.alternates().forEach(this::dropSelf);
        }

        if (com.simplebuilding.version.McVersion.NATURE_VARIANTS) {
            // Naturvarianten (N24/N25): Stufen wie Stufen (doppelt = 2), Eis wie Eis (nur mit Behutsamkeit), der Rest sich selbst.
            // Die Gras-Stufe (Behutsamkeit: sie selbst, sonst die Erd-Stufe) steht handgeschrieben unter mc26_3/overlay.
            for (Block slab : List.of(ModBlocks.DIRT_SLAB, ModBlocks.SAND_SLAB, ModBlocks.GRAVEL_SLAB)) {
                add(slab, createSlabItemTable(slab));
            }
            for (Block ice : List.of(ModBlocks.CRACKED_ICE, ModBlocks.CHISELED_PACKED_ICE, ModBlocks.CHISELED_BLUE_ICE)) {
                dropWhenSilkTouch(ice);
            }
            for (Block block : List.of(ModBlocks.NAUTILUS_SHELL_BLOCK, ModBlocks.SCARLET_FROGLIGHT, ModBlocks.AQUA_FROGLIGHT, ModBlocks.AZURE_FROGLIGHT)) {
                dropSelf(block);
            }
        }
        dropSelf(ModBlocks.SUSPENDED_SAND);
        dropSelf(ModBlocks.SUSPENDED_GRAVEL);
        dropSelf(ModBlocks.LEVITATING_SAND);
        dropSelf(ModBlocks.LEVITATING_GRAVEL);

        // Rucksaecke: immer das Rucksack-Item mit allen Komponenten des Blocks (Inhalt, Name,
        // Verzauberungen). Bewusst ohne survives_explosion und ohne Werkzeugbedingung.
        add(ModBlocks.BACKPACK, backpackDrop(ModBlocks.BACKPACK));
        add(ModBlocks.REINFORCED_BACKPACK, backpackDrop(ModBlocks.REINFORCED_BACKPACK));
        add(ModBlocks.NETHERITE_BACKPACK, backpackDrop(ModBlocks.NETHERITE_BACKPACK));
        add(ModBlocks.ENDERITE_BACKPACK, backpackDrop(ModBlocks.ENDERITE_BACKPACK));
    }

    private static LootTable.Builder backpackDrop(Block block) {
        return LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(LootNumbers.exactly(1))
                .add(LootItem.lootTableItem(block)
                        .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY))));
    }

    private static LootTable.Builder tieredShulkerBoxDrop(Block block) {
        return LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(LootNumbers.exactly(1))
                .add(LootItem.lootTableItem(block)
                        .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                                .include(net.minecraft.core.component.DataComponents.CUSTOM_NAME)
                                .include(net.minecraft.core.component.DataComponents.CONTAINER)
                                .include(net.minecraft.core.component.DataComponents.LOCK)
                                .include(net.minecraft.core.component.DataComponents.CONTAINER_LOOT)
                                .include(net.minecraft.core.component.DataComponents.BASE_COLOR)
                                .include(com.simplebuilding.component.ModDataComponentTypes.CONTAINER_COUNTS))));
    }

    public static void modifyLootTables() {
        net.fabricmc.fabric.api.loot.v3.LootTableEvents.MODIFY.register((key, tableBuilder, source, registry) ->
                com.simplebuilding.loot.LootInjection.apply(key, new ModLootTableModifications.Editor() {
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
