package com.simplepads.block;

import com.simplepads.SimplePads;
import com.simplepads.util.PadsVersion;
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
 * Die Pads und Druckplatten von Simple Pads (bis 04.11.2026 SimpleBuilding {@code TweaksBlocks}, Ids von
 * {@code simplebuilding:} verschoben). Registrierung direkt in die Vanilla-Registry beim ersten Zugriff ({@link #init()}).
 * Die sechs alten Stufenbloecke der Fuenf-Stufen-Zeit gibt es nicht mehr (vor dem ersten Release entfernt, simplepads-4):
 * ihre Ids fuehren per SimpleLib-IdMoves auf die neue Stufe ({@code SimplePads#registerIdMoves}).
 */
public final class PadsBlocks {

    private static final List<Block> ALL = new ArrayList<>();

    // --- Spawn-Teleporter I-III (Wartezeit 50/20/5 s; III = Enderit, eigener Wiedereinstiegspunkt) ---
    public static final Block SPAWN_TELEPORTER = register("spawn_teleporter",
            p -> new SpawnTeleporterBlock(sturdy(p).lightLevel(s -> 10), 1));
    public static final Block SPAWN_TELEPORTER_TIER_2 = register("spawn_teleporter_tier_2",
            p -> new SpawnTeleporterBlock(sturdy(p).lightLevel(s -> 12).mapColor(MapColor.DIAMOND), 2));
    public static final Block ENDERITE_SPAWN_TELEPORTER = register("enderite_spawn_teleporter",
            p -> new SpawnTeleporterBlock(sturdy(p).lightLevel(s -> 15).mapColor(MapColor.COLOR_PURPLE), SpawnTeleporterBlock.ENDERITE_TIER));

    // --- Launchpads I-III (4/8/16 Ladungen; III = Enderit, kein Fallschaden) ---
    public static final Block LAUNCHPAD = register("launchpad",
            p -> new LaunchpadBlock(sturdy(p).lightLevel(s -> 5), 1));
    public static final Block NETHERITE_LAUNCHPAD = register("netherite_launchpad",
            p -> new LaunchpadBlock(sturdy(p).lightLevel(s -> 6).mapColor(MapColor.COLOR_BLACK).strength(4.0f).sound(SoundType.NETHERITE_BLOCK), 2));
    public static final Block ENDERITE_LAUNCHPAD = register("enderite_launchpad",
            p -> new LaunchpadBlock(sturdy(p).lightLevel(s -> 7).mapColor(MapColor.COLOR_PURPLE).strength(4.0f).sound(SoundType.NETHERITE_BLOCK), LaunchpadBlock.ENDERITE_TIER));

    // --- Druckplatten: Diamant, Netherit, Enderit gehoeren seit N48 (14.11.2026) SimpleLib (Gruppe material_pressure_plates, Ids simplelib:*);
    // Simple Pads fordert die Gruppe an (SimplePads#init) und nutzt die Bloecke von dort. ---
    public static final Block DIAMOND_PRESSURE_PLATE = com.simplelib.api.SimpleLibApi.materialBlock("diamond_pressure_plate");
    public static final Block NETHERITE_PRESSURE_PLATE = com.simplelib.api.SimpleLibApi.materialBlock("netherite_pressure_plate");
    public static final Block ENDERITE_PRESSURE_PLATE = com.simplelib.api.SimpleLibApi.materialBlock("enderite_pressure_plate");
    public static final Block EMERALD_PRESSURE_PLATE = com.simplelib.api.SimpleLibApi.materialBlock("emerald_pressure_plate");

    // --- Elytra-Pads I-III (seit 2026-10-07 drei Stufen) ---
    public static final Block ELYTRA_PAD = register("elytra_pad",
            p -> new ElytraPadBlock(sturdy(p).mapColor(MapColor.COLOR_CYAN).strength(1.5f).sound(SoundType.METAL), 1));
    public static final Block NETHERITE_ELYTRA_PAD = register("netherite_elytra_pad",
            p -> new ElytraPadBlock(sturdy(p).mapColor(MapColor.COLOR_BLACK).strength(4.0f).sound(SoundType.NETHERITE_BLOCK), 2));
    public static final Block ENDERITE_ELYTRA_PAD = register("enderite_elytra_pad",
            p -> new ElytraPadBlock(sturdy(p).mapColor(MapColor.COLOR_PURPLE).strength(4.5f).sound(SoundType.NETHERITE_BLOCK).lightLevel(s -> 7), 3));

    // --- Flypads I-III aus Enderit (4x4x8 / 8x8x16 / 16x16x32, Besitzer 2026-09-27, Hoehe 2026-10-02) ---
    public static final Block FLYPAD = register("flypad",
            p -> new FlypadBlock(sturdy(p).mapColor(MapColor.COLOR_PURPLE).strength(4.5f).sound(SoundType.NETHERITE_BLOCK).lightLevel(s -> 7), 1));
    public static final Block REINFORCED_FLYPAD = register("reinforced_flypad",
            p -> new FlypadBlock(sturdy(p).mapColor(MapColor.COLOR_PURPLE).strength(5.0f).sound(SoundType.NETHERITE_BLOCK).lightLevel(s -> 10), 2));
    public static final Block STELLAR_FLYPAD = register("stellar_flypad",
            p -> new FlypadBlock(sturdy(p).mapColor(MapColor.COLOR_PURPLE).strength(5.0f).sound(SoundType.NETHERITE_BLOCK).lightLevel(s -> 15), 3));

    // --- Chunk-Loader I-III (eigener Chunk / 5 Chunks im Kreuz / 3x3) ---
    public static final Block CHUNK_LOADER = register("chunk_loader",
            p -> new ChunkLoaderBlock(sturdy(p).mapColor(MapColor.DIAMOND).strength(4.0f).lightLevel(s -> ChunkLoaderBlock.lightLevel(s, 7)), 1));
    public static final Block NETHERITE_CHUNK_LOADER = register("netherite_chunk_loader",
            p -> new ChunkLoaderBlock(sturdy(p).mapColor(MapColor.COLOR_BLACK).strength(4.5f).lightLevel(s -> ChunkLoaderBlock.lightLevel(s, 8)).sound(SoundType.NETHERITE_BLOCK), 2));
    public static final Block ENDERITE_CHUNK_LOADER = register("enderite_chunk_loader",
            p -> new ChunkLoaderBlock(sturdy(p).mapColor(MapColor.COLOR_PURPLE).strength(5.0f).lightLevel(s -> ChunkLoaderBlock.lightLevel(s, 9)), 3));

    // --- Trank-Pads I-III (Besitzer 2026-09-28): gespeicherter Wurftrank fuer 45/90/180 s beim Betreten ---
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

    private PadsBlocks() {
    }

    /** Alle Bloecke in Registrierungsreihenfolge (Datagen, Tests, Kreativ-Tab). */
    public static List<Block> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static void init() {
    }

    /** Unverwuestliche Pads: kein Kolben, keine Verdeckung, kein Ersticken (Simple Tweaks: sturdyPadSettings). */
    private static BlockBehaviour.Properties sturdy(BlockBehaviour.Properties p) {
        return PadsVersion.neverViewBlocking(p.noOcclusion()
                .isRedstoneConductor((s, l, pos) -> false)
                .isSuffocating((s, l, pos) -> false))
                .pushReaction(PadsVersion.PUSH_BLOCKED);
    }

    /** Kupferplatten: Kolben zerstoeren sie (Simple Tweaks: fragilePadSettings). */
    private static BlockBehaviour.Properties fragile(BlockBehaviour.Properties p) {
        return PadsVersion.neverViewBlocking(p.noOcclusion()
                .isRedstoneConductor((s, l, pos) -> false)
                .isSuffocating((s, l, pos) -> false))
                .pushReaction(PadsVersion.PUSH_DESTROYS)
                .strength(1.5f)
                .sound(SoundType.COPPER)
                .noCollision();
    }

    private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimplePads.id(name));
        Block block = Registry.register(BuiltInRegistries.BLOCK, SimplePads.id(name),factory.apply(BlockBehaviour.Properties.of().setId(key)));
        ALL.add(block);
        return block;
    }
}
