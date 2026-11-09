package com.simpleores;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import com.simpleores.block.SilverMirrorBlock;
import com.simpleores.block.LeadPlateBlock;
import com.simpleores.block.SilverSheetBlock;
import com.simpleores.item.FoodEffectsItem;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
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
 * Everything Simple Ores registers: silver (three ores, raw silver, ingot, nugget, blocks), lead (two ores, raw lead, ingot, nugget, blocks), the silver mirror (the silver arrow moved to Simple Arrows on 03.11.), the two rose gold alloys (ingot,
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
        ROSE_GOLD("rose_gold", OresMaterials.ROSE_GOLD_TOOLS, OresMaterials.ROSE_GOLD_ARMOR, -3.0F, 0.0F, -3.0F);

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
    public static final Block ROSE_GOLD_BLOCK = block("rose_gold_block", Blocks.GOLD_BLOCK, Block::new);
    /** 06.11. (BLOCKS07): storage of nine pressings each, a pure storage block (no melting; the blast furnace takes the single pressling). */
    public static final Block ROSE_BRONZE_PRESSLING_BLOCK = block("rose_bronze_pressling_block", Blocks.GOLD_BLOCK, Block::new);
    public static final Block ROSE_GOLD_PRESSLING_BLOCK = block("rose_gold_pressling_block", Blocks.GOLD_BLOCK, Block::new);

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
    /** 05.11.: the lead plate's little sister: a one pixel silver sheet over a whole side of the cell, laid like the plate. */
    public static final Block SILVER_SHEET = block("silver_sheet", Blocks.IRON_BLOCK,
            s -> new SilverSheetBlock(s.strength(3.0F, 6.0F).sound(SoundType.METAL).noOcclusion()));
    /** Wall decoration of silver and glass: thin, no mechanic. */
    public static final Block SILVER_MIRROR = block("silver_mirror", Blocks.GLASS,
            s -> new SilverMirrorBlock(s.strength(0.5F).sound(SoundType.METAL)));

    /** RG30-2 (08.11.): zinc, nickel, tin and the plain alloys brass, stainless steel, bronze (building metals, no tools). Properties are copies of iron. */
    public static final String[] METALS = {"zinc", "nickel", "tin"};
    public static final String[] MIXES = {"brass", "stainless_steel", "bronze"};
    static {
        for (String m : METALS) {
            block(m + "_ore", Blocks.IRON_ORE, s -> new DropExperienceBlock(ConstantInt.of(0), s));
            block("deepslate_" + m + "_ore", Blocks.DEEPSLATE_IRON_ORE, s -> new DropExperienceBlock(ConstantInt.of(0), s));
            block("raw_" + m + "_block", Blocks.RAW_IRON_BLOCK, Block::new);
            block(m + "_block", Blocks.IRON_BLOCK, Block::new);
        }
        for (String a : MIXES) block(a + "_block", Blocks.IRON_BLOCK, Block::new);
    }

    /**
     * N4 (b-ores13, owner 09.11.): End lead and End zinc in End stone (drop like their Overworld ores), the crust of the closed mini
     * geodes ({@link MiniGeodeFeature}), and tin-bearing gravel and sand of river and beach beds (shovel, they fall like gravel/sand,
     * drop tin nuggets).
     */
    public static final Block END_LEAD_ORE = block("end_lead_ore", Blocks.END_STONE, s -> new DropExperienceBlock(ConstantInt.of(0), s));
    public static final Block END_ZINC_ORE = block("end_zinc_ore", Blocks.END_STONE, s -> new DropExperienceBlock(ConstantInt.of(0), s));
    public static final Block GEODE_CRUST = block("geode_crust", Blocks.SMOOTH_BASALT, Block::new);
    public static final Block TIN_BEARING_GRAVEL = block("tin_bearing_gravel", Blocks.GRAVEL,
            s -> new net.minecraft.world.level.block.ColoredFallingBlock(new net.minecraft.util.ColorRGBA(-8356741), s));
    public static final Block TIN_BEARING_SAND = block("tin_bearing_sand", Blocks.SAND,
            s -> new net.minecraft.world.level.block.ColoredFallingBlock(new net.minecraft.util.ColorRGBA(14406560), s));

    // IG31-13: moved materials. Enderite block like the netherite block (it was built from the same numbers).
    public static final Block ENDERITE_BLOCK = block("enderite_block", Blocks.NETHERITE_BLOCK, Block::new);
    /** Sage Ore (owner 2026-10-01): rare like diamond, drops only experience (3-7) and rarely a sage orb (loot table); iron pickaxe. */
    public static final Block SAGE_ORE = block("sage_ore", Blocks.DIAMOND_ORE, s -> new DropExperienceBlock(UniformInt.of(3, 7), s));
    public static final Block DEEPSLATE_SAGE_ORE = block("deepslate_sage_ore", Blocks.DEEPSLATE_DIAMOND_ORE, s -> new DropExperienceBlock(UniformInt.of(3, 7), s));
    /** 06.11.: Nether gold ore with rose flecks; one per Nether gold vein at most, below the lava ocean ({@link RoseGoldVeinFeature}). Drops a rose gold pressling. */
    public static final Block NETHER_ROSE_GOLD_ORE = block("nether_rose_gold_ore", Blocks.NETHER_GOLD_ORE, s -> new DropExperienceBlock(UniformInt.of(0, 2), s));
    /** 06.11.: sage ore of the Nether (netherrack) and the End (end stone); drops sage crystal shards. */
    public static final Block NETHER_SAGE_ORE = block("nether_sage_ore", Blocks.NETHER_QUARTZ_ORE, s -> new DropExperienceBlock(UniformInt.of(2, 5), s));
    public static final Block END_SAGE_ORE = block("end_sage_ore", Blocks.DIAMOND_ORE, s -> new DropExperienceBlock(UniformInt.of(2, 5), s));
    public static final Block SAGE_CRYSTAL_BLOCK = block("sage_crystal_block", Blocks.GOLD_BLOCK, Block::new);
    /** 06.11. (BLOCKS07): nine scrap each; like ancient debris (hardness 30, resistance 1200), fire resistant. */
    public static final Block NETHERITE_SCRAP_BLOCK = block("netherite_scrap_block", Blocks.ANCIENT_DEBRIS, Block::new);
    public static final Block ENDERITE_SCRAP_BLOCK = block("enderite_scrap_block", Blocks.ANCIENT_DEBRIS, Block::new);
    /** Nine sage orbs; like the emerald block. */
    public static final Block SAGE_ORB_BLOCK = block("sage_orb_block", Blocks.EMERALD_BLOCK, Block::new);
    public static final Block DIMENSIONAL_SCRAP = scrap("dimensional_scrap", Blocks.DEEPSLATE);
    public static final Block NETHER_DIMENSIONAL_SCRAP = scrap("nether_dimensional_scrap", Blocks.ANCIENT_DEBRIS);
    public static final Block END_DIMENSIONAL_SCRAP = scrap("end_dimensional_scrap", Blocks.END_STONE);
    /**
     * IG31-58 (a-dims10, docs/ai/PLAN-A-DIMS10-2026-11-08.md): the scrap of each dimension melts (blast furnace, or a crucible at
     * extreme heat) into rift shards; nine make a block like the netherite scrap block. Four shards and one partner ingot make the
     * end material of the dimension (Overworld + Enderite = Terranite, Nether + Terranite = Infernite, End + Infernite = Zenithite);
     * nine ingots make its showpiece block, like the netherite block. Prestige and building materials beside Enderite, no gear tier.
     */
    public static final String[] DIMENSIONS = {"", "nether_", "end_"};
    public static final String[] RIFT_METALS = {"terranite", "infernite", "zenithite"};
    static {
        for (String d : DIMENSIONS) block(d + "rift_shard_block", Blocks.ANCIENT_DEBRIS, Block::new);
        for (String m : RIFT_METALS) block(m + "_block", Blocks.NETHERITE_BLOCK, Block::new);
    }
    /** Block items that survive fire and lava (Enderite, scrap). */
    private static final java.util.Set<String> FIRE_RESISTANT = java.util.Set.of("enderite_block", "dimensional_scrap", "nether_dimensional_scrap",
            "end_dimensional_scrap", "enderite_nugget", "enderite_scrap", "enderite_ingot", "netherite_scrap_block", "enderite_scrap_block",
            "rift_shard", "nether_rift_shard", "end_rift_shard", "rift_shard_block", "nether_rift_shard_block", "end_rift_shard_block",
            "terranite_ingot", "infernite_ingot", "zenithite_ingot", "terranite_block", "infernite_block", "zenithite_block");

    static {
        for (String path : List.copyOf(BLOCKS.keySet())) {
            ResourceKey<Item> key = itemKey(path);
            Item.Properties properties = com.simplelib.api.RarityRules.apply(path, new Item.Properties()).useBlockDescriptionPrefix().setId(key);
            ITEMS.put(path, new BlockItem(BLOCKS.get(path), FIRE_RESISTANT.contains(path) ? properties.fireResistant() : properties));
        }
        plain("raw_silver");
        plain("silver_ingot");
        plain("silver_nugget");
        plain("raw_lead");
        plain("lead_ingot");
        plain("lead_nugget");
        // IG31-13: the Enderite chain (SimpleBuilding adds the armor trim material to the ingot), the template, the sage orb.
        for (String path : List.of("raw_enderite", "layered_raw_enderite", "enderite_scrap", "enderite_nugget", "enderite_ingot")) {
            item(path, p -> FIRE_RESISTANT.contains(path) ? p.fireResistant() : p);
        }
        // IG31-58: the rift shards and the three end materials of the Dimensional Scrap.
        for (String d : DIMENSIONS) item(d + "rift_shard", Item.Properties::fireResistant);
        for (String m : RIFT_METALS) item(m + "_ingot", Item.Properties::fireResistant);
        ITEMS.put("enderite_upgrade_template", upgradeTemplate(com.simplelib.api.RarityRules.apply("enderite_upgrade_template", new Item.Properties()).setId(itemKey("enderite_upgrade_template"))));
        for (String m : METALS) {
            plain("raw_" + m);
            plain(m + "_ingot");
            plain(m + "_nugget");
        }
        for (String a : MIXES) plain(a + "_ingot");
        plain("sage_crystal_shard");
        plain("sage_crystal");
        ITEMS.put("sage_orb", new com.simpleores.item.SageOrbItem(new Item.Properties().stacksTo(16).setId(itemKey("sage_orb"))));
        for (Alloy alloy : Alloy.values()) {
            plain(alloy.id + "_ingot");
            plain(alloy.id + "_nugget");
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
        // BLOCKS07: the carrots, a step above the golden carrot (numbers: OresFood); not enchanted, no effects.
        item("rose_bronze_carrot", p -> p.food(OresFood.ROSE_BRONZE_CARROT_FOOD));
        item("rose_gold_carrot", p -> p.food(OresFood.ROSE_GOLD_CARROT_FOOD));
        // IG31-41: the rose gold apples between the golden and the Netherite apple (numbers: OresFood).
        food("rose_bronze_apple", p -> p.food(OresFood.ROSE_BRONZE_APPLE_FOOD, OresFood.ROSE_BRONZE_APPLE));
        food("enchanted_rose_bronze_apple", p -> p.food(OresFood.ROSE_BRONZE_APPLE_FOOD, OresFood.ENCHANTED_ROSE_BRONZE_APPLE)
                .rarity(Rarity.RARE).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
        food("rose_gold_apple", p -> p.food(OresFood.ROSE_GOLD_APPLE_FOOD, OresFood.ROSE_GOLD_APPLE));
        food("enchanted_rose_gold_apple", p -> p.food(OresFood.ROSE_GOLD_APPLE_FOOD, OresFood.ENCHANTED_ROSE_GOLD_APPLE)
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
        ITEMS.put(path, new Item(properties.apply(com.simplelib.api.RarityRules.apply(path, new Item.Properties())).setId(itemKey(path))));
    }

    private static void food(String path, Function<Item.Properties, Item.Properties> properties) {
        ITEMS.put(path, new FoodEffectsItem(properties.apply(new Item.Properties()).setId(itemKey(path))));
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

    public static final String[] NATURAL = {"silver_ore", "deepslate_silver_ore", "end_silver_ore", "raw_silver_block", "lead_ore", "deepslate_lead_ore", "raw_lead_block",
            "end_lead_ore", "zinc_ore", "deepslate_zinc_ore", "end_zinc_ore", "geode_crust", "raw_zinc_block", "nickel_ore", "deepslate_nickel_ore", "raw_nickel_block",
            "tin_ore", "deepslate_tin_ore", "tin_bearing_gravel", "tin_bearing_sand", "raw_tin_block",
            "sage_ore", "deepslate_sage_ore", "nether_sage_ore", "end_sage_ore", "nether_rose_gold_ore", "dimensional_scrap", "nether_dimensional_scrap", "end_dimensional_scrap"};
    public static final String[] BUILDING = {"silver_block", "lead_block", "lead_plate", "silver_sheet", "silver_mirror", "zinc_block", "nickel_block", "tin_block", "brass_block", "stainless_steel_block", "bronze_block", "rose_bronze_block", "rose_gold_block", "rose_bronze_pressling_block", "rose_gold_pressling_block", "sage_crystal_block", "enderite_block",
            "netherite_scrap_block", "enderite_scrap_block", "sage_orb_block", "rift_shard_block", "nether_rift_shard_block", "end_rift_shard_block",
            "terranite_block", "infernite_block", "zenithite_block"};
    public static final String[] INGREDIENTS = {"raw_silver", "silver_nugget", "silver_ingot", "raw_lead", "lead_nugget", "lead_ingot", "raw_zinc", "zinc_nugget", "zinc_ingot", "raw_nickel", "nickel_nugget", "nickel_ingot", "raw_tin", "tin_nugget", "tin_ingot",
            "brass_ingot", "stainless_steel_ingot", "bronze_ingot", "rose_bronze_pressling",
            "rose_bronze_nugget", "rose_bronze_ingot", "rose_gold_pressling", "rose_gold_nugget", "rose_gold_ingot", "raw_enderite", "layered_raw_enderite", "enderite_scrap", "enderite_nugget",
            "enderite_ingot", "enderite_upgrade_template", "rift_shard", "nether_rift_shard", "end_rift_shard", "terranite_ingot", "infernite_ingot", "zenithite_ingot", "sage_crystal_shard", "sage_crystal", "sage_orb"};
    /** IG31-13: ids that moved here (items; the blocks have block items of the same path) - from SimpleLib the Enderite seven. */
    public static final List<String> FROM_SIMPLELIB = List.of("enderite_nugget", "raw_enderite", "layered_raw_enderite", "enderite_scrap", "enderite_ingot",
            "enderite_block", "enderite_upgrade_template");
    /** Recipes SimpleLib had for them -> folder of their recipe advancement (same paths here). */
    public static final Map<String, String> RECIPES_FROM_SIMPLELIB = Map.of("enderite_nugget_from_ingot", "misc", "enderite_ingot_from_nugget", "misc",
            "layered_raw_enderite", "misc", "enderite_scrap_from_blasting_layered_raw_enderite", "misc", "enderite_ingot_from_scrap", "misc",
            "enderite_block", "decorations", "enderite_ingot", "building_blocks", "enderite_upgrade_template", "misc");
    public static final String[] TOOLS = {"rose_bronze_shovel", "rose_bronze_pickaxe", "rose_bronze_axe", "rose_bronze_hoe",
            "rose_gold_shovel", "rose_gold_pickaxe", "rose_gold_axe", "rose_gold_hoe"};
    public static final String[] COMBAT = {"rose_bronze_sword", "rose_bronze_helmet", "rose_bronze_chestplate", "rose_bronze_leggings",
            "rose_bronze_boots", "rose_gold_sword", "rose_gold_helmet", "rose_gold_chestplate", "rose_gold_leggings", "rose_gold_boots"};

    /** IG31-41: the four rose gold apples, shown in the Food and Drinks tab behind the enchanted golden apple. */
    public static final String[] FOOD = {"rose_bronze_apple", "enchanted_rose_bronze_apple", "rose_gold_apple", "enchanted_rose_gold_apple",
            "rose_bronze_carrot", "rose_gold_carrot"};

    private OresRegistry() {}
}
