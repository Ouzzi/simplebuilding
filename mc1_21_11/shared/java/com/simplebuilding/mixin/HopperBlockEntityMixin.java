package com.simplebuilding.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simplebuilding.blocks.entity.custom.TieredChestBlockEntity;
import com.simplebuilding.util.TieredChests;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Trichter fuellen die uebergrossen Plaetze der Netherit- (x2) und Enderittruhe (x4) bis zu deren
 * Grenze. Vanilla rechnet beim Einfuellen mit {@code stack.getMaxStackSize()} (64), haelt einen
 * Platz mit mehr als 64 fuer nicht zusammenfuehrbar und einen Container mit lauter 64ern fuer
 * voll. Nur fuer Mod-Truhen (auch als Doppeltruhe hinter {@code CompoundContainer}) wird die
 * Grenze ersetzt, jeder andere Container bleibt bei Vanilla. Gilt fuer Vanillas Trichter, die
 * Trichterlore und - ueber {@code HopperBlockEntity#addItem} - die Mod-Trichter; auf NeoForge
 * nimmt der Trichter fuer einen {@code Container} ebenfalls diesen Weg.
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {

    @WrapOperation(method = "tryMoveInItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;canMergeItems(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean simplebuilding$mergeIntoOversizedSlots(ItemStack current, ItemStack incoming, Operation<Boolean> original,
                                                                  @Local(argsOnly = true, ordinal = 1) Container container) {
        TieredChestBlockEntity chest = TieredChests.chestBehind(container);
        if (chest == null) {
            return original.call(current, incoming);
        }
        return current.getCount() < chest.getMaxStackSize(current) && ItemStack.isSameItemSameComponents(current, incoming);
    }

    @WrapOperation(method = "tryMoveInItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"))
    private static int simplebuilding$spaceInOversizedSlots(ItemStack stack, Operation<Integer> original,
                                                          @Local(argsOnly = true, ordinal = 1) Container container) {
        return TieredChests.maxStackSize(container, stack, original.call(stack));
    }

    @WrapOperation(method = "isFullContainer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"))
    private static int simplebuilding$oversizedSlotsAreNotFull(ItemStack stack, Operation<Integer> original,
                                                             @Local(argsOnly = true) Container container) {
        return TieredChests.maxStackSize(container, stack, original.call(stack));
    }
}
