package com.simplebuilding.datafix;

import com.simplebuilding.Simplebuilding;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;

/**
 * Item and block ids the mod renamed, old -> new (ids match the item names).
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
 * decodes as the renamed item and is written back under the new id on the next save. The tables
 * cover stored data of every age on every Minecraft line (and run after {@code ModDataFixer}
 * for upgraded worlds, because decoding comes after fixing), and ids that exist cost nothing.
 *
 * <p>Item ids and the renamed End redstone block ids are aliased, preserving both stacks and
 * placed blocks. Only the {@code simplebuilding} namespace is looked at.
 */
public final class LegacyItemIds {

    /** Block items renamed on 26.3; their placed blocks use the same aliases. */
    public static final Map<String, String> RENAMED_BLOCKS = com.simplebuilding.version.McVersion.END_SYSTEMS
            ? Map.of("astralit_powder", "astral_redstone", "nihilith_powder", "nihil_redstone") : Map.of();

    /** Old path -> new path, both in the {@code simplebuilding} namespace. */
    public static final Map<String, String> RENAMED = !com.simplebuilding.version.McVersion.MEGA_GUIDES ? Map.of(
            "velocity-gauge", "velocity_gauge", "echo_compass", "echo_sounder",
            "laser_pointer", "amethyst_lens", "ore_detector", "detector") : Map.ofEntries(
            Map.entry("velocity-gauge", "velocity_gauge"),
            Map.entry("echo_compass", "echo_sounder"),
            Map.entry("laser_pointer", "amethyst_lens"),
            // Besitzer 2026-09-29: "Ore Detector" heisst jetzt "Detector".
            Map.entry("ore_detector", "detector"),
            Map.entry("guide_book_tools", "guide_book"),
            Map.entry("guide_book_enchantments", "guide_book"),
            Map.entry("guide_book_building", "guide_book"),
            Map.entry("guide_book_storage", "guide_book"),
            Map.entry("guide_book_machines", "guide_book"),
            Map.entry("guide_book_end", "guide_book"),
            Map.entry("guide_book_tweaks", "guide_book"),
            Map.entry("guide_book_gadgets", "guide_book"),
            Map.entry("guide_book_trims", "guide_book"),
            Map.entry("guide_book_admin", "guide_book"),
            Map.entry("guide_book_vanilla_overworld", "guide_book_vanilla_start"),
            Map.entry("guide_book_vanilla_caves", "guide_book_vanilla_start"),
            Map.entry("guide_book_vanilla_ocean", "guide_book_vanilla_start"),
            Map.entry("guide_book_vanilla_nether", "guide_book_vanilla_start"),
            Map.entry("guide_book_vanilla_end", "guide_book_vanilla_start"),
            Map.entry("guide_book_vanilla_redstone", "guide_book_vanilla_start"),
            Map.entry("guide_book_vanilla_gear", "guide_book_vanilla_start"),
            Map.entry("guide_book_vanilla_farming", "guide_book_vanilla_start"));

    private LegacyItemIds() {
    }

    /** The current id for an old item id, or {@code null} if {@code id} was never renamed. */
    @Nullable
    public static Identifier renamedTo(@Nullable Identifier id) {
        if (id == null || !Simplebuilding.MOD_ID.equals(id.getNamespace())) {
            return null;
        }
        if (!com.simplebuilding.version.McVersion.MEGA_GUIDES && id.getPath().startsWith("guide_book_")) return null;
        String now = RENAMED_BLOCKS.getOrDefault(id.getPath(), RENAMED.get(id.getPath()));
        return now == null ? null : Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, now);
    }

    /** Resolves item aliases and, in the block registry, only renamed block aliases. */
    @Nullable
    public static Identifier renamedIn(Object registry, @Nullable Identifier id) {
        if (id == null || !Simplebuilding.MOD_ID.equals(id.getNamespace())
                || !(registry instanceof Registry<?> r)) {
            return null;
        }
        if (r.key().equals(Registries.ITEM)) return renamedTo(id);
        if (!r.key().equals(Registries.BLOCK)) return null;
        String now = RENAMED_BLOCKS.get(id.getPath());
        return now == null ? null : Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, now);
    }

    /** Same for a resource key; returns the key of the new id in the same registry. */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> ResourceKey<T> renamedIn(Object registry, @Nullable ResourceKey<T> key) {
        if (key == null) {
            return null;
        }
        Identifier now = renamedIn(registry, key.identifier());
        return now == null ? null : ResourceKey.create((ResourceKey<? extends Registry<T>>) (Object) ((Registry<?>) registry).key(), now);
    }
}
