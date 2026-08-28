package com.simplebuilding.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

public class DynamicLightHandler {
    private static final Map<UUID, BlockPos> lightSources = new HashMap<>();

    public static void tick(Player player) {
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer)) return;

        Level world = player.level();
        UUID uuid = player.getUUID();
        BlockPos currentPos = player.blockPosition().above(); // Kopfhöhe für bessere Ausleuchtung

        // 1. Berechne das Licht-Level NUR basierend auf Emission (nicht Visual Glow)
        int totalEmissionPoints = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack stack = player.getItemBySlot(slot);
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
            boolean isWater = currentState.getFluidState().is(FluidTags.WATER);

            // Wir setzen Licht nur in Luft oder Wasser (um nichts zu zerstören)
            if (currentState.isAir() || (isWater && currentState.getFluidState().isSource())) {

                // Prüfen ob wir updaten müssen (nur wenn Level sich ändert)
                if (currentState.is(Blocks.LIGHT)) {
                    int currentLightInBlock = currentState.getValue(LightBlock.LEVEL);
                    if (currentLightInBlock != lightLevel) {
                        world.setBlock(currentPos, Blocks.LIGHT.defaultBlockState()
                                .setValue(LightBlock.LEVEL, lightLevel)
                                .setValue(LightBlock.WATERLOGGED, isWater), 3);
                    }
                } else {
                    world.setBlock(currentPos, Blocks.LIGHT.defaultBlockState()
                            .setValue(LightBlock.LEVEL, lightLevel)
                            .setValue(LightBlock.WATERLOGGED, isWater), 3);
                }
                lightSources.put(uuid, currentPos);
            }
        } else if (oldPos != null) {
            // Wenn Lichtlevel auf 0 gefallen ist, altes Licht entfernen
            removeLight(world, oldPos);
            lightSources.remove(uuid);
        }
    }

    private static void removeLight(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.is(Blocks.LIGHT)) {
            if (state.getValue(LightBlock.WATERLOGGED)) {
                world.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
            } else {
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    public static void onDisconnect(ServerPlayer player) {
        BlockPos pos = lightSources.remove(player.getUUID());
        if (pos != null) {
            removeLight(player.level(), pos);
        }
    }
}