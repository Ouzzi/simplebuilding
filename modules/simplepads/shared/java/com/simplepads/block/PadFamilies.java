package com.simplepads.block;

import java.util.List;
import net.minecraft.world.level.block.Block;

/**
 * Die Stufen jeder Pad-Familie, aufsteigend (docs/SIMPLETWEAKS-UEBERNAHME.md, Abschnitt 2). Die
 * letzte Stufe einer Familie ist {@link #lastTier(Family)} - dort setzen spaetere Erweiterungen an
 * (z. B. eine Kette ueber den Endstufen), ohne die Stufen selbst kennen zu muessen. Alte, abgeloeste
 * Bloecke ({@link PadsBlocks#legacy()}) gehoeren zu keiner Familie.
 */
public final class PadFamilies {

    public enum Family {
        ELYTRA_PAD, FLYPAD, SPAWN_TELEPORTER, LAUNCHPAD, CHUNK_LOADER, PRESSURE_PLATE, POTION_PAD
    }

    private PadFamilies() {
    }

    /** Stufen der Familie, niedrigste zuerst. */
    public static List<Block> tiers(Family family) {
        return switch (family) {
            case ELYTRA_PAD -> List.of(PadsBlocks.ELYTRA_PAD, PadsBlocks.NETHERITE_ELYTRA_PAD, PadsBlocks.ENDERITE_ELYTRA_PAD);
            case FLYPAD -> List.of(PadsBlocks.FLYPAD, PadsBlocks.REINFORCED_FLYPAD, PadsBlocks.STELLAR_FLYPAD);
            case SPAWN_TELEPORTER -> List.of(PadsBlocks.SPAWN_TELEPORTER, PadsBlocks.SPAWN_TELEPORTER_TIER_2,
                    PadsBlocks.ENDERITE_SPAWN_TELEPORTER);
            case LAUNCHPAD -> List.of(PadsBlocks.LAUNCHPAD, PadsBlocks.NETHERITE_LAUNCHPAD, PadsBlocks.ENDERITE_LAUNCHPAD);
            case CHUNK_LOADER -> List.of(PadsBlocks.CHUNK_LOADER, PadsBlocks.NETHERITE_CHUNK_LOADER, PadsBlocks.ENDERITE_CHUNK_LOADER);
            case PRESSURE_PLATE -> List.of(PadsBlocks.DIAMOND_PRESSURE_PLATE, PadsBlocks.NETHERITE_PRESSURE_PLATE,
                    PadsBlocks.ENDERITE_PRESSURE_PLATE);
            case POTION_PAD -> List.of(PadsBlocks.POTION_PAD, PadsBlocks.REINFORCED_POTION_PAD, PadsBlocks.INFUSED_POTION_PAD);
        };
    }

    /** Hoechste Stufe der Familie. */
    public static Block lastTier(Family family) {
        List<Block> tiers = tiers(family);
        return tiers.get(tiers.size() - 1);
    }
}
