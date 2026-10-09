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
    public static final Block SILENT_DANDELION = McVersion.SILENT_DANDELION
            ? registerBlock("silent_dandelion", Blocks.DANDELION, s -> new net.minecraft.world.level.block.FlowerBlock(
                    net.minecraft.world.effect.MobEffects.SATURATION, 0.35F, s)) : null;
    public static final Block POTTED_SILENT_DANDELION = McVersion.SILENT_DANDELION
            ? registerBlock("potted_silent_dandelion", Blocks.POTTED_DANDELION,
                    s -> new net.minecraft.world.level.block.FlowerPotBlock(SILENT_DANDELION, s)) : null;
    public static final Block NIHIL_REDSTONE = McVersion.END_SYSTEMS ? registerBlock("nihil_redstone", s -> new com.simplebuilding.blocks.custom.EndSignalPowderBlock(false, s.strength(0.2F).noOcclusion().isRedstoneConductor((state, world, pos) -> false).sound(SoundType.AMETHYST).lightLevel(state -> state.getValue(EndSignalBlock.POWER) > 0 ? 3 : 0))) : null;
    public static final Block NIHILITH_SWITCH = McVersion.END_SYSTEMS ? registerBlock("nihilith_switch", s -> new EndSignalBlock(false, EndSignalBlock.Kind.SWITCH, s.strength(0.2F).noOcclusion().isRedstoneConductor((state, world, pos) -> false).sound(SoundType.AMETHYST).lightLevel(state -> state.getValue(EndSignalBlock.POWER) > 0 ? 3 : 0))) : null;
    public static final Block NIHILITH_LAMP = McVersion.END_SYSTEMS ? registerBlock("nihilith_lamp", s -> new EndSignalBlock(false, EndSignalBlock.Kind.LAMP, s.strength(0.2F).noOcclusion().isRedstoneConductor((state, world, pos) -> false).sound(SoundType.AMETHYST).lightLevel(state -> state.getValue(EndSignalBlock.POWER) > 0 ? 12 : 0))) : null;
    public static final Block ASTRAL_REDSTONE = McVersion.END_SYSTEMS ? registerBlock("astral_redstone", s -> new com.simplebuilding.blocks.custom.EndSignalPowderBlock(true, s.strength(0.2F).noOcclusion().isRedstoneConductor((state, world, pos) -> false).sound(SoundType.AMETHYST).lightLevel(state -> state.getValue(EndSignalBlock.POWER) > 0 ? 3 : 0))) : null;
    public static final Block ASTRALIT_SWITCH = McVersion.END_SYSTEMS ? registerBlock("astralit_switch", s -> new EndSignalBlock(true, EndSignalBlock.Kind.SWITCH, s.strength(0.2F).noOcclusion().isRedstoneConductor((state, world, pos) -> false).sound(SoundType.AMETHYST).lightLevel(state -> state.getValue(EndSignalBlock.POWER) > 0 ? 3 : 0))) : null;
    public static final Block ASTRALIT_LAMP = McVersion.END_SYSTEMS ? registerBlock("astralit_lamp", s -> new EndSignalBlock(true, EndSignalBlock.Kind.LAMP, s.strength(0.2F).noOcclusion().isRedstoneConductor((state, world, pos) -> false).sound(SoundType.AMETHYST).lightLevel(state -> state.getValue(EndSignalBlock.POWER) > 0 ? 12 : 0))) : null;
    // Astral-/Nihil-Kolben (2026-10-03, docs/ai/PLAN-ASTRAL-KOLBEN-2026-10-02.md): Empfaenger des eigenen Kanals wie
    // die Lampe, bewegen beim Einschalten je einen Block in alle 6 Richtungen; selbst unverschiebbar.
    public static final Block NIHIL_PISTON = McVersion.END_SYSTEMS ? registerBlock("nihil_piston", s -> new com.simplebuilding.blocks.custom.EndPistonBlock(false, s.strength(1.5F).sound(SoundType.AMETHYST).isRedstoneConductor((state, world, pos) -> false).pushReaction(McVersion.PUSH_BLOCKED))) : null;
    public static final Block ASTRAL_PISTON = McVersion.END_SYSTEMS ? registerBlock("astral_piston", s -> new com.simplebuilding.blocks.custom.EndPistonBlock(true, s.strength(1.5F).sound(SoundType.AMETHYST).isRedstoneConductor((state, world, pos) -> false).pushReaction(McVersion.PUSH_BLOCKED))) : null;
    // Astral-/Nihil-Schienen (2026-10-04, docs/ai/PLAN-ASTRAL-NIHIL-SCHIENEN-2026-10-02.md): gerade Schienen mit den
    // Eigenschaften der Antriebsschiene, gespeist ueber den eigenen End-Kanal; Physik in EndRailPhysics.
    public static final Block NIHIL_RAIL = McVersion.END_RAILS ? registerBlock("nihil_rail", Blocks.POWERED_RAIL, s -> new com.simplebuilding.blocks.custom.EndRailBlock(false, s)) : null;
    public static final Block ASTRAL_RAIL = McVersion.END_RAILS ? registerBlock("astral_rail", Blocks.POWERED_RAIL, s -> new com.simplebuilding.blocks.custom.EndRailBlock(true, s)) : null;
    public static final Block ASTRAL_VAULT = McVersion.END_SYSTEMS ? registerBlock("astral_vault", Blocks.ENDER_CHEST, s -> new AstralVaultBlock(s.strength(50.0F, 1200.0F))) : null;
    // Nihil-Gewoelbe (2026-10-04, docs/ai/PLAN-NIHIL-GEWOELBE-2026-10-02.md): eine weltweit geteilte Endertruhe.
    public static final Block NIHIL_VAULT = McVersion.END_SYSTEMS ? registerBlock("nihil_vault", Blocks.ENDER_CHEST, s -> new com.simplebuilding.blocks.custom.NihilVaultBlock(s.strength(50.0F, 1200.0F))) : null;




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
    // Die Koepfe der Kolben: Eigenschaften von Vanillas Kolbenkopf (unverschiebbar, keine Beute),
    // Haerte, Explosionswiderstand und Klang wie die Basis ihrer Stufe. Kein Item, wie minecraft:piston_head.
    public static final Block REINFORCED_PISTON_HEAD = registerBlock("reinforced_piston_head", Blocks.PISTON_HEAD, s -> new ModPistonHeadBlock(ModPistonHeadBlock.Tier.REINFORCED, s.strength(1.5F).noLootTable().sound(SoundType.METAL)));
    public static final Block NETHERITE_PISTON_HEAD = registerBlock("netherite_piston_head", Blocks.PISTON_HEAD, s -> new ModPistonHeadBlock(ModPistonHeadBlock.Tier.NETHERITE, s.strength(5.0F, 1200.0F).noLootTable().sound(SoundType.NETHERITE_BLOCK)));
    public static final Block ENDERITE_PISTON_HEAD = registerBlock("enderite_piston_head", Blocks.PISTON_HEAD, s -> new ModPistonHeadBlock(ModPistonHeadBlock.Tier.ENDERITE, s.strength(6.0F, 1500.0F).noLootTable().sound(SoundType.NETHERITE_BLOCK)));

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

    // Truhen-Stufen ueber der Vanilla-Kupfertruhe (siehe ChestTier, TieredChests). Haerte wie die
    // Maschinen derselben Stufe, abbaubar mit der Spitzhacke wie die Kupfertruhe (Werkzeug noetig).
    public static final Block REINFORCED_CHEST = registerBlock("reinforced_chest", Blocks.IRON_BLOCK, s -> new TieredChestBlock(ChestTier.REINFORCED,
            net.minecraft.sounds.SoundEvents.COPPER_CHEST_OPEN, net.minecraft.sounds.SoundEvents.COPPER_CHEST_CLOSE,
            s.strength(3.5F, 6.0F).sound(SoundType.COPPER).mapColor(MapColor.METAL)));
    public static final Block NETHERITE_CHEST = registerBlock("netherite_chest", Blocks.IRON_BLOCK, s -> new TieredChestBlock(ChestTier.NETHERITE,
            s.strength(5.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK).mapColor(MapColor.COLOR_BLACK)));
    public static final Block ENDERITE_CHEST = registerBlock("enderite_chest", Blocks.IRON_BLOCK, s -> new TieredChestBlock(ChestTier.ENDERITE,
            s.strength(6.0F, 1500.0F).sound(SoundType.NETHERITE_BLOCK).mapColor(MapColor.COLOR_PURPLE)));

    public static final Block REINFORCED_TRAPPED_CHEST = McVersion.TRAPPED_TIERED_CHESTS
            ? registerBlock("reinforced_trapped_chest", REINFORCED_CHEST,
                    s -> new com.simplebuilding.blocks.custom.TieredTrappedChestBlock((TieredChestBlock) REINFORCED_CHEST, s)) : null;
    public static final Block NETHERITE_TRAPPED_CHEST = McVersion.TRAPPED_TIERED_CHESTS
            ? registerBlock("netherite_trapped_chest", NETHERITE_CHEST,
                    s -> new com.simplebuilding.blocks.custom.TieredTrappedChestBlock((TieredChestBlock) NETHERITE_CHEST, s)) : null;
    public static final Block ENDERITE_TRAPPED_CHEST = McVersion.TRAPPED_TIERED_CHESTS
            ? registerBlock("enderite_trapped_chest", ENDERITE_CHEST,
                    s -> new com.simplebuilding.blocks.custom.TieredTrappedChestBlock((TieredChestBlock) ENDERITE_CHEST, s)) : null;

    /** Every registered block sharing the tier chest entity, without nulls on older lines. */
    public static Block[] tieredChests() {
        return McVersion.TRAPPED_TIERED_CHESTS
                ? new Block[]{REINFORCED_CHEST, NETHERITE_CHEST, ENDERITE_CHEST,
                        REINFORCED_TRAPPED_CHEST, NETHERITE_TRAPPED_CHEST, ENDERITE_TRAPPED_CHEST}
                : new Block[]{REINFORCED_CHEST, NETHERITE_CHEST, ENDERITE_CHEST};
    }

    // Shulkerkisten-Stufen ueber der Vanilla-Shulkerkiste (siehe TieredShulkerBoxes): Plaetze und
    // Stapelfaktor der Truhen derselben Stufe. Aus Vanillas Shulkerkiste kopiert (dynamische Form,
    // keine Verdeckung, vom Kolben zerstoert und mit Inhalt fallen gelassen, kein Werkzeug noetig),
    // fester und explosionsfester wie die Truhen; offen nicht erstickend.
    public static final Block REINFORCED_SHULKER_BOX = registerBlock("reinforced_shulker_box", Blocks.SHULKER_BOX,
            s -> new TieredShulkerBoxBlock(ChestTier.REINFORCED, shulkerBoxProperties(s).strength(3.0F, 6.0F).mapColor(MapColor.METAL)));
    public static final Block NETHERITE_SHULKER_BOX = registerBlock("netherite_shulker_box", Blocks.SHULKER_BOX,
            s -> new TieredShulkerBoxBlock(ChestTier.NETHERITE, shulkerBoxProperties(s).strength(4.0F, 1200.0F)
                    .sound(SoundType.NETHERITE_BLOCK).mapColor(MapColor.COLOR_BLACK)));
    public static final Block ENDERITE_SHULKER_BOX = registerBlock("enderite_shulker_box", Blocks.SHULKER_BOX,
            s -> new TieredShulkerBoxBlock(ChestTier.ENDERITE, shulkerBoxProperties(s).strength(5.0F, 1500.0F)
                    .sound(SoundType.NETHERITE_BLOCK).mapColor(MapColor.COLOR_PURPLE)));

    // --- 5. RUCKSAECKE (abgestellt) ---
    // Aus der Glas-Vorlage (keine Verdeckung, kein Ersticken, kein Redstone-Leiter - passend zur
    // kleinen Form), dann Wolle-Klang, weich wie Wolle und von Kolben zerstoert statt geschoben.
    // Netherit und Enderit halten Explosionen aus wie ihre Items.
    public static final Block BACKPACK = registerBlock("backpack", s -> new BackpackBlock(BackpackTier.BASIC, backpackProperties(s, false)));
    public static final Block REINFORCED_BACKPACK = registerBlock("reinforced_backpack", s -> new BackpackBlock(BackpackTier.REINFORCED, backpackProperties(s, false)));
    public static final Block NETHERITE_BACKPACK = registerBlock("netherite_backpack", s -> new BackpackBlock(BackpackTier.NETHERITE, backpackProperties(s, true)));
    public static final Block ENDERITE_BACKPACK = registerBlock("enderite_backpack", s -> new BackpackBlock(BackpackTier.ENDERITE, backpackProperties(s, true)));

    // --- 6. ABGELEGTE SCHMIEDEVORLAGE ---
    // Schleichen + Rechtsklick mit einer Vorlage (PlacedTemplates). Kein Item: die Vorlage selbst
    // liegt in der Block-Entity und faellt beim Abbauen wieder heraus (getDrops, keine Loot-Tabelle).
    // Ohne Kollision wie eine Druckplatte, von Kolben zerstoert, mit Metallklang.
    public static final Block PLACED_SMITHING_TEMPLATE = registerBlock("placed_smithing_template", s -> new PlacedTemplateBlock(s
            .strength(0.5F).sound(SoundType.METAL).noCollision().noLootTable().mapColor(MapColor.NONE)
            .pushReaction(McVersion.PUSH_DESTROYS)));

    // Abgelegte Blaupause: derselbe Block (Klasse, Block-Entity, Renderer), eigener Name und eigene Id.
    public static final Block PLACED_BLUEPRINT = registerBlock("placed_blueprint", s -> new PlacedTemplateBlock(s
            .strength(0.5F).sound(SoundType.WOOL).noCollision().noLootTable().mapColor(MapColor.NONE)
            .pushReaction(McVersion.PUSH_DESTROYS)));

    // Gelegtes Ei (2026-10-02): Schleichen + Rechtsklick mit einem Ei (PlacedEggs). Kein Item, keine Loot-Tabelle -
    // Behutsamkeit gibt das Ei zurueck, sonst zerbricht es und schluepft wie ein geworfenes (PlacedEggBlock).
    public static final Block PLACED_EGG = McVersion.SMALL_PLACEABLES ? registerBlock("placed_egg", s -> new com.simplebuilding.blocks.custom.PlacedEggBlock(s
            .strength(0.0F).sound(SoundType.BONE_BLOCK).noLootTable().noOcclusion().mapColor(MapColor.SAND)
            .pushReaction(McVersion.PUSH_DESTROYS))) : null;

    // Kleinteile auf einem Fleck (2026-10-02, wie Seegurken): bis zu 4 Kiesel, Splitter, Vanilla-Kleinteile und Eier
    // gemischt (PlacedSmallParts). Kein Item, keine Loot-Tabelle - die Teile liegen in der Block-Entity.
    /**
     * Lautsprecher (2026-10-03, McVersion.MUSIC_DISCS): Holz wie der Notenblock; Astralit verstaerkt angrenzende
     * Plattenspieler, Nihilit angrenzende Notenbloecke ({@link com.simplebuilding.util.SpeakerBoost}).
     */
    public static final Block JUKEBOX_AMPLIFIER = McVersion.MUSIC_DISCS ? registerBlock("jukebox_amplifier", Blocks.NOTE_BLOCK,
            s -> new com.simplebuilding.blocks.custom.SpeakerBlock(com.simplebuilding.util.SpeakerBoost.Source.JUKEBOX, s)) : null;
    public static final Block NOTE_AMPLIFIER = McVersion.MUSIC_DISCS ? registerBlock("note_amplifier", Blocks.NOTE_BLOCK,
            s -> new com.simplebuilding.blocks.custom.SpeakerBlock(com.simplebuilding.util.SpeakerBoost.Source.NOTE_BLOCK, s)) : null;

    /** Auto-Schmied: der Crafter des Schmiedetischs (2026-10-02, McVersion.AUTO_SMITHER). Eigenschaften wie der Crafter. */
    public static final Block AUTO_SMITHER = McVersion.AUTO_SMITHER
            ? registerBlock("auto_smither", Blocks.CRAFTER, com.simplebuilding.blocks.custom.AutoSmitherBlock::new) : null;
    /** Autonomer Crafter: craftet selbst in den Trichter darunter (2026-10-09, McVersion.AUTONOMOUS_CRAFTER). Wie der Crafter. */
    public static final Block AUTONOMOUS_CRAFTER = McVersion.AUTONOMOUS_CRAFTER
            ? registerBlock("autonomous_crafter", Blocks.CRAFTER, com.simplebuilding.blocks.custom.AutonomousCrafterBlock::new) : null;
    // Astral-Verzauberung (2026-10-09, N27, docs/ai/KONZEPT-ASTRAL-VERZAUBERUNG-2026-10-09.md, McVersion.ASTRAL_ENCHANTING).
    /** Astral-Verzauberungstisch: wie der Verzauberungstisch, doppelt so lange abzubauen (Haerte 10 statt 5). */
    public static final Block ASTRAL_ENCHANTING_TABLE = McVersion.ASTRAL_ENCHANTING ? registerBlock("astral_enchanting_table",
            Blocks.ENCHANTING_TABLE, s -> new com.simplebuilding.enchanting.AstralEnchantingTableBlock(s
                    .strength(com.simplebuilding.enchanting.AstralEnchantingTableBlock.HARDNESS, 1200.0F).lightLevel(state -> 10))) : null;
    /** Lohenholz je Nether-Holzart: 8 Lohenstaub um ein Brett. */
    public static final Block CRIMSON_BLAZEWOOD_PLANKS = McVersion.ASTRAL_ENCHANTING
            ? registerBlock("crimson_blazewood_planks", Blocks.CRIMSON_PLANKS, Block::new) : null;
    public static final Block WARPED_BLAZEWOOD_PLANKS = McVersion.ASTRAL_ENCHANTING
            ? registerBlock("warped_blazewood_planks", Blocks.WARPED_PLANKS, Block::new) : null;
    /** Lohen-Buecherregale: zaehlen am Astral-Tisch doppelt (am Vanilla-Tisch wie ein Regal). */
    public static final Block CRIMSON_BLAZEWOOD_BOOKSHELF = McVersion.ASTRAL_ENCHANTING
            ? registerBlock("crimson_blazewood_bookshelf", Blocks.BOOKSHELF, s -> new Block(s.sound(SoundType.NETHER_WOOD))) : null;
    public static final Block WARPED_BLAZEWOOD_BOOKSHELF = McVersion.ASTRAL_ENCHANTING
            ? registerBlock("warped_blazewood_bookshelf", Blocks.BOOKSHELF, s -> new Block(s.sound(SoundType.NETHER_WOOD))) : null;
    public static final List<Block> BLAZEWOOD_PLANKS = McVersion.ASTRAL_ENCHANTING
            ? List.of(CRIMSON_BLAZEWOOD_PLANKS, WARPED_BLAZEWOOD_PLANKS) : List.of();
    public static final List<Block> BLAZEWOOD_BOOKSHELVES = McVersion.ASTRAL_ENCHANTING
            ? List.of(CRIMSON_BLAZEWOOD_BOOKSHELF, WARPED_BLAZEWOOD_BOOKSHELF) : List.of();
    /** Lohen-Obsidian: leuchtende Variante des weinenden Obsidians, 5x5 unter dem Astral-Tisch fuer Stufe 40/50. */
    public static final Block BLAZING_OBSIDIAN = McVersion.ASTRAL_ENCHANTING ? registerBlock("blazing_obsidian", Blocks.CRYING_OBSIDIAN,
            s -> new com.simplebuilding.enchanting.BlazingObsidianBlock(s.lightLevel(state -> 12))) : null;
    /** Werkbank mit Lager (2026-10-09, McVersion.STORAGE_CRAFTING_TABLE): Eigenschaften wie die Werkbank. */
    public static final Block STORAGE_CRAFTING_TABLE = McVersion.STORAGE_CRAFTING_TABLE
            ? registerBlock("storage_crafting_table", Blocks.CRAFTING_TABLE, com.simplebuilding.blocks.custom.StorageCraftingTableBlock::new) : null;
    public static final Block PLACED_SMALL_PARTS = McVersion.SMALL_PLACEABLES ? registerBlock("placed_small_parts", s -> new com.simplebuilding.blocks.custom.PlacedSmallPartsBlock(s
            .strength(0.2F).sound(SoundType.STONE).noCollision().noLootTable().noOcclusion().mapColor(MapColor.NONE)
            .lightLevel(com.simplebuilding.blocks.custom.PlacedSmallPartsBlock::light)
            .pushReaction(McVersion.PUSH_DESTROYS))) : null;

    // Eisenstab (2026-10-02): Blitzableiter aus Eisen, zieht Blitze nur in 32 Bloecken an (MetalRodBlock).
    public static final Block IRON_ROD = McVersion.GADGET_REWORK ? registerBlock("iron_rod", s -> new com.simplebuilding.blocks.custom.MetalRodBlock(
            com.simplebuilding.blocks.custom.MetalRodBlock.IRON_RANGE, s
            .mapColor(MapColor.METAL).forceSolidOn().requiresCorrectToolForDrops().strength(5.0F, 6.0F)
            .sound(SoundType.METAL).noOcclusion())) : null;
    // Goldstab (2026-10-02): Blitzableiter aus Gold, zieht Blitze in 64 Bloecken an (MetalRodBlock).
    public static final Block GOLD_ROD = McVersion.GADGET_REWORK ? registerBlock("gold_rod", s -> new com.simplebuilding.blocks.custom.MetalRodBlock(
            com.simplebuilding.blocks.custom.MetalRodBlock.GOLD_RANGE, s
            .mapColor(MapColor.GOLD).forceSolidOn().requiresCorrectToolForDrops().strength(3.0F, 6.0F)
            .sound(SoundType.METAL).noOcclusion())) : null;
    // Netherit- und Enderitstab (Besitzer 2026-10-03): Blitzableiter wie Eisen-/Goldstab, Reichweite 96 bzw. 128;
    // Haerte, Klang und Kartenfarbe wie der Netherit- bzw. Enderitblock.
    public static final Block NETHERITE_ROD = McVersion.GADGET_REWORK ? registerBlock("netherite_rod", s -> new com.simplebuilding.blocks.custom.MetalRodBlock(
            com.simplebuilding.blocks.custom.MetalRodBlock.NETHERITE_RANGE, s
            .mapColor(MapColor.COLOR_BLACK).forceSolidOn().requiresCorrectToolForDrops().strength(50.0F, 1200.0F)
            .sound(SoundType.NETHERITE_BLOCK).noOcclusion())) : null;
    public static final Block ENDERITE_ROD = McVersion.GADGET_REWORK ? registerBlock("enderite_rod", s -> new com.simplebuilding.blocks.custom.MetalRodBlock(
            com.simplebuilding.blocks.custom.MetalRodBlock.ENDERITE_RANGE, s
            .mapColor(MapColor.COLOR_BLACK).forceSolidOn().requiresCorrectToolForDrops().strength(50.0F, 1200.0F)
            .sound(SoundType.NETHERITE_BLOCK).noOcclusion())) : null;

    // Aufgestellte Staebe (2026-10-04, McVersion.STANDING_RODS): Stock, Knochen, Lohen-/Boeen-/Diamantstab senkrecht;
    // ein Block, der Zustand rod waehlt Modell, Licht, Klang und Drop (StandingRodBlock). Kein Block-Item.
    public static final Block STANDING_ROD = McVersion.STANDING_RODS ? registerBlock("standing_rod", s -> new com.simplebuilding.blocks.custom.StandingRodBlock(s
            .strength(0.3F).noLootTable().noOcclusion().mapColor(MapColor.NONE)
            .lightLevel(com.simplebuilding.blocks.custom.StandingRodBlock::light)
            .pushReaction(McVersion.PUSH_DESTROYS))) : null;

    // Ziegenhorn-Halter (Queue N24, 2026-10-09): abgelegtes Ziegenhorn, haelt eine Fackel oder einen Stab (GoatHornHolderBlock).
    // Kein Block-Item: das Horn selbst wird abgelegt und faellt wieder heraus.
    public static final Block GOAT_HORN_HOLDER = McVersion.SMALL_PLACEABLES ? registerBlock("goat_horn_holder", s -> new com.simplebuilding.blocks.custom.GoatHornHolderBlock(s
            .strength(0.5F).sound(SoundType.BONE_BLOCK).noLootTable().noOcclusion().mapColor(MapColor.TERRACOTTA_WHITE)
            .lightLevel(com.simplebuilding.blocks.custom.GoatHornHolderBlock::light)
            .pushReaction(McVersion.PUSH_DESTROYS))) : null;

    // Haengematten (2026-10-02, McVersion.HAMMOCK): 16 Farben wie Betten, haengen zwischen zwei Ankern (HammockLayout).
    // Untere Lage = Tuch, obere = Seile; nur das untere Kopfteil hat Beute (das Item). Bei 3 Bloecken Abstand
    // ueberbrueckt das Seilstueck (ohne Item/Beute) den Rest bis zum Kopf-Anker.
    public static final Block HAMMOCK_ROPE = McVersion.HAMMOCK ? registerBlock("hammock_rope", s -> new com.simplebuilding.blocks.custom.HammockRopeBlock(s
            .strength(0.2F).sound(SoundType.WOOL).noCollision().noOcclusion().noLootTable().mapColor(MapColor.WOOL)
            .pushReaction(McVersion.PUSH_DESTROYS))) : null;
    public static final Block WHITE_HAMMOCK = hammock(net.minecraft.world.item.DyeColor.WHITE);
    public static final Block ORANGE_HAMMOCK = hammock(net.minecraft.world.item.DyeColor.ORANGE);
    public static final Block MAGENTA_HAMMOCK = hammock(net.minecraft.world.item.DyeColor.MAGENTA);
    public static final Block LIGHT_BLUE_HAMMOCK = hammock(net.minecraft.world.item.DyeColor.LIGHT_BLUE);
    public static final Block YELLOW_HAMMOCK = hammock(net.minecraft.world.item.DyeColor.YELLOW);
    public static final Block LIME_HAMMOCK = hammock(net.minecraft.world.item.DyeColor.LIME);
    public static final Block PINK_HAMMOCK = hammock(net.minecraft.world.item.DyeColor.PINK);
    public static final Block GRAY_HAMMOCK = hammock(net.minecraft.world.item.DyeColor.GRAY);
    public static final Block LIGHT_GRAY_HAMMOCK = hammock(net.minecraft.world.item.DyeColor.LIGHT_GRAY);
    public static final Block CYAN_HAMMOCK = hammock(net.minecraft.world.item.DyeColor.CYAN);
    public static final Block PURPLE_HAMMOCK = hammock(net.minecraft.world.item.DyeColor.PURPLE);
    public static final Block BLUE_HAMMOCK = hammock(net.minecraft.world.item.DyeColor.BLUE);
    public static final Block BROWN_HAMMOCK = hammock(net.minecraft.world.item.DyeColor.BROWN);
    public static final Block GREEN_HAMMOCK = hammock(net.minecraft.world.item.DyeColor.GREEN);
    public static final Block RED_HAMMOCK = hammock(net.minecraft.world.item.DyeColor.RED);
    public static final Block BLACK_HAMMOCK = hammock(net.minecraft.world.item.DyeColor.BLACK);
    /** Alle Haengematten in der Farbreihenfolge der Kreativ-Tabs (wie Vanillas Betten); leer ohne McVersion.HAMMOCK. */
    public static final List<Block> HAMMOCKS = McVersion.HAMMOCK ? List.of(WHITE_HAMMOCK, LIGHT_GRAY_HAMMOCK, GRAY_HAMMOCK,
            BLACK_HAMMOCK, BROWN_HAMMOCK, RED_HAMMOCK, ORANGE_HAMMOCK, YELLOW_HAMMOCK, LIME_HAMMOCK, GREEN_HAMMOCK, CYAN_HAMMOCK,
            LIGHT_BLUE_HAMMOCK, BLUE_HAMMOCK, PURPLE_HAMMOCK, MAGENTA_HAMMOCK, PINK_HAMMOCK) : List.of();

    /** Die Bloecke mit Haengematten-Block-Entity (HammockBlockEntity): alle Tuchfarben und das Seil; leer ohne McVersion.HAMMOCK. */
    public static Block[] hammockBlockEntityBlocks() {
        if (!McVersion.HAMMOCK) {
            return new Block[0];
        }
        List<Block> blocks = new java.util.ArrayList<>(HAMMOCKS);
        blocks.add(HAMMOCK_ROPE);
        return blocks.toArray(new Block[0]);
    }

    private static Block hammock(net.minecraft.world.item.DyeColor color) {
        if (!McVersion.HAMMOCK) {
            return null;
        }
        return registerBlock(color.getSerializedName() + "_hammock", s -> new com.simplebuilding.blocks.custom.HammockBlock(color, s
                .strength(0.2F).sound(SoundType.WOOL).noOcclusion().mapColor(color.getMapColor())
                .pushReaction(McVersion.PUSH_DESTROYS)));
    }
    // --- 7. ABGESTELLTES BUENDEL ---
    // Schleichen + Rechtsklick mit einem Buendel auf die Oberseite eines Blocks (PlacedBundles): ein
    // 3D-Buendel je Stufe. Kein Item - das Buendel samt Inhalt liegt in der Block-Entity und faellt
    // beim Abbauen wieder heraus (getDrops, keine Loot-Tabelle). Weich wie Wolle, von Kolben zerstoert.
    public static final Block PLACED_BUNDLE = registerBlock("placed_bundle", s -> new com.simplebuilding.blocks.custom.PlacedBundleBlock(s
            .strength(0.3F).sound(SoundType.WOOL).noLootTable().mapColor(MapColor.COLOR_BROWN)
            .pushReaction(McVersion.PUSH_DESTROYS)));


    // --- 1. DECORATION BLOCKS --- // todo add stonecutting and crafting recipie like vanilla
    public static final Block POLISHED_END_STONE = registerBlock("polished_end_stone", unused -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).requiresCorrectToolForDrops().strength(3.0F, 9.0F).sound(SoundType.STONE).setId(keyOf("polished_end_stone"))));

    // Checker Blocks
    public static final Block PURPUR_QUARTZ_CHECKER = registerBlock("purpur_quartz_checker", unused -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.PURPUR_BLOCK).setId(keyOf("purpur_quartz_checker")).isValidSpawn((state, world, pos, type) -> false)));
    public static final Block LAPIS_QUARTZ_CHECKER = registerBlock("lapis_quartz_checker", unused -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LAPIS_BLOCK).setId(keyOf("lapis_quartz_checker")).isValidSpawn((state, world, pos, type) -> false)));
    public static final Block BLACKSTONE_QUARTZ_CHECKER = registerBlock("blackstone_quartz_checker", unused -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_BLACKSTONE).setId(keyOf("blackstone_quartz_checker")).isValidSpawn((state, world, pos, type) -> false)));
    public static final Block RESIN_QUARTZ_CHECKER = registerBlock("resin_quartz_checker", unused -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.RESIN_BRICKS).setId(keyOf("resin_quartz_checker")).isValidSpawn((state, world, pos, type) -> false)));
    public static final Block NETHER_BRICK_QUARTZ_CHECKER = registerBlock("nether_brick_quartz_checker", unused -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_BRICKS).setId(keyOf("nether_brick_quartz_checker")).isValidSpawn((state, world, pos, type) -> false)));
    public static final Block RED_NETHER_BRICK_QUARTZ_CHECKER = registerBlock("red_nether_brick_quartz_checker", unused -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.RED_NETHER_BRICKS).setId(keyOf("red_nether_brick_quartz_checker")).isValidSpawn((state, world, pos, type) -> false)));

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
    // Schachbretter aus den polierten End-Bloecken (Besitzer 2026-10-02): Quarz neben den Kacheln des
    // polierten Blocks, Eigenschaften vom polierten Block; Astralit halb aus Quarz leuchtet wie das
    // Astralit-Quarz-Schachbrett mit 5.
    public static final Block POLISHED_ASTRALIT_CHECKER = registerBlock("polished_astralit_checker", POLISHED_ASTRALIT,
            s -> new RotatedPillarBlock(s.lightLevel(state -> 5).isValidSpawn((state, world, pos, type) -> false)));
    public static final Block POLISHED_NIHILITH_CHECKER = registerBlock("polished_nihilith_checker", POLISHED_NIHILITH,
            s -> new RotatedPillarBlock(s.isValidSpawn((state, world, pos, type) -> false)));
    public static final Block POLISHED_ENDER_QUARTZ_CHECKER = registerBlock("polished_ender_quartz_checker", POLISHED_ENDER_QUARTZ,
            s -> new RotatedPillarBlock(s.isValidSpawn((state, world, pos, type) -> false)));
    // Wie quartz_stairs/quartz_slab am Quarzblock: Treppe und Stufe direkt am Grundblock (2026-09-25).
    public static final Block ENDER_QUARTZ_STAIRS = registerBlock("ender_quartz_stairs", ENDER_QUARTZ_BLOCK, s -> new StairBlock(ENDER_QUARTZ_BLOCK.defaultBlockState(), s));
    // Alternativbloecke (Besitzer 2026-10-03): je drei weitere Grundblock-Muster fuer Astralit und Nihilith
    // (Texturen aus astralit_nihilit_alternates_2026_10_03.py), Eigenschaften vom Grundblock (Astralit leuchtet).
    // Der Grundblock selbst bleibt unveraendert; Gewinnung siehe END_ALTERNATES.
    public static final Block VEINED_ASTRALIT = registerBlock("veined_astralit", ASTRALIT_BLOCK, Block::new);
    public static final Block CRYSTALLINE_ASTRALIT = registerBlock("crystalline_astralit", ASTRALIT_BLOCK, Block::new);
    public static final Block LAYERED_ASTRALIT = registerBlock("layered_astralit", ASTRALIT_BLOCK, Block::new);
    public static final Block VEINED_NIHILITH = registerBlock("veined_nihilith", NIHILITH_BLOCK, Block::new);
    public static final Block CRYSTALLINE_NIHILITH = registerBlock("crystalline_nihilith", NIHILITH_BLOCK, Block::new);
    public static final Block FROSTED_NIHILITH = registerBlock("frosted_nihilith", NIHILITH_BLOCK, Block::new);
    public static final Block ENDER_QUARTZ_SLAB = registerBlock("ender_quartz_slab", ENDER_QUARTZ_BLOCK, SlabBlock::new);

    // --- SCHACH (docs/ai/PLAN-SCHACH-2026-10-06.md, McVersion.CHESS) ---
    /** Treppe und Stufe eines Quarz-Schachbretts (Eigenschaften vom Schachbrett, Astralit leuchtet mit 5). */
    public record CheckerShapes(com.simplebuilding.chess.ChessColor color, Block checker, Block stairs, Block slab) {
    }

    public static final List<CheckerShapes> CHECKER_SHAPES = McVersion.CHESS ? checkerShapes() : List.of();

    private static List<CheckerShapes> checkerShapes() {
        List<CheckerShapes> out = new java.util.ArrayList<>();
        for (com.simplebuilding.chess.ChessColor color : com.simplebuilding.chess.ChessColor.values()) {
            Block checker = color.checker();
            if (checker == null) {
                continue;
            }
            String name = BuiltInRegistries.BLOCK.getKey(checker).getPath();
            Block stairs = registerBlock(name + "_stairs", checker, s -> new StairBlock(checker.defaultBlockState(), s));
            Block slab = registerBlock(name + "_slab", checker, SlabBlock::new);
            out.add(new CheckerShapes(color, checker, stairs, slab));
        }
        return List.copyOf(out);
    }

    /**
     * Achtelbloecke aller Farben in einem Block (Farbe und acht Bits im Zustand); kein Werkzeug noetig, mit der
     * Spitzhacke schneller. Drops aus dem Zustand (CheckerOctetBlock#getDrops), keine Loot-Tabelle.
     */
    public static final Block CHECKER_OCTET = McVersion.CHESS ? registerBlock("checker_octet", unused -> new CheckerOctetBlock(
            BlockBehaviour.Properties.of().setId(keyOf("checker_octet")).mapColor(MapColor.QUARTZ).strength(0.8F)
                    .sound(SoundType.STONE).noLootTable().isValidSpawn((state, world, pos, type) -> false)
                    .lightLevel(state -> state.getValue(CheckerOctetBlock.COLOR).light()))) : null;
    /** Bis zu vier Schachfiguren auf einem Block (Block-Entity, ChessPiecesRenderer); droppt die Figuren. */
    public static final Block CHESS_PIECES = McVersion.CHESS ? registerBlock("chess_pieces", unused -> new ChessPiecesBlock(
            BlockBehaviour.Properties.of().setId(keyOf("chess_pieces")).mapColor(MapColor.NONE).strength(0.3F)
                    .sound(SoundType.STONE).noLootTable().noOcclusion().pushReaction(McVersion.PUSH_DESTROYS))) : null;

    // --- MATERIAL-ACHTEL (Queue Nachtrag 24, docs/ai/PLAN-Q-HAMMER-OCTETS-2026-10-09.md) ---
    /** Holzarten mit Brettern, Treppe und Stufe; was die Version nicht kennt, entfaellt. */
    public static final List<String> OCTET_WOODS = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "mangrove", "cherry", "pale_oak", "poplar", "bamboo", "crimson", "warped");
    /** Je Holzart eine Achtelzelle {@code <holz>_octet} mit den Eigenschaften der Bretter (Klang, Haerte). */
    public static final List<Block> WOOD_OCTETS = McVersion.CHESS ? woodOctets() : List.of();
    /** Melonen-Achtel: eine gesetzte Melonenscheibe (Schleichen + Rechtsklick); droppt Melonenscheiben. */
    public static final Block MELON_OCTET = McVersion.CHESS ? registerBlock("melon_octet", Blocks.MELON,
            s -> new MaterialOctetBlock(Blocks.MELON, () -> net.minecraft.world.item.Items.MELON_SLICE, octetProperties(s))) : null;

    private static List<Block> woodOctets() {
        List<Block> out = new java.util.ArrayList<>();
        for (String wood : OCTET_WOODS) {
            Block planks = BuiltInRegistries.BLOCK.getOptional(Identifier.withDefaultNamespace(wood + "_planks")).orElse(null);
            if (planks == null) {
                continue;
            }
            String name = wood + "_octet";
            Identifier itemId = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name);
            out.add(registerBlock(name, planks, s -> new MaterialOctetBlock(planks,
                    () -> BuiltInRegistries.ITEM.getValue(itemId), octetProperties(s))));
        }
        return List.copyOf(out);
    }

    private static BlockBehaviour.Properties octetProperties(BlockBehaviour.Properties s) {
        return s.noLootTable().isValidSpawn((state, world, pos, type) -> false);
    }

    /** Holz-Achtel brennen wie ihre Bretter (Feuer-Tabelle; Nether-Holz brennt nicht). */
    private static void registerOctetFlammability() {
        for (Block cell : WOOD_OCTETS) {
            if (((MaterialOctetBlock) cell).source().defaultBlockState().ignitedByLava()) {
                ((com.simplebuilding.mixin.FireBlockFlammables) Blocks.FIRE).simplebuilding$setFlammable(cell, 5, 20);
            }
        }
    }

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

    /**
     * Drei Alternativbloecke zu einem Grundblock (Besitzer 2026-10-03), in Kettenreihenfolge. Gewinnung:
     * der Enderit-Meissel fuehrt die Kette der Palette weiter (... gemeisselte Ziegel -> Grundblock -> erster ->
     * zweiter -> dritter; der Spachtel zurueck), der Steinmetz schneidet jeden aus dem Grundblock (1 -> 1), und
     * 4 im Quadrat ergeben 4 des naechsten (erster -> zweiter -> dritter -> Grundblock). Der Grundblock selbst
     * behaelt sein Quadrat-Rezept (-> 4 polierte); ein Meissel-Ring dritter -> Grundblock wuerde den Rueckweg der
     * Palettenkette (Grundblock -> gemeisselte Ziegel) ueberschreiben. Datagen und Tests laufen ueber
     * {@link #END_ALTERNATES}.
     */
    public record EndAlternates(Block base, Block first, Block second, Block third) {
        public List<Block> alternates() {
            return List.of(first, second, third);
        }

        /** Die Quadrat-Kette: jeder Block ergibt 4 des naechsten, der dritte wieder den Grundblock. */
        public List<Block> squareChain() {
            return List.of(first, second, third, base);
        }
    }

    public static final List<EndAlternates> END_ALTERNATES = List.of(
            new EndAlternates(ASTRALIT_BLOCK, VEINED_ASTRALIT, CRYSTALLINE_ASTRALIT, LAYERED_ASTRALIT),
            new EndAlternates(NIHILITH_BLOCK, VEINED_NIHILITH, CRYSTALLINE_NIHILITH, FROSTED_NIHILITH));


    // Beide schwebenden Bloecke haben eine volle Kollisionsbox: Man steht auf ihnen, Gegenstaende
    // bleiben liegen und aufsteigende Bloecke landen darunter. Bis 2026-09-24 war Sand noCollision().
    public static final Block SUSPENDED_SAND = registerBlock("suspended_sand", unused -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).setId(keyOf("suspended_sand"))));
    public static final Block SUSPENDED_GRAVEL = registerBlock("suspended_gravel", unused -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GRAVEL).setId(keyOf("suspended_gravel"))));
    public static final Block LEVITATING_SAND = registerBlock("levitating_sand", unused -> new LevitatingBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).setId(keyOf("levitating_sand"))));
    public static final Block LEVITATING_GRAVEL = registerBlock("levitating_gravel", unused -> new LevitatingBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GRAVEL).setId(keyOf("levitating_gravel"))));

    // --- Naturvarianten (N24/N25, 2026-10-09, nur Hauptlinie): Stufen aus Erde/Gras/Sand/Kies, gemeisseltes Pack-/Blaueis,
    // rissiges Eis, Nautilusschalen-Block, Froschlichter in weiteren Farben. Modelle zeigen auf Vanilla-Texturen, wo es passt.
    public static final Block DIRT_SLAB = McVersion.NATURE_VARIANTS ? registerBlock("dirt_slab", Blocks.DIRT, SlabBlock::new) : null;
    /** Biomgefaerbt wie der Grasblock; ohne Behutsamkeit droppt sie die Erd-Stufe. Breitet sich nicht aus und stirbt nicht ab. */
    public static final Block GRASS_SLAB = McVersion.NATURE_VARIANTS ? registerBlock("grass_slab", Blocks.GRASS_BLOCK, SlabBlock::new) : null;
    /** Fallen wie Sand und Kies ({@link FallingSlabBlock}). */
    public static final Block SAND_SLAB = McVersion.NATURE_VARIANTS ? registerBlock("sand_slab", Blocks.SAND, FallingSlabBlock::new) : null;
    public static final Block GRAVEL_SLAB = McVersion.NATURE_VARIANTS ? registerBlock("gravel_slab", Blocks.GRAVEL, FallingSlabBlock::new) : null;
    public static final Block CHISELED_PACKED_ICE = McVersion.NATURE_VARIANTS ? registerBlock("chiseled_packed_ice", Blocks.PACKED_ICE, Block::new) : null;
    public static final Block CHISELED_BLUE_ICE = McVersion.NATURE_VARIANTS ? registerBlock("chiseled_blue_ice", Blocks.BLUE_ICE, Block::new) : null;
    /** Reisst unter Lebewesen in vier Stufen und wird dann zu Wasser ({@link CrackedIceBlock}). */
    public static final Block CRACKED_ICE = McVersion.NATURE_VARIANTS ? registerBlock("cracked_ice", Blocks.ICE, CrackedIceBlock::new) : null;
    /** Saeule wie der Knochenblock: Stirnseite mit Spirale, Seiten gestreift. */
    public static final Block NAUTILUS_SHELL_BLOCK = McVersion.NATURE_VARIANTS ? registerBlock("nautilus_shell_block", Blocks.BONE_BLOCK,
            s -> new RotatedPillarBlock(s.mapColor(MapColor.TERRACOTTA_WHITE))) : null;
    /** Froschlichter in den Farbkreis-Luecken der drei Vanilla-Farben: rot, cyan, blau. */
    public static final Block SCARLET_FROGLIGHT = McVersion.NATURE_VARIANTS ? froglight("scarlet_froglight", MapColor.COLOR_RED) : null;
    public static final Block AQUA_FROGLIGHT = McVersion.NATURE_VARIANTS ? froglight("aqua_froglight", MapColor.COLOR_CYAN) : null;
    public static final Block AZURE_FROGLIGHT = McVersion.NATURE_VARIANTS ? froglight("azure_froglight", MapColor.COLOR_BLUE) : null;

    private static Block froglight(String name, MapColor color) {
        return registerBlock(name, Blocks.OCHRE_FROGLIGHT, s -> new RotatedPillarBlock(s.mapColor(color)));
    }

    /** Alle Naturvarianten in Tab-Reihenfolge (leer auf Linien ohne {@link McVersion#NATURE_VARIANTS}). */
    public static List<Block> natureVariants() {
        return McVersion.NATURE_VARIANTS ? List.of(DIRT_SLAB, GRASS_SLAB, SAND_SLAB, GRAVEL_SLAB, CRACKED_ICE, CHISELED_PACKED_ICE,
                CHISELED_BLUE_ICE, NAUTILUS_SHELL_BLOCK, SCARLET_FROGLIGHT, AQUA_FROGLIGHT, AZURE_FROGLIGHT) : List.of();
    }

    public static final Block ENDERITE_BLOCK = registerBlock("enderite_block", unused -> new Block(BlockBehaviour.Properties.of().setId(keyOf("enderite_block")).mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(50.0f, 1200.0f).sound(SoundType.NETHERITE_BLOCK)));
    public static final Block NIHILITH_ORE = registerBlock("nihilith_ore", unused -> new DropExperienceBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE).setId(keyOf("nihilith_ore")).strength(25.0f, 1200.0f).requiresCorrectToolForDrops()));
    /**
     * Weisheitserz (Besitzer 2026-10-01): selten wie Diamant, droppt nur Erfahrung (3-7 wie Diamanterz) und
     * selten eine Weisheitskugel (Beutetabelle). Abbau ab Eisen, wie Diamanterz.
     */
    public static final Block SAGE_ORE = McVersion.SAGE_ORE ? registerBlock("sage_ore", unused -> new DropExperienceBlock(UniformInt.of(3, 7),
            BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE).setId(keyOf("sage_ore")))) : null;
    public static final Block DEEPSLATE_SAGE_ORE = McVersion.SAGE_ORE ? registerBlock("deepslate_sage_ore", unused -> new DropExperienceBlock(UniformInt.of(3, 7),
            BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE).setId(keyOf("deepslate_sage_ore")))) : null;
    /** Dimensions-Schrott, je Dimension ein Block mit eigener Textur ({@link com.simplebuilding.blocks.custom.DimensionalScrapBlock}). */
    public static final Block DIMENSIONAL_SCRAP = McVersion.DIMENSIONAL_SCRAP ? scrap("dimensional_scrap", Blocks.DEEPSLATE) : null;
    public static final Block NETHER_DIMENSIONAL_SCRAP = McVersion.DIMENSIONAL_SCRAP ? scrap("nether_dimensional_scrap", Blocks.ANCIENT_DEBRIS) : null;
    public static final Block END_DIMENSIONAL_SCRAP = McVersion.DIMENSIONAL_SCRAP ? scrap("end_dimensional_scrap", Blocks.END_STONE) : null;

    private static Block scrap(String name, Block look) {
        return registerBlock(name, unused -> new com.simplebuilding.blocks.custom.DimensionalScrapBlock(BlockBehaviour.Properties.ofFullCopy(look)
                .setId(keyOf(name)).strength(com.simplebuilding.blocks.custom.DimensionalScrapBlock.HARDNESS, 3600000.0F)
                .requiresCorrectToolForDrops().sound(net.minecraft.world.level.block.SoundType.ANCIENT_DEBRIS)
                .pushReaction(McVersion.immovable())));
    }

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
        registerOctetFlammability();
        if (McVersion.CRUCIBLE) {
            // Crucible P5: soul lava (fluids first; on Forge already in the FLUID event) and the SimpleLib-based blocks.
            com.simplebuilding.fluid.ModFluids.registerFluids();
            com.simplebuilding.fluid.ModFluids.registerBlocks();
            com.simplebuilding.crucible.CrucibleCompat.registerBlocks();
        }
    }

    /** Vanillas Shulkerkisten-Eigenschaften, mit der Offen-Pruefung gegen die Block-Entity der Stufen. */
    private static BlockBehaviour.Properties shulkerBoxProperties(BlockBehaviour.Properties settings) {
        // Nur das Ersticken: die Sicht-Pruefung hat auf 26.3 eine andere Signatur (mit AABB), dort bleibt Vanillas.
        return settings.isSuffocating(TieredShulkerBoxBlock::isClosedAt);
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
