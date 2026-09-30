package com.simplequalityoflife.mixin;

import net.minecraft.world.level.block.entity.vault.VaultServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;
import java.util.UUID;

@Mixin(VaultServerData.class)
public interface VaultServerDataAccessor {
    @Accessor("rewardedPlayers")
    Set<UUID> getRewardedPlayersSet();

    // NEU: Damit wir markieren können, dass sich Daten geändert haben
    @Accessor("isDirty")
    void setDirty(boolean dirty);
}
