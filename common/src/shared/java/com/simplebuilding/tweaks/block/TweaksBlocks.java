package com.simplebuilding.tweaks.block;

import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

/**
 * Alle Bloecke aus Simple Tweaks ({@code ModBlocks}) plus die neuen Enderit-Stufen
 * (docs/SIMPLETWEAKS-UEBERNAHME.md). Registrierung wie {@code ModBlocks}: direkt in die
 * Vanilla-Registry, NeoForge/Forge rufen {@link #init()} im Block-RegisterEvent.
 */
public final class TweaksBlocks {

    private static final List<Block> ALL = new ArrayList<>();

    // --- Spawn-Teleporter I-III (Wartezeit 50/20/5 s; III = Enderit, eigener Wiedereinstiegspunkt) ---
    public static final Block SPAWN_TELEPORTER = register("spawn_teleporter",
            p -> new SpawnTeleporterBlock(sturdy(p).lightLevel(s -> 10), 1));
    public static final Block SPAWN_TELEPORTER_TIER_2 = register("spawn_teleporter_tier_2",
            p -> new SpawnTeleporterBlock(sturdy(p).lightLevel(s -> 12).mapColor(MapColor.DIAMOND), 2));
    // Alte Stufen III und IV (bis 2026-09-28 fuenf Stufen): nur noch zum Laden alter Welten, werden zu II bzw. III.
    public static final Block SPAWN_TELEPORTER_TIER_3 = register("spawn_teleporter_tier_3",
            p -> new LegacySpawnTeleporterBlock(sturdy(p).lightLevel(s -> 14).mapColor(MapColor.EMERALD), 2, () -> TweaksBlocks.SPAWN_TELEPORTER_TIER_2));
    public static final Block SPAWN_TELEPORTER_TIER_4 = register("spawn_teleporter_tier_4",
            p -> new LegacySpawnTeleporterBlock(sturdy(p).lightLevel(s -> 15).mapColor(MapColor.GOLD), SpawnTeleporterBlock.ENDERITE_TIER,
                    () -> TweaksBlocks.ENDERITE_SPAWN_TELEPORTER));
    public static final Block ENDERITE_SPAWN_TELEPORTER = register("enderite_spawn_teleporter",
            p -> new SpawnTeleporterBlock(sturdy(p).lightLevel(s -> 15).mapColor(MapColor.COLOR_PURPLE), SpawnTeleporterBlock.ENDERITE_TIER));

    // --- Launchpads I-III (4/8/16 Ladungen; III = Enderit, kein Fallschaden) ---
    public static final Block LAUNCHPAD = register("launchpad",
            p -> new LaunchpadBlock(sturdy(p).lightLevel(s -> 5), 1));
    public static final Block NETHERITE_LAUNCHPAD = register("netherite_launchpad",
            p -> new LaunchpadBlock(sturdy(p).lightLevel(s -> 6).mapColor(MapColor.COLOR_BLACK).strength(4.0f).sound(SoundType.NETHERITE_BLOCK), 2));
    public static final Block ENDERITE_LAUNCHPAD = register("enderite_launchpad",
            p -> new LaunchpadBlock(sturdy(p).lightLevel(s -> 7).mapColor(MapColor.COLOR_PURPLE).strength(4.0f).sound(SoundType.NETHERITE_BLOCK), LaunchpadBlock.ENDERITE_TIER));

    // --- Druckplatten ---
    public static final Block DIAMOND_PRESSURE_PLATE = register("diamond_pressure_plate",
            p -> new DiamondPressurePlateBlock(BlockSetType.IRON,
                    p.mapColor(MapColor.DIAMOND).noCollision().strength(0.5f).pushReaction(McVersion.PUSH_BLOCKED)));
    public static final Block NETHERITE_PRESSURE_PLATE = register("netherite_pressure_plate",
            p -> new FilterPressurePlateBlock(BlockSetType.IRON,
                    p.mapColor(MapColor.COLOR_BLACK).noCollision().strength(4.0f).pushReaction(McVersion.PUSH_BLOCKED), false));
    public static final Block ENDERITE_PRESSURE_PLATE = register("enderite_pressure_plate",
            p -> new FilterPressurePlateBlock(BlockSetType.IRON,
                    p.mapColor(MapColor.COLOR_PURPLE).noCollision().strength(5.0f).pushReaction(McVersion.PUSH_BLOCKED), true));

    // --- Elytra-Pads I-V ---
    public static final Block ELYTRA_PAD = register("elytra_pad",
            p -> new ElytraPadBlock(sturdy(p).mapColor(MapColor.COLOR_CYAN).strength(1.5f).sound(SoundType.METAL), 1));
    public static final Block REINFORCED_ELYTRA_PAD = register("reinforced_elytra_pad",
            p -> new ElytraPadBlock(sturdy(p).mapColor(MapColor.DIAMOND).strength(2.0f).sound(SoundType.METAL), 2));
    public static final Block NETHERITE_ELYTRA_PAD = register("netherite_elytra_pad",
            p -> new ElytraPadBlock(sturdy(p).mapColor(MapColor.COLOR_BLACK).strength(4.0f).sound(SoundType.NETHERITE_BLOCK), 3));
    public static final Block ENDERITE_ELYTRA_PAD = register("enderite_elytra_pad",
            p -> new ElytraPadBlock(sturdy(p).mapColor(MapColor.COLOR_PURPLE).strength(4.5f).sound(SoundType.NETHERITE_BLOCK).lightLevel(s -> 7), 4));
    public static final Block FINE_ELYTRA_PAD = register("fine_elytra_pad",
            p -> new ElytraPadBlock(sturdy(p).mapColor(MapColor.GOLD).strength(4.0f).sound(SoundType.NETHERITE_BLOCK).lightLevel(s -> 10), 5));

    // --- Flypads I-III aus Enderit (4x4x6 / 8x8x12 / 16x16x24, Besitzer 2026-09-27) ---
    public static final Block FLYPAD = register("flypad",
            p -> new FlypadBlock(sturdy(p).mapColor(MapColor.COLOR_PURPLE).strength(4.5f).sound(SoundType.NETHERITE_BLOCK).lightLevel(s -> 7), 1));
    public static final Block REINFORCED_FLYPAD = register("reinforced_flypad",
            p -> new FlypadBlock(sturdy(p).mapColor(MapColor.COLOR_PURPLE).strength(5.0f).sound(SoundType.NETHERITE_BLOCK).lightLevel(s -> 10), 2));
    public static final Block STELLAR_FLYPAD = register("stellar_flypad",
            p -> new FlypadBlock(sturdy(p).mapColor(MapColor.COLOR_PURPLE).strength(5.0f).sound(SoundType.NETHERITE_BLOCK).lightLevel(s -> 15), 3));
    // Alte Flypads (vorher Stufen III und IV von fuenf): nur noch zum Laden alter Welten, werden zur neuen Stufe.
    public static final Block NETHERITE_FLYPAD = register("netherite_flypad",
            p -> new LegacyFlypadBlock(sturdy(p).mapColor(MapColor.COLOR_BLACK).strength(5.0f), 2, () -> TweaksBlocks.REINFORCED_FLYPAD));
    public static final Block ENDERITE_FLYPAD = register("enderite_flypad",
            p -> new LegacyFlypadBlock(sturdy(p).mapColor(MapColor.COLOR_PURPLE).strength(5.0f).lightLevel(s -> 10), 3, () -> TweaksBlocks.STELLAR_FLYPAD));

    // --- Chunk-Loader I-III (eigener Chunk / 5 Chunks im Kreuz / 3x3) ---
    public static final Block CHUNK_LOADER = register("chunk_loader",
            p -> new ChunkLoaderBlock(sturdy(p).mapColor(MapColor.DIAMOND).strength(4.0f).lightLevel(s -> ChunkLoaderBlock.lightLevel(s, 7)), 1));
    public static final Block NETHERITE_CHUNK_LOADER = register("netherite_chunk_loader",
            p -> new ChunkLoaderBlock(sturdy(p).mapColor(MapColor.COLOR_BLACK).strength(4.5f).lightLevel(s -> ChunkLoaderBlock.lightLevel(s, 8)).sound(SoundType.NETHERITE_BLOCK), 2));
    public static final Block ENDERITE_CHUNK_LOADER = register("enderite_chunk_loader",
            p -> new ChunkLoaderBlock(sturdy(p).mapColor(MapColor.COLOR_PURPLE).strength(5.0f).lightLevel(s -> ChunkLoaderBlock.lightLevel(s, 9)), 3));

    // --- Trank-Pads I-III (Besitzer 2026-09-28): gespeicherter Wurftrank fuer 30/60/120 s beim Betreten ---
    public static final Block POTION_PAD = register("potion_pad",
            p -> new PotionPadBlock(sturdy(p).mapColor(MapColor.COLOR_BLACK).strength(4.0f).sound(SoundType.NETHERITE_BLOCK).lightLevel(s -> 5), 1));
    public static final Block REINFORCED_POTION_PAD = register("reinforced_potion_pad",
            p -> new PotionPadBlock(sturdy(p).mapColor(MapColor.COLOR_BLACK).strength(4.5f).sound(SoundType.NETHERITE_BLOCK).lightLevel(s -> 7), 2));
    public static final Block INFUSED_POTION_PAD = register("infused_potion_pad",
            p -> new PotionPadBlock(sturdy(p).mapColor(MapColor.COLOR_BLACK).strength(5.0f).sound(SoundType.NETHERITE_BLOCK).lightLevel(s -> 10), 3));

    // --- Kupfer-Druckplatten (zerbrechlich, oxidieren; gewachst wie Vanilla-Kupfer: oxidieren nicht) ---
    public static final Block COPPER_PRESSURE_PLATE = register("copper_pressure_plate",
            p -> new CopperPressurePlateBlock(WeatheringCopper.WeatherState.UNAFFECTED, false, fragile(p).mapColor(MapColor.COLOR_ORANGE)));
    public static final Block EXPOSED_COPPER_PRESSURE_PLATE = register("exposed_copper_pressure_plate",
            p -> new CopperPressurePlateBlock(WeatheringCopper.WeatherState.EXPOSED, false, fragile(p).mapColor(MapColor.TERRACOTTA_LIGHT_GRAY)));
    public static final Block WEATHERED_COPPER_PRESSURE_PLATE = register("weathered_copper_pressure_plate",
            p -> new CopperPressurePlateBlock(WeatheringCopper.WeatherState.WEATHERED, false, fragile(p).mapColor(MapColor.WARPED_STEM)));
    public static final Block OXIDIZED_COPPER_PRESSURE_PLATE = register("oxidized_copper_pressure_plate",
            p -> new CopperPressurePlateBlock(WeatheringCopper.WeatherState.OXIDIZED, false, fragile(p).mapColor(MapColor.WARPED_NYLIUM)));
    public static final Block WAXED_COPPER_PRESSURE_PLATE = register("waxed_copper_pressure_plate",
            p -> new CopperPressurePlateBlock(WeatheringCopper.WeatherState.UNAFFECTED, true, fragile(p).mapColor(MapColor.COLOR_ORANGE)));
    public static final Block WAXED_EXPOSED_COPPER_PRESSURE_PLATE = register("waxed_exposed_copper_pressure_plate",
            p -> new CopperPressurePlateBlock(WeatheringCopper.WeatherState.EXPOSED, true, fragile(p).mapColor(MapColor.TERRACOTTA_LIGHT_GRAY)));
    public static final Block WAXED_WEATHERED_COPPER_PRESSURE_PLATE = register("waxed_weathered_copper_pressure_plate",
            p -> new CopperPressurePlateBlock(WeatheringCopper.WeatherState.WEATHERED, true, fragile(p).mapColor(MapColor.WARPED_STEM)));
    public static final Block WAXED_OXIDIZED_COPPER_PRESSURE_PLATE = register("waxed_oxidized_copper_pressure_plate",
            p -> new CopperPressurePlateBlock(WeatheringCopper.WeatherState.OXIDIZED, true, fragile(p).mapColor(MapColor.WARPED_NYLIUM)));

    // --- Mob-Koepfe (keine Pads): nicht in all(), eigene Liste heads(). Je Kopf-Typ ein stehender Kopf und
    // eine Wandvariante (wie Vanillas wallVariant(kopf, true): Beute und Name vom stehenden Kopf). Reihenfolge
    // = BlazeHeadType: Lohe und Enderman zuerst, damit ihre Registrierung wie vorher bleibt.
    private static final List<Block> HEADS = new ArrayList<>();
    private static final Map<BlazeHeadType, Block> STANDING_HEADS = new EnumMap<>(BlazeHeadType.class);
    private static final Map<BlazeHeadType, Block> WALL_HEADS = new EnumMap<>(BlazeHeadType.class);

    static {
        for (BlazeHeadType type : BlazeHeadType.values()) {
            Block standing = registerHead(type.blockName(),
                    p -> new SkullBlock(type, p.instrument(NoteBlockInstrument.CUSTOM_HEAD).strength(1.0f)
                            .pushReaction(McVersion.PUSH_DESTROYS).noOcclusion()));
            Block wall = registerHead(type.wallBlockName(),
                    p -> new WallSkullBlock(type, p.overrideLootTable(standing.getLootTable())
                            .overrideDescription(standing.getDescriptionId()).strength(1.0f).pushReaction(McVersion.PUSH_DESTROYS)));
            STANDING_HEADS.put(type, standing);
            WALL_HEADS.put(type, wall);
        }
    }

    // Lohenkopf: Zutat des Trank-Pads I. Endermankopf (2026-09-28): Zutat des Spawn-Teleporters I.
    public static final Block BLAZE_HEAD = STANDING_HEADS.get(BlazeHeadType.BLAZE);
    public static final Block BLAZE_WALL_HEAD = WALL_HEADS.get(BlazeHeadType.BLAZE);
    public static final Block ENDERMAN_HEAD = STANDING_HEADS.get(BlazeHeadType.ENDERMAN);
    public static final Block ENDERMAN_WALL_HEAD = WALL_HEADS.get(BlazeHeadType.ENDERMAN);

    private TweaksBlocks() {
    }

    /** Alte, abgeloeste Stufenbloecke (nur zum Laden alter Welten; kein Rezept, kein Kreativ-Tab). */
    public static List<Block> legacy() {
        return List.of(NETHERITE_FLYPAD, ENDERITE_FLYPAD, SPAWN_TELEPORTER_TIER_3, SPAWN_TELEPORTER_TIER_4);
    }

    /** Alle Mob-Koepfe der Mod (stehend und an der Wand); keine Pads, darum nicht in {@link #all()}. */
    public static List<Block> heads() {
        return Collections.unmodifiableList(HEADS);
    }

    /** Der stehende Kopf eines Kopf-Typs. */
    public static Block head(BlazeHeadType type) {
        return STANDING_HEADS.get(type);
    }

    /** Die Wandvariante eines Kopf-Typs. */
    public static Block wallHead(BlazeHeadType type) {
        return WALL_HEADS.get(type);
    }

    /** Alle Bloecke in Registrierungsreihenfolge (Datagen, Tests, Kreativ-Tab). */
    public static List<Block> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static void init() {
    }

    /** Unverwuestliche Pads: kein Kolben, keine Verdeckung, kein Ersticken (Simple Tweaks: sturdyPadSettings). */
    private static BlockBehaviour.Properties sturdy(BlockBehaviour.Properties p) {
        return McVersion.neverViewBlocking(p.noOcclusion()
                .isRedstoneConductor((s, l, pos) -> false)
                .isSuffocating((s, l, pos) -> false))
                .pushReaction(McVersion.PUSH_BLOCKED);
    }

    /** Kupferplatten: Kolben zerstoeren sie (Simple Tweaks: fragilePadSettings). */
    private static BlockBehaviour.Properties fragile(BlockBehaviour.Properties p) {
        return McVersion.neverViewBlocking(p.noOcclusion()
                .isRedstoneConductor((s, l, pos) -> false)
                .isSuffocating((s, l, pos) -> false))
                .pushReaction(McVersion.PUSH_DESTROYS)
                .strength(1.5f)
                .sound(SoundType.COPPER)
                .noCollision();
    }

    private static Block registerHead(String name, Function<BlockBehaviour.Properties, Block> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleTweaks.id(name));
        Block block = Registry.register(BuiltInRegistries.BLOCK, SimpleTweaks.id(name), factory.apply(BlockBehaviour.Properties.of().setId(key)));
        HEADS.add(block);
        return block;
    }

    private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleTweaks.id(name));
        Block block = Registry.register(BuiltInRegistries.BLOCK, SimpleTweaks.id(name),factory.apply(BlockBehaviour.Properties.of().setId(key)));
        ALL.add(block);
        return block;
    }
}
