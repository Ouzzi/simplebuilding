package com.simplebuilding.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightBlock;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DynamicLightHandler {
    private static final Map<UUID, BlockPos> lightSources = new HashMap<>();

    public static void tick(PlayerEntity player) {
        if (player.getEntityWorld().isClient()) return;
        if (!(player instanceof ServerPlayerEntity)) return;

        World world = player.getEntityWorld();
        UUID uuid = player.getUuid();
        BlockPos currentPos = player.getBlockPos().up(); // Kopfhöhe für bessere Ausleuchtung

        // 1. Berechne das Licht-Level NUR basierend auf Emission (nicht Visual Glow)
        int totalEmissionPoints = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack stack = player.getEquippedStack(slot);
                // WICHTIG: Hier rufen wir jetzt getEmissionLevel auf
                totalEmissionPoints += GlowingTrimUtils.getEmissionLevel(stack);
            }
        }

        // Berechnung: Ein Level bringt 3 Lichtpunkte -> 5 Upgrades = 15 (Max)
        int lightLevel = Math.min(15, totalEmissionPoints * 3);

        BlockPos oldPos = lightSources.get(uuid);

        // 2. Aufräumen (Wenn bewegt oder Licht aus)
        if (oldPos != null && !oldPos.equals(currentPos)) {
            removeLight(world, oldPos);
            lightSources.remove(uuid);
        }

        // 3. Neues Licht setzen
        if (lightLevel > 0) {
            BlockState currentState = world.getBlockState(currentPos);
            boolean isWater = currentState.getFluidState().isIn(FluidTags.WATER);

            // Wir setzen Licht nur in Luft oder Wasser (um nichts zu zerstören)
            if (currentState.isAir() || (isWater && currentState.getFluidState().isStill())) {

                // Prüfen ob wir updaten müssen (nur wenn Level sich ändert)
                if (currentState.isOf(Blocks.LIGHT)) {
                    int currentLightInBlock = currentState.get(LightBlock.LEVEL_15);
                    if (currentLightInBlock != lightLevel) {
                        world.setBlockState(currentPos, Blocks.LIGHT.getDefaultState()
                                .with(LightBlock.LEVEL_15, lightLevel)
                                .with(LightBlock.WATERLOGGED, isWater), 3);
                    }
                } else {
                    world.setBlockState(currentPos, Blocks.LIGHT.getDefaultState()
                            .with(LightBlock.LEVEL_15, lightLevel)
                            .with(LightBlock.WATERLOGGED, isWater), 3);
                }
                lightSources.put(uuid, currentPos);
            }
        } else if (oldPos != null) {
            // Wenn Lichtlevel auf 0 gefallen ist, altes Licht entfernen
            removeLight(world, oldPos);
            lightSources.remove(uuid);
        }
    }

    private static void removeLight(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isOf(Blocks.LIGHT)) {
            if (state.get(LightBlock.WATERLOGGED)) {
                world.setBlockState(pos, Blocks.WATER.getDefaultState(), 3);
            } else {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
            }
        }
    }

    public static void onDisconnect(ServerPlayerEntity player) {
        BlockPos pos = lightSources.remove(player.getUuid());
        if (pos != null) {
            removeLight(player.getEntityWorld(), pos);
        }
    }
}