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

    /**
     * Kerzen und Seegurken (2026-10-03) mischen sich mit den Kleinteilen: Schleichen + Rechtsklick auf ein Haeufchen
     * legt sie dazu, auf einen Vanilla-Kerzen-/Seegurkenblock einer anderen Art macht daraus ein Haeufchen
     * ({@link com.simplebuilding.util.PlacedSmallParts#tryPlace}). Allein legen sie sich weiter als Vanilla-Block.
     * {@code BlockItem} ueberschreibt {@code useOn}, daher hier und nicht im {@code ItemUseOnMixin}.
     */
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$mixIntoSmallParts(net.minecraft.world.item.context.UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!com.simplebuilding.util.PlacedSmallParts.isBlockPart(context.getItemInHand())) {
            return;
        }
        InteractionResult result = com.simplebuilding.util.PlacedSmallParts.tryPlace(context);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }

    @Inject(method = "place", at = @At("HEAD"))
    private void simplebuilding$rememberPlacedStack(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        // nur serverseitig mit Spieler: nur dort wird der Rucksack-Nachschub gebraucht (die tiefe Kopie spart der Client)
        if (!context.getLevel().isClientSide() && context.getPlayer() != null) {
            SIMPLEBUILDING_PLACING.set(context.getItemInHand().copy());
        }
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
        refillFromWornBackpack(player, hand, before);
    }

    /**
     * Konstrukteurs Hand am getragenen Rucksack: ist beim Platzieren der Blockstapel in der Hand leer geworden, fuellt ein
     * gleicher Stapel aus dem Rucksack sie wieder auf (hoechstens ein normaler Stapel). Split stage 8c-2: through
     * util/WornStorage (26.3: Simple Storage's backpack, SimpleLib CarriedStacks); was {@code BackpackItem#refillHandFromWornBackpack}.
     */
    @org.spongepowered.asm.mixin.Unique
    private static void refillFromWornBackpack(Player player, InteractionHand hand, ItemStack placed) {
        if (placed.isEmpty() || player.getAbilities().instabuild || !player.getItemInHand(hand).isEmpty()) {
            return;
        }
        for (com.simplebuilding.util.WornStorage.Entry entry : com.simplebuilding.util.WornStorage.entries(player,
                com.simplebuilding.enchantment.ModEnchantments.CONSTRUCTORS_TOUCH)) {
            if (!ItemStack.isSameItemSameComponents(entry.stack(), placed)) continue;
            ItemStack left = entry.stack().copy();
            ItemStack refill = left.split(placed.getMaxStackSize());
            if (refill.isEmpty()) return;
            entry.write(left);
            player.setItemInHand(hand, refill);
            player.playSound(net.minecraft.sounds.SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
            return;
        }
    }
}
