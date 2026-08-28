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
}