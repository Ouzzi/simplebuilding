package com.simplebuilding.mixin;

import com.simplebuilding.blocks.ModBlocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// WICHTIG: Wir mixen in HostileEntity, nicht EndermanEntity!
@Mixin(HostileEntity.class)
public abstract class EndermanSpawnMixin {

    // Wir nutzen die Methode 'canSpawnInDark', die von Endermen verwendet wird
    @Inject(method = "canSpawnInDark", at = @At("HEAD"), cancellable = true)
    private static void checkEnderiteShield(EntityType<? extends HostileEntity> type, ServerWorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random, CallbackInfoReturnable<Boolean> cir) {

        // 1. Wir prüfen, ob es überhaupt ein Enderman ist
        if (type == EntityType.ENDERMAN) {

            // 2. Nur bei natürlichem Spawning prüfen (Performance & Logic)
            if (spawnReason == SpawnReason.NATURAL || spawnReason == SpawnReason.CHUNK_GENERATION) {

                // ACHTUNG: Radius 64 = 2.146.689 Checks pro Spawn! Das ist extrem viel.
                // Ich empfehle max 32 (ca. 260k Checks) oder noch besser 16, wenn möglich.
                int radius = 64;

                // Bereich scannen
                BlockPos start = pos.add(-radius, -radius, -radius);
                BlockPos end = pos.add(radius, radius, radius);

                for (BlockPos p : BlockPos.iterate(start, end)) {

                    // --- FIX ANFANG ---
                    // WICHTIG: Prüfen, ob der Chunk geladen ist, BEVOR wir den Block abfragen.
                    // Verhindert den Watchdog Crash durch synchrones Chunk-Laden.
                    if (!world.toServerWorld().isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)) {
                        continue; // Überspringe ungeladene Chunks
                    }
                    // --- FIX ENDE ---

                    // Prüfen auf Enderite Block
                    if (world.getBlockState(p).isOf(ModBlocks.ENDERITE_BLOCK)) {
                        // Prüfen ob gepowert
                        if (world.getReceivedRedstonePower(p) > 0) {
                            // Wenn ja: Spawn verbieten!
                            cir.setReturnValue(false);
                            return;
                        }
                    }
                }
            }
        }
    }
}