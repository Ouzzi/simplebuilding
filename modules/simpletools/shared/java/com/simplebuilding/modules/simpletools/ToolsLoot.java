package com.simplebuilding.modules.simpletools;

import com.simplebuilding.modules.simpletools.soulbound.Soulbound;
import java.util.function.Consumer;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/** Soulbound book, very rare: one extra pool in the End City treasure ({@code soulboundEndCityChance}, 2 %). */
public final class ToolsLoot {
    public static final String END_CITY = "minecraft:chests/end_city_treasure";

    private ToolsLoot() {}

    public static void apply(ResourceKey<LootTable> key, Consumer<LootPool.Builder> add, HolderGetter.Provider registries) {
        if (!END_CITY.equals(key.identifier().toString()) || !SimpleTools.config().enableSoulbound) return;
        double chance = SimpleTools.config().soulboundEndCityChance;
        if (chance <= 0) return;
        var soulbound = registries.lookupOrThrow(Registries.ENCHANTMENT).get(Soulbound.KEY);
        if (soulbound.isEmpty()) return;
        add.accept(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)).when(LootItemRandomChanceCondition.randomChance((float) chance))
                .add(LootItem.lootTableItem(Items.BOOK).apply(EnchantRandomlyFunction.randomEnchantment().withEnchantment(soulbound.get()))));
    }
}
