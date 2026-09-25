package com.simplebuilding.util;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.config.SimplebuildingConfig;
import com.simplebuilding.items.custom.BuildingWandItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * EXPERIMENTELL (Besitzer 2026-09-25): Bauen mit dem Baustab macht hungrig. Jeder gesetzte Block
 * - normales Flaechenbauen wie Blaupausen-Bau - kostet Vanilla-Erschoepfung (exhaustion), je
 * staerker der Stab, desto weniger pro Block. Schalter: {@code tools.buildingWandHungerCost}
 * (Standard an). Kreativmodus ({@code Abilities.instabuild}) ist ausgenommen.
 *
 * <p><b>Vanilla-Einheiten:</b> 4.0 Erschoepfung = 1 Punkt; die Saettigung wird zuerst
 * abgebaut, danach die Hungerleiste. Eine "volle Leiste" sind hier 20 Hunger + 20 Saettigung
 * (mehr Saettigung als Hunger gibt es nicht) = 40 Punkte = {@link #FULL_BAR_EXHAUSTION} 160.
 *
 * <p><b>Eichung:</b> Kupfer-Stab fuellt seinen groessten Wuerfel (16³ = 4096 Bloecke) → ein
 * Viertel der vollen Leiste (40.0); Enderit-Stab baut 128³ (2 097 152 Bloecke) → die ganze Leiste
 * (160.0), alles Groessere leert sie auch nur. Die Stufen dazwischen liegen geometrisch
 * (Faktor 2^-1.4 ≈ 0.3789 je Stufe, 128^(1/5)):
 *
 * <pre>
 * Stufe      Erschoepfung/Block  Bloecke je Punkt  16³      eigener Wuerfel      128³
 * Kupfer     9.765625e-3          410             40.0     16³:   40.0 (1/4)   20480
 * Eisen      3.700480e-3         1081             15.2     32³:  121.3          7760
 * Gold       1.402220e-3         2853              5.7     48³:  155.1          2941
 * Diamant    5.313419e-4         7528              2.2     64³:  139.3          1114
 * Netherit   2.013409e-4        19867              0.8    128³:  422.2           422
 * Enderit    7.629395e-5        52429              0.3    256³: 1280.0           160 (voll)
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
    /** 20 Hunger + 20 Saettigung, in Erschoepfung. */
    public static final float FULL_BAR_EXHAUSTION = 40 * EXHAUSTION_PER_POINT;

    /** Eichpunkt Kupfer: 16³ Bloecke = eine Viertel-Leiste. */
    public static final double COPPER_PER_BLOCK = FULL_BAR_EXHAUSTION / 4.0 / (16 * 16 * 16);
    /** Eichpunkt Enderit: 128³ Bloecke = die volle Leiste. */
    public static final double ENDERITE_PER_BLOCK = FULL_BAR_EXHAUSTION / (128.0 * 128 * 128);

    /** Stufen Kupfer, Eisen, Gold, Diamant, Netherit, Enderit (wie {@code BlueprintTiers.NAMES}). */
    public static final String[] TIERS = {"copper", "iron", "gold", "diamond", "netherite", "enderite"};

    /** Erschoepfung je Block, parallel zu {@link #TIERS}: geometrisch zwischen den Eichpunkten. */
    public static final double[] PER_BLOCK = new double[TIERS.length];

    static {
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

    /** Erschoepfung fuer {@code blocks} Bloecke mit diesem Stab, ohne Schalter/Kreativ-Pruefung. */
    public static double exhaustionFor(Item wand, long blocks) {
        int tier = tierOf(wand);
        return tier < 0 || blocks <= 0 ? 0.0 : PER_BLOCK[tier] * blocks;
    }

    /** Ist die (experimentelle) Hungerkosten-Option an? Ohne geladene Config: an (Standard). */
    public static boolean enabled() {
        SimplebuildingConfig config = Simplebuilding.getConfig();
        return config == null || config.tools.buildingWandHungerCost;
    }

    /**
     * Rechnet {@code blocks} gesetzte Bloecke ab: fuegt die Erschoepfung direkt der
     * {@code FoodData} hinzu und liefert, wie viel. 0 im Kreativmodus, auf dem Client, bei
     * abgeschalteter Option oder fuer einen Nicht-Baustab.
     */
    public static float exhaust(Player player, Item wand, int blocks) {
        if (blocks <= 0 || player.level().isClientSide() || player.getAbilities().instabuild || !enabled()) {
            return 0.0F;
        }
        float amount = (float) exhaustionFor(wand, blocks);
        if (amount > 0.0F) {
            player.getFoodData().addExhaustion(amount);
        }
        return amount;
    }
}
