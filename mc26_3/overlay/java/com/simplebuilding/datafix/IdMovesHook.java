package com.simplebuilding.datafix;

import com.simplebuilding.Simplebuilding;
import com.simplelib.api.IdMoves;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * 26.3: the renamed ids of {@link LegacyItemIds} go into SimpleLib's {@link IdMoves} (split plan stage 3a), whose
 * mixins and Forge aliases resolve them; the mod's own alias mixins are switched off ({@link AliasMixinPlugin}) and
 * {@code ForgeRegistryBootstrap} adds no aliases of its own. Same tables, same registries as before: items (renamed
 * items, renamed End redstone blocks, renamed stands, vanilla replacements), blocks (End redstone, vanilla
 * replacements), entity types (stands). Called from every
 * loader's mod constructor / initializer, before the registry events and before any world loads.
 *
 * <p>Also registers the test namespace {@code simpletest}, which moves to {@code simplebuilding} as a whole: the
 * migration tests ({@code MigrationWorldTests}) load a saved world whose SimpleBuilding ids were spelled
 * {@code simpletest:} - the shape of every later namespace move. The namespace never exists, so it costs nothing.
 */
public final class IdMovesHook {
    /** Constant (inlined by javac, read by {@link AliasMixinPlugin} before any game class loads). */
    public static final boolean LIB_ALIASES = true;
    /** Namespace of the migration tests' "old" ids. */
    public static final String TEST_NAMESPACE = "simpletest";

    private static boolean registered;

    private IdMovesHook() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        // LegacyItemIds.renamedTo: blocks first, then items, then stands (the tables share no key).
        Map<String, String> items = new java.util.LinkedHashMap<>();
        LegacyItemIds.RENAMED_STANDS.forEach(items::put);
        LegacyItemIds.RENAMED.forEach((oldPath, newPath) -> {
            if (com.simplebuilding.version.McVersion.MEGA_GUIDES || !oldPath.startsWith("guide_book_")) items.put(oldPath, newPath);
        });
        LegacyItemIds.RENAMED_BLOCKS.forEach(items::put);
        move(Registries.ITEM, items);
        move(Registries.BLOCK, LegacyItemIds.RENAMED_BLOCKS);
        move(Registries.ENTITY_TYPE, LegacyItemIds.RENAMED_STANDS);
        // Removed blocks that load as a vanilla block (Nachtrag 36): item and block, target in the minecraft namespace.
        LegacyItemIds.REPLACED_BY_VANILLA.forEach((oldPath, vanilla) -> {
            Identifier old = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, oldPath);
            IdMoves.move(Registries.ITEM, old, vanilla);
            IdMoves.move(Registries.BLOCK, old, vanilla);
        });
        IdMoves.moveNamespace(TEST_NAMESPACE, Simplebuilding.MOD_ID);
    }

    private static void move(ResourceKey<? extends Registry<?>> registry, Map<String, String> paths) {
        paths.forEach((oldPath, newPath) -> IdMoves.move(registry,
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, oldPath),
                Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, newPath)));
    }
}
