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
 * Fabric entry: registries are open during init, so everything registers directly. The silver veins are placed features in
 * the module's data (NeoForge uses biome modifier files with the same features); here they are added to the Overworld and to
 * the outer End biomes (tag {@code simpleores:has_end_silver_ore}), only while the module is switched on.
 */
public final class OresFabric implements ModInitializer {
    private static final ResourceKey<PlacedFeature> SILVER = ResourceKey.create(Registries.PLACED_FEATURE, SimpleOres.id("silver_ore"));
    private static final ResourceKey<PlacedFeature> END_SILVER = ResourceKey.create(Registries.PLACED_FEATURE, SimpleOres.id("end_silver_ore"));
    private static final TagKey<Biome> END_BIOMES = TagKey.create(Registries.BIOME, SimpleOres.id("has_end_silver_ore"));

    @Override
    public void onInitialize() {
        SimpleOres.init(FabricLoader.getInstance().getConfigDir());
        // CMD24: /simpleores config for every option.
        com.simplelib.api.config.ConfigCommands.server(SimpleOres.MOD_ID).json(com.simpleores.config.OresConfig::toJson, com.simpleores.config.OresConfig::apply)
                .file(FabricLoader.getInstance().getConfigDir(), com.simpleores.config.OresConfig.FILE).register();
        OresRegistry.registerAll();
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld().and(ctx -> SimpleOres.on()), GenerationStep.Decoration.UNDERGROUND_ORES, SILVER);
        BiomeModifications.addFeature(BiomeSelectors.tag(END_BIOMES).and(ctx -> SimpleOres.on()), GenerationStep.Decoration.UNDERGROUND_ORES, END_SILVER);
        tab(CreativeModeTabs.NATURAL_BLOCKS, Items.GOLD_ORE, OresRegistry.NATURAL);
        tab(CreativeModeTabs.BUILDING_BLOCKS, Items.GOLD_BLOCK, OresRegistry.BUILDING);
        tab(CreativeModeTabs.INGREDIENTS, Items.GOLD_INGOT, OresRegistry.INGREDIENTS);
        tab(CreativeModeTabs.TOOLS_AND_UTILITIES, Items.GOLDEN_HOE, OresRegistry.TOOLS);
        tab(CreativeModeTabs.COMBAT, Items.GOLDEN_BOOTS, OresRegistry.COMBAT);
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
