package com.simplebuilding.blocks;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.custom.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import com.simplebuilding.items.custom.BackpackTier;
import java.util.function.Function;

public class ModBlocks {




    // --- 3. GRAVITY BLOCKS ---

    // --- 4. INDUSTRIAL BLOCKS ---
    public static final Block CONSTRUCTION_LIGHT = registerBlock("construction_light",
            // Wir ignorieren das 'settings' Argument der Factory
            unused -> new Block(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "construction_light")))
                    .mapColor(net.minecraft.world.level.material.MapColor.DIAMOND)
                    .strength(0.3F) // Zerbricht schnell
                    .sound(net.minecraft.world.level.block.SoundType.GLASS) // Klingt wie Glas
                    .lightLevel(state -> 15) // Leuchtet hell
                    .isValidSpawn((state, world, pos, type) -> true) // Erlaubt Spawns
                    .isRedstoneConductor((state, world, pos) -> true) // WICHTIG: Gilt als voller Block für Mobs
                    .isViewBlocking((state, world, pos) -> false) // WICHTIG: Lässt Licht durch (optisch)
                    .isSuffocating((state, world, pos) -> false) // Man erstickt nicht darin
            )
    );

    public static final Block CRACKED_DIAMOND_BLOCK = registerBlock("cracked_diamond_block",
            unused -> new Block(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_BLOCK)
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "cracked_diamond_block")))
                    .strength(7.0F, 14.0F)
            ));



    // Hoppers and the six furnaces are built from their VANILLA counterpart, not from the glass
    // every other block of this helper starts from: glass handed them no tool requirement (a bare
    // hand dropped them), no light while burning, no occlusion and glass's map colour and note.
    // The mod's own strength and sound still override what the copy brings.
    public static final Block REINFORCED_HOPPER = registerBlock("reinforced_hopper", Blocks.HOPPER, s -> new ModHopperBlock(s.strength(3.0F, 4.8F).noOcclusion().sound(SoundType.METAL)));
    public static final Block NETHERITE_HOPPER = registerBlock("netherite_hopper", Blocks.HOPPER, s -> new ModHopperBlock(s.strength(5.0F, 1200.0F).noOcclusion().sound(SoundType.NETHERITE_BLOCK)));

    // Beide verstaerkten Kolben starten wie alle anderen hier von GLASS, nicht von Vanillas
    // Kolben-Properties: deren pushReaction(BLOCK) wuerde einen EINGEFAHRENEN Mod-Kolben
    // unverschiebbar machen, weil isPushable nur Blocks.PISTON / STICKY_PISTON beim Namen ausnimmt.
    public static final Block REINFORCED_PISTON = registerBlock("reinforced_piston", s -> new ReinforcedPistonBlock(false, s.strength(1.5F).sound(SoundType.METAL))); // sticky=false
    public static final Block REINFORCED_STICKY_PISTON = registerBlock("reinforced_sticky_piston", s -> new ReinforcedPistonBlock(true, s.strength(1.5F).sound(SoundType.METAL))); // sticky=true
    public static final Block NETHERITE_PISTON = registerBlock("netherite_piston", s -> new NetheriteBreakerPistonBlock(s.strength(5.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK)));
    public static final Block ENDERITE_PISTON = registerBlock("enderite_piston", s -> new EnderitePistonBlock(s.strength(6.0F, 1500.0F).sound(SoundType.NETHERITE_BLOCK)));
    public static final Block NETHERITE_PISTON_HEAD = registerBlock("netherite_piston_head", s -> new NetheritePistonHeadBlock(s.noCollision().noLootTable().sound(SoundType.NETHERITE_BLOCK)));

    public static final Block REINFORCED_FURNACE = registerBlock("reinforced_furnace", Blocks.FURNACE, s -> new ModFurnaceBlock(s.strength(3.5F).sound(SoundType.METAL)));
    public static final Block NETHERITE_FURNACE = registerBlock("netherite_furnace", Blocks.FURNACE, s -> new ModFurnaceBlock(s.strength(5.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK)));
    public static final Block REINFORCED_SMOKER = registerBlock("reinforced_smoker", Blocks.SMOKER, s -> new ModSmokerBlock(s.strength(3.5F).sound(SoundType.METAL)));
    public static final Block NETHERITE_SMOKER = registerBlock("netherite_smoker", Blocks.SMOKER, s -> new ModSmokerBlock(s.strength(5.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK)));
    public static final Block REINFORCED_BLAST_FURNACE = registerBlock("reinforced_blast_furnace", Blocks.BLAST_FURNACE, s -> new ModBlastFurnaceBlock(s.strength(3.5F).sound(SoundType.METAL)));
    public static final Block NETHERITE_BLAST_FURNACE = registerBlock("netherite_blast_furnace", Blocks.BLAST_FURNACE, s -> new ModBlastFurnaceBlock(s.strength(5.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK)));

    // Enderit-Stufe: nur in der Welt erreichbar, per Vorschlaghammer und Enderit-Nugget aus der
    // Netherit-Stufe (SledgehammerUpgrades); kein Werkbankrezept. Dieselben Klassen und damit
    // dieselben Block-Entities wie die beiden anderen Stufen, Haerte und Explosionsfestigkeit wie
    // beim Enderitkolben.
    public static final Block ENDERITE_HOPPER = registerBlock("enderite_hopper", Blocks.HOPPER, s -> new ModHopperBlock(s.strength(6.0F, 1500.0F).noOcclusion().sound(SoundType.NETHERITE_BLOCK)));
    public static final Block ENDERITE_FURNACE = registerBlock("enderite_furnace", Blocks.FURNACE, s -> new ModFurnaceBlock(s.strength(6.0F, 1500.0F).sound(SoundType.NETHERITE_BLOCK)));
    public static final Block ENDERITE_SMOKER = registerBlock("enderite_smoker", Blocks.SMOKER, s -> new ModSmokerBlock(s.strength(6.0F, 1500.0F).sound(SoundType.NETHERITE_BLOCK)));
    public static final Block ENDERITE_BLAST_FURNACE = registerBlock("enderite_blast_furnace", Blocks.BLAST_FURNACE, s -> new ModBlastFurnaceBlock(s.strength(6.0F, 1500.0F).sound(SoundType.NETHERITE_BLOCK)));

    // --- 5. RUCKSAECKE (abgestellt) ---
    // Aus der Glas-Vorlage (keine Verdeckung, kein Ersticken, kein Redstone-Leiter - passend zur
    // kleinen Form), dann Wolle-Klang, weich wie Wolle und von Kolben zerstoert statt geschoben.
    // Netherit und Enderit halten Explosionen aus wie ihre Items.
    public static final Block BACKPACK = registerBlock("backpack", s -> new BackpackBlock(BackpackTier.BASIC, backpackProperties(s, false)));
    public static final Block REINFORCED_BACKPACK = registerBlock("reinforced_backpack", s -> new BackpackBlock(BackpackTier.REINFORCED, backpackProperties(s, false)));
    public static final Block NETHERITE_BACKPACK = registerBlock("netherite_backpack", s -> new BackpackBlock(BackpackTier.NETHERITE, backpackProperties(s, true)));
    public static final Block ENDERITE_BACKPACK = registerBlock("enderite_backpack", s -> new BackpackBlock(BackpackTier.ENDERITE, backpackProperties(s, true)));


    // --- 1. DECORATION BLOCKS --- // todo add stonecutting and crafting recipie like vanilla
    public static final Block POLISHED_END_STONE = registerBlock("polished_end_stone", unused -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).requiresCorrectToolForDrops().strength(3.0F, 9.0F).sound(SoundType.STONE).setId(keyOf("polished_end_stone"))));

    // Checker Blocks
    public static final Block PURPUR_QUARTZ_CHECKER = registerBlock("purpur_quartz_checker", unused -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.PURPUR_BLOCK).setId(keyOf("purpur_quartz_checker")).isValidSpawn((state, world, pos, type) -> false)));
    public static final Block LAPIS_QUARTZ_CHECKER = registerBlock("lapis_quartz_checker", unused -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LAPIS_BLOCK).setId(keyOf("lapis_quartz_checker")).isValidSpawn((state, world, pos, type) -> false)));
    public static final Block BLACKSTONE_QUARTZ_CHECKER = registerBlock("blackstone_quartz_checker", unused -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_BLACKSTONE).setId(keyOf("blackstone_quartz_checker")).isValidSpawn((state, world, pos, type) -> false)));
    public static final Block RESIN_QUARTZ_CHECKER = registerBlock("resin_quartz_checker", unused -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.RESIN_BRICKS).setId(keyOf("resin_quartz_checker")).isValidSpawn((state, world, pos, type) -> false)));

    // --- 2. ASTRAL & NIHIL VARIANTS ---
    public static final Block ASTRAL_PURPUR_BLOCK = registerBlock("astral_purpur_block", unused -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.PURPUR_BLOCK).lightLevel(state -> 10).setId(keyOf("astral_purpur_block"))));
    public static final Block NIHIL_PURPUR_BLOCK = registerBlock("nihil_purpur_block", unused -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.PURPUR_BLOCK).setId(keyOf("nihil_purpur_block"))));
    public static final Block ASTRAL_END_STONE = registerBlock("astral_end_stone", unused -> new Block(BlockBehaviour.Properties.ofFullCopy(POLISHED_END_STONE).lightLevel(state -> 10).setId(keyOf("astral_end_stone"))));
    public static final Block NIHIL_END_STONE = registerBlock("nihil_end_stone", unused -> new Block(BlockBehaviour.Properties.ofFullCopy(POLISHED_END_STONE).setId(keyOf("nihil_end_stone"))));
    // Nihilith- und Astralit-Schachbrett: wie die uebrigen Quarz-Schachbretter (Saeulenblock, keine
    // Spawns), aber aus dem End-Material; stehen hinter NIHIL_END_STONE, deren Eigenschaften sie kopieren.
    public static final Block NIHILITH_QUARTZ_CHECKER = registerBlock("nihilith_quartz_checker", unused -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(NIHIL_END_STONE).setId(keyOf("nihilith_quartz_checker")).isValidSpawn((state, world, pos, type) -> false)));
    // Astralit leuchtet (Erz 5, beschichtete Bloecke 10); das halb aus Quarz bestehende Schachbrett liegt mit 5 dazwischen.
    public static final Block ASTRALIT_QUARTZ_CHECKER = registerBlock("astralit_quartz_checker", unused -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(NIHIL_END_STONE).lightLevel(state -> 5).setId(keyOf("astralit_quartz_checker")).isValidSpawn((state, world, pos, type) -> false)));

    // Astralit-/Nihilith-Bausatz wie Endstein/Purpur: Ziegel samt Treppe, Stufe und Mauer, dazu
    // Saeule und gemeisselte Ziegel. Alle kopieren den beschichteten Endstein ihres Materials
    // (Haerte 3/9, Spitzhacke noetig); Astralit leuchtet damit wie die beschichteten Bloecke mit 10.
    public static final Block ASTRALIT_BRICKS = registerBlock("astralit_bricks", ASTRAL_END_STONE, Block::new);
    public static final Block ASTRALIT_BRICK_STAIRS = registerBlock("astralit_brick_stairs", ASTRALIT_BRICKS, s -> new StairBlock(ASTRALIT_BRICKS.defaultBlockState(), s));
    public static final Block ASTRALIT_BRICK_SLAB = registerBlock("astralit_brick_slab", ASTRALIT_BRICKS, SlabBlock::new);
    public static final Block ASTRALIT_BRICK_WALL = registerBlock("astralit_brick_wall", ASTRALIT_BRICKS, s -> new WallBlock(s.forceSolidOn()));
    public static final Block ASTRALIT_PILLAR = registerBlock("astralit_pillar", ASTRAL_END_STONE, RotatedPillarBlock::new);
    public static final Block CHISELED_ASTRALIT_BRICKS = registerBlock("chiseled_astralit_bricks", ASTRAL_END_STONE, Block::new);
    public static final Block NIHILITH_BRICKS = registerBlock("nihilith_bricks", NIHIL_END_STONE, Block::new);
    public static final Block NIHILITH_BRICK_STAIRS = registerBlock("nihilith_brick_stairs", NIHILITH_BRICKS, s -> new StairBlock(NIHILITH_BRICKS.defaultBlockState(), s));
    public static final Block NIHILITH_BRICK_SLAB = registerBlock("nihilith_brick_slab", NIHILITH_BRICKS, SlabBlock::new);
    public static final Block NIHILITH_BRICK_WALL = registerBlock("nihilith_brick_wall", NIHILITH_BRICKS, s -> new WallBlock(s.forceSolidOn()));
    public static final Block NIHILITH_PILLAR = registerBlock("nihilith_pillar", NIHIL_END_STONE, RotatedPillarBlock::new);
    public static final Block CHISELED_NIHILITH_BRICKS = registerBlock("chiseled_nihilith_bricks", NIHIL_END_STONE, Block::new);


    public static final Block SUSPENDED_SAND = registerBlock("suspended_sand", unused -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).noCollision().setId(keyOf("suspended_sand"))));
    public static final Block SUSPENDED_GRAVEL = registerBlock("suspended_gravel", unused -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GRAVEL).setId(keyOf("suspended_gravel"))));
    public static final Block LEVITATING_SAND = registerBlock("levitating_sand", unused -> new LevitatingBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).setId(keyOf("levitating_sand"))));
    public static final Block LEVITATING_GRAVEL = registerBlock("levitating_gravel", unused -> new LevitatingBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GRAVEL).setId(keyOf("levitating_gravel"))));

    public static final Block ENDERITE_BLOCK = registerBlock("enderite_block", unused -> new Block(BlockBehaviour.Properties.of().setId(keyOf("enderite_block")).mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(50.0f, 1200.0f).sound(SoundType.NETHERITE_BLOCK)));
    public static final Block NIHILITH_ORE = registerBlock("nihilith_ore", unused -> new DropExperienceBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE).setId(keyOf("nihilith_ore")).strength(25.0f, 1200.0f).requiresCorrectToolForDrops()));
    public static final Block ASTRALIT_ORE = registerBlock("astralit_ore", unused -> new DropExperienceBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE).setId(keyOf("astralit_ore")).strength(20.0f, 1200.0f).lightLevel(state -> 5).requiresCorrectToolForDrops()));

    /**
     * Registriert einen Block und weist ihm vor der Erstellung den notwendigen RegistryKey zu.
     */
    private static Block registerBlock(String name, Function<BlockBehaviour.Properties, Block> factory) {
        return registerBlock(name, Blocks.GLASS, factory);
    }

    /** As above, with the properties copied from {@code base} instead of glass. */
    private static Block registerBlock(String name, Block base, Function<BlockBehaviour.Properties, Block> factory) {
        Identifier id = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name);
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
        BlockBehaviour.Properties settings = BlockBehaviour.Properties.ofFullCopy(base).setId(key);
        Block block = factory.apply(settings);
        return Registry.register(BuiltInRegistries.BLOCK, id, block);
    }

    public static void registerModBlocks() {
        Simplebuilding.LOGGER.info("Registering Mod Blocks for " + Simplebuilding.MOD_ID);
    }

    private static BlockBehaviour.Properties backpackProperties(BlockBehaviour.Properties settings, boolean blastProof) {
        return settings.strength(0.8F, blastProof ? 1200.0F : 0.8F)
                .sound(SoundType.WOOL)
                .mapColor(MapColor.COLOR_BROWN)
                .pushReaction(PushReaction.DESTROY);
    }

    private static ResourceKey<Block> keyOf(String name) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name));
    }
}
