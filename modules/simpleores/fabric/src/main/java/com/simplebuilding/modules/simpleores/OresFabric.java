package com.simplebuilding.modules.simpleores;

import com.simpleores.OresRegistry;
import com.simpleores.SimpleOres;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Fabric entry: registries are open during init, so everything registers directly. The silver and lead veins are placed features in
 * the module's data (NeoForge uses biome modifier files with the same features); here they are added to the Overworld and to
 * the outer End biomes (tag {@code simpleores:has_end_silver_ore}), only while the module is switched on.
 */
public final class OresFabric implements ModInitializer {
    private static final ResourceKey<PlacedFeature> SILVER = ResourceKey.create(Registries.PLACED_FEATURE, SimpleOres.id("silver_ore"));
    private static final ResourceKey<PlacedFeature> LEAD = ResourceKey.create(Registries.PLACED_FEATURE, SimpleOres.id("lead_ore"));
    private static final ResourceKey<PlacedFeature> END_SILVER = ResourceKey.create(Registries.PLACED_FEATURE, SimpleOres.id("end_silver_ore"));
    private static final TagKey<Biome> END_BIOMES = TagKey.create(Registries.BIOME, SimpleOres.id("has_end_silver_ore"));
    /** IG31-13: Sage Ore and Dimensional Scrap, moved from SimpleBuilding with their placed features. */
    private static final ResourceKey<PlacedFeature> SAGE = ResourceKey.create(Registries.PLACED_FEATURE, SimpleOres.id("sage_ore_placed"));
    private static final ResourceKey<PlacedFeature> SCRAP = ResourceKey.create(Registries.PLACED_FEATURE, SimpleOres.id("dimensional_scrap_placed"));
    private static final ResourceKey<PlacedFeature> NETHER_SCRAP = ResourceKey.create(Registries.PLACED_FEATURE, SimpleOres.id("nether_dimensional_scrap_placed"));
    private static final ResourceKey<PlacedFeature> END_SCRAP = ResourceKey.create(Registries.PLACED_FEATURE, SimpleOres.id("end_dimensional_scrap_placed"));
    /** IG31-13: SimpleBuilding runs this initializer first (its items point at the Enderite here); the loader's own call then returns. */
    private static boolean initialized;

    @Override
    public void onInitialize() {
        if (initialized) return;
        initialized = true;
        SimpleOres.init(FabricLoader.getInstance().getConfigDir());
        // CMD24: /simpleores config for every option.
        com.simplelib.api.config.ConfigCommands.server(SimpleOres.MOD_ID).json(com.simpleores.config.OresConfig::toJson, com.simpleores.config.OresConfig::apply)
                .file(FabricLoader.getInstance().getConfigDir(), com.simpleores.config.OresConfig.FILE).register();
        OresRegistry.registerAll();
        com.simplelib.api.ModTabs.register(SimpleOres.MOD_ID, "ores", "itemgroup.simpleores.ores", () -> new ItemStack(OresRegistry.ITEMS.get("silver_ingot")));
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld().and(ctx -> SimpleOres.on()), GenerationStep.Decoration.UNDERGROUND_ORES, SILVER);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld().and(ctx -> SimpleOres.on()), GenerationStep.Decoration.UNDERGROUND_ORES, LEAD);
        BiomeModifications.addFeature(BiomeSelectors.tag(END_BIOMES).and(ctx -> SimpleOres.on()), GenerationStep.Decoration.UNDERGROUND_ORES, END_SILVER);
        // Sage Ore and Dimensional Scrap as SimpleBuilding placed them (the flat test world reports its biomes through the tags only).
        var overworld = BiomeSelectors.foundInOverworld().or(BiomeSelectors.tag(net.minecraft.tags.BiomeTags.IS_OVERWORLD));
        BiomeModifications.addFeature(overworld.and(ctx -> SimpleOres.on() && com.simpleores.config.OresConfig.sageOre), GenerationStep.Decoration.UNDERGROUND_ORES, SAGE);
        BiomeModifications.addFeature(overworld.and(ctx -> SimpleOres.on() && com.simpleores.config.OresConfig.dimensionalScrap), GenerationStep.Decoration.UNDERGROUND_ORES, SCRAP);
        BiomeModifications.addFeature(BiomeSelectors.foundInTheNether().or(BiomeSelectors.tag(net.minecraft.tags.BiomeTags.IS_NETHER))
                .and(ctx -> SimpleOres.on() && com.simpleores.config.OresConfig.dimensionalScrap), GenerationStep.Decoration.UNDERGROUND_ORES, NETHER_SCRAP);
        BiomeModifications.addFeature(BiomeSelectors.foundInTheEnd().or(BiomeSelectors.tag(net.minecraft.tags.BiomeTags.IS_END))
                .and(ctx -> SimpleOres.on() && com.simpleores.config.OresConfig.dimensionalScrap), GenerationStep.Decoration.UNDERGROUND_ORES, END_SCRAP);
        // Enderite in End city treasure on its own (SimpleBuilding feeds it itself: SimpleLibApi.materialLootByPartner).
        net.fabricmc.fabric.api.loot.v3.LootTableEvents.MODIFY.register((key, table, source, registries) -> com.simpleores.OresLoot.apply(key, table::withPool));
        tab(CreativeModeTabs.NATURAL_BLOCKS, Items.GOLD_ORE, OresRegistry.NATURAL);
        tab(CreativeModeTabs.BUILDING_BLOCKS, Items.GOLD_BLOCK, OresRegistry.BUILDING);
        tab(CreativeModeTabs.INGREDIENTS, Items.GOLD_INGOT, OresRegistry.INGREDIENTS);
        tab(CreativeModeTabs.TOOLS_AND_UTILITIES, Items.GOLDEN_HOE, OresRegistry.TOOLS);
        tab(CreativeModeTabs.COMBAT, Items.GOLDEN_BOOTS, OresRegistry.COMBAT);
        tab(CreativeModeTabs.FOOD_AND_DRINKS, Items.ENCHANTED_GOLDEN_APPLE, OresRegistry.FOOD);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleOres.on()) return;
            var book = java.util.List.of(com.simpleores.guide.OresGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simpleores.guide.OresGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }

    /** Vanilla tab entries behind the matching gold item, at the tab's end when the anchor is missing. */
    private static void tab(ResourceKey<CreativeModeTab> tab, Item anchor, String[] paths) {
        CreativeModeTabEvents.modifyOutputEvent(tab).register(out -> insert(out, anchor, OresRegistry.stacks(paths)));
    }

    private static void insert(FabricCreativeModeTabOutput out, Item anchor, java.util.List<ItemStack> stacks) {
        if (out.getDisplayStacks().stream().anyMatch(s -> s.is(anchor))) out.insertAfter(anchor, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        else stacks.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
    }
}
