package com.simplebuilding.tweaks.block;

import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;

/**
 * Alle Bloecke aus Simple Tweaks ({@code ModBlocks}) plus die neuen Enderit-Stufen
 * (docs/SIMPLETWEAKS-UEBERNAHME.md). Registrierung wie {@code ModBlocks}: direkt in die
 * Vanilla-Registry, NeoForge/Forge rufen {@link #init()} im Block-RegisterEvent.
 */
public final class TweaksBlocks {

    private static final List<Block> ALL = new ArrayList<>();

    // --- Spawn-Teleporter (Stufe = Ziel Spawn 1-4; V = eigener Wiedereinstiegspunkt) ---
    public static final Block SPAWN_TELEPORTER = register("spawn_teleporter",
            p -> new SpawnTeleporterBlock(sturdy(p).lightLevel(s -> 10), 1));
    public static final Block SPAWN_TELEPORTER_TIER_2 = register("spawn_teleporter_tier_2",
            p -> new SpawnTeleporterBlock(sturdy(p).lightLevel(s -> 12).mapColor(MapColor.DIAMOND), 2));
    public static final Block SPAWN_TELEPORTER_TIER_3 = register("spawn_teleporter_tier_3",
            p -> new SpawnTeleporterBlock(sturdy(p).lightLevel(s -> 14).mapColor(MapColor.EMERALD), 3));
    public static final Block SPAWN_TELEPORTER_TIER_4 = register("spawn_teleporter_tier_4",
            p -> new SpawnTeleporterBlock(sturdy(p).lightLevel(s -> 15).mapColor(MapColor.GOLD), 4));
    public static final Block ENDERITE_SPAWN_TELEPORTER = register("enderite_spawn_teleporter",
            p -> new SpawnTeleporterBlock(sturdy(p).lightLevel(s -> 15).mapColor(MapColor.COLOR_PURPLE), SpawnTeleporterBlock.ENDERITE_TIER));

    // --- Launchpads ---
    public static final Block LAUNCHPAD = register("launchpad",
            p -> new LaunchpadBlock(sturdy(p).lightLevel(s -> 5), false));
    public static final Block ENDERITE_LAUNCHPAD = register("enderite_launchpad",
            p -> new LaunchpadBlock(sturdy(p).lightLevel(s -> 7).mapColor(MapColor.COLOR_PURPLE).strength(4.0f).sound(SoundType.NETHERITE_BLOCK), true));

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

    // --- Flypads I-V ---
    public static final Block FLYPAD = register("flypad",
            p -> new FlypadBlock(sturdy(p).mapColor(MapColor.EMERALD).strength(2.0f), 1));
    public static final Block REINFORCED_FLYPAD = register("reinforced_flypad",
            p -> new FlypadBlock(sturdy(p).mapColor(MapColor.DIAMOND).strength(3.0f), 2));
    public static final Block NETHERITE_FLYPAD = register("netherite_flypad",
            p -> new FlypadBlock(sturdy(p).mapColor(MapColor.COLOR_BLACK).strength(5.0f), 3));
    public static final Block ENDERITE_FLYPAD = register("enderite_flypad",
            p -> new FlypadBlock(sturdy(p).mapColor(MapColor.COLOR_PURPLE).strength(5.0f).lightLevel(s -> 10), 4));
    public static final Block STELLAR_FLYPAD = register("stellar_flypad",
            p -> new FlypadBlock(sturdy(p).mapColor(MapColor.COLOR_PURPLE).strength(5.0f).lightLevel(s -> 15), 5));

    // --- Chunk-Loader ---
    public static final Block CHUNK_LOADER = register("chunk_loader",
            p -> new ChunkLoaderBlock(sturdy(p).mapColor(MapColor.DIAMOND).strength(4.0f).lightLevel(s -> 7), 0));
    public static final Block ENDERITE_CHUNK_LOADER = register("enderite_chunk_loader",
            p -> new ChunkLoaderBlock(sturdy(p).mapColor(MapColor.COLOR_PURPLE).strength(5.0f).lightLevel(s -> 9), 1));

    // --- Kupfer-Druckplatten (zerbrechlich, oxidieren) ---
    public static final Block COPPER_PRESSURE_PLATE = register("copper_pressure_plate",
            p -> new CopperPressurePlateBlock(WeatheringCopper.WeatherState.UNAFFECTED, fragile(p).mapColor(MapColor.COLOR_ORANGE)));
    public static final Block EXPOSED_COPPER_PRESSURE_PLATE = register("exposed_copper_pressure_plate",
            p -> new CopperPressurePlateBlock(WeatheringCopper.WeatherState.EXPOSED, fragile(p).mapColor(MapColor.TERRACOTTA_LIGHT_GRAY)));
    public static final Block WEATHERED_COPPER_PRESSURE_PLATE = register("weathered_copper_pressure_plate",
            p -> new CopperPressurePlateBlock(WeatheringCopper.WeatherState.WEATHERED, fragile(p).mapColor(MapColor.WARPED_STEM)));
    public static final Block OXIDIZED_COPPER_PRESSURE_PLATE = register("oxidized_copper_pressure_plate",
            p -> new CopperPressurePlateBlock(WeatheringCopper.WeatherState.OXIDIZED, fragile(p).mapColor(MapColor.WARPED_NYLIUM)));

    private TweaksBlocks() {
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

    private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleTweaks.id(name));
        Block block = Registry.register(BuiltInRegistries.BLOCK, SimpleTweaks.id(name),factory.apply(BlockBehaviour.Properties.of().setId(key)));
        ALL.add(block);
        return block;
    }
}
