package com.simplebuilding.mixin;

import com.simplebuilding.networking.SurvivalSyncPayload;
import com.simplebuilding.networking.TrimDataPayload;
import com.simplebuilding.util.SurvivalTracerAccessor;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class SurvivalTracerMixin implements SurvivalTracerAccessor {

    @Unique private int baselineDistanceWalked = 0;
    @Unique private int baselineMobsKilled = 0;
    @Unique private int baselineTimePlayed = 0;

    // --- Accessor Implementierung ---
    @Override public int simplebuilding$getBaseDistance() { return baselineDistanceWalked; }
    @Override public int simplebuilding$getBaseKills() { return baselineMobsKilled; }
    @Override public int simplebuilding$getBaseTime() { return baselineTimePlayed; }

    @Override
    public void simplebuilding$setBaseValues(int dist, int kills, int time) {
        this.baselineDistanceWalked = dist;
        this.baselineMobsKilled = kills;
        this.baselineTimePlayed = time;
    }

    // Server braucht diese Felder nicht speichern, da er die echten Stats hat
    @Override public int simplebuilding$getCurrentDistance() { return 0; }
    @Override public int simplebuilding$getCurrentKills() { return 0; }
    @Override public int simplebuilding$getCurrentTime() { return 0; }
    @Override public void simplebuilding$setCurrentValues(int dist, int kills, int time) { }

    @Override
    public void simplebuilding$syncTrimData() {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        // WICHTIG: Prüfen, ob der Spieler verbunden ist, um Crash beim Laden zu verhindern!
        if (player.networkHandler != null && ServerPlayNetworking.canSend(player, TrimDataPayload.ID)) {
            ServerPlayNetworking.send(player, new TrimDataPayload(baselineDistanceWalked, baselineMobsKilled, baselineTimePlayed));
        }
    }

    // --- Ticker für Live-Update ---
    @Inject(method = "tick", at = @At("TAIL"))
    private void simplebuilding$onTick(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        // Sende Update jede Sekunde (20 Ticks)
        if (player.age % 20 == 0 && player.networkHandler != null) {
            int currentDist = getStatTotalDistance(player);
            // Kills temporär 0 wie besprochen, oder echte Logik wenn verfügbar
            int currentKills = 0;
            int currentTime = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME));

            if (ServerPlayNetworking.canSend(player, SurvivalSyncPayload.ID)) {
                ServerPlayNetworking.send(player, new SurvivalSyncPayload(currentDist, currentKills, currentTime));
            }
        }
    }

    // --- Bestehende NBT & Respawn Logik (wie zuvor) ---
    @Inject(method = "writeCustomData", at = @At("TAIL"))
    public void writeSurvivalData(WriteView view, CallbackInfo ci) {
        NbtCompound survivalTag = new NbtCompound();
        survivalTag.putInt("BaseWalk", baselineDistanceWalked);
        survivalTag.putInt("BaseKills", baselineMobsKilled);
        survivalTag.putInt("BaseTime", baselineTimePlayed);

        view.put("SimpleBuildingSurvival", NbtCompound.CODEC, survivalTag);
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    public void readSurvivalData(ReadView view, CallbackInfo ci) {
        view.read("SimpleBuildingSurvival", NbtCompound.CODEC).ifPresent(tag -> {
            this.baselineDistanceWalked = tag.getInt("BaseWalk", 0);
            this.baselineMobsKilled = tag.getInt("BaseKills", 0);
            this.baselineTimePlayed = tag.getInt("BaseTime", 0);

            // FIX: Hier KEIN Sync senden! Der Spieler ist noch nicht fertig geladen.
            // Der Sync passiert jetzt beim "Join"-Event (siehe ModMessages.java).
        });
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    public void onRespawn(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        SurvivalTracerAccessor oldAccessor = (SurvivalTracerAccessor) oldPlayer;

        if (!alive) {
            ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
            this.baselineDistanceWalked = getStatTotalDistance(player);

            // Platzhalter für Kills, um Crash zu vermeiden (Stats.MOB_KILLS ist kein einzelner Stat)
            this.baselineMobsKilled = 0;

            this.baselineTimePlayed = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME));
        } else {
            this.baselineDistanceWalked = oldAccessor.simplebuilding$getBaseDistance();
            this.baselineMobsKilled = oldAccessor.simplebuilding$getBaseKills();
            this.baselineTimePlayed = oldAccessor.simplebuilding$getBaseTime();
        }
        // Sync wird beim Join/Respawn separat getriggert oder hier:
        // simplebuilding$syncTrimData(); // Optional, da Join Event es meist auch macht
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