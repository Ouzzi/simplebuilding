package com.simplebuilding.modules.simpledinos;

import com.simpledinos.DinosRegistry;
import com.simpledinos.SimpleDinos;
import com.simpledinos.block.DinoEggBlockEntity;
import com.simpledinos.block.FossilBlockEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Fabric entry: registries are open during init, so everything registers directly (blocks, items, entity types, then the block
 * entity types). The fossil block is added to the underground of deserts, badlands and swamps through the biome tag
 * {@code simpledinos:has_fossil_block} (NeoForge does the same with a biome modifier file).
 */
public final class DinosFabric implements ModInitializer {
    private static final ResourceKey<PlacedFeature> FOSSIL_FEATURE = ResourceKey.create(Registries.PLACED_FEATURE, SimpleDinos.id("fossil_block"));
    private static final TagKey<Biome> FOSSIL_BIOMES = TagKey.create(Registries.BIOME, SimpleDinos.id("has_fossil_block"));

    @Override
    public void onInitialize() {
        SimpleDinos.init(FabricLoader.getInstance().getConfigDir());
        // CMD24: /simpledinos config for every option.
        com.simplelib.api.config.ConfigCommands.server(SimpleDinos.MOD_ID).json(com.simpledinos.config.DinosConfig::toJson, com.simpledinos.config.DinosConfig::apply)
                .file(FabricLoader.getInstance().getConfigDir(), com.simpledinos.config.DinosConfig.FILE).register();
        DinosRegistry.registerAll();
        DinosRegistry.registerBlockEntityTypes(new BlockEntityType<>(FossilBlockEntity::new, DinosRegistry.fossilBlocks()),
                new BlockEntityType<>(DinoEggBlockEntity::new, DinosRegistry.eggBlocks()));
        FabricDefaultAttributeRegistry.register(DinosRegistry.TREX, DinosRegistry.trexAttributes());
        BiomeModifications.addFeature(BiomeSelectors.tag(FOSSIL_BIOMES), GenerationStep.Decoration.UNDERGROUND_ORES, FOSSIL_FEATURE);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(out -> {
            if (!SimpleDinos.on()) return;
            var blocks = DinosRegistry.tabStacks().subList(0, 1 + com.simpledinos.DinoKind.values().length);
            insert(out, Items.BONE_BLOCK, blocks);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleDinos.on()) return;
            insert(out, Items.SADDLE, java.util.List.of(new ItemStack(DinosRegistry.LARGE_SADDLE)));
            var book = java.util.List.of(com.simpledinos.guide.DinosGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(out -> {
            if (!SimpleDinos.on()) return;
            insert(out, Items.DIAMOND_HORSE_ARMOR, java.util.List.of(new ItemStack(DinosRegistry.TREX_ARMOR_LEATHER),
                    new ItemStack(DinosRegistry.TREX_ARMOR_IRON), new ItemStack(DinosRegistry.TREX_ARMOR_DIAMOND)));
        });
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simpledinos.guide.DinosGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }

    private static void insert(net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput out, Item anchor, java.util.List<ItemStack> stacks) {
        if (out.getDisplayStacks().stream().anyMatch(s -> s.is(anchor))) out.insertAfter(anchor, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        else stacks.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
    }
}
