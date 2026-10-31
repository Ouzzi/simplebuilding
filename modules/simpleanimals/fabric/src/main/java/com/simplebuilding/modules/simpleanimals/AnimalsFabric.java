package com.simplebuilding.modules.simpleanimals;

import com.simpleanimals.AnimalsRegistry;
import com.simpleanimals.SimpleAnimals;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;

/**
 * Fabric entry: registries are open during init, so everything registers directly. Natural spawns are added to the Vanilla
 * biomes here; the per-animal rules (ground, light, config thinning) live in each entity's {@code checkSpawnRules}.
 */
public final class AnimalsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleAnimals.init(FabricLoader.getInstance().getConfigDir());
        // CMD24: /simpleanimals config for every option.
        com.simplelib.api.config.ConfigCommands.server(SimpleAnimals.MOD_ID).json(com.simpleanimals.config.AnimalsConfig::toJson, com.simpleanimals.config.AnimalsConfig::apply).file(FabricLoader.getInstance().getConfigDir(), com.simpleanimals.config.AnimalsConfig.FILE).register();
        AnimalsRegistry.registerAll();
        FabricDefaultAttributeRegistry.register(AnimalsRegistry.DEER, AnimalsRegistry.deerAttributes());
        FabricDefaultAttributeRegistry.register(AnimalsRegistry.BOAR, AnimalsRegistry.boarAttributes());
        FabricDefaultAttributeRegistry.register(AnimalsRegistry.HEDGEHOG, AnimalsRegistry.hedgehogAttributes());
        FabricDefaultAttributeRegistry.register(AnimalsRegistry.CRAB, AnimalsRegistry.crabAttributes());
        FabricDefaultAttributeRegistry.register(AnimalsRegistry.ZEBRA, AnimalsRegistry.horseAttributes());
        FabricDefaultAttributeRegistry.register(AnimalsRegistry.ZORSE, AnimalsRegistry.horseAttributes());
        // Spawn weights are low next to Vanilla (Vanilla farm animals: sheep 12, pig 10, cow 8): A12, never a flood of new animals.
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_FOREST), MobCategory.CREATURE, AnimalsRegistry.DEER, 4, 2, 3);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_TAIGA), MobCategory.CREATURE, AnimalsRegistry.DEER, 3, 2, 3);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_FOREST), MobCategory.CREATURE, AnimalsRegistry.BOAR, 3, 2, 4);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_TAIGA), MobCategory.CREATURE, AnimalsRegistry.BOAR, 3, 2, 4);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_FOREST), MobCategory.CREATURE, AnimalsRegistry.HEDGEHOG, 3, 1, 2);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.PLAINS, Biomes.MEADOW), MobCategory.CREATURE, AnimalsRegistry.HEDGEHOG, 2, 1, 2);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_BEACH), MobCategory.CREATURE, AnimalsRegistry.CRAB, 1, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.MANGROVE_SWAMP), MobCategory.CREATURE, AnimalsRegistry.CRAB, 1, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_SAVANNA), MobCategory.CREATURE, AnimalsRegistry.ZEBRA, 2, 2, 4);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(out -> {
            if (!SimpleAnimals.on()) return;
            var eggs = AnimalsRegistry.eggStacks();
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.ARMADILLO_SPAWN_EGG))) {
                out.insertAfter(Items.ARMADILLO_SPAWN_EGG, eggs, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            } else {
                eggs.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(out -> {
            if (!SimpleAnimals.on()) return;
            var food = java.util.List.of(new net.minecraft.world.item.ItemStack(AnimalsRegistry.RAW_CRAB), new net.minecraft.world.item.ItemStack(AnimalsRegistry.COOKED_CRAB),
                    new net.minecraft.world.item.ItemStack(AnimalsRegistry.TRUFFLE), new net.minecraft.world.item.ItemStack(AnimalsRegistry.TRUFFLE_SOUP));
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.COOKED_COD))) {
                out.insertAfter(Items.COOKED_COD, food, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            } else {
                food.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
            }
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleAnimals.on()) return;
            var book = java.util.List.of(com.simpleanimals.guide.AnimalsGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simpleanimals.guide.AnimalsGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }
}
