package com.simplebuilding.tweaks.mixin;

import com.simplebuilding.tweaks.spawn.SpawnElytra;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Eine Spawn-Elytra wird nie zum liegenden Gegenstand (Audit 2026-09-26 #3): fallen gelassen,
 * beim Tod verstreut oder aus einem Speicherstand geladen, wird der Stapel leer, und Vanilla
 * entfernt das leere Item-Entity im naechsten Tick. Trichter und Spieler koennen es davor nicht
 * aufsammeln, weil es nichts enthaelt. Jeder Weg zum Item-Entity laeuft ueber {@code setItem}.
 */
@Mixin(ItemEntity.class)
public abstract class SpawnElytraItemEntityMixin {

    @ModifyVariable(method = "setItem", at = @At("HEAD"), argsOnly = true)
    private ItemStack simplebuilding$noLooseSpawnElytra(ItemStack stack) {
        return SpawnElytra.isSpawnElytra(stack) ? ItemStack.EMPTY : stack;
    }
}
