package com.simplebuilding.modules.simpledimensions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WD25-14 / ST-13: the mod id moved from {@code simpledimension} to {@code simpledimensions} (owner, 25.10.). Old worlds
 * and configs keep working: registry ids move through SimpleLib's {@code IdMoves} (blocks, items, block entities,
 * dimension types, recipes, advancements), saved dimension keys are re-keyed ({@code WorldDimensionsMigrationMixin}),
 * the region folder {@code dimensions/simpledimension/<id>} moves once ({@code DimensionFolderMigrationMixin}), an old
 * player's {@code Dimension} resolves through {@code ServerLevelMigrationMixin}, and the module's own strings (portal
 * block entities, the return ledger, definition files) pass {@link #migrate}. The old config folder is copied once.
 */
public final class LegacyIds {
    public static final String OLD = "simpledimension", NEW = "simpledimensions";
    private static final Logger LOG = LoggerFactory.getLogger(NEW);
    private static boolean moved;

    private LegacyIds() {}

    /** {@code simpledimension:x} becomes {@code simpledimensions:x}; everything else (also {@code null}) stays. */
    public static String migrate(String id) {
        return id != null && id.startsWith(OLD + ":") ? NEW + id.substring(OLD.length()) : id;
    }

    /** The new identifier for an id of the old namespace, or {@code null}. */
    public static Identifier migrate(Identifier id) {
        return id != null && id.getNamespace().equals(OLD) ? Identifier.fromNamespaceAndPath(NEW, id.getPath()) : null;
    }

    /** Saved dimension keys of the old namespace re-keyed; {@code null} when nothing is old (the common case). */
    public static <T> Map<ResourceKey<LevelStem>, T> rekey(Map<ResourceKey<LevelStem>, T> saved) {
        if (saved.keySet().stream().noneMatch(k -> k.identifier().getNamespace().equals(OLD))) return null;
        Map<ResourceKey<LevelStem>, T> out = new LinkedHashMap<>();
        saved.forEach((key, value) -> {
            if (!key.identifier().getNamespace().equals(OLD)) out.put(key, value);
        });
        saved.forEach((key, value) -> {
            Identifier now = migrate(key.identifier());
            if (now != null) out.putIfAbsent(ResourceKey.create(Registries.LEVEL_STEM, now), value);
        });
        return out;
    }

    /**
     * The world's old region folder ({@code dimensions/simpledimension/<id>}) moves to the new one once, before the
     * level opens it; an existing new folder always wins.
     */
    public static void moveDimensionFolder(Identifier dimension, Path worldRoot) {
        if (!dimension.getNamespace().equals(NEW)) return;
        Path dimensions = worldRoot.resolve("dimensions");
        Path old = dimensions.resolve(OLD).resolve(dimension.getPath()), now = dimensions.resolve(NEW).resolve(dimension.getPath());
        if (!Files.isDirectory(old) || Files.isSymbolicLink(old) || Files.exists(now)) return;
        try {
            Files.createDirectories(now.getParent());
            Files.move(old, now);
            LOG.info("Simple Dimensions moved the world folder {} to {} (mod id simpledimension -> simpledimensions)", old, now);
        } catch (IOException e) {
            LOG.error("Cannot move the old dimension folder {} to {}", old, now, e);
        }
    }

    /**
     * Copies the old config folder ({@code <configDir>/simpledimension}, or the old working-directory one
     * {@code config/simpledimension}) to {@code target} once: {@code server.json} and the definitions. The old folder
     * stays untouched, the generated pack is rebuilt. Returns whether something was taken over.
     */
    public static boolean adoptConfig(Path configDir, Path target) {
        if (Files.exists(target)) return false;
        for (Path old : List.of(configDir.resolve(OLD), Path.of("config", OLD).toAbsolutePath())) {
            if (!Files.isDirectory(old) || Files.isSymbolicLink(old)) continue;
            try {
                Files.createDirectories(target.resolve("dimensions"));
                Path server = old.resolve("server.json");
                if (Files.isRegularFile(server)) Files.copy(server, target.resolve("server.json"));
                Path definitions = old.resolve("dimensions");
                if (Files.isDirectory(definitions)) {
                    try (var files = Files.list(definitions)) {
                        for (Path file : files.filter(f -> f.getFileName().toString().endsWith(".json") && Files.isRegularFile(f)).limit(ConfigLimits.MAX_DEFINITIONS).toList()) {
                            Files.copy(file, target.resolve("dimensions").resolve(file.getFileName().toString()));
                        }
                    }
                }
                LOG.info("Simple Dimensions took over the old config folder {} into {}", old, target);
                return true;
            } catch (IOException e) {
                LOG.error("Cannot take over the old config folder {}", old, e);
                return false;
            }
        }
        return false;
    }

    /**
     * Registers the id moves with SimpleLib's {@code IdMoves} (by reflection: the NeoForge/Forge builds have no
     * SimpleLib on the class path, principle 11). Namespace move for every registry plus explicit moves of the
     * blocks, items and block entities (Forge registry aliases only see explicit moves). Without SimpleLib, nothing.
     */
    public static synchronized void registerIdMoves() {
        if (moved) return;
        moved = true;
        try {
            Class<?> moves = Class.forName("com.simplelib.api.IdMoves");
            moves.getMethod("moveNamespace", String.class, String.class).invoke(null, OLD, NEW);
            var move = moves.getMethod("move", ResourceKey.class, Identifier.class, Identifier.class);
            for (String path : List.of("sky_portal", "light_blue_portal")) {
                move.invoke(null, Registries.BLOCK, Identifier.fromNamespaceAndPath(OLD, path), Identifier.fromNamespaceAndPath(NEW, path));
                move.invoke(null, Registries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(OLD, path), Identifier.fromNamespaceAndPath(NEW, path));
            }
            for (String path : List.of("light_blue_portal", "guide_book")) {
                move.invoke(null, Registries.ITEM, Identifier.fromNamespaceAndPath(OLD, path), Identifier.fromNamespaceAndPath(NEW, path));
            }
        } catch (ClassNotFoundException e) {
            LOG.warn("SimpleLib missing: old simpledimension ids are not migrated");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot register the simpledimension -> simpledimensions id moves", e);
        }
    }
}
