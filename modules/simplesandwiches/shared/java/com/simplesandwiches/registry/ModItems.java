package com.simplesandwiches.registry;

import com.simplesandwiches.Sandwiches;
import com.simplesandwiches.item.KnifeItem;
import com.simplesandwiches.sandwich.SandwichContents;
import com.simplesandwiches.sandwich.SandwichItem;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.block.Block;

public final class ModItems {
    public static final int SANDWICH_STACK = 16;
    /** Owner decision F11: a cake slice is one Vanilla cake bite ({@code FoodData.eat(2, 0.1F)} = 2 / 0.4). */
    public static final FoodProperties CAKE_SLICE_FOOD = new FoodProperties(2, 0.4F, false);
    /** Same count as a cake has bites ({@code CakeBlock.MAX_BITES + 1}). */
    public static final int CAKE_SLICE_STACK = 7;
    public static final FoodProperties CHEESE_SLICE_FOOD = new FoodProperties(2, 1.2F, false);

    public static Item KNIFE, SANDWICH, CHEESE_SLICE, BUTTER_SLICE, CAKE_SLICE, CHEESE_BLOCK, BUTTER_BLOCK;
    public static final Map<String, Item> CUTTING_BOARDS = new LinkedHashMap<>();

    public static void register() {
        com.simplesandwiches.guide.SandwichGuide.register();
        KNIFE = register("knife", p -> new KnifeItem(KnifeItem.properties(p)));
        SANDWICH = register("sandwich", p -> new SandwichItem(p.stacksTo(SANDWICH_STACK)
                .component(ModComponents.SANDWICH_CONTENTS, SandwichContents.EMPTY)));
        CHEESE_SLICE = register("cheese_slice", p -> new Item(p.food(CHEESE_SLICE_FOOD)));
        BUTTER_SLICE = register("butter_slice", Item::new);
        // Eaten in one go, like a cake bite.
        CAKE_SLICE = register("cake_slice", p -> new Item(p.stacksTo(CAKE_SLICE_STACK)
                .food(CAKE_SLICE_FOOD, Consumable.builder().consumeSeconds(0.0F).build())));
        for (var e : ModBlocks.CUTTING_BOARDS.entrySet()) {
            CUTTING_BOARDS.put(e.getKey(), block(e.getValue()));
        }
        CHEESE_BLOCK = block(ModBlocks.CHEESE_BLOCK);
        BUTTER_BLOCK = block(ModBlocks.BUTTER_BLOCK);
    }

    private static Item block(Block block) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BuiltInRegistries.BLOCK.getKey(block));
        return Registry.register(BuiltInRegistries.ITEM, key, new BlockItem(block, new Item.Properties().setId(key).useBlockDescriptionPrefix()));
    }

    private static Item register(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Sandwiches.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
    }

    private ModItems() {}
}
