package com.simplepads;

import com.simplelib.api.IdMoves;
import com.simplepads.advancement.PadTriggers;
import com.simplepads.block.PadsBlocks;
import com.simplepads.block.entity.PadsBlockEntities;
import com.simplepads.component.PadsComponents;
import com.simplepads.easter.EasterEggs;
import com.simplepads.easter.EasterSmithingRecipe;
import com.simplepads.item.PadsItems;
import com.simplepads.recipe.EnchantedShapelessRecipe;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Pads (b-pads01, 04.11.2026, plan docs/ai/PLAN-B-PADS01-2026-11-04.md, owner DW04C-5): a required sub-mod of SimpleBuilding
 * with the pads and pressure plates of the old "Simple Pads" tab - Launchpad, Elytra Pad, Flypad, Potion Pad, Spawn Teleporter and
 * Chunk Loader (three tiers each), the Diamond, Netherite, Enderite and copper pressure plates, the Spawn Elytra and the hidden
 * chain above the end tiers. Until 04.11. SimpleBuilding's {@code com.simplebuilding.tweaks}; every {@code simplebuilding:} id of it
 * moves to {@code simplepads:} ({@link #registerIdMoves}). Requires SimpleLib (cores), Simple Ores (Enderite) and Simple Mobheads
 * (the heads of the first tiers). The loader entry calls {@link #init} once with its config directory.
 */
public final class SimplePads {
    public static final String MOD_ID = "simplepads";
    public static final Logger LOG = LoggerFactory.getLogger("Simple Pads");
    /** Where the pads lived until 04.11.2026. */
    public static final String SB_NAMESPACE = "simplebuilding";

    private static PadsConfig local = new PadsConfig().validate();
    private static volatile @Nullable PadsConfig synced;
    private static @Nullable Path configDir;
    private static boolean initialized;
    private static boolean movesRegistered;

    private SimplePads() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Super-mod switch "Enable Simple Pads" (framework SubMods, set by SimpleBuilding): off = no tab entries. */
    public static boolean on() {
        return com.simplebuilding.framework.api.SubMods.enabled(MOD_ID);
    }

    /** What applies now: on the client thread the server's config (once sent), otherwise the own file. */
    public static PadsConfig config() {
        PadsConfig s = synced;
        if (s != null && PadsClientHooks.onClientThread()) return s;
        return local;
    }

    /** The own config (server side), for commands and tests. */
    public static PadsConfig localConfig() {
        return local;
    }

    public static void replaceConfig(PadsConfig config) {
        local = (config == null ? new PadsConfig() : config).validate();
    }

    public static void saveConfig() {
        PadsConfig.save(configDir, local);
    }

    public static @Nullable Path configDir() {
        return configDir;
    }

    public static String localConfigJson() {
        return new com.google.gson.Gson().toJson(local);
    }

    /** Client: the server's config arrived (null = disconnected). */
    public static void setSynced(@Nullable String json) {
        if (json == null) {
            synced = null;
            return;
        }
        try {
            synced = new com.google.gson.Gson().fromJson(json, PadsConfig.class).validate();
        } catch (RuntimeException e) {
            LOG.warn("Ignoring the server's pad config: {}", e.toString());
        }
    }

    /** Loads the config and registers everything (registries are open during the Fabric init). Runs once. */
    public static synchronized void init(@Nullable Path dir) {
        if (initialized) return;
        initialized = true;
        configDir = dir;
        local = PadsConfig.load(dir);
        registerIdMoves();
        // The building cores of the first tiers are SimpleLib's shared material (recipes only once requested).
        com.simplelib.api.SimpleLibApi.requestMaterial("cores");
        // In-world upgrade data of this namespace (none yet: the pads climb at the smithing table), as in Simple Machines.
        com.simplelib.api.InWorldUpgrades.registerDefaults(MOD_ID);
        PadTriggers.register();
        PadsComponents.init();
        EasterEggs.registerComponents();
        PadsBlocks.init();
        PadsItems.init();
        PadsViewer.register();   // a-fix09b: JEI info pages of the pads (were SimpleBuilding's until the split)
        EasterEggs.registerItems();
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id("easter_smithing"), EasterSmithingRecipe.SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id("enchanted_shapeless"), EnchantedShapelessRecipe.SERIALIZER);
        PadsBlockEntities.register(new PadsBlockEntities.Factory() {
            @Override
            public <T extends BlockEntity> BlockEntityType<T> create(BlockEntityType.BlockEntitySupplier<T> supplier, Block... blocks) {
                return new BlockEntityType<>(supplier, Set.of(blocks));
            }
        });
        ChunkLoaderMapLayer.register();
        // Hand hint (SimpleBuilding's renderer): honeycomb waxes, an axe scrapes a copper pressure plate.
        com.simplebuilding.framework.api.TransformHints.register(MOD_ID + ":copper_plate", query ->
                query.player() instanceof net.minecraft.world.entity.player.Player player
                        && query.hit() instanceof net.minecraft.world.phys.BlockHitResult hit
                        && query.level() instanceof net.minecraft.world.level.Level level
                        && level.getBlockState(hit.getBlockPos()).getBlock() instanceof com.simplepads.block.CopperPressurePlateBlock plate
                        && plate.transformWith(level.getBlockState(hit.getBlockPos()), player.getItemInHand(query.mainHand()
                                ? net.minecraft.world.InteractionHand.MAIN_HAND : net.minecraft.world.InteractionHand.OFF_HAND)).isPresent());
        // A switched-off family loses its recipes too (as in SimpleBuilding until 04.11.; live, /reload brings them back).
        com.simplelib.api.RecipeFilters.register(MOD_ID, path -> {
            PadsConfig.Pads p = local.pads;
            return (!p.enableChunkLoaders && path.contains("chunk_loader")) || (!p.enableElytraPads && path.contains("elytra_pad"))
                    || (!p.enableFlypads && path.contains("flypad")) || (!p.enableSpawnTeleporters && path.contains("spawn_teleporter"))
                    || (!p.enableLaunchpads && path.contains("launchpad")) || (!p.enablePotionPads && path.contains("potion_pad"));
        });
        com.simplebuilding.framework.api.TestCentreStations.register(MOD_ID, "pads", "simplepads.testcentre.station",
                PadsItems.all().stream().map(i -> BuiltInRegistries.ITEM.getKey(i).toString())
                        .filter(s -> !PadsBlocks.legacy().stream().anyMatch(b -> BuiltInRegistries.BLOCK.getKey(b).toString().equals(s)))
                        .toList());
    }

    /**
     * Every {@code simplebuilding:} id of the pads resolves to {@code simplepads:}: blocks and items (also the legacy tiers, the
     * Spawn Elytra and the Funny Stick), the eight block entity types (same NBT keys), the item components, the two recipe
     * serializers, every pad recipe and its recipe advancement, the advancements {@code pads/*} (not the two head ones, those are
     * Simple Mobheads') and {@code easter/*}.
     */
    public static synchronized void registerIdMoves() {
        if (movesRegistered) return;
        movesRegistered = true;
        for (String path : PadsIds.BLOCKS) {
            move(Registries.BLOCK, path);
            move(Registries.ITEM, path);
        }
        move(Registries.ITEM, "spawn_elytra");
        move(Registries.ITEM, "funny_stick");
        for (String path : PadsIds.BLOCK_ENTITY_TYPES) move(Registries.BLOCK_ENTITY_TYPE, path);
        for (String path : PadsIds.COMPONENTS) move(Registries.DATA_COMPONENT_TYPE, path);
        move(Registries.RECIPE_SERIALIZER, "easter_smithing");
        move(Registries.RECIPE_SERIALIZER, "enchanted_shapeless");
        for (String path : PadsIds.RECIPES) move(Registries.RECIPE, path);
        for (String path : PadsIds.ADVANCEMENTS) move(Registries.ADVANCEMENT, path);
    }

    private static void move(ResourceKey<? extends Registry<?>> registry, String path) {
        IdMoves.move(registry, Identifier.fromNamespaceAndPath(SB_NAMESPACE, path), id(path));
    }

    /** Ids moved from SimpleBuilding (the old paths are the new ones). */
    public static List<String> movedBlocks() {
        return PadsIds.BLOCKS;
    }
}
