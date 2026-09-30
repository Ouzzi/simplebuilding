package com.simplequalityoflife.mixin;

import net.minecraft.world.level.block.entity.vault.VaultSharedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;
import java.util.UUID;

@Mixin(VaultSharedData.class)
public interface VaultSharedDataAccessor {
    @Accessor("connectedPlayers")
    Set<UUID> getConnectedPlayersSet();

    // NEU: Damit der Client das Update (Partikel weg) mitbekommt
    @Accessor("isDirty")
    void setDirty(boolean dirty);
}
