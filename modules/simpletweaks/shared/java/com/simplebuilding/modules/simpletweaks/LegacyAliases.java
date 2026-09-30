package com.simplebuilding.modules.simpletweaks;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
/** Exact legacy whitelist, no registrations, reflection, packets, or client-selected targets. */
public final class LegacyAliases {
    public static final Set<String> BLOCKS = Set.of(
            "spawn_teleporter",
            "spawn_teleporter_tier_2",
            "spawn_teleporter_tier_3",
            "spawn_teleporter_tier_4",
            "launchpad",
            "diamond_pressure_plate",
            "netherite_pressure_plate",
            "elytra_pad",
            "reinforced_elytra_pad",
            "netherite_elytra_pad",
            "fine_elytra_pad",
            "flypad",
            "reinforced_flypad",
            "netherite_flypad",
            "stellar_flypad",
            "chunk_loader",
            "copper_pressure_plate",
            "exposed_copper_pressure_plate",
            "weathered_copper_pressure_plate",
            "oxidized_copper_pressure_plate");
    public static final Set<String> BLOCK_ENTITIES = Set.of("spawn_teleporter_be", "launchpad_be", "elytra_pad_be", "flypad_be", "chunk_loader_be", "copper_pressure_plate_be", "netherite_pressure_plate_be");
    public static final Set<String> COMPONENTS = Set.of("flight_time", "boost_level", "last_pad_tick", "is_safe_elytra");
    public static Identifier target(Object registry, Identifier old) {
        if (old == null || !old.getNamespace().equals("simpletweaks") || !(registry instanceof Registry<?> r)) return null;
        String path = old.getPath();
        String target = null;
        if (r.key().equals(Registries.BLOCK) && BLOCKS.contains(path)) target = path;
        if (r.key().equals(Registries.ITEM)) {
            if (BLOCKS.contains(path) || path.equals("spawn_elytra")) target = path;
            if (path.equals("laser_pointer")) target = "amethyst_lens";
        }
        if (r.key().equals(Registries.BLOCK_ENTITY_TYPE) && BLOCK_ENTITIES.contains(path)) target = path;
        if (r.key().equals(Registries.DATA_COMPONENT_TYPE) && COMPONENTS.contains(path)) target = path;
        return target == null ? null : Identifier.fromNamespaceAndPath("simplebuilding", target);
    }
    private LegacyAliases() {}
}
