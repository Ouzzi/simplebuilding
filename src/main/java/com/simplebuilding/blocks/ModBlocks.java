package com.simplebuilding.blocks;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.custom.*;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModBlocks {

    // CONSTRUCTION_LIGHT: Leuchtet (15), ist durchsichtig (Glas-Basis), erlaubt Spawning
    public static final Block CONSTRUCTION_LIGHT = registerBlock("construction_light",
            settings -> new Block(settings
                    .luminance(state -> 15)
                    .nonOpaque()
                    .allowsSpawning((state, world, pos, entityType) -> true))
    );

    public static final Block CRACKED_DIAMOND_BLOCK = registerBlock("cracked_diamond_block",
            settings -> new Block(settings.requiresTool().strength(5.0F, 6.0F).sounds(net.minecraft.sound.BlockSoundGroup.METAL))
    );


    public static final Block REINFORCED_HOPPER = registerBlock("reinforced_hopper",
            s -> new ModHopperBlock(s.strength(3.0F, 4.8F).nonOpaque(), 1)); // 1 = Speed Multiplier (Basislogik wird im BE handled)
    public static final Block NETHERITE_HOPPER = registerBlock("netherite_hopper",
            s -> new ModHopperBlock(s.strength(5.0F, 1200.0F).nonOpaque(), 2));
    // todo chest:

    // public static final Block REINFORCED_CHEST = registerBlock("reinforced_chest",
    //        s -> new ModChestBlock(s.strength(2.5F).nonOpaque(), ModChestBlock.Type.REINFORCED));
    // public static final Block NETHERITE_CHEST = registerBlock("netherite_chest",
    //        s -> new ModChestBlock(s.strength(5.0F, 1200.0F).nonOpaque(), ModChestBlock.Type.NETHERITE));
    public static final Block REINFORCED_PISTON = registerBlock("reinforced_piston",
            s -> new PistonBlock(false, s.strength(1.5F))); // sticky=false
    public static final Block NETHERITE_PISTON = registerBlock("netherite_piston",
            s -> new NetheriteBreakerPistonBlock(s.strength(5.0F, 1200.0F)));
    public static final Block REINFORCED_BLAST_FURNACE = registerBlock("reinforced_blast_furnace",
            s -> new ModBlastFurnaceBlock(s.strength(3.5F), 1.25f));
    public static final Block NETHERITE_BLAST_FURNACE = registerBlock("netherite_blast_furnace",
            s -> new ModBlastFurnaceBlock(s.strength(5.0F, 1200.0F), 1.50f));
    public static final Block NETHERITE_PISTON_HEAD = registerBlock("netherite_piston_head",
            s -> new NetheritePistonHeadBlock(s.noCollision().dropsNothing()));

    /**
     * Registriert einen Block und weist ihm vor der Erstellung den notwendigen RegistryKey zu.
     */
    private static Block registerBlock(String name, Function<AbstractBlock.Settings, Block> factory) {
        Identifier id = Identifier.of(Simplebuilding.MOD_ID, name);
        RegistryKey<Block> key = RegistryKey.of(RegistryKeys.BLOCK, id);
        AbstractBlock.Settings settings = AbstractBlock.Settings.copy(Blocks.GLASS).registryKey(key);

        Block block = factory.apply(settings);
        return Registry.register(Registries.BLOCK, id, block);
    }

    public static void registerModBlocks() {
        Simplebuilding.LOGGER.info("Registering Mod Blocks for " + Simplebuilding.MOD_ID);
    }
}