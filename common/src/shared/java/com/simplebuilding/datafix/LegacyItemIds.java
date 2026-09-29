package com.simplebuilding.datafix;

import com.simplebuilding.Simplebuilding;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;

/**
 * Item ids the mod renamed, old -> new (owner decision 2026-09-28: ids match the item names).
 *
 * <p><b>Why a registry alias and not a DataFixer step.</b> {@code ModDataFixer} only runs when a
 * world comes from an older <em>Minecraft</em> version ({@code from < to}); a rename inside the mod
 * keeps the data version, so a world saved yesterday on the same Minecraft version would never
 * reach it. Every place that turns a saved id back into an item - item stacks in chunks, player
 * data, containers, backpack contents, bundles, item frames, item entities, structure files,
 * recipes/advancements/loot of a data pack, {@code /give} - asks the item registry by name.
 * {@code MappedRegistryAliasMixin} (Fabric, NeoForge) and, on Forge, {@code NamespacedWrapperAliasMixin}
 * for the holder lookups plus Forge's own registry aliases ({@code ForgeRegistryBootstrap}, for
 * {@code getValue}/{@code containsKey}, which the defaulted item wrapper answers without the
 * mixin) answer a lookup that <em>missed</em> with the item under its new id, so an old stack
 * decodes as the renamed item and is written back under the new id on the next save. One table
 * covers stored data of every age on every Minecraft line (and runs after {@code ModDataFixer}
 * for upgraded worlds, because decoding comes after fixing), and ids that exist cost nothing.
 *
 * <p>Only item ids are aliased: none of the renamed items has a block, so there are no placed
 * blocks to fix. Only the {@code simplebuilding} namespace is looked at.
 */
public final class LegacyItemIds {

    /** Old path -> new path, both in the {@code simplebuilding} namespace. */
    public static final Map<String, String> RENAMED = Map.of(
            "velocity-gauge", "velocity_gauge",
            "echo_compass", "echo_sounder",
            "laser_pointer", "amethyst_lens",
            // Besitzer 2026-09-29: "Ore Detector" heisst jetzt "Detector".
            "ore_detector", "detector");

    private LegacyItemIds() {
    }

    /** The current id for an old item id, or {@code null} if {@code id} was never renamed. */
    @Nullable
    public static Identifier renamedTo(@Nullable Identifier id) {
        if (id == null || !Simplebuilding.MOD_ID.equals(id.getNamespace())) {
            return null;
        }
        String now = RENAMED.get(id.getPath());
        return now == null ? null : Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, now);
    }

    /** {@link #renamedTo(Identifier)}, but only when {@code registry} is the item registry. */
    @Nullable
    public static Identifier renamedIn(Object registry, @Nullable Identifier id) {
        if (id == null || !Simplebuilding.MOD_ID.equals(id.getNamespace())
                || !(registry instanceof Registry<?> r) || !r.key().equals(Registries.ITEM)) {
            return null;
        }
        return renamedTo(id);
    }

    /** Same for a resource key; returns the key of the new id in the same registry. */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> ResourceKey<T> renamedIn(Object registry, @Nullable ResourceKey<T> key) {
        if (key == null) {
            return null;
        }
        Identifier now = renamedIn(registry, key.identifier());
        return now == null ? null : ResourceKey.create((ResourceKey<? extends Registry<T>>) (Object) Registries.ITEM, now);
    }
}
