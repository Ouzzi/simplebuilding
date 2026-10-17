package com.simplebuilding.modules.simpletweaks.play.mixin;

import com.simplebuilding.modules.simpletweaks.play.keep.KeepInventory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/**
 * Partial keep inventory (T27): the kept stacks leave the inventory at the first read of the slot list in {@code dropAll},
 * i.e. after every HEAD hook of other mods (Simple Tools Soulbound takes its stacks there first), and go back at the end.
 * The read is the loop condition, so the hook fires on every pass; {@link KeepInventory#beforeDrops} acts on the first only.
 */
@Mixin(Inventory.class)
public abstract class PlayInventoryMixin {
    @Shadow @Final public Player player;

    @Inject(method = "dropAll", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Inventory;items:Lnet/minecraft/core/NonNullList;", opcode = Opcodes.GETFIELD, ordinal = 0))
    private void simpletweaks$playKeep(CallbackInfo ci) { KeepInventory.beforeDrops(player); }

    @Inject(method = "dropAll", at = @At("RETURN"))
    private void simpletweaks$playKeepBack(CallbackInfo ci) { KeepInventory.afterDrops(player); }
}
