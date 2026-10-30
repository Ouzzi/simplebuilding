package com.simplebuilding.modules.simplemobs.mixin;

import com.simplebuilding.modules.simplemobs.trials.EvilRaid;
import net.minecraft.world.entity.raid.Raid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

/** The wave count of a raid is saved with it ({@code group_count}), so one more wave for an Evil Raid survives a restart. */
@Mixin(Raid.class)
public abstract class RaidEvilMixin implements EvilRaid {
    @Shadow @Final @Mutable private int numGroups;

    @Override
    public void simplemobs$addWave() {
        numGroups++;
    }

    @Override
    public int simplemobs$waves() {
        return numGroups;
    }
}
