package com.simplebuilding.dummy;

import com.simplebuilding.config.ServerTuning;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Vogelscheuche (Besitzer 2026-10-03): ein Stroh-Ruestungsstaender oder eine Trainingspuppe haelt Tiere und Monster
 * davon ab, Ackerland zu zertrampeln - im Umkreis {@code server.features.scarecrowRadius} (Standard 8, 0 = aus,
 * hoechstens {@link #MAX_RADIUS}). Spieler trampeln weiter (ihre eigene Entscheidung). Haengt am selben Haken wie der
 * Breezekopf ({@code HeadAbilities#tramplesFarmland}): Vanilla-Mixin auf Fabric, Trampel-Ereignis auf NeoForge/Forge.
 * Es entsteht nichts (kein Item, kein Ertrag), daher nichts auszunutzen; gesucht wird nur bei einem Trampel-Versuch.
 */
public final class Scarecrow {
    public static final int MAX_RADIUS = 16;

    private Scarecrow() {
    }

    /** Ob eine Vogelscheuche in der Naehe {@code entity} am Zertrampeln hindert. */
    public static boolean guards(Entity entity) {
        if (entity instanceof Player || entity.level().isClientSide()) {
            return false;
        }
        int radius = ServerTuning.get().features.scarecrowRadius;
        if (radius <= 0) {
            return false;
        }
        return !entity.level().getEntitiesOfClass(TrainingDummy.class, entity.getBoundingBox().inflate(radius),
                dummy -> dummy.distanceToSqr(entity) <= (double) radius * radius).isEmpty();
    }
}
