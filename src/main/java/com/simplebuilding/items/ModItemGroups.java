package com.simplebuilding.items;

import com.simplebuilding.Simplebuilding;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    public static final ItemGroup BUILDING_ITEMS_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(Simplebuilding.MOD_ID, "building_items"),
            FabricItemGroup.builder().icon(() -> new ItemStack(ModItems.IRON_SPATULA))
                    .displayName(Text.translatable("itemgroup.simplebuilding.building_items"))
                    .entries((displayContext, entries) -> {
                        entries.add(ModItems.STONE_CHISEL);
                        entries.add(ModItems.COPPER_CHISEL);
                        entries.add(ModItems.IRON_CHISEL);
                        entries.add(ModItems.GOLD_CHISEL);
                        entries.add(ModItems.DIAMOND_CHISEL);
                        entries.add(ModItems.NETHERITE_CHISEL);

                        entries.add(ModItems.STONE_SPATULA);
                        entries.add(ModItems.COPPER_SPATULA);
                        entries.add(ModItems.IRON_SPATULA);
                        entries.add(ModItems.GOLD_SPATULA);
                        entries.add(ModItems.DIAMOND_SPATULA);
                        entries.add(ModItems.NETHERITE_SPATULA);

                        entries.add(ModItems.COPPER_BUILDING_CORE);
                        entries.add(ModItems.IRON_BUILDING_CORE);
                        entries.add(ModItems.GOLD_BUILDING_CORE);
                        entries.add(ModItems.DIAMOND_BUILDING_CORE);
                        entries.add(ModItems.NETHERITE_BUILDING_CORE);

                        entries.add(ModItems.COPPER_BUILDING_WAND);
                        entries.add(ModItems.IRON_BUILDING_WAND);
                        entries.add(ModItems.GOLD_BUILDING_WAND);
                        entries.add(ModItems.DIAMOND_BUILDING_WAND);
                        entries.add(ModItems.NETHERITE_BUILDING_WAND);

                        entries.add(ModItems.RANGEFINDER_ITEM);

                    }).build());


    public static void registerItemGroups() {
        Simplebuilding.LOGGER.info("Registering Item Groups for " + Simplebuilding.MOD_ID);
    }
}
