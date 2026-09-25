package com.simplebuilding.blocks;

import com.simplebuilding.version.McVersion;

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
import java.util.List;
import java.util.function.Function;

public class ModBlocks {




    // --- 3. GRAVITY BLOCKS ---

    // --- 4. INDUSTRIAL BLOCKS ---
    public static final Block CONSTRUCTION_LIGHT = registerBlock("construction_light",
            // Wir ignorieren das 'settings' Argument der Factory
            // neverViewBlocking: Laesst Licht durch (optisch); die Praedikat-Signatur unterscheidet sich je MC-Version.
            unused -> new Block(McVersion.neverViewBlocking(BlockBehaviour.Properties.of())
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "construction_light")))
                    .mapColor(net.minecraft.world.level.material.MapColor.DIAMOND)
                    .strength(0.3F) // Zerbricht schnell
                    .sound(net.minecraft.world.level.block.SoundType.GLASS) // Klingt wie Glas
                    .lightLevel(state -> 15) // Leuchtet hell
                    .isValidSpawn((state, world, pos, type) -> true) // Erlaubt Spawns
                    .isRedstoneConductor((state, world, pos) -> true) // WICHTIG: Gilt als voller Block für Mobs
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

    // Die Paletten vervollstaendigt (2026-09-24): je Material ein Grundblock (Gegenstueck zu Endstein),
    // polierter Block samt Treppe, Stufe und Mauer (Gegenstueck zum Purpurblock) neben den Ziegeln,
    // der Saeule und den gemeisselten Ziegeln oben. Astralit und Nihilith kopieren weiter ihren
    // beschichteten Endstein (Astralit leuchtet mit 10); Enderquarz ist die dritte, violette Palette
    // aus Astralit, Nihilith und Quarz, ohne Leuchten.
    public static final Block ASTRALIT_BLOCK = registerBlock("astralit_block", ASTRAL_END_STONE, Block::new);
    public static final Block POLISHED_ASTRALIT = registerBlock("polished_astralit", ASTRAL_END_STONE, Block::new);
    public static final Block POLISHED_ASTRALIT_STAIRS = registerBlock("polished_astralit_stairs", POLISHED_ASTRALIT, s -> new StairBlock(POLISHED_ASTRALIT.defaultBlockState(), s));
    public static final Block POLISHED_ASTRALIT_SLAB = registerBlock("polished_astralit_slab", POLISHED_ASTRALIT, SlabBlock::new);
    public static final Block POLISHED_ASTRALIT_WALL = registerBlock("polished_astralit_wall", POLISHED_ASTRALIT, s -> new WallBlock(s.forceSolidOn()));
    public static final Block NIHILITH_BLOCK = registerBlock("nihilith_block", NIHIL_END_STONE, Block::new);
    public static final Block POLISHED_NIHILITH = registerBlock("polished_nihilith", NIHIL_END_STONE, Block::new);
    public static final Block POLISHED_NIHILITH_STAIRS = registerBlock("polished_nihilith_stairs", POLISHED_NIHILITH, s -> new StairBlock(POLISHED_NIHILITH.defaultBlockState(), s));
    public static final Block POLISHED_NIHILITH_SLAB = registerBlock("polished_nihilith_slab", POLISHED_NIHILITH, SlabBlock::new);
    public static final Block POLISHED_NIHILITH_WALL = registerBlock("polished_nihilith_wall", POLISHED_NIHILITH, s -> new WallBlock(s.forceSolidOn()));
    public static final Block ENDER_QUARTZ_BLOCK = registerBlock("ender_quartz_block", NIHIL_END_STONE, s -> new Block(s.mapColor(MapColor.COLOR_PURPLE)));
    public static final Block ENDER_QUARTZ_BRICKS = registerBlock("ender_quartz_bricks", ENDER_QUARTZ_BLOCK, Block::new);
    public static final Block ENDER_QUARTZ_BRICK_STAIRS = registerBlock("ender_quartz_brick_stairs", ENDER_QUARTZ_BRICKS, s -> new StairBlock(ENDER_QUARTZ_BRICKS.defaultBlockState(), s));
    public static final Block ENDER_QUARTZ_BRICK_SLAB = registerBlock("ender_quartz_brick_slab", ENDER_QUARTZ_BRICKS, SlabBlock::new);
    public static final Block ENDER_QUARTZ_BRICK_WALL = registerBlock("ender_quartz_brick_wall", ENDER_QUARTZ_BRICKS, s -> new WallBlock(s.forceSolidOn()));
    public static final Block POLISHED_ENDER_QUARTZ = registerBlock("polished_ender_quartz", ENDER_QUARTZ_BLOCK, Block::new);
    public static final Block POLISHED_ENDER_QUARTZ_STAIRS = registerBlock("polished_ender_quartz_stairs", POLISHED_ENDER_QUARTZ, s -> new StairBlock(POLISHED_ENDER_QUARTZ.defaultBlockState(), s));
    public static final Block POLISHED_ENDER_QUARTZ_SLAB = registerBlock("polished_ender_quartz_slab", POLISHED_ENDER_QUARTZ, SlabBlock::new);
    public static final Block POLISHED_ENDER_QUARTZ_WALL = registerBlock("polished_ender_quartz_wall", POLISHED_ENDER_QUARTZ, s -> new WallBlock(s.forceSolidOn()));
    public static final Block ENDER_QUARTZ_PILLAR = registerBlock("ender_quartz_pillar", ENDER_QUARTZ_BLOCK, RotatedPillarBlock::new);
    public static final Block CHISELED_ENDER_QUARTZ_BRICKS = registerBlock("chiseled_ender_quartz_bricks", ENDER_QUARTZ_BLOCK, Block::new);
    // Enderquarz-Schachbrett: wie die uebrigen Quarz-Schachbretter (Saeulenblock, keine Spawns),
    // Eigenschaften vom Enderquarzblock.
    public static final Block ENDER_QUARTZ_CHECKER = registerBlock("ender_quartz_checker", ENDER_QUARTZ_BLOCK,
            s -> new RotatedPillarBlock(s.isValidSpawn((state, world, pos, type) -> false)));
    // Wie quartz_stairs/quartz_slab am Quarzblock: Treppe und Stufe direkt am Grundblock (2026-09-25).
    public static final Block ENDER_QUARTZ_STAIRS = registerBlock("ender_quartz_stairs", ENDER_QUARTZ_BLOCK, s -> new StairBlock(ENDER_QUARTZ_BLOCK.defaultBlockState(), s));
    public static final Block ENDER_QUARTZ_SLAB = registerBlock("ender_quartz_slab", ENDER_QUARTZ_BLOCK, SlabBlock::new);

    /**
     * Eine End-Palette nach dem Vorbild von Endstein und Purpur: Grundblock, Ziegel samt Treppe,
     * Stufe und Mauer, polierter Block samt Treppe, Stufe und Mauer, Saeule und gemeisselte Ziegel.
     * {@code blockStairs}/{@code blockSlab} sind Treppe und Stufe direkt am Grundblock, wie
     * {@code quartz_stairs}/{@code quartz_slab} am Quarzblock; nur Enderquarz hat sie, sonst null.
     * Datagen (Modelle, Tags, Beute, Rezepte) und die Spieltests laufen ueber {@link #END_PALETTES},
     * damit keine der drei Paletten an einer Stelle vergessen wird.
     */
    public record EndPalette(String material, Block block, Block bricks, Block brickStairs, Block brickSlab,
                             Block brickWall, Block polished, Block polishedStairs, Block polishedSlab,
                             Block polishedWall, Block pillar, Block chiseled, Block blockStairs, Block blockSlab) {
        /** Alle Bloecke in der Reihenfolge des Kreativ-Tabs (elf, bei Enderquarz dreizehn). */
        public List<Block> blocks() {
            List<Block> out = new java.util.ArrayList<>(List.of(block));
            if (blockStairs != null) {
                out.add(blockStairs);
                out.add(blockSlab);
            }
            out.addAll(List.of(bricks, brickStairs, brickSlab, brickWall, polished, polishedStairs, polishedSlab,
                    polishedWall, pillar, chiseled));
            return List.copyOf(out);
        }

        public List<Block> stairs() {
            return blockStairs == null ? List.of(brickStairs, polishedStairs) : List.of(blockStairs, brickStairs, polishedStairs);
        }

        public List<Block> slabs() {
            return blockSlab == null ? List.of(brickSlab, polishedSlab) : List.of(blockSlab, brickSlab, polishedSlab);
        }

        public List<Block> walls() {
            return List.of(brickWall, polishedWall);
        }
    }

    public static final EndPalette ASTRALIT_PALETTE = new EndPalette("astralit", ASTRALIT_BLOCK, ASTRALIT_BRICKS,
            ASTRALIT_BRICK_STAIRS, ASTRALIT_BRICK_SLAB, ASTRALIT_BRICK_WALL, POLISHED_ASTRALIT, POLISHED_ASTRALIT_STAIRS,
            POLISHED_ASTRALIT_SLAB, POLISHED_ASTRALIT_WALL, ASTRALIT_PILLAR, CHISELED_ASTRALIT_BRICKS, null, null);
    public static final EndPalette NIHILITH_PALETTE = new EndPalette("nihilith", NIHILITH_BLOCK, NIHILITH_BRICKS,
            NIHILITH_BRICK_STAIRS, NIHILITH_BRICK_SLAB, NIHILITH_BRICK_WALL, POLISHED_NIHILITH, POLISHED_NIHILITH_STAIRS,
            POLISHED_NIHILITH_SLAB, POLISHED_NIHILITH_WALL, NIHILITH_PILLAR, CHISELED_NIHILITH_BRICKS, null, null);
    public static final EndPalette ENDER_QUARTZ_PALETTE = new EndPalette("ender_quartz", ENDER_QUARTZ_BLOCK, ENDER_QUARTZ_BRICKS,
            ENDER_QUARTZ_BRICK_STAIRS, ENDER_QUARTZ_BRICK_SLAB, ENDER_QUARTZ_BRICK_WALL, POLISHED_ENDER_QUARTZ,
            POLISHED_ENDER_QUARTZ_STAIRS, POLISHED_ENDER_QUARTZ_SLAB, POLISHED_ENDER_QUARTZ_WALL, ENDER_QUARTZ_PILLAR,
            CHISELED_ENDER_QUARTZ_BRICKS, ENDER_QUARTZ_STAIRS, ENDER_QUARTZ_SLAB);
    public static final List<EndPalette> END_PALETTES = List.of(ASTRALIT_PALETTE, NIHILITH_PALETTE, ENDER_QUARTZ_PALETTE);


    // Beide schwebenden Bloecke haben eine volle Kollisionsbox: Man steht auf ihnen, Gegenstaende
    // bleiben liegen und aufsteigende Bloecke landen darunter. Bis 2026-09-24 war Sand noCollision().
    public static final Block SUSPENDED_SAND = registerBlock("suspended_sand", unused -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).setId(keyOf("suspended_sand"))));
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
                .pushReaction(McVersion.PUSH_DESTROYS);
    }

    private static ResourceKey<Block> keyOf(String name) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name));
    }
}
