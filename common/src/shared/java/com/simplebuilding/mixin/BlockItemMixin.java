package com.simplebuilding.mixin;

import com.simplebuilding.items.custom.BackpackItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Konstrukteurs Hand am getragenen Rucksack: wird der Blockstapel in der Hand beim Platzieren
 * leer, fuellt {@link BackpackItem#refillHandFromWornBackpack} ihn aus dem Rucksack nach.
 *
 * <p>Der Stapel wird am Anfang von {@code BlockItem#place} kopiert, weil er am Ende leer ist - und
 * ein leerer Stapel verraet weder Item noch Komponenten mehr. Nur Platzierungen direkt aus der
 * Hand zaehlen: platziert etwa ein Meisterbauer-Buendel eine Kopie, ist der Handstapel ein
 * anderer und nichts passiert.
 */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {

    @Unique
    private static final ThreadLocal<ItemStack> SIMPLEBUILDING_PLACING = new ThreadLocal<>();

    @Inject(method = "place", at = @At("HEAD"))
    private void simplebuilding$rememberPlacedStack(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        SIMPLEBUILDING_PLACING.set(context.getItemInHand().copy());
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void simplebuilding$refillFromBackpack(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack before = SIMPLEBUILDING_PLACING.get();
        SIMPLEBUILDING_PLACING.remove();
        if (before == null || context.getLevel().isClientSide() || !cir.getReturnValue().consumesAction()) {
            return;
        }
        Player player = context.getPlayer();
        if (player == null) {
            return;
        }
        InteractionHand hand = context.getHand();
        if (player.getItemInHand(hand) != context.getItemInHand()) {
            return;
        }
        BackpackItem.refillHandFromWornBackpack(player, hand, before);
    }
}
