package com.simplebuilding.util;

public interface SurvivalTracerAccessor {
    // Basis-Werte (Snapshot beim Tod)
    int simplebuilding$getBaseDistance();
    int simplebuilding$getBaseKills();
    int simplebuilding$getBaseTime();
    void simplebuilding$setBaseValues(int dist, int kills, int time);

    // NEU: Aktuelle Live-Werte (vom Server gesendet)
    int simplebuilding$getCurrentDistance();
    int simplebuilding$getCurrentKills();
    int simplebuilding$getCurrentTime();
    void simplebuilding$setCurrentValues(int dist, int kills, int time);

    default void simplebuilding$syncTrimData() {}
}