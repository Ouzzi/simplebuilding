package com.simplebuilding.trade;

import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import net.minecraft.util.RandomSource;

/**
 * Gewichtete Zufallsauswahl aus einer festen Liste — loader-neutral.
 *
 * <p>Einzige Quelle der Auswahl-Logik hinter {@code simplebuilding:weighted_enchant}: sowohl die
 * Loot-Funktion {@link com.simplebuilding.loot.WeightedEnchantFunction} (26.2-Datenpfad) als auch
 * die code-registrierten Trades der 1.21.11-Linie ({@link EnchantmentPool}) rufen hier hinein,
 * damit beide Pfade dieselbe Verteilung erzeugen.
 */
public final class WeightedPicker {
    private WeightedPicker() {
    }

    /**
     * Zieht genau einen Eintrag nach Gewicht. Liefert {@code null}, wenn die Liste leer ist oder
     * das Gesamtgewicht nicht positiv ist.
     */
    public static <T> T pick(List<T> entries, ToIntFunction<T> weight, RandomSource random) {
        int totalWeight = 0;
        for (T entry : entries) {
            totalWeight += weight.applyAsInt(entry);
        }
        if (totalWeight <= 0) {
            return null;
        }
        int pick = random.nextInt(totalWeight);
        int currentWeight = 0;
        for (T entry : entries) {
            currentWeight += weight.applyAsInt(entry);
            if (pick < currentWeight) {
                return entry;
            }
        }
        return entries.get(0);
    }

    /**
     * Zieht einen Eintrag nach Gewicht und — mit Wahrscheinlichkeit {@code secondChance} — einen
     * zweiten aus demselben Pool. Der zweite Zug wird bis zu zehnmal wiederholt, solange er
     * dieselbe Identität wie der erste hat (z. B. dieselbe Verzauberung in anderer Stufe);
     * bleibt er kollidierend, entfällt der zweite Eintrag.
     *
     * @return leere Liste (kein Zug möglich), ein Element oder zwei Elemente in Zugreihenfolge.
     */
    public static <T> List<T> pickOneOrTwo(List<T> entries, ToIntFunction<T> weight,
                                           Function<T, ?> identity, float secondChance, RandomSource random) {
        T first = pick(entries, weight, random);
        if (first == null) {
            return List.of();
        }
        // Kurzschluss beibehalten: bei Pools mit einem Eintrag wird kein Zufallswert verbraucht.
        if (entries.size() <= 1 || random.nextFloat() >= secondChance) {
            return List.of(first);
        }

        Object firstIdentity = identity.apply(first);
        T second = pick(entries, weight, random);
        int attempts = 0;
        while (second != null && firstIdentity.equals(identity.apply(second)) && attempts < 10) {
            second = pick(entries, weight, random);
            attempts++;
        }
        if (second == null || firstIdentity.equals(identity.apply(second))) {
            return List.of(first);
        }
        return List.of(first, second);
    }
}
