package com.simplebuilding.util;

import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ContainerData;

/**
 * Die Ofenmenue-Daten, wie sie an den Client gehen: Brennzeit (0, 1) und Kochzeit (2, 3) werden
 * paarweise durch denselben Teiler geteilt, sobald die jeweilige Gesamtzeit nicht mehr in ein short
 * passt. Aus 10000 von 72000 wird so 3333 von 24000 - der Client zeichnet Pfeil und Flamme ohnehin
 * nur aus dem Verhaeltnis. Kurze Zeiten gehen unveraendert durch, Schreiben geht immer unveraendert
 * an das Original. Siehe {@code AbstractFurnaceMenuMixin}.
 */
public final class LongCookContainerData implements ContainerData {

    private final ContainerData delegate;

    public LongCookContainerData(ContainerData delegate) {
        this.delegate = delegate;
    }

    @Override
    public int get(int index) {
        int value = this.delegate.get(index);
        if (index < 0 || index > 3 || this.delegate.getCount() < 4) {
            return value;
        }
        int total = this.delegate.get(index < 2 ? 1 : 3);
        if (total <= Short.MAX_VALUE) {
            return value;
        }
        int divisor = Mth.positiveCeilDiv(total, Short.MAX_VALUE);
        // Ein laufender Wert darf durch das Teilen nicht auf 0 fallen: 0 heisst fuer den Client
        // "brennt nicht" bzw. "kocht nicht".
        return value <= 0 ? value : Math.max(1, value / divisor);
    }

    @Override
    public void set(int index, int value) {
        this.delegate.set(index, value);
    }

    @Override
    public int getCount() {
        return this.delegate.getCount();
    }
}
