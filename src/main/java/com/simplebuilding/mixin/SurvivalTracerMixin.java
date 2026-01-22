package com.simplebuilding.mixin;

import com.simplebuilding.util.SurvivalTracerAccessor;
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

    @Unique
    private int baselineDistanceWalked = 0;
    @Unique
    private int baselineMobsKilled = 0;
    @Unique
    private int baselineTimePlayed = 0;

    // Speichern mit WriteView (neu in 1.21/Snapshots)
    @Inject(method = "writeCustomData", at = @At("TAIL"))
    public void writeSurvivalData(WriteView view, CallbackInfo ci) {
        NbtCompound survivalTag = new NbtCompound();
        survivalTag.putInt("BaseWalk", baselineDistanceWalked);
        survivalTag.putInt("BaseKills", baselineMobsKilled);
        survivalTag.putInt("BaseTime", baselineTimePlayed);

        view.put("SimpleBuildingSurvival", NbtCompound.CODEC, survivalTag);
    }

    // Laden mit ReadView (neu in 1.21/Snapshots)
    @Inject(method = "readCustomData", at = @At("TAIL"))
    public void readSurvivalData(ReadView view, CallbackInfo ci) {
        view.read("SimpleBuildingSurvival", NbtCompound.CODEC).ifPresent(tag -> {
            // FIX: getInt gibt jetzt Optional zurück -> Wir nutzen getInt(key, default)
            this.baselineDistanceWalked = tag.getInt("BaseWalk", 0);
            this.baselineMobsKilled = tag.getInt("BaseKills", 0);
            this.baselineTimePlayed = tag.getInt("BaseTime", 0);
        });
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    public void onRespawn(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        SurvivalTracerAccessor oldAccessor = (SurvivalTracerAccessor) oldPlayer;

        if (!alive) {
            ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
            this.baselineDistanceWalked = getStatTotalDistance(player);

            // FIX: Scoreboard Fehler behoben -> Wir nutzen Stats.MOB_KILLS via StatHandler
            // Stats.MOB_KILLS ist ein Identifier, wir müssen den Stat daraus erzeugen.
            this.baselineMobsKilled = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.MOB_KILLS));

            this.baselineTimePlayed = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME));
        } else {
            this.baselineDistanceWalked = oldAccessor.simplebuilding$getBaseDistance();
            this.baselineMobsKilled = oldAccessor.simplebuilding$getBaseKills();
            this.baselineTimePlayed = oldAccessor.simplebuilding$getBaseTime();
        }
    }

    @Unique
    private int getStatTotalDistance(ServerPlayerEntity player) {
        return player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.WALK_ONE_CM)) / 100
                + player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.SPRINT_ONE_CM)) / 100
                + player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.CROUCH_ONE_CM)) / 100
                + player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.FLY_ONE_CM)) / 100
                + player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.CLIMB_ONE_CM)) / 100;
    }

    @Override
    public int simplebuilding$getBaseDistance() { return baselineDistanceWalked; }
    @Override
    public int simplebuilding$getBaseKills() { return baselineMobsKilled; }
    @Override
    public int simplebuilding$getBaseTime() { return baselineTimePlayed; }
}