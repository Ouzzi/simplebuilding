package com.simplebuilding.blocks;

import com.simplebuilding.Simplebuilding;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModBlocks {

    // Lapis Light: Leuchtet (15), ist durchsichtig (Glas-Basis), erlaubt Spawning
    public static final Block CONSTRUCTION_LIGHT = registerBlock("construction_light",
            settings -> new Block(settings
                    .luminance(state -> 15)
                    .nonOpaque()
                    .allowsSpawning((state, world, pos, entityType) -> true))
    );

    /**
     * Registriert einen Block und weist ihm vor der Erstellung den notwendigen RegistryKey zu.
     */
    private static Block registerBlock(String name, Function<AbstractBlock.Settings, Block> factory) {
        Identifier id = Identifier.of(Simplebuilding.MOD_ID, name);
        RegistryKey<Block> key = RegistryKey.of(RegistryKeys.BLOCK, id);

        // Settings von Glas kopieren und den Key setzen (WICHTIG gegen den Absturz)
        AbstractBlock.Settings settings = AbstractBlock.Settings.copy(Blocks.GLASS)
                .registryKey(key);

        Block block = factory.apply(settings);
        return Registry.register(Registries.BLOCK, id, block);
    }

    public static void registerModBlocks() {
        Simplebuilding.LOGGER.info("Registering Mod Blocks for " + Simplebuilding.MOD_ID);
    }
}