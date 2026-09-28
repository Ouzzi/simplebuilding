package com.simplebuilding.mixin;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.BackpackItem;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {

    @Shadow public abstract ItemStack getItem();
    @Shadow private int pickupDelay;
    @Shadow private java.util.UUID target;

    public ItemEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    /**
     * Der Explosionsschutz haengt an der Stufe, nicht am einzelnen Gegenstand: das Netherit-Buendel
     * hatte ihn von Anfang an, und eine Aufwertung darf nichts wegnehmen - deshalb tragen ihn das
     * Enderit-Buendel und der Enderit-Koecher als hoechste Stufe genauso. Netherit- und
     * Enderit-Rucksack sind auf Wunsch des Mod-Autors explosionsfest wie die passenden Buendel, und
     * seit 2026-09-28 (Besitzer-Entscheidung) auch der Netherit-Koecher wie die anderen
     * Spitzenstufen.
     */
    @Inject(method = "ignoreExplosion", at = @At("HEAD"), cancellable = true)
    private void isTopTierContainerImmune(Explosion explosion, CallbackInfoReturnable<Boolean> cir) {
        ItemStack droppedStack = this.getItem();
        if (droppedStack.is(ModItems.NETHERITE_BUNDLE)
                || droppedStack.is(ModItems.ENDERITE_BUNDLE)
                || droppedStack.is(ModItems.NETHERITE_QUIVER)
                || droppedStack.is(ModItems.ENDERITE_QUIVER)
                || droppedStack.is(ModItems.NETHERITE_BACKPACK)
                || droppedStack.is(ModItems.ENDERITE_BACKPACK)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void onPlayerCollision(Player player, CallbackInfo ci) {
        if (this.level().isClientSide()) return;

        // Wenn du möchtest, dass es auch beim Fliegen funktioniert (ohne Sneaken),
        // müsstest du diese Zeile entfernen oder anpassen:
        if (player.isShiftKeyDown()) return;

        ItemStack itemOnGround = this.getItem();
        if (itemOnGround.isEmpty()) return;

        // Dieselben Bedingungen wie Vanillas eigene Aufnahme in playerTouch, fuer JEDEN Weg (Hand,
        // Inventar, Rucksack): abgelaufene Aufhebeverzoegerung und kein fremdes Ziel. Sonst saugt
        // ein Buendel die Anzeige-Items anderer Mods (pickupDelay 32767) und die Fake-Items eines
        // /give mit vollem Inventar auf (audit 2026-09-26 #14).
        if (this.pickupDelay != 0) return;
        if (this.target != null && !this.target.equals(player.getUUID())) return;

        // Vorher festhalten: die Wege unten schrumpfen den Stapel am Boden, und ein leerer Stapel
        // meldet getCount() 0 und getItem() Luft (Audit N7).
        int countBefore = itemOnGround.getCount();
        Item pickedItem = itemOnGround.getItem();

        // 1. Suche in den HÄNDEN (höchste Priorität)
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack heldItem = player.getItemInHand(hand);
            if (tryPickupWithBundle(heldItem, itemOnGround, player)) {
                handlePickupSuccess(player, itemOnGround, pickedItem, countBefore, ci);
                return;
            }
        }

        // 2. Suche im INVENTAR
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack inventoryStack = player.getInventory().getItem(i);
            if (tryPickupWithBundle(inventoryStack, itemOnGround, player)) {
                handlePickupSuccess(player, itemOnGround, pickedItem, countBefore, ci);
                return;
            }
        }

        // 3. Getragener Rucksack mit Trichter - nach allen Buendeln.
        if (BackpackItem.tryFunnelPickup(player, itemOnGround)) {
            handlePickupSuccess(player, itemOnGround, pickedItem, countBefore, ci);
        }
    }

    @Unique
    private boolean tryPickupWithBundle(ItemStack bundleStack, ItemStack itemToPickup, Player player) {
        // Prüfen, ob es ein ReinforcedBundle ist
        if (bundleStack.getItem() instanceof ReinforcedBundleItem bundleItem) {

            // --- NEUE LOGIK ---
            // Wir nutzen die Methode aus dem Item, die Level 1 (Filter) und Level 2 (Alles) unterscheidet.
            if (bundleItem.canAutoPickup(bundleStack, itemToPickup, player.level())) {

                // Wenn erlaubt, versuchen wir das Item einzufügen (Drawer-Logik passiert hier drin)
                return bundleItem.tryInsertStackFromWorld(bundleStack, itemToPickup, player);
            }
        }
        return false;
    }

    /**
     * Aufhebe-Animation und Statistik fuer die aufgesaugte Menge {@code countBefore - Rest}. Bis
     * 2026-09-27 ging die Menge NACH dem Schrumpfen hinein: bei voller Aufnahme 0, also keine
     * Animation, keine Statistik (und die Statistik auf Luft); bei Teilaufnahme der Rest statt des
     * Aufgenommenen (Audit N7).
     */
    @Unique
    private void handlePickupSuccess(Player player, ItemStack itemOnGround, Item pickedItem, int countBefore, CallbackInfo ci) {
        int picked = countBefore - itemOnGround.getCount();
        if (picked > 0) {
            player.take(this, picked);
            player.awardStat(Stats.ITEM_PICKED_UP.get(pickedItem), picked);
        }

        // Wenn das Item komplett aufgesaugt wurde
        if (itemOnGround.isEmpty()) {
            this.discard(); // Entity aus der Welt entfernen
            ci.cancel();    // Das Vanilla-Event abbrechen, damit es nicht doppelt aufgehoben wird
        }
    }


}