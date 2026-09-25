package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blueprint.BlueprintTiers;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.items.custom.BuildingWandItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * EXPERIMENTELL (Besitzer 2026-09-25, nachgeschaerft am selben Tag): Viel auf einmal bauen macht
 * hungrig, normales Bauen nicht. Je Bauvorgang - ein Klick mit dem Baustab bzw. ein
 * Blaupausen-Bau - sind die ersten {@link #ALLOWANCE} Bloecke frei (1/16 des eigenen
 * Maximalwuerfels, mindestens 256); erst jeder weitere Block kostet Vanilla-Erschoepfung, je
 * staerker der Stab, desto weniger. Flaechen bis 13x13 (169 Bloecke) kosten damit nie etwas.
 * Schalter: {@code tools.buildingWandHungerCost} (Standard an). Kreativmodus
 * ({@code Abilities.instabuild}) ist ausgenommen.
 *
 * <p><b>Einheiten:</b> 4.0 Erschoepfung = 1 Punkt (Vanilla; Saettigung sinkt zuerst). "Volle
 * Leiste" = die sichtbaren 20 Hungerpunkte = {@link #FULL_BAR_EXHAUSTION} 80.
 *
 * <p><b>Eichung</b> (nach Abzug des Freibetrags): Kupfer fuellt 16³ (4096 - 256 = 3840 bezahlte
 * Bloecke) → ein Viertel der Leiste (20.0); Enderit baut 128³ (2 097 152 - 1 048 576 = 1 048 576
 * bezahlte Bloecke) → die volle Leiste (80.0), alles Groessere leert sie auch nur. Die Raten
 * dazwischen liegen geometrisch (Faktor ≈ 0.4297 je Stufe):
 *
 * <pre>
 * Stufe      Freibetrag  Erschoepfung/Block  Bloecke je Punkt  eigener Wuerfel     64³     128³
 * Kupfer          256    5.208333e-3          768             16³:   20.0 (1/4)  1364    10921
 * Eisen          2048    2.237984e-3         1787             32³:   68.8         582     4689
 * Gold           6912    9.616461e-4         4160             48³:   99.7         245     2010
 * Diamant       16384    4.132126e-4         9680             64³:  101.6         102      860
 * Netherit     131072    1.775546e-4        22528            128³:  349.1          23      349
 * Enderit     1048576    7.629395e-5        52429            256³: 1200.0           0       80 (voll)
 * </pre>
 *
 * <p>Die Funktion schadet nie direkt: sie fuegt nur Erschoepfung hinzu. Verhungert der Spieler
 * danach bei leerer Leiste, ist das Vanillas Hungerschaden (Schwierigkeit entscheidet), nicht
 * dieser Code. {@code FoodData#addExhaustion} deckelt den Puffer bei 40 und baut je Tick 4 ab;
 * Blaupausen-Bauten verteilen sich ueber bis zu 9 Sekunden, daher geht selbst beim groessten
 * Bau hoechstens Erschoepfung verloren, die die Leiste ohnehin schon leeren wuerde.
 */
public final class WandHunger {
    /** Vanilla: so viel Erschoepfung kostet einen Punkt Saettigung oder Hunger. */
    public static final float EXHAUSTION_PER_POINT = 4.0F;
    /** Die sichtbaren 20 Hungerpunkte, in Erschoepfung. */
    public static final float FULL_BAR_EXHAUSTION = 20 * EXHAUSTION_PER_POINT;

    /** Stufen Kupfer, Eisen, Gold, Diamant, Netherit, Enderit (wie {@code BlueprintTiers.NAMES}). */
    public static final String[] TIERS = {"copper", "iron", "gold", "diamond", "netherite", "enderite"};

    /** Freie Bloecke je Bauvorgang, parallel zu {@link #TIERS}: 1/16 des Maximalwuerfels, mind. 256. */
    public static final long[] ALLOWANCE = new long[TIERS.length];

    /** Eichpunkt Kupfer: 16³ Bloecke nach Freibetrag = eine Viertel-Leiste. */
    public static final double COPPER_PER_BLOCK;
    /** Eichpunkt Enderit: 128³ Bloecke nach Freibetrag = die volle Leiste. */
    public static final double ENDERITE_PER_BLOCK;

    /** Erschoepfung je bezahltem Block, parallel zu {@link #TIERS}: geometrisch zwischen den Eichpunkten. */
    public static final double[] PER_BLOCK = new double[TIERS.length];

    static {
        for (int i = 0; i < ALLOWANCE.length; i++) {
            long edge = BlueprintTiers.EDGES[i];
            ALLOWANCE[i] = Math.max(256L, edge * edge * edge / 16);
        }
        COPPER_PER_BLOCK = FULL_BAR_EXHAUSTION / 4.0 / (16L * 16 * 16 - ALLOWANCE[0]);
        ENDERITE_PER_BLOCK = FULL_BAR_EXHAUSTION / (double) (128L * 128 * 128 - ALLOWANCE[TIERS.length - 1]);
        for (int i = 0; i < PER_BLOCK.length; i++) {
            PER_BLOCK[i] = COPPER_PER_BLOCK
                    * Math.pow(ENDERITE_PER_BLOCK / COPPER_PER_BLOCK, i / (double) (PER_BLOCK.length - 1));
        }
        PER_BLOCK[PER_BLOCK.length - 1] = ENDERITE_PER_BLOCK; // exakt, ohne Rundung aus pow
    }

    private WandHunger() {
    }

    /** Stufe des Baustabs (Index in {@link #TIERS}); -1 fuer alles, was kein Baustab ist. */
    public static int tierOf(Item wand) {
        if (!(wand instanceof BuildingWandItem wandItem)) {
            return -1;
        }
        return switch (wandItem.getWandSquareDiameter()) {
            case BuildingWandItem.BUILDING_WAND_SQUARE_IRON -> 1;
            case BuildingWandItem.BUILDING_WAND_SQUARE_GOLD -> 2;
            case BuildingWandItem.BUILDING_WAND_SQUARE_DIAMOND -> 3;
            case BuildingWandItem.BUILDING_WAND_SQUARE_NETHERITE -> 4;
            case BuildingWandItem.BUILDING_WAND_SQUARE_ENDERITE -> 5;
            default -> 0;
        };
    }

    /** Freie Bloecke je Bauvorgang fuer diesen Stab; 0 fuer einen Nicht-Baustab. */
    public static long allowanceFor(Item wand) {
        int tier = tierOf(wand);
        return tier < 0 ? 0 : ALLOWANCE[tier];
    }

    /**
     * Erschoepfung eines ganzen Bauvorgangs von {@code blocks} Bloecken mit diesem Stab (Freibetrag
     * abgezogen), ohne Schalter/Kreativ-Pruefung.
     */
    public static double exhaustionFor(Item wand, long blocks) {
        int tier = tierOf(wand);
        return tier < 0 ? 0.0 : PER_BLOCK[tier] * Math.max(0L, blocks - ALLOWANCE[tier]);
    }

    /** Ist die (experimentelle) Hungerkosten-Option an? Ohne geladene Config: an (Standard). */
    public static boolean enabled() {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        return config == null || config.tools.buildingWandHungerCost;
    }

    /**
     * Rechnet einen gerade gesetzten Block ab, den {@code indexInOperation}-ten (ab 1) des laufenden
     * Bauvorgangs: innerhalb des Freibetrags nichts, danach die Rate der Stufe, direkt auf die
     * {@code FoodData}. Liefert die hinzugefuegte Erschoepfung; 0 im Kreativmodus, auf dem Client,
     * bei abgeschalteter Option oder fuer einen Nicht-Baustab.
     */
    public static float exhaust(Player player, Item wand, long indexInOperation) {
        int tier = tierOf(wand);
        if (tier < 0 || indexInOperation <= ALLOWANCE[tier] || player.level().isClientSide()
                || player.getAbilities().instabuild || !enabled()) {
            return 0.0F;
        }
        float amount = (float) PER_BLOCK[tier];
        player.getFoodData().addExhaustion(amount);
        return amount;
    }
}
