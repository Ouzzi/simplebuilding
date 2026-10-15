package com.simplebuilding.modules.simplestats.mixin;

import com.simplebuilding.modules.simplestats.StatsPlayer;
import com.simplebuilding.modules.simplestats.StatsRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Holds the transient counting state and listens to the vanilla movement statistics (distances are measured by vanilla, not by us). */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements StatsPlayer {
    @Unique private final State simplestats$state = new State();

    @Override
    public State simplestats$state() {
        return simplestats$state;
    }

    @SuppressWarnings("unchecked")
    @Inject(method = "awardStat", at = @At("HEAD"))
    private void simplestats$stat(Stat<?> stat, int count, CallbackInfo ci) {
        if (stat.getType() == Stats.CUSTOM) StatsRuntime.onStat((ServerPlayer) (Object) this, ((Stat<Identifier>) stat).getValue(), count);
    }
}
