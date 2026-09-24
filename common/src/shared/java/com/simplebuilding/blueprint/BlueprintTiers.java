package com.simplebuilding.blueprint;

import com.simplebuilding.items.custom.BuildingWandItem;
import net.minecraft.network.chat.Component;

/**
 * Wie gross ein Bauwerk sein darf, das ein Baustab aus einer Blaupause baut: die laengste Kante
 * der Bounding Box, je Stufe ein Wuerfel. Vorgabe des Besitzers: "bei 16 anfangen, Wuerfel";
 * die Zwischenstufen sind ein Vorschlag (docs/BLUEPRINT.md).
 */
public final class BlueprintTiers {
    public static final int COPPER = 16;
    public static final int IRON = 32;
    public static final int GOLD = 48;
    public static final int DIAMOND = 64;
    public static final int NETHERITE = 96;
    public static final int ENDERITE = 128;

    /** Stufen aufsteigend, parallel zu {@link #NAMES}. */
    public static final int[] EDGES = {COPPER, IRON, GOLD, DIAMOND, NETHERITE, ENDERITE};
    public static final String[] NAMES = {"copper", "iron", "gold", "diamond", "netherite", "enderite"};

    private BlueprintTiers() {
    }

    /** Kantenlaenge, die ein Baustab schafft - aus seinem Flaechen-Durchmesser (3, 5, ... 13). */
    public static int edgeFor(BuildingWandItem wand) {
        return switch (wand.getWandSquareDiameter()) {
            case BuildingWandItem.BUILDING_WAND_SQUARE_IRON -> IRON;
            case BuildingWandItem.BUILDING_WAND_SQUARE_GOLD -> GOLD;
            case BuildingWandItem.BUILDING_WAND_SQUARE_DIAMOND -> DIAMOND;
            case BuildingWandItem.BUILDING_WAND_SQUARE_NETHERITE -> NETHERITE;
            case BuildingWandItem.BUILDING_WAND_SQUARE_ENDERITE -> ENDERITE;
            default -> COPPER;
        };
    }

    /** Index der kleinsten Stufe, die eine Kante schafft; -1 wenn keine (ueber 128). */
    public static int tierIndexFor(int maxEdge) {
        for (int i = 0; i < EDGES.length; i++) {
            if (maxEdge <= EDGES[i]) {
                return i;
            }
        }
        return -1;
    }

    /** Name der Baustab-Stufe, z. B. "Eisen-Baustab". */
    public static Component wandName(int tierIndex) {
        return Component.translatable("item.simplebuilding." + NAMES[tierIndex] + "_building_wand");
    }
}
