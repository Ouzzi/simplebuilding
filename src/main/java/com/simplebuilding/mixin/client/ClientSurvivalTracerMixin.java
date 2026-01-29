package com.simplebuilding.mixin.client;

import com.simplebuilding.util.SurvivalTracerAccessor;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientPlayerEntity.class)
public class ClientSurvivalTracerMixin implements SurvivalTracerAccessor {
    @Unique private int baseDist = 0;
    @Unique private int baseTime = 0;
    @Unique private int baseHostile = 0;
    @Unique private int basePassive = 0;
    @Unique private int baseDamage = 0;

    @Unique private int curDist = 0;
    @Unique private int curTime = 0;
    @Unique private int curHostile = 0;
    @Unique private int curPassive = 0;
    @Unique private int curDamage = 0;

    @Override public int simplebuilding$getBaseDistance() { return baseDist; }
    @Override public int simplebuilding$getBaseTime() { return baseTime; }
    @Override public int simplebuilding$getBaseHostileKills() { return baseHostile; }
    @Override public int simplebuilding$getBasePassiveKills() { return basePassive; }
    @Override public int simplebuilding$getBaseDamageTaken() { return baseDamage; }

    @Override
    public void simplebuilding$setBaseValues(int dist, int time, int hostile, int passive, int damage) {
        this.baseDist = dist;
        this.baseTime = time;
        this.baseHostile = hostile;
        this.basePassive = passive;
        this.baseDamage = damage;
    }

    @Override public int simplebuilding$getCurrentDistance() { return curDist; }
    @Override public int simplebuilding$getCurrentTime() { return curTime; }
    @Override public int simplebuilding$getCurrentHostileKills() { return curHostile; }
    @Override public int simplebuilding$getCurrentPassiveKills() { return curPassive; }
    @Override public int simplebuilding$getCurrentDamageTaken() { return curDamage; }

    @Override
    public void simplebuilding$setCurrentValues(int dist, int time, int hostile, int passive, int damage) {
        this.curDist = dist;
        this.curTime = time;
        this.curHostile = hostile;
        this.curPassive = passive;
        this.curDamage = damage;
    }
}