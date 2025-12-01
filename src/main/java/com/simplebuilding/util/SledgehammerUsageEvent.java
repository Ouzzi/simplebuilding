package com.simplebuilding.util;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.SledgehammerItem;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class SledgehammerUsageEvent implements PlayerBlockBreakEvents.Before {

    private static final Set<BlockPos> HARVESTED_BLOCKS = new HashSet<>();

    @Override
    public boolean beforeBlockBreak(World world, PlayerEntity player, BlockPos pos,
                                    BlockState state, @Nullable BlockEntity blockEntity) {

        ItemStack mainHandItem = player.getMainHandStack();

        // Nur serverseitig und wenn es unser Hammer ist
        if (mainHandItem.getItem() instanceof SledgehammerItem && player instanceof ServerPlayerEntity serverPlayer) {

            // Verhindert Rekursion (wenn der Hammer selbst Blöcke bricht)
            if (HARVESTED_BLOCKS.contains(pos)) {
                return true;
            }

            // --- FILTER LOGIK VORBEREITEN ---
            var registry = world.getRegistryManager();
            var enchantLookup = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
            var ignoreTypeKey = enchantLookup.getOptional(ModEnchantments.IGNORE_BLOCK_TYPE);

            int ignoreLevel = 0;
            if (ignoreTypeKey.isPresent()) {
                ignoreLevel = EnchantmentHelper.getLevel(ignoreTypeKey.get(), mainHandItem);
            }

            BlockState originState = world.getBlockState(pos);

            // Alle betroffenen Blöcke durchgehen
            for (BlockPos position : SledgehammerItem.getBlocksToBeDestroyed(1, pos, serverPlayer)) {
                // Den Hauptblock lassen wir von Vanilla behandeln (auch den Schaden dafür)
                if (pos.equals(position)) continue;

                BlockState targetState = world.getBlockState(position);

                // Sicherheits-Check: Unzerstörbare Blöcke
                if (targetState.isAir() || targetState.getHardness(world, position) < 0) {
                    continue;
                }

                boolean shouldBreak = false;

                // --- LOGIK: Welche Blöcke dürfen abgebaut werden? ---
                if (ignoreLevel == 0) {
                    // Level 0: Nur EXAKT gleicher Blocktyp
                    if (targetState.getBlock() == originState.getBlock()) {
                        shouldBreak = true;
                    }
                }
                else if (ignoreLevel == 1) {
                    // Level 1: Gleicher Typ ODER "Supported" (Werkzeug ist geeignet)
                    if (targetState.getBlock() == originState.getBlock() || mainHandItem.isSuitableFor(targetState)) {
                        shouldBreak = true;
                    }
                }
                else if (ignoreLevel >= 2) {
                    // Level 2: Alles (außer Bedrock, siehe oben)
                    shouldBreak = true;
                }

                if (shouldBreak) {
                    HARVESTED_BLOCKS.add(position);

                    // Versuche den Block abzubauen
                    boolean wasBroken = serverPlayer.interactionManager.tryBreakBlock(position);

                    HARVESTED_BLOCKS.remove(position);

                    // --- DAMAGE LOGIK HIER ---
                    if (wasBroken) {
                        // Prüfen: Ist das Item eigentlich geeignet für diesen Block?
                        boolean isSuitable = mainHandItem.isSuitableFor(targetState);

                        // Wenn geeignet: 1 Schaden. Wenn ungeeignet (aber durch Enchantment erlaubt): 2 Schaden.
                        int damageAmount = isSuitable ? 1 : 2;

                        // Schaden zufügen
                        mainHandItem.damage(damageAmount, serverPlayer, EquipmentSlot.MAINHAND);

                        // Wenn das Item dabei kaputt geht, hören wir auf
                        if (mainHandItem.isEmpty()) {
                            break;
                        }
                    }
                }
            }
        }

        return true;
    }
}