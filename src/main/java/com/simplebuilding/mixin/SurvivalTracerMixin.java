package com.simplebuilding.mixin;

import com.simplebuilding.networking.SurvivalSyncPayload;
import com.simplebuilding.networking.TrimDataPayload;
import com.simplebuilding.util.SurvivalTracerAccessor;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class SurvivalTracerMixin implements SurvivalTracerAccessor {

    @Unique private int totalHostileKills = 0;
    @Unique private int totalPassiveKills = 0;

    @Unique private int baseDist = 0;
    @Unique private int baseTime = 0;
    @Unique private int baseHostile = 0;
    @Unique private int basePassive = 0;
    @Unique private int baseDamage = 0; // NEU

    // Accessor
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

    // Server Live-Werte (Dummy für Interface, echte Werte kommen aus Logic)
    @Override public int simplebuilding$getCurrentDistance() { return 0; }
    @Override public int simplebuilding$getCurrentTime() { return 0; }
    @Override public int simplebuilding$getCurrentHostileKills() { return totalHostileKills; }
    @Override public int simplebuilding$getCurrentPassiveKills() { return totalPassiveKills; }
    @Override public int simplebuilding$getCurrentDamageTaken() { return 0; } // Live aus Stats
    @Override public void simplebuilding$setCurrentValues(int dist, int time, int hostile, int passive, int damage) {}

    // Kill Tracking
    @Inject(method = "updateKilledAdvancementCriterion", at = @At("HEAD"))
    private void onUpdateKilledAdvancementCriterion(Entity entityKilled, DamageSource damageSource, CallbackInfo ci) {
        if (entityKilled == null) return;
        SpawnGroup group = entityKilled.getType().getSpawnGroup();
        if (group == SpawnGroup.MONSTER) {
            totalHostileKills++;
        } else if (group == SpawnGroup.CREATURE || group == SpawnGroup.AMBIENT || group == SpawnGroup.WATER_CREATURE || group == SpawnGroup.UNDERGROUND_WATER_CREATURE || group == SpawnGroup.AXOLOTLS) {
            totalPassiveKills++;
        }
    }

    // Sync
    @Override
    public void simplebuilding$syncTrimData() {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (player.networkHandler != null && ServerPlayNetworking.canSend(player, TrimDataPayload.ID)) {
            ServerPlayNetworking.send(player, new TrimDataPayload(baseDist, baseTime, baseHostile, basePassive, baseDamage));
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void simplebuilding$onTick(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (player.age % 20 == 0 && player.networkHandler != null) {
            int currentDist = getStatTotalDistance(player);
            int currentTime = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME));
            int currentDamage = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_TAKEN));

            if (ServerPlayNetworking.canSend(player, SurvivalSyncPayload.ID)) {
                ServerPlayNetworking.send(player, new SurvivalSyncPayload(currentDist, currentTime, totalHostileKills, totalPassiveKills, currentDamage));
            }
        }
    }

    // NBT
    @Inject(method = "writeCustomData", at = @At("TAIL"))
    public void writeSurvivalData(WriteView view, CallbackInfo ci) {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("BaseDist", baseDist);
        nbt.putInt("BaseTime", baseTime);
        nbt.putInt("BaseHostile", baseHostile);
        nbt.putInt("BasePassive", basePassive);
        nbt.putInt("BaseDamage", baseDamage);

        nbt.putInt("TotalHostile", totalHostileKills);
        nbt.putInt("TotalPassive", totalPassiveKills);
        view.put("SimpleBuildingData", NbtCompound.CODEC, nbt);
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    public void readSurvivalData(ReadView view, CallbackInfo ci) {
        view.read("SimpleBuildingData", NbtCompound.CODEC).ifPresent(nbt -> {
            baseDist = nbt.getInt("BaseDist", 0);
            baseTime = nbt.getInt("BaseTime", 0);
            baseHostile = nbt.getInt("BaseHostile", 0);
            basePassive = nbt.getInt("BasePassive", 0);
            baseDamage = nbt.getInt("BaseDamage", 0);
            totalHostileKills = nbt.getInt("TotalHostile", 0);
            totalPassiveKills = nbt.getInt("TotalPassive", 0);
        });
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    public void onRespawn(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        SurvivalTracerAccessor oldAccessor = (SurvivalTracerAccessor) oldPlayer;
        if (!alive) {
            // Reset
            ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
            this.baseDist = getStatTotalDistance(player);
            this.baseTime = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME));
            this.baseDamage = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_TAKEN));

            this.totalHostileKills = oldAccessor.simplebuilding$getCurrentHostileKills();
            this.totalPassiveKills = oldAccessor.simplebuilding$getCurrentPassiveKills();
            this.baseHostile = this.totalHostileKills;
            this.basePassive = this.totalPassiveKills;
        } else {
            // Keep
            this.baseDist = oldAccessor.simplebuilding$getBaseDistance();
            this.baseTime = oldAccessor.simplebuilding$getBaseTime();
            this.baseHostile = oldAccessor.simplebuilding$getBaseHostileKills();
            this.basePassive = oldAccessor.simplebuilding$getBasePassiveKills();
            this.baseDamage = oldAccessor.simplebuilding$getBaseDamageTaken();

            this.totalHostileKills = oldAccessor.simplebuilding$getCurrentHostileKills();
            this.totalPassiveKills = oldAccessor.simplebuilding$getCurrentPassiveKills();
        }
        this.simplebuilding$syncTrimData();
    }

    @Unique
    private int getStatTotalDistance(ServerPlayerEntity player) {
        return player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.WALK_ONE_CM)) / 100
                + player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.SPRINT_ONE_CM)) / 100
                + player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.CROUCH_ONE_CM)) / 100
                + player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.FLY_ONE_CM)) / 100
                + player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.CLIMB_ONE_CM)) / 100;
    }
}