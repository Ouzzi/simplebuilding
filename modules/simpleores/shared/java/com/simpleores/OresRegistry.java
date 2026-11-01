package com.simpleores;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import com.simpleores.block.SilverMirrorBlock;
import com.simpleores.block.LeadPlateBlock;
import com.simpleores.entity.SilverArrow;
import com.simpleores.item.SilverArrowItem;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.Rarity;
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
 *
 * <p>IG31-13 (31.10.): the Enderite materials (raw fragment, layered raw scrap, scrap, nugget, ingot, block, upgrade template;
 * formerly SimpleLib's), Dimensional Scrap of the three dimensions, Sage Ore and Deepslate Sage Ore with their drop the Sage
 * Orb (formerly SimpleBuilding's) live here; their old ids move ({@code SimpleOres#registerIdMoves}, SimpleBuilding's
 * {@code IdMovesHook}). Same block properties and numbers as before.
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
    /** Owner ORES31-BEXTRA: a carpet-like plate that lies on any side of its cell; half the muffling of a lead block. */
    public static final Block LEAD_PLATE = block("lead_plate", Blocks.IRON_BLOCK,
            s -> new LeadPlateBlock(s.strength(LEAD_HARDNESS, LEAD_RESISTANCE).sound(LEAD_SOUND).noOcclusion()));
    /** Wall decoration of silver and glass: thin, no mechanic. */
    public static final Block SILVER_MIRROR = block("silver_mirror", Blocks.GLASS,
            s -> new SilverMirrorBlock(s.strength(0.5F).sound(SoundType.METAL)));

    // IG31-13: moved materials. Enderite block like the netherite block (it was built from the same numbers).
    public static final Block ENDERITE_BLOCK = block("enderite_block", Blocks.NETHERITE_BLOCK, Block::new);
    /** Sage Ore (owner 2026-10-01): rare like diamond, drops only experience (3-7) and rarely a sage orb (loot table); iron pickaxe. */
    public static final Block SAGE_ORE = block("sage_ore", Blocks.DIAMOND_ORE, s -> new DropExperienceBlock(UniformInt.of(3, 7), s));
    public static final Block DEEPSLATE_SAGE_ORE = block("deepslate_sage_ore", Blocks.DEEPSLATE_DIAMOND_ORE, s -> new DropExperienceBlock(UniformInt.of(3, 7), s));
    public static final Block DIMENSIONAL_SCRAP = scrap("dimensional_scrap", Blocks.DEEPSLATE);
    public static final Block NETHER_DIMENSIONAL_SCRAP = scrap("nether_dimensional_scrap", Blocks.ANCIENT_DEBRIS);
    public static final Block END_DIMENSIONAL_SCRAP = scrap("end_dimensional_scrap", Blocks.END_STONE);
    /** Block items that survive fire and lava (Enderite, scrap). */
    private static final java.util.Set<String> FIRE_RESISTANT = java.util.Set.of("enderite_block", "dimensional_scrap", "nether_dimensional_scrap",
            "end_dimensional_scrap", "enderite_nugget", "enderite_scrap", "enderite_ingot");

    public static final ResourceKey<EntityType<?>> SILVER_ARROW_KEY = ResourceKey.create(Registries.ENTITY_TYPE, SimpleOres.id("silver_arrow"));
    /** Like Vanilla's arrow entity: 0.5 x 0.5, tracked from 4 chunks, updated every 20 ticks. */
    public static final EntityType<SilverArrow> SILVER_ARROW_ENTITY = EntityType.Builder.<SilverArrow>of(SilverArrow::new, MobCategory.MISC)
            .noLootTable().sized(0.5F, 0.5F).eyeHeight(0.13F).clientTrackingRange(4).updateInterval(20).build(SILVER_ARROW_KEY);

    static {
        for (String path : List.copyOf(BLOCKS.keySet())) {
            ResourceKey<Item> key = itemKey(path);
            Item.Properties properties = new Item.Properties().useBlockDescriptionPrefix().setId(key);
            ITEMS.put(path, new BlockItem(BLOCKS.get(path), FIRE_RESISTANT.contains(path) ? properties.fireResistant() : properties));
        }
        plain("raw_silver");
        plain("silver_ingot");
        plain("silver_nugget");
        plain("raw_lead");
        plain("lead_ingot");
        plain("lead_nugget");
        ITEMS.put("silver_arrow", new SilverArrowItem(new Item.Properties().setId(itemKey("silver_arrow"))));
        // IG31-13: the Enderite chain (SimpleBuilding adds the armor trim material to the ingot), the template, the sage orb.
        for (String path : List.of("raw_enderite", "layered_raw_enderite", "enderite_scrap", "enderite_nugget", "enderite_ingot")) {
            item(path, p -> FIRE_RESISTANT.contains(path) ? p.fireResistant() : p);
        }
        ITEMS.put("enderite_upgrade_template", upgradeTemplate(new Item.Properties().setId(itemKey("enderite_upgrade_template"))));
        ITEMS.put("sage_orb", new com.simpleores.item.SageOrbItem(new Item.Properties().stacksTo(16).setId(itemKey("sage_orb"))));
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
        // IG31-41: the rose gold apples between the golden and the Netherite apple (numbers: OresFood).
        item("rose_bronze_apple", p -> p.food(OresFood.ROSE_BRONZE_APPLE_FOOD, OresFood.ROSE_BRONZE_APPLE));
        item("enchanted_rose_bronze_apple", p -> p.food(OresFood.ROSE_BRONZE_APPLE_FOOD, OresFood.ENCHANTED_ROSE_BRONZE_APPLE)
                .rarity(Rarity.RARE).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
        item("blossom_gold_apple", p -> p.food(OresFood.BLOSSOM_GOLD_APPLE_FOOD, OresFood.BLOSSOM_GOLD_APPLE));
        item("enchanted_blossom_gold_apple", p -> p.food(OresFood.BLOSSOM_GOLD_APPLE_FOOD, OresFood.ENCHANTED_BLOSSOM_GOLD_APPLE)
                .rarity(Rarity.RARE).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    }

    private static Block block(String path, Block base, Function<BlockBehaviour.Properties, Block> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleOres.id(path));
        Block block = factory.apply(BlockBehaviour.Properties.ofFullCopy(base).setId(key));
        BLOCKS.put(path, block);
        return block;
    }

    /** Dimensional Scrap of one dimension: looks like {@code look}, only Enderite tools break it, never moves (piston). */
    private static Block scrap(String path, Block look) {
        return block(path, look, s -> new com.simpleores.block.DimensionalScrapBlock(s.strength(com.simpleores.block.DimensionalScrapBlock.HARDNESS, 3600000.0F)
                .requiresCorrectToolForDrops().sound(SoundType.ANCIENT_DEBRIS).pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE)));
    }

    /** The Enderite upgrade (smithing template of the Netherite -> Enderite upgrades), as SimpleLib had it. */
    private static net.minecraft.world.item.SmithingTemplateItem upgradeTemplate(Item.Properties properties) {
        String key = "item." + SimpleOres.MOD_ID + ".enderite_upgrade_template";
        return new net.minecraft.world.item.SmithingTemplateItem(
                net.minecraft.network.chat.Component.translatable(key + ".applies_to").withStyle(net.minecraft.ChatFormatting.BLUE),
                net.minecraft.network.chat.Component.translatable(key + ".ingredients").withStyle(net.minecraft.ChatFormatting.BLUE),
                net.minecraft.network.chat.Component.translatable(key + ".base_slot_description"),
                net.minecraft.network.chat.Component.translatable(key + ".additions_slot_description"),
                List.of(net.minecraft.resources.Identifier.withDefaultNamespace("container/slot/pickaxe"),
                        net.minecraft.resources.Identifier.withDefaultNamespace("container/slot/chestplate")),
                List.of(net.minecraft.resources.Identifier.withDefaultNamespace("container/slot/ingot")),
                properties.stacksTo(64).rarity(net.minecraft.world.item.Rarity.UNCOMMON));
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

    public static final String[] NATURAL = {"silver_ore", "deepslate_silver_ore", "end_silver_ore", "raw_silver_block", "lead_ore", "deepslate_lead_ore", "raw_lead_block",
            "sage_ore", "deepslate_sage_ore", "dimensional_scrap", "nether_dimensional_scrap", "end_dimensional_scrap"};
    public static final String[] BUILDING = {"silver_block", "lead_block", "lead_plate", "silver_mirror", "rose_bronze_block", "blossom_gold_block", "enderite_block"};
    public static final String[] INGREDIENTS = {"raw_silver", "silver_nugget", "silver_ingot", "raw_lead", "lead_nugget", "lead_ingot", "rose_bronze_pressling",
            "rose_bronze_ingot", "blossom_gold_pressling", "blossom_gold_ingot", "raw_enderite", "layered_raw_enderite", "enderite_scrap", "enderite_nugget",
            "enderite_ingot", "enderite_upgrade_template", "sage_orb"};
    /** IG31-13: ids that moved here (items; the blocks have block items of the same path) - from SimpleLib the Enderite seven. */
    public static final List<String> FROM_SIMPLELIB = List.of("enderite_nugget", "raw_enderite", "layered_raw_enderite", "enderite_scrap", "enderite_ingot",
            "enderite_block", "enderite_upgrade_template");
    /** Recipes SimpleLib had for them -> folder of their recipe advancement (same paths here). */
    public static final Map<String, String> RECIPES_FROM_SIMPLELIB = Map.of("enderite_nugget_from_ingot", "misc", "enderite_ingot_from_nugget", "misc",
            "layered_raw_enderite", "misc", "enderite_scrap_from_blasting_layered_raw_enderite", "misc", "enderite_ingot_from_scrap", "misc",
            "enderite_block", "decorations", "enderite_ingot", "building_blocks", "enderite_upgrade_template", "misc");
    public static final String[] TOOLS = {"rose_bronze_shovel", "rose_bronze_pickaxe", "rose_bronze_axe", "rose_bronze_hoe",
            "blossom_gold_shovel", "blossom_gold_pickaxe", "blossom_gold_axe", "blossom_gold_hoe"};
    public static final String[] COMBAT = {"rose_bronze_sword", "rose_bronze_helmet", "rose_bronze_chestplate", "rose_bronze_leggings",
            "rose_bronze_boots", "blossom_gold_sword", "blossom_gold_helmet", "blossom_gold_chestplate", "blossom_gold_leggings", "blossom_gold_boots", "silver_arrow"};

    /** IG31-41: the four rose gold apples, shown in the Food and Drinks tab behind the enchanted golden apple. */
    public static final String[] FOOD = {"rose_bronze_apple", "enchanted_rose_bronze_apple", "blossom_gold_apple", "enchanted_blossom_gold_apple"};

    private OresRegistry() {}
}
