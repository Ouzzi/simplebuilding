package com.simplebuilding.blocks;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.custom.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.intprovider.UniformIntProvider;

import java.util.function.Function;

public class ModBlocks {




    // --- 3. GRAVITY BLOCKS ---

    // --- 4. INDUSTRIAL BLOCKS ---
    public static final Block CONSTRUCTION_LIGHT = registerBlock("construction_light",
            // Wir ignorieren das 'settings' Argument der Factory
            unused -> new Block(AbstractBlock.Settings.create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(Simplebuilding.MOD_ID, "construction_light")))
                    .mapColor(net.minecraft.block.MapColor.DIAMOND_BLUE)
                    .strength(0.3F) // Zerbricht schnell
                    .sounds(net.minecraft.sound.BlockSoundGroup.GLASS) // Klingt wie Glas
                    .luminance(state -> 15) // Leuchtet hell
                    .allowsSpawning((state, world, pos, type) -> true) // Erlaubt Spawns
                    .solidBlock((state, world, pos) -> true) // WICHTIG: Gilt als voller Block für Mobs
                    .blockVision((state, world, pos) -> false) // WICHTIG: Lässt Licht durch (optisch)
                    .suffocates((state, world, pos) -> false) // Man erstickt nicht darin
            )
    );

    public static final Block CRACKED_DIAMOND_BLOCK = registerBlock("cracked_diamond_block",
            unused -> new Block(
                    AbstractBlock.Settings.copy(Blocks.DIAMOND_BLOCK)
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(Simplebuilding.MOD_ID, "cracked_diamond_block")))
                    .strength(7.0F, 14.0F)
            ));



    public static final Block REINFORCED_HOPPER = registerBlock("reinforced_hopper", s -> new ModHopperBlock(s.strength(3.0F, 4.8F).nonOpaque().sounds(BlockSoundGroup.METAL), 1)); // 1 = Speed Multiplier (Basislogik wird im BE handled)
    public static final Block NETHERITE_HOPPER = registerBlock("netherite_hopper", s -> new ModHopperBlock(s.strength(5.0F, 1200.0F).nonOpaque().sounds(BlockSoundGroup.NETHERITE), 2));

    public static final Block REINFORCED_PISTON = registerBlock("reinforced_piston", s -> new PistonBlock(false, s.strength(1.5F).sounds(BlockSoundGroup.METAL))); // sticky=false
    public static final Block NETHERITE_PISTON = registerBlock("netherite_piston", s -> new NetheriteBreakerPistonBlock(s.strength(5.0F, 1200.0F).sounds(BlockSoundGroup.NETHERITE)));
    public static final Block NETHERITE_PISTON_HEAD = registerBlock("netherite_piston_head", s -> new NetheritePistonHeadBlock(s.noCollision().dropsNothing().sounds(BlockSoundGroup.NETHERITE)));

    public static final Block REINFORCED_FURNACE = registerBlock("reinforced_furnace", s -> new ModFurnaceBlock(s.strength(3.5F).sounds(BlockSoundGroup.METAL), 1.25f)); // SpeedMultiplier wird hier nicht direkt genutzt, aber Konstruktor braucht ihn
    public static final Block NETHERITE_FURNACE = registerBlock("netherite_furnace", s -> new ModFurnaceBlock(s.strength(5.0F, 1200.0F).sounds(BlockSoundGroup.NETHERITE), 1.50f));
    public static final Block REINFORCED_SMOKER = registerBlock("reinforced_smoker", s -> new ModSmokerBlock(s.strength(3.5F).sounds(BlockSoundGroup.METAL), 1.25f));
    public static final Block NETHERITE_SMOKER = registerBlock("netherite_smoker", s -> new ModSmokerBlock(s.strength(5.0F, 1200.0F).sounds(BlockSoundGroup.NETHERITE), 1.50f));
    public static final Block REINFORCED_BLAST_FURNACE = registerBlock("reinforced_blast_furnace", s -> new ModBlastFurnaceBlock(s.strength(3.5F).sounds(BlockSoundGroup.METAL), 1.25f));
    public static final Block NETHERITE_BLAST_FURNACE = registerBlock("netherite_blast_furnace", s -> new ModBlastFurnaceBlock(s.strength(5.0F, 1200.0F).sounds(BlockSoundGroup.NETHERITE), 1.50f));


    // --- 1. DECORATION BLOCKS --- // todo add stonecutting and crafting recipie like vanilla
    public static final Block POLISHED_END_STONE = registerBlock("polished_end_stone", unused -> new Block(AbstractBlock.Settings.create().mapColor(MapColor.PALE_YELLOW).requiresTool().strength(3.0F, 9.0F).sounds(BlockSoundGroup.STONE).registryKey(keyOf("polished_end_stone"))));

    // Checker Blocks
    public static final Block PURPUR_QUARTZ_CHECKER = registerBlock("purpur_quartz_checker", unused -> new PillarBlock(AbstractBlock.Settings.copy(Blocks.PURPUR_BLOCK).registryKey(keyOf("purpur_quartz_checker")).allowsSpawning((state, world, pos, type) -> false)));
    public static final Block LAPIS_QUARTZ_CHECKER = registerBlock("lapis_quartz_checker", unused -> new PillarBlock(AbstractBlock.Settings.copy(Blocks.LAPIS_BLOCK).registryKey(keyOf("lapis_quartz_checker")).allowsSpawning((state, world, pos, type) -> false)));
    public static final Block BLACKSTONE_QUARTZ_CHECKER = registerBlock("blackstone_quartz_checker", unused -> new PillarBlock(AbstractBlock.Settings.copy(Blocks.POLISHED_BLACKSTONE).registryKey(keyOf("blackstone_quartz_checker")).allowsSpawning((state, world, pos, type) -> false)));
    public static final Block RESIN_QUARTZ_CHECKER = registerBlock("resin_quartz_checker", unused -> new PillarBlock(AbstractBlock.Settings.copy(Blocks.RESIN_BRICKS).registryKey(keyOf("resin_quartz_checker")).allowsSpawning((state, world, pos, type) -> false)));

    // --- 2. ASTRAL & NIHIL VARIANTS ---
    public static final Block ASTRAL_PURPUR_BLOCK = registerBlock("astral_purpur_block", unused -> new Block(AbstractBlock.Settings.copy(Blocks.PURPUR_BLOCK).luminance(state -> 10).registryKey(keyOf("astral_purpur_block"))));
    public static final Block NIHIL_PURPUR_BLOCK = registerBlock("nihil_purpur_block", unused -> new Block(AbstractBlock.Settings.copy(Blocks.PURPUR_BLOCK).registryKey(keyOf("nihil_purpur_block"))));
    public static final Block ASTRAL_END_STONE = registerBlock("astral_end_stone", unused -> new Block(AbstractBlock.Settings.copy(POLISHED_END_STONE).luminance(state -> 10).registryKey(keyOf("astral_end_stone"))));
    public static final Block NIHIL_END_STONE = registerBlock("nihil_end_stone", unused -> new Block(AbstractBlock.Settings.copy(POLISHED_END_STONE).registryKey(keyOf("nihil_end_stone"))));


    public static final Block SUSPENDED_SAND = registerBlock("suspended_sand", unused -> new Block(AbstractBlock.Settings.copy(Blocks.SAND).noCollision().registryKey(keyOf("suspended_sand"))));
    public static final Block SUSPENDED_GRAVEL = registerBlock("suspended_gravel", unused -> new Block(AbstractBlock.Settings.copy(Blocks.GRAVEL).registryKey(keyOf("suspended_gravel"))));
    public static final Block LEVITATING_SAND = registerBlock("levitating_sand", unused -> new LevitatingBlock(AbstractBlock.Settings.copy(Blocks.SAND).registryKey(keyOf("levitating_sand"))));
    public static final Block LEVITATING_GRAVEL = registerBlock("levitating_gravel", unused -> new LevitatingBlock(AbstractBlock.Settings.copy(Blocks.GRAVEL).registryKey(keyOf("levitating_gravel"))));

    public static final Block ENDERITE_BLOCK = registerBlock("enderite_block", unused -> new Block(AbstractBlock.Settings.create().registryKey(keyOf("enderite_block")).mapColor(MapColor.BLACK).requiresTool().strength(50.0f, 1200.0f).sounds(BlockSoundGroup.NETHERITE)));
    public static final Block NIHILITH_ORE = registerBlock("nihilith_ore", unused -> new ExperienceDroppingBlock(UniformIntProvider.create(3, 7), AbstractBlock.Settings.copy(Blocks.END_STONE).registryKey(keyOf("nihilith_ore")).strength(25.0f, 1200.0f).requiresTool()));
    public static final Block ASTRALIT_ORE = registerBlock("astralit_ore", unused -> new ExperienceDroppingBlock(UniformIntProvider.create(3, 7), AbstractBlock.Settings.copy(Blocks.END_STONE).registryKey(keyOf("astralit_ore")).strength(20.0f, 1200.0f).luminance(state -> 5).requiresTool()));

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

    private static RegistryKey<Block> keyOf(String name) {
        return RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(Simplebuilding.MOD_ID, name));
    }
}