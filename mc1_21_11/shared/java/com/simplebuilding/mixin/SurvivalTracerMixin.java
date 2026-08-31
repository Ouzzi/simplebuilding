package com.simplebuilding.mixin;

import com.simplebuilding.networking.SurvivalSyncPayload;
import com.simplebuilding.networking.TrimDataPayload;
import com.simplebuilding.platform.PlatformServices;
import com.simplebuilding.util.SurvivalTracerAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
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
    @Inject(method = "awardKillScore", at = @At("HEAD"))
    private void onUpdateKilledAdvancementCriterion(Entity entityKilled, DamageSource damageSource, CallbackInfo ci) {
        if (entityKilled == null) return;
        MobCategory group = entityKilled.getType().getCategory();
        if (group == MobCategory.MONSTER) {
            totalHostileKills++;
        } else if (group == MobCategory.CREATURE || group == MobCategory.AMBIENT || group == MobCategory.WATER_CREATURE || group == MobCategory.UNDERGROUND_WATER_CREATURE || group == MobCategory.AXOLOTLS) {
            totalPassiveKills++;
        }
    }

    // Sync
    @Override
    public void simplebuilding$syncTrimData() {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.connection != null && PlatformServices.canSendToPlayer(player, TrimDataPayload.ID)) {
            PlatformServices.sendToPlayer(player, new TrimDataPayload(baseDist, baseTime, baseHostile, basePassive, baseDamage));
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void simplebuilding$onTick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.tickCount % 20 == 0 && player.connection != null) {
            int currentDist = getStatTotalDistance(player);
            int currentTime = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
            int currentDamage = player.getStats().getValue(Stats.CUSTOM.get(Stats.DAMAGE_TAKEN));

            if (PlatformServices.canSendToPlayer(player, SurvivalSyncPayload.ID)) {
                PlatformServices.sendToPlayer(player, new SurvivalSyncPayload(currentDist, currentTime, totalHostileKills, totalPassiveKills, currentDamage));
            }
        }
    }

    // NBT
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    public void writeSurvivalData(ValueOutput view, CallbackInfo ci) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("BaseDist", baseDist);
        nbt.putInt("BaseTime", baseTime);
        nbt.putInt("BaseHostile", baseHostile);
        nbt.putInt("BasePassive", basePassive);
        nbt.putInt("BaseDamage", baseDamage);

        nbt.putInt("TotalHostile", totalHostileKills);
        nbt.putInt("TotalPassive", totalPassiveKills);
        view.store("SimpleBuildingData", CompoundTag.CODEC, nbt);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    public void readSurvivalData(ValueInput view, CallbackInfo ci) {
        view.read("SimpleBuildingData", CompoundTag.CODEC).ifPresent(nbt -> {
            baseDist = nbt.getIntOr("BaseDist", 0);
            baseTime = nbt.getIntOr("BaseTime", 0);
            baseHostile = nbt.getIntOr("BaseHostile", 0);
            basePassive = nbt.getIntOr("BasePassive", 0);
            baseDamage = nbt.getIntOr("BaseDamage", 0);
            totalHostileKills = nbt.getIntOr("TotalHostile", 0);
            totalPassiveKills = nbt.getIntOr("TotalPassive", 0);
        });
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    public void onRespawn(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        SurvivalTracerAccessor oldAccessor = (SurvivalTracerAccessor) oldPlayer;
        if (!alive) {
            // Reset
            ServerPlayer player = (ServerPlayer) (Object) this;
            this.baseDist = getStatTotalDistance(player);
            this.baseTime = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
            this.baseDamage = player.getStats().getValue(Stats.CUSTOM.get(Stats.DAMAGE_TAKEN));

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
    private int getStatTotalDistance(ServerPlayer player) {
        return player.getStats().getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM)) / 100
                + player.getStats().getValue(Stats.CUSTOM.get(Stats.SPRINT_ONE_CM)) / 100
                + player.getStats().getValue(Stats.CUSTOM.get(Stats.CROUCH_ONE_CM)) / 100
                + player.getStats().getValue(Stats.CUSTOM.get(Stats.FLY_ONE_CM)) / 100
                + player.getStats().getValue(Stats.CUSTOM.get(Stats.CLIMB_ONE_CM)) / 100;
    }
}