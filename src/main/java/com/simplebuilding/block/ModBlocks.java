package com.simplebuilding.block;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.block.custom.ReinforcedShulkerBoxBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModBlocks {
    /*
        public static final Block PINK_GARNET_BLOCK = registerBlock("pink_garnet_block",
                properties -> new Block(properties.strength(4f)
                        .requiresTool().sounds(BlockSoundGroup.AMETHYST_BLOCK)));
    */

    // KORREKTUR: Wir nutzen registerBlockWithoutBlockItem, da das Item in ModItems ist.
    // KORREKTUR 2: Wir nutzen das übergebene 'settings' Objekt, damit der RegistryKey gesetzt ist!
    public static final Block REINFORCED_SHULKER_BOX = registerBlockWithoutBlockItem("reinforced_shulker_box",
            settings -> new ReinforcedShulkerBoxBlock(
                    DyeColor.CYAN,
                    // Wir nehmen die settings (die den Key haben) und kopieren dann die Werte der ShulkerBox hinein
                    settings.strength(2.0f).resistance(2.0f).nonOpaque()
            ));

    // Hinweis: AbstractBlock.Settings.copy() überschreibt den RegistryKey oft nicht korrekt,
    // wenn man es falsch herum macht. Am sichersten ist es, die Settings manuell zu setzen
    // oder sicherzustellen, dass .registryKey() als ALLERLETZTES aufgerufen wird (was unsere Methode unten tut).

    private static Block registerBlock(String name, Function<AbstractBlock.Settings, Block> function) {
        // Hier wird der Key gesetzt
        AbstractBlock.Settings settings = AbstractBlock.Settings.create()
                .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(Simplebuilding.MOD_ID, name)));

        Block toRegister = function.apply(settings);
        registerBlockItem(name, toRegister);
        return Registry.register(Registries.BLOCK, Identifier.of(Simplebuilding.MOD_ID, name), toRegister);
    }

    private static Block registerBlockWithoutBlockItem(String name, Function<AbstractBlock.Settings, Block> function) {
        AbstractBlock.Settings settings = AbstractBlock.Settings.create()
                .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(Simplebuilding.MOD_ID, name)));

        return Registry.register(Registries.BLOCK, Identifier.of(Simplebuilding.MOD_ID, name),
                function.apply(settings));
    }

    private static void registerBlockItem(String name, Block block) {
        Registry.register(Registries.ITEM, Identifier.of(Simplebuilding.MOD_ID, name),
                new BlockItem(block, new Item.Settings().useBlockPrefixedTranslationKey()
                        .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Simplebuilding.MOD_ID, name)))));
    }

    public static void registerModBlocks() {
        Simplebuilding.LOGGER.info("Registering Mod Blocks for " + Simplebuilding.MOD_ID);
    }
}
