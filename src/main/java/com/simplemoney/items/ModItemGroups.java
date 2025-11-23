package com.simplemoney.items;

import com.simplemoney.Simplemoney;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    public static final ItemGroup MONEY_ITEMS_GROUP = Registry.register(Registries.ITEM_GROUP,
            Identifier.of(Simplemoney.MOD_ID, "money_items"),
            FabricItemGroup.builder().icon(() -> new ItemStack(ModItems.MONEY_BILL))
                    .displayName(Text.translatable("itemgroup.simplemoney.money_items"))
                    .entries((displayContext, entries) -> {
                        entries.add(ModItems.SPECIAL_PAPER);
                        entries.add(ModItems.SPECIAL_FIBER);
                        entries.add(ModItems.BLANK_NOTE);
                        entries.add(ModItems.RAW_BILL);
                        entries.add(ModItems.MONEY_BILL);
                    }).build());


    public static void registerItemGroups() {
        Simplemoney.LOGGER.info("Registering Item Groups for " + Simplemoney.MOD_ID);
    }
}
