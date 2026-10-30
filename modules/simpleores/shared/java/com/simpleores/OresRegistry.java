package com.simpleores;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import com.simpleores.block.SilverMirrorBlock;
import com.simpleores.entity.SilverArrow;
import com.simpleores.item.SilverArrowItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Everything Simple Ores registers: silver (three ores, raw silver, ingot, nugget, blocks), lead (two ores, raw lead, ingot, nugget, blocks), the silver arrow and the silver mirror, the two rose gold alloys (ingot,
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

    /**
     * Lead (concept ORES31 section 3): quiet metal it is not - the heavy sound is the point (walking on it or mining it is loud, a lead wall
     * only keeps the Sculk and the world's noise out, see {@code Soundproofing}). Netherite hits and falls, anvil steps and placing, pitched low.
     */
    public static final SoundType LEAD_SOUND = new SoundType(1.4F, 0.6F, SoundEvents.NETHERITE_BLOCK_BREAK, SoundEvents.ANVIL_STEP,
            SoundEvents.ANVIL_PLACE, SoundEvents.NETHERITE_BLOCK_HIT, SoundEvents.NETHERITE_BLOCK_FALL);
    /** Blast resistance of lead, a little below obsidian (1200). */
    public static final float LEAD_RESISTANCE = 1000.0F;
    /** Hardness of lead (obsidian 50). */
    public static final float LEAD_HARDNESS = 25.0F;

    public static final Block LEAD_ORE = block("lead_ore", Blocks.IRON_ORE, s -> new DropExperienceBlock(ConstantInt.of(0), s));
    public static final Block DEEPSLATE_LEAD_ORE = block("deepslate_lead_ore", Blocks.DEEPSLATE_IRON_ORE, s -> new DropExperienceBlock(ConstantInt.of(0), s));
    public static final Block RAW_LEAD_BLOCK = block("raw_lead_block", Blocks.RAW_IRON_BLOCK, Block::new);
    public static final Block LEAD_BLOCK = block("lead_block", Blocks.IRON_BLOCK,
            s -> new Block(s.strength(LEAD_HARDNESS, LEAD_RESISTANCE).sound(LEAD_SOUND)));
    /** Wall decoration of silver and glass: thin, no mechanic. */
    public static final Block SILVER_MIRROR = block("silver_mirror", Blocks.GLASS,
            s -> new SilverMirrorBlock(s.strength(0.5F).sound(SoundType.METAL)));

    public static final ResourceKey<EntityType<?>> SILVER_ARROW_KEY = ResourceKey.create(Registries.ENTITY_TYPE, SimpleOres.id("silver_arrow"));
    /** Like Vanilla's arrow entity: 0.5 x 0.5, tracked from 4 chunks, updated every 20 ticks. */
    public static final EntityType<SilverArrow> SILVER_ARROW_ENTITY = EntityType.Builder.<SilverArrow>of(SilverArrow::new, MobCategory.MISC)
            .noLootTable().sized(0.5F, 0.5F).eyeHeight(0.13F).clientTrackingRange(4).updateInterval(20).build(SILVER_ARROW_KEY);

    static {
        for (String path : List.copyOf(BLOCKS.keySet())) {
            ResourceKey<Item> key = itemKey(path);
            ITEMS.put(path, new BlockItem(BLOCKS.get(path), new Item.Properties().useBlockDescriptionPrefix().setId(key)));
        }
        plain("raw_silver");
        plain("silver_ingot");
        plain("silver_nugget");
        plain("raw_lead");
        plain("lead_ingot");
        plain("lead_nugget");
        ITEMS.put("silver_arrow", new SilverArrowItem(new Item.Properties().setId(itemKey("silver_arrow"))));
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
        Registry.register(BuiltInRegistries.ENTITY_TYPE, SILVER_ARROW_KEY, SILVER_ARROW_ENTITY);
        blocks((k, v) -> Registry.register(BuiltInRegistries.BLOCK, k, v));
        items((k, v) -> Registry.register(BuiltInRegistries.ITEM, k, v));
        net.minecraft.world.level.block.DispenserBlock.registerProjectileBehavior(item("silver_arrow"));
    }

    /** Creative stacks of the paths, empty while the module is switched off. */
    public static List<ItemStack> stacks(String... paths) {
        List<ItemStack> out = new ArrayList<>();
        if (!SimpleOres.on()) return out;
        for (String path : paths) out.add(new ItemStack(ITEMS.get(path)));
        return out;
    }

    public static final String[] NATURAL = {"silver_ore", "deepslate_silver_ore", "end_silver_ore", "raw_silver_block", "lead_ore", "deepslate_lead_ore", "raw_lead_block"};
    public static final String[] BUILDING = {"silver_block", "lead_block", "silver_mirror", "rose_bronze_block", "blossom_gold_block"};
    public static final String[] INGREDIENTS = {"raw_silver", "silver_nugget", "silver_ingot", "raw_lead", "lead_nugget", "lead_ingot", "rose_bronze_pressling",
            "rose_bronze_ingot", "blossom_gold_pressling", "blossom_gold_ingot"};
    public static final String[] TOOLS = {"rose_bronze_shovel", "rose_bronze_pickaxe", "rose_bronze_axe", "rose_bronze_hoe",
            "blossom_gold_shovel", "blossom_gold_pickaxe", "blossom_gold_axe", "blossom_gold_hoe"};
    public static final String[] COMBAT = {"rose_bronze_sword", "rose_bronze_helmet", "rose_bronze_chestplate", "rose_bronze_leggings",
            "rose_bronze_boots", "blossom_gold_sword", "blossom_gold_helmet", "blossom_gold_chestplate", "blossom_gold_leggings", "blossom_gold_boots", "silver_arrow"};

    private OresRegistry() {}
}
