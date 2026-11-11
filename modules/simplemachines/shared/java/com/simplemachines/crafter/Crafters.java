package com.simplemachines.crafter;

import com.simplemachines.SimpleMachines;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The Autonomous Crafter ("Auto Crafter") of the Simple Machines tab (MACH32-MOD stage 4b, docs/ai/PLAN-B-MACH32S4B-2026-11-10.md):
 * a Vanilla Crafter that crafts by itself into the hopper below. Until 10.11. SimpleBuilding's ({@code simplebuilding:autonomous_crafter},
 * block entity of the same id; the ids move, {@link SimpleMachines#registerIdMoves}); properties, recipe, look and numbers unchanged.
 */
public final class Crafters {
    public static final String ID = "autonomous_crafter";
    /** Its recipe advancement folder ({@code recipes/redstone/autonomous_crafter}). */
    public static final String RECIPE_FOLDER = "redstone";
    public static Block BLOCK;
    public static Item ITEM;
    public static BlockEntityType<AutonomousCrafterBlockEntity> BLOCK_ENTITY;
    public static MenuType<AutonomousCrafterMenu> MENU;

    private Crafters() {}

    /** The Crafter's properties (as SimpleBuilding registered it). */
    public static void registerBlocks() {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleMachines.id(ID));
        BLOCK = Registry.register(BuiltInRegistries.BLOCK, key, new AutonomousCrafterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTER).setId(key)));
    }

    public static void registerItems() {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleMachines.id(ID));
        ITEM = Registry.register(BuiltInRegistries.ITEM, key, new com.simplemachines.item.MachinePartItem(BLOCK, ID, new Item.Properties().setId(key)));
    }

    public static void registerBlockEntity(BlockEntityType<AutonomousCrafterBlockEntity> type) {
        BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleMachines.id(ID), type);
    }

    public static void registerMenu() {
        MENU = Registry.register(BuiltInRegistries.MENU, SimpleMachines.id(ID), new MenuType<>(AutonomousCrafterMenu::new, FeatureFlags.VANILLA_SET));
    }

    public static List<ItemStack> stacks() {
        return List.of(new ItemStack(ITEM));
    }
}
