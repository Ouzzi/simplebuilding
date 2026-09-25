package com.simplebuilding.tweaks.xp;

import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.mixin.ExperienceOrbAccessor;
import java.util.List;
import net.minecraft.world.entity.ExperienceOrb;

/**
 * Verklumpen von XP-Kugeln (Simple Tweaks): einmal pro Sekunde schluckt eine Kugel alle im Umkreis
 * von 2 Bloecken und setzt ihr Alter zurueck. Anders als in Simple Tweaks zaehlt der Vanilla-Zaehler
 * {@code count} (gleichwertige Kugeln, die Vanilla schon zusammengelegt hat) mit - vorher gingen
 * dabei XP verloren bzw. wurden verdoppelt -, und der Wert bleibt unter 32767, weil Vanilla ihn als
 * Short speichert.
 */
public final class XpClumping {
    public static final int INTERVAL = 20;
    public static final double RADIUS = 2.0;
    public static final int MAX_VALUE = Short.MAX_VALUE;

    private XpClumping() {
    }

    public static void tick(ExperienceOrb orb) {
        if (orb.level().isClientSide() || !SimpleTweaks.config().optimization.enableXpClumps || orb.tickCount % INTERVAL != 0) {
            return;
        }
        clump(orb);
    }

    /** Schluckt die Nachbarn; gibt die Anzahl geschluckter Kugeln zurueck. */
    public static int clump(ExperienceOrb orb) {
        List<ExperienceOrb> others = orb.level().getEntitiesOfClass(ExperienceOrb.class, orb.getBoundingBox().inflate(RADIUS),
                other -> other != orb && other.isAlive());
        ExperienceOrbAccessor self = (ExperienceOrbAccessor) orb;
        long total = (long) orb.getValue() * self.simplebuilding$getCount();
        int swallowed = 0;
        for (ExperienceOrb other : others) {
            long otherTotal = (long) other.getValue() * ((ExperienceOrbAccessor) other).simplebuilding$getCount();
            if (total + otherTotal > MAX_VALUE) {
                continue;
            }
            total += otherTotal;
            other.discard();
            swallowed++;
        }
        if (swallowed > 0) {
            self.simplebuilding$setValue((int) total);
            self.simplebuilding$setCount(1);
            self.simplebuilding$setAge(0);
        }
        return swallowed;
    }
}
