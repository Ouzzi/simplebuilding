package com.simplebuilding.tweaks.block;

import java.util.List;
import net.minecraft.world.level.block.Block;

/**
 * Die Stufen jeder Pad-Familie, aufsteigend (docs/SIMPLETWEAKS-UEBERNAHME.md, Abschnitt 2). Die
 * letzte Stufe einer Familie ist {@link #lastTier(Family)} - dort setzen spaetere Erweiterungen an
 * (z. B. eine Kette ueber den Endstufen), ohne die Stufen selbst kennen zu muessen. Alte, abgeloeste
 * Bloecke ({@link TweaksBlocks#legacy()}) gehoeren zu keiner Familie.
 */
public final class TweaksFamilies {

    public enum Family {
        ELYTRA_PAD, FLYPAD, SPAWN_TELEPORTER, LAUNCHPAD, CHUNK_LOADER, PRESSURE_PLATE, POTION_PAD
    }

    private TweaksFamilies() {
    }

    /** Stufen der Familie, niedrigste zuerst. */
    public static List<Block> tiers(Family family) {
        return switch (family) {
            case ELYTRA_PAD -> List.of(TweaksBlocks.ELYTRA_PAD, TweaksBlocks.NETHERITE_ELYTRA_PAD, TweaksBlocks.ENDERITE_ELYTRA_PAD);
            case FLYPAD -> List.of(TweaksBlocks.FLYPAD, TweaksBlocks.REINFORCED_FLYPAD, TweaksBlocks.STELLAR_FLYPAD);
            case SPAWN_TELEPORTER -> List.of(TweaksBlocks.SPAWN_TELEPORTER, TweaksBlocks.SPAWN_TELEPORTER_TIER_2,
                    TweaksBlocks.ENDERITE_SPAWN_TELEPORTER);
            case LAUNCHPAD -> List.of(TweaksBlocks.LAUNCHPAD, TweaksBlocks.NETHERITE_LAUNCHPAD, TweaksBlocks.ENDERITE_LAUNCHPAD);
            case CHUNK_LOADER -> List.of(TweaksBlocks.CHUNK_LOADER, TweaksBlocks.NETHERITE_CHUNK_LOADER, TweaksBlocks.ENDERITE_CHUNK_LOADER);
            case PRESSURE_PLATE -> List.of(TweaksBlocks.DIAMOND_PRESSURE_PLATE, TweaksBlocks.NETHERITE_PRESSURE_PLATE,
                    TweaksBlocks.ENDERITE_PRESSURE_PLATE);
            case POTION_PAD -> List.of(TweaksBlocks.POTION_PAD, TweaksBlocks.REINFORCED_POTION_PAD, TweaksBlocks.INFUSED_POTION_PAD);
        };
    }

    /** Hoechste Stufe der Familie. */
    public static Block lastTier(Family family) {
        List<Block> tiers = tiers(family);
        return tiers.get(tiers.size() - 1);
    }
}
