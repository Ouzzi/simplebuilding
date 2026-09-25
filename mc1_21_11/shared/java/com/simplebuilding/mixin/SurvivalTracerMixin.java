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
    @Unique private int baseXp = 0;

    /** Aktiv verbrachte Ticks (Summe ueber alle Leben); die Zeitkurve liest activeTicks - baseTime. */
    @Unique private int activeTicks = 0;
    /** Distanz beim letzten Blick und der Tick, zu dem sie sich zuletzt geaendert hat. */
    @Unique private int lastSeenDistance = -1;
    @Unique private int lastMovedTick = Integer.MIN_VALUE / 2;

    /** So lange nach der letzten Bewegung zaehlt die Zeit noch (Bauen im Stehen ist kein AFK). */
    @Unique private static final int ACTIVE_GRACE_TICKS = 1200;

    // Accessor
    @Override public int simplebuilding$getBaseDistance() { return baseDist; }
    @Override public int simplebuilding$getBaseTime() { return baseTime; }
    @Override public int simplebuilding$getBaseHostileKills() { return baseHostile; }
    @Override public int simplebuilding$getBasePassiveKills() { return basePassive; }
    @Override public int simplebuilding$getBaseDamageTaken() { return baseDamage; }
    @Override public int simplebuilding$getBaseXp() { return baseXp; }
    @Override public void simplebuilding$setBaseXp(int xp) { this.baseXp = xp; }
    @Override public void simplebuilding$setActiveTime(int ticks) { this.activeTicks = ticks; }

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
    @Override public int simplebuilding$getCurrentTime() { return activeTicks; }
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
            PlatformServices.sendToPlayer(player, new TrimDataPayload(baseDist, baseTime, baseHostile, basePassive, baseDamage, baseXp));
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void simplebuilding$onTick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.tickCount % 20 == 0) {
            // Kein AFK: die Zeit zaehlt nur, solange sich der Spieler in der letzten Minute bewegt hat.
            int distance = getStatTotalDistance(player);
            if (lastSeenDistance >= 0 && distance != lastSeenDistance) {
                lastMovedTick = player.tickCount;
            }
            lastSeenDistance = distance;
            if (player.tickCount - lastMovedTick <= ACTIVE_GRACE_TICKS) {
                activeTicks += 20;
            }
        }
        if (player.tickCount % 20 == 0 && player.connection != null) {
            int currentDist = getStatTotalDistance(player);
            int currentTime = activeTicks;
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
        nbt.putInt("BaseXp", baseXp);
        nbt.putInt("ActiveTicks", activeTicks);

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
            baseXp = nbt.getIntOr("BaseXp", 0);
            // Aeltere Spielstaende kennen keinen Aktivzaehler; ihre BaseTime war eine Spielzeit-Statistik
            // und passt nicht zum neuen Zaehler, der bei 0 beginnt.
            if (nbt.contains("ActiveTicks")) {
                activeTicks = nbt.getIntOr("ActiveTicks", 0);
            } else {
                activeTicks = 0;
                baseTime = 0;
            }
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
            this.activeTicks = oldAccessor.simplebuilding$getCurrentTime();
            this.baseTime = this.activeTicks;
            this.baseDamage = player.getStats().getValue(Stats.CUSTOM.get(Stats.DAMAGE_TAKEN));
            // Vanilla hat totalExperience hier schon uebernommen (keepInventory) oder bei 0 gelassen.
            this.baseXp = player.totalExperience;

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
            this.baseXp = oldAccessor.simplebuilding$getBaseXp();
            this.activeTicks = oldAccessor.simplebuilding$getCurrentTime();

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