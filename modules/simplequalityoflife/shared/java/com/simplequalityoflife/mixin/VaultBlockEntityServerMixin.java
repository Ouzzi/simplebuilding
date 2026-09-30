package com.simplequalityoflife.mixin;

import com.simplequalityoflife.Simplequalityoflife;
import com.simplequalityoflife.util.IVaultCooldown;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
import net.minecraft.world.level.block.entity.vault.VaultServerData;
import net.minecraft.world.level.block.entity.vault.VaultSharedData;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mixin(VaultBlockEntity.Server.class)
public class VaultBlockEntityServerMixin {

    // Hält fest, ob der Spieler beim Eintritt in die Vanilla-tryInsertKey-Logik bereits belohnt war.
    // tryInsertKey läuft serverseitig und synchron (HEAD vor TAIL), daher ist ein statisches Feld sicher.
    @Unique
    private static boolean simplequalityoflife$wasRewardedBeforeUnlock;

    // --- TICK LOGIK ---
    @Inject(method = "tick", at = @At("HEAD"))
    private static void tickCooldowns(ServerLevel world, BlockPos pos, BlockState state, VaultConfig config, VaultServerData serverData, VaultSharedData sharedData, CallbackInfo ci) {
        if (!Simplequalityoflife.getConfig().qOL.enableVaultCooldown) return;

        if (world.getGameTime() % 40 != 0) return;

        if (serverData instanceof IVaultCooldown cooldownData) {
            Set<UUID> rewardedPlayers = ((VaultServerDataAccessor) serverData).getRewardedPlayersSet();

            if (rewardedPlayers.isEmpty()) return;

            long now = world.getGameTime();
            Set<UUID> connectedPlayers = ((VaultSharedDataAccessor) sharedData).getConnectedPlayersSet();

            Set<UUID> playersToCheck = new HashSet<>(rewardedPlayers);
            boolean changed = false;

            for (UUID uuid : playersToCheck) {
                if (!cooldownData.getLootTimesMap().containsKey(uuid)) changed = true;
                if (!cooldownData.hasLootedRecently(uuid, now)) {
                    rewardedPlayers.remove(uuid);
                    connectedPlayers.remove(uuid);
                    cooldownData.removeLootData(uuid);
                    changed = true;
                }
            }

            if (changed) {
                ((VaultServerDataAccessor) serverData).setDirty(true);
                // connectedPlayers (sharedData) wurde mutiert -> Client muss neu synchronisiert werden.
                ((VaultSharedDataAccessor) sharedData).setDirty(true);

                world.blockEntityChanged(pos);
                world.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
            }
        }
    }

    // --- ALTE LOGIK (Als Sicherheit beim Klicken) ---
    @Inject(method = "tryInsertKey", at = @At("HEAD"))
    private static void checkCooldownClick(ServerLevel world, BlockPos pos, BlockState state, VaultConfig config, VaultServerData serverData, VaultSharedData sharedData, Player player, ItemStack stack, CallbackInfo ci) {
        // Diese Methode fängt Fälle ab, wo der Tick vielleicht noch nicht lief,
        // der Spieler aber schon klickt.
        simplequalityoflife$wasRewardedBeforeUnlock = false;
        if (serverData instanceof IVaultCooldown cooldownData) {
            UUID uuid = player.getUUID();
            Set<UUID> rewardedPlayers = ((VaultServerDataAccessor) serverData).getRewardedPlayersSet();

            if (Simplequalityoflife.getConfig().qOL.enableVaultCooldown && rewardedPlayers.contains(uuid) && !cooldownData.hasLootedRecently(uuid, world.getGameTime())) {
                // Sofortiger Reset beim Klick
                rewardedPlayers.remove(uuid);
                Set<UUID> connectedPlayers = ((VaultSharedDataAccessor) sharedData).getConnectedPlayersSet();
                connectedPlayers.remove(uuid);

                ((VaultServerDataAccessor) serverData).setDirty(true);
                ((VaultSharedDataAccessor) sharedData).setDirty(true);
                world.sendBlockUpdated(pos, state, state, 3);

                Simplequalityoflife.LOGGER.debug("Vault Klick-Reset für {}", player.getName().getString());
            }

            // Zustand festhalten, mit dem die Vanilla-Logik startet (nach einem evtl. Reset oben).
            simplequalityoflife$wasRewardedBeforeUnlock = rewardedPlayers.contains(uuid);
        }
    }

    @Inject(method = "tryInsertKey", at = @At("TAIL"))
    private static void saveCooldown(ServerLevel world, BlockPos pos, BlockState state, VaultConfig config, VaultServerData serverData, VaultSharedData sharedData, Player player, ItemStack stack, CallbackInfo ci) {
        Set<UUID> rewardedPlayers = ((VaultServerDataAccessor) serverData).getRewardedPlayersSet();

        // Nur stempeln, wenn dieser Klick FRISCHEN Loot gewährt hat (vorher nicht belohnt, jetzt schon).
        // Verhindert, dass wiederholtes Anklicken eines bereits geplünderten Vaults die Abklingzeit zurücksetzt.
        if (!simplequalityoflife$wasRewardedBeforeUnlock && rewardedPlayers.contains(player.getUUID())) {
            if (serverData instanceof IVaultCooldown cooldownData) {
                cooldownData.markLooted(player.getUUID(), world.getGameTime());
                ((VaultServerDataAccessor) serverData).setDirty(true);

                Simplequalityoflife.LOGGER.debug("Vault: Zeit gespeichert für {}", player.getName().getString());
            }
        }
    }
}
