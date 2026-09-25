package com.simplebuilding.util;

public interface SurvivalTracerAccessor {
    // Basis-Werte
    int simplebuilding$getBaseDistance();
    int simplebuilding$getBaseTime();
    int simplebuilding$getBaseHostileKills();
    int simplebuilding$getBasePassiveKills();
    int simplebuilding$getBaseDamageTaken(); // NEU

    void simplebuilding$setBaseValues(int dist, int time, int hostile, int passive, int damage);

    // Live-Werte
    int simplebuilding$getCurrentDistance();
    int simplebuilding$getCurrentTime();
    int simplebuilding$getCurrentHostileKills();
    int simplebuilding$getCurrentPassiveKills();
    int simplebuilding$getCurrentDamageTaken(); // NEU

    void simplebuilding$setCurrentValues(int dist, int time, int hostile, int passive, int damage);

    default void simplebuilding$syncTrimData() {}

    /**
     * Erfahrungspunkte ({@code Player#totalExperience}) zum Zeitpunkt des letzten Todes. Die Resonanz
     * zaehlt nur, was seitdem gesammelt wurde; mit keepInventory behaelt der Spieler seine Punkte,
     * der Tod kostet trotzdem.
     */
    default int simplebuilding$getBaseXp() { return 0; }

    default void simplebuilding$setBaseXp(int xp) {}

    /**
     * Server: die aktiv verbrachten Ticks (bewegt innerhalb der letzten Minute), die die Zeitkurve
     * der Resonanz statt der reinen Spielzeit speisen. Setter fuer Tests.
     */
    default void simplebuilding$setActiveTime(int ticks) {}
}