package com.simpleores;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Everything Simple Ores registers: silver (three ores, raw silver, ingot, nugget, blocks), the two rose gold alloys (ingot,
 * block, pressing each), their tools and armor, and the guide book. Loaders call {@link #blocks}/{@link #items} in their
 * matching registry event (Fabric: {@link #registerAll}). Block properties are copies of the matching gold blocks.
 */
public final class OresRegistry {
    /** The two alloys: path prefix, tool material, armor material, the tool numbers of the concept. */
    public enum Alloy {
        ROSE_BRONZE("rose_bronze", OresMaterials.ROSE_BRONZE_TOOLS, OresMaterials.ROSE_BRONZE_ARMOR, -3.1F, -2.0F, -1.0F),
        BLOSSOM_GOLD("blossom_gold", OresMaterials.BLOSSOM_GOLD_TOOLS, OresMaterials.BLOSSOM_GOLD_ARMOR, -3.0F, 0.0F, -3.0F);

        public final String id;
        public final ToolMaterial tools;
        public final ArmorMaterial armor;
        final float axeSpeed, hoeDamage, hoeSpeed;

        Alloy(String id, ToolMaterial tools, ArmorMaterial armor, float axeSpeed, float hoeDamage, float hoeSpeed) {
            this.id = id;
            this.tools = tools;
            this.armor = armor;
            this.axeSpeed = axeSpeed;
            this.hoeDamage = hoeDamage;
            this.hoeSpeed = hoeSpeed;
        }
    }

    /** Block path -> block and item path -> item, both in creative-tab order. */
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();

    public static final Block SILVER_ORE = block("silver_ore", Blocks.GOLD_ORE, s -> new DropExperienceBlock(ConstantInt.of(0), s));
    public static final Block DEEPSLATE_SILVER_ORE = block("deepslate_silver_ore", Blocks.DEEPSLATE_GOLD_ORE, s -> new DropExperienceBlock(ConstantInt.of(0), s));
    /** Like the Nether gold ore: nuggets (2 to 6), a little experience. */
    public static final Block END_SILVER_ORE = block("end_silver_ore", Blocks.GOLD_ORE, s -> new DropExperienceBlock(UniformInt.of(0, 1), s));
    public static final Block RAW_SILVER_BLOCK = block("raw_silver_block", Blocks.RAW_GOLD_BLOCK, Block::new);
    public static final Block SILVER_BLOCK = block("silver_block", Blocks.GOLD_BLOCK, Block::new);
    public static final Block ROSE_BRONZE_BLOCK = block("rose_bronze_block", Blocks.GOLD_BLOCK, Block::new);
    public static final Block BLOSSOM_GOLD_BLOCK = block("blossom_gold_block", Blocks.GOLD_BLOCK, Block::new);

    static {
        for (String path : List.copyOf(BLOCKS.keySet())) {
            ResourceKey<Item> key = itemKey(path);
            ITEMS.put(path, new BlockItem(BLOCKS.get(path), new Item.Properties().useBlockDescriptionPrefix().setId(key)));
        }
        plain("raw_silver");
        plain("silver_ingot");
        plain("silver_nugget");
        for (Alloy alloy : Alloy.values()) {
            plain(alloy.id + "_ingot");
            plain(alloy.id + "_pressling");
        }
        for (Alloy alloy : Alloy.values()) {
            item(alloy.id + "_sword", p -> p.sword(alloy.tools, 3.0F, -2.4F));
            item(alloy.id + "_pickaxe", p -> p.pickaxe(alloy.tools, 1.0F, -2.8F));
            item(alloy.id + "_axe", p -> p.axe(alloy.tools, 6.0F, alloy.axeSpeed));
            item(alloy.id + "_shovel", p -> p.shovel(alloy.tools, 1.5F, -3.0F));
            item(alloy.id + "_hoe", p -> p.hoe(alloy.tools, alloy.hoeDamage, alloy.hoeSpeed));
            item(alloy.id + "_helmet", p -> p.humanoidArmor(alloy.armor, ArmorType.HELMET));
            item(alloy.id + "_chestplate", p -> p.humanoidArmor(alloy.armor, ArmorType.CHESTPLATE));
            item(alloy.id + "_leggings", p -> p.humanoidArmor(alloy.armor, ArmorType.LEGGINGS));
            item(alloy.id + "_boots", p -> p.humanoidArmor(alloy.armor, ArmorType.BOOTS));
        }
    }

    private static Block block(String path, Block base, Function<BlockBehaviour.Properties, Block> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleOres.id(path));
        Block block = factory.apply(BlockBehaviour.Properties.ofFullCopy(base).setId(key));
        BLOCKS.put(path, block);
        return block;
    }

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, SimpleOres.id(path));
    }

    private static void plain(String path) {
        item(path, p -> p);
    }

    private static void item(String path, Function<Item.Properties, Item.Properties> properties) {
        ITEMS.put(path, new Item(properties.apply(new Item.Properties()).setId(itemKey(path))));
    }

    public static Item item(String path) {
        return ITEMS.get(path);
    }

    /** Ids ("simpleores:path") of every item incl. the guide, for the test centre station. */
    public static List<String> itemIds() {
        List<String> out = new ArrayList<>();
        ITEMS.keySet().forEach(path -> out.add(SimpleOres.MOD_ID + ":" + path));
        out.add(SimpleOres.MOD_ID + ":guide_book");
        return out;
    }

    public static void blocks(BiConsumer<ResourceKey<Block>, Block> sink) {
        BLOCKS.forEach((path, block) -> sink.accept(ResourceKey.create(Registries.BLOCK, SimpleOres.id(path)), block));
    }

    public static void items(BiConsumer<ResourceKey<Item>, Item> sink) {
        ITEMS.forEach((path, item) -> sink.accept(itemKey(path), item));
        com.simpleores.guide.OresGuide.register();
    }

    /** Fabric: registries are open during init. */
    public static void registerAll() {
        blocks((k, v) -> Registry.register(BuiltInRegistries.BLOCK, k, v));
        items((k, v) -> Registry.register(BuiltInRegistries.ITEM, k, v));
    }

    /** Creative stacks of the paths, empty while the module is switched off. */
    public static List<ItemStack> stacks(String... paths) {
        List<ItemStack> out = new ArrayList<>();
        if (!SimpleOres.on()) return out;
        for (String path : paths) out.add(new ItemStack(ITEMS.get(path)));
        return out;
    }

    public static final String[] NATURAL = {"silver_ore", "deepslate_silver_ore", "end_silver_ore", "raw_silver_block"};
    public static final String[] BUILDING = {"silver_block", "rose_bronze_block", "blossom_gold_block"};
    public static final String[] INGREDIENTS = {"raw_silver", "silver_nugget", "silver_ingot", "rose_bronze_pressling", "rose_bronze_ingot",
            "blossom_gold_pressling", "blossom_gold_ingot"};
    public static final String[] TOOLS = {"rose_bronze_shovel", "rose_bronze_pickaxe", "rose_bronze_axe", "rose_bronze_hoe",
            "blossom_gold_shovel", "blossom_gold_pickaxe", "blossom_gold_axe", "blossom_gold_hoe"};
    public static final String[] COMBAT = {"rose_bronze_sword", "rose_bronze_helmet", "rose_bronze_chestplate", "rose_bronze_leggings",
            "rose_bronze_boots", "blossom_gold_sword", "blossom_gold_helmet", "blossom_gold_chestplate", "blossom_gold_leggings", "blossom_gold_boots"};

    private OresRegistry() {}
}
