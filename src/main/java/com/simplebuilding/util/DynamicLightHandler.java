package com.simplebuilding.util;

import com.simplebuilding.items.ModItems;
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
    // Speichert die letzte Position des Lichts für jeden Spieler
    private static final Map<UUID, BlockPos> lightSources = new HashMap<>();

    public static void tick(PlayerEntity player) {
        if (player.getEntityWorld().isClient()) return; // Nur Server-seitig für echtes Licht
        if (!(player instanceof ServerPlayerEntity)) return;

        World world = player.getEntityWorld();
        UUID uuid = player.getUuid();
        BlockPos currentPos = player.getBlockPos().up(); // Kopfhöhe

        // 1. Berechne das Licht-Level basierend auf der Rüstung
        int totalRadiance = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack stack = player.getEquippedStack(slot);
                totalRadiance += GlowingTrimUtils.getGlowLevel(stack);
            }
        }

        // Maximale Lichtstärke in Minecraft ist 15
        int lightLevel = Math.min(15, totalRadiance * 3);

        // Alte Position abrufen
        BlockPos oldPos = lightSources.get(uuid);

        // 2. Aufräumen (Wenn wir uns bewegt haben oder kein Licht mehr haben)
        if (oldPos != null && !oldPos.equals(currentPos)) {
            removeLight(world, oldPos);
            lightSources.remove(uuid);
        }

        // 3. Neues Licht setzen
        if (lightLevel > 0) {
            // Nur setzen, wenn der Block Luft oder Wasser ist (nichts überschreiben!)
            BlockState currentState = world.getBlockState(currentPos);
            boolean isWater = currentState.getFluidState().isIn(FluidTags.WATER);

            if (currentState.isAir() || (isWater && currentState.getFluidState().isStill())) {
                // Wenn wir schon ein Licht an dieser Stelle haben, prüfen wir, ob das Level stimmt
                if (currentState.isOf(Blocks.LIGHT)) {
                    int currentLight = currentState.get(LightBlock.LEVEL_15);
                    if (currentLight != lightLevel) {
                        world.setBlockState(currentPos, Blocks.LIGHT.getDefaultState()
                                .with(LightBlock.LEVEL_15, lightLevel)
                                .with(LightBlock.WATERLOGGED, isWater), 3);
                    }
                } else {
                    // Setze neuen Lichtblock
                    world.setBlockState(currentPos, Blocks.LIGHT.getDefaultState()
                            .with(LightBlock.LEVEL_15, lightLevel)
                            .with(LightBlock.WATERLOGGED, isWater), 3);
                }
                
                // Speichern, damit wir es später löschen können
                lightSources.put(uuid, currentPos);
            }
        } else if (oldPos != null) {
            // Wenn Radiance 0 ist, aber noch ein alter Block existiert -> Löschen
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
    
    // Aufräumen wenn Spieler den Server verlässt
    public static void onDisconnect(ServerPlayerEntity player) {
        BlockPos pos = lightSources.remove(player.getUuid());
        if (pos != null) {
            removeLight(player.getEntityWorld(), pos);
        }
    }
}