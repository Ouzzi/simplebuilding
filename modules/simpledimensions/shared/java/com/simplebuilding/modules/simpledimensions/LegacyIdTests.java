package com.simplebuilding.modules.simpledimensions;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import dev.simpledimension.common.config.DimensionConfigStore;
import dev.simpledimension.common.portal.DimensionPortalConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

/** WD25-14 / ST-13: worlds, players and configs of the old mod id {@code simpledimension} load as {@code simpledimensions}. */
public final class LegacyIdTests {
    private LegacyIdTests() {}

    private static Identifier old(String path) {
        return Identifier.fromNamespaceAndPath(LegacyIds.OLD, path);
    }

    /** Old block, item, block entity, item stack and advancement ids resolve to the new ones. */
    public static void ids(GameTestHelper h) {
        h.assertTrue(BuiltInRegistries.BLOCK.getValue(old("sky_portal")) == DimensionRegistry.PORTAL, "old sky_portal block id");
        h.assertTrue(BuiltInRegistries.BLOCK.getValue(old("light_blue_portal")) == DimensionRegistry.LEGACY, "old light_blue_portal block id");
        h.assertTrue(BuiltInRegistries.ITEM.getValue(old("light_blue_portal")) == DimensionRegistry.LEGACY.asItem(), "old light_blue_portal item id");
        h.assertTrue(BuiltInRegistries.ITEM.getValue(old("guide_book")) == com.simplebuilding.modules.simpledimensions.guide.DimensionsGuide.book(), "old guide_book item id");
        h.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(old("sky_portal")) == DimensionRegistry.PORTAL_ENTITY, "old sky_portal block entity id");
        h.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(old("light_blue_portal")) == DimensionRegistry.LEGACY_ENTITY, "old light_blue_portal block entity id");
        var ops = h.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        ItemStack stack = ItemStack.CODEC.parse(ops, JsonParser.parseString("{\"id\":\"simpledimension:guide_book\",\"count\":1}")).getOrThrow();
        h.assertTrue(stack.is(com.simplebuilding.modules.simpledimensions.guide.DimensionsGuide.book()), "saved stack with id simpledimension:guide_book loads as the new book");
        h.assertTrue(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(LegacyIds.NEW), "the next save writes the new id");
        h.assertTrue(h.getLevel().getServer().getAdvancements().get(old("guide/welcome")) != null, "old advancement progress guide/welcome finds the new advancement");
        h.succeed();
    }

    /** Old dimension keys: player position, saved world dimensions, region folder. */
    public static void dimensions(GameTestHelper h) {
        try {
            var server = h.getLevel().getServer();
            var level = server.getLevel(ResourceKey.create(Registries.DIMENSION, DimensionRegistry.id("skyblock")));
            h.assertTrue(level != null, "skyblock level present");
            // As PrepareSpawnTask reads an old player's "Dimension" and maps it through server::getLevel.
            ResourceKey<Level> saved = Level.RESOURCE_KEY_CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive("simpledimension:skyblock")).getOrThrow();
            h.assertTrue(server.getLevel(saved) == level, "an old player's dimension simpledimension:skyblock is the new skyblock level");
            h.assertTrue(server.getLevel(ResourceKey.create(Registries.DIMENSION, old("missing"))) == null, "unknown old keys stay unknown");

            // Distinct stem objects: a registry refuses the same value under two keys.
            java.util.function.Supplier<LevelStem> stem = () -> new LevelStem(level.dimensionTypeRegistration(), level.getChunkSource().getGenerator());
            var oldKey = ResourceKey.create(Registries.LEVEL_STEM, old("skyblock"));
            var newKey = ResourceKey.create(Registries.LEVEL_STEM, DimensionRegistry.id("skyblock"));
            var empty = new MappedRegistry<LevelStem>(Registries.LEVEL_STEM, Lifecycle.stable()).freeze();
            // A saved world always lists the overworld (the WorldDimensions constructor insists on it).
            var keys = new WorldDimensions(Map.of(LevelStem.OVERWORLD, stem.get(), oldKey, stem.get())).bake(empty).dimensions().registryKeySet();
            h.assertTrue(keys.contains(newKey) && !keys.contains(oldKey), "saved world dimension re-keyed: " + keys);
            Map<ResourceKey<LevelStem>, LevelStem> both = new LinkedHashMap<>();
            both.put(LevelStem.OVERWORLD, stem.get());
            both.put(oldKey, stem.get());
            both.put(newKey, stem.get());
            keys = new WorldDimensions(both).bake(empty).dimensions().registryKeySet();
            h.assertTrue(keys.size() == 2 && keys.contains(newKey) && !keys.contains(oldKey), "old and new key saved: one level only: " + keys);

            Path world = Files.createTempDirectory("simpledimensions-world");
            Path oldRegion = world.resolve("dimensions/simpledimension/skyblock/region/r.0.0.mca");
            Files.createDirectories(oldRegion.getParent());
            Files.writeString(oldRegion, "chunks");
            Path folder = DimensionType.getStorageFolder(ResourceKey.create(Registries.DIMENSION, DimensionRegistry.id("skyblock")), world);
            h.assertTrue(folder.equals(world.resolve("dimensions/simpledimensions/skyblock")), "new folder " + folder);
            h.assertTrue(Files.readString(folder.resolve("region/r.0.0.mca")).equals("chunks"), "old region files moved along");
            h.assertTrue(!Files.exists(world.resolve("dimensions/simpledimension/skyblock")), "old folder gone after the move");
            Files.createDirectories(oldRegion.getParent());
            Files.writeString(oldRegion, "stale");
            DimensionType.getStorageFolder(ResourceKey.create(Registries.DIMENSION, DimensionRegistry.id("skyblock")), world);
            h.assertTrue(Files.readString(folder.resolve("region/r.0.0.mca")).equals("chunks") && Files.exists(oldRegion), "an existing new folder always wins");
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
        h.succeed();
    }

    /** REV2-A-5b + WD25-14: the old config folder is taken over once, its old target ids load as new ones. */
    public static void config(GameTestHelper h) {
        try {
            Path dir = Files.createTempDirectory("simpledimensions-config");
            Path old = dir.resolve(LegacyIds.OLD);
            Files.createDirectories(old.resolve("dimensions"));
            Files.writeString(old.resolve("server.json"), "{\"accessEnabled\":false}");
            var gson = new GsonBuilder().setPrettyPrinting().create();
            var sky = gson.toJsonTree(DimensionPortalConfig.defaultSkyblock()).getAsJsonObject();
            sky.addProperty("targetDimensionId", "simpledimension:skyblock");
            Files.writeString(old.resolve("dimensions/skyblock.json"), gson.toJson(sky));
            Path target = dir.resolve(LegacyIds.NEW);
            h.assertTrue(LegacyIds.adoptConfig(dir, target), "old config folder taken over");
            h.assertTrue(!LegacyIds.adoptConfig(dir, target), "taken over only once");
            h.assertTrue(!DimensionSettings.load(target).accessEnabled, "old server.json values kept");
            var definitions = DimensionConfigStore.loadAndGenerate(target, 100);
            h.assertTrue(definitions.stream().anyMatch(d -> d.id.equals("skyblock") && d.targetDimensionId.equals("simpledimensions:skyblock")), "old target id loads as simpledimensions:skyblock");
            h.assertTrue(Files.isRegularFile(DimensionConfigStore.generatedDatapackRoot(target).resolve("data/simpledimensions/dimension/skyblock.json")), "pack generated under the new namespace");
            h.assertTrue(Files.isRegularFile(old.resolve("server.json")), "old folder untouched");
            h.assertTrue(DimensionRuntime.CONFIG_ROOT.getFileName().toString().equals(LegacyIds.NEW), "runtime config folder " + DimensionRuntime.CONFIG_ROOT);
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
        h.succeed();
    }
}
