package com.simplebuilding.items;

import com.simplebuilding.Simplebuilding;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModItemGroups {
    public static final CreativeModeTab BUILDING_ITEMS_GROUP = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "building_items"),
            FabricCreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.IRON_CHISEL))
                    .title(Component.translatable("itemgroup.simplebuilding.building_items"))
                    .displayItems((displayContext, entries) -> ModItemGroupsContent.populate(entries, displayContext.holders()))
                    .build());

    public static void registerItemGroups() {
        Simplebuilding.LOGGER.info("Registering Item Groups for {}", Simplebuilding.MOD_ID);
    }
}
