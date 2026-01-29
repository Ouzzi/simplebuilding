package com.simplebuilding.mixin.client;

import com.simplebuilding.util.SurvivalTracerAccessor;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientPlayerEntity.class)
public class ClientSurvivalTracerMixin implements SurvivalTracerAccessor {
    // Basis Werte
    @Unique private int baseDistance = 0;
    @Unique private int baseKills = 0;
    @Unique private int baseTime = 0;

    // NEU: Live Werte Cache
    @Unique private int currentDistance = 0;
    @Unique private int currentKills = 0;
    @Unique private int currentTime = 0;

    @Override public int simplebuilding$getBaseDistance() { return baseDistance; }
    @Override public int simplebuilding$getBaseKills() { return baseKills; }
    @Override public int simplebuilding$getBaseTime() { return baseTime; }

    @Override
    public void simplebuilding$setBaseValues(int dist, int kills, int time) {
        this.baseDistance = dist;
        this.baseKills = kills;
        this.baseTime = time;
    }

    // Accessor Implementierung für Live Werte
    @Override public int simplebuilding$getCurrentDistance() { return currentDistance; }
    @Override public int simplebuilding$getCurrentKills() { return currentKills; }
    @Override public int simplebuilding$getCurrentTime() { return currentTime; }

    @Override
    public void simplebuilding$setCurrentValues(int dist, int kills, int time) {
        this.currentDistance = dist;
        this.currentKills = kills;
        this.currentTime = time;
    }
}