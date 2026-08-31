package com.simplebuilding.mixin;

import com.simplebuilding.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// WICHTIG: Wir mixen in HostileEntity, nicht EndermanEntity!
@Mixin(Monster.class)
public abstract class EndermanSpawnMixin {

    // Wir nutzen die Methode 'canSpawnInDark', die von Endermen verwendet wird
    @Inject(method = "checkMonsterSpawnRules", at = @At("HEAD"), cancellable = true)
    private static void checkEnderiteShield(EntityType<? extends Monster> type, ServerLevelAccessor world, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> cir) {

        // 1. Wir prüfen, ob es überhaupt ein Enderman ist
        // MC 1.21.11: Die EntityType-Konstanten liegen noch in EntityType selbst
        // (nach EntityTypes ausgelagert wurden sie erst mit 26.2).
        if (type == EntityType.ENDERMAN) {

            // 2. Nur bei natürlichem Spawning prüfen (Performance & Logic)
            if (spawnReason == EntitySpawnReason.NATURAL || spawnReason == EntitySpawnReason.CHUNK_GENERATION) {

                // ACHTUNG: Radius 64 = 2.146.689 Checks pro Spawn! Das ist extrem viel.
                // Ich empfehle max 32 (ca. 260k Checks) oder noch besser 16, wenn möglich.
                int radius = 64;

                // Bereich scannen
                BlockPos start = pos.offset(-radius, -radius, -radius);
                BlockPos end = pos.offset(radius, radius, radius);

                for (BlockPos p : BlockPos.betweenClosed(start, end)) {

                    // --- FIX ANFANG ---
                    // WICHTIG: Prüfen, ob der Chunk geladen ist, BEVOR wir den Block abfragen.
                    // Verhindert den Watchdog Crash durch synchrones Chunk-Laden.
                    if (!world.getLevel().hasChunk(p.getX() >> 4, p.getZ() >> 4)) {
                        continue; // Überspringe ungeladene Chunks
                    }
                    // --- FIX ENDE ---

                    // Prüfen auf Enderite Block
                    if (world.getBlockState(p).is(ModBlocks.ENDERITE_BLOCK)) {
                        // Prüfen ob gepowert
                        if (world.getBestNeighborSignal(p) > 0) {
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