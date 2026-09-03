package com.simplebuilding.mixin;

import com.simplebuilding.items.ModItems;
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

    public ItemEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Inject(method = "ignoreExplosion", at = @At("HEAD"), cancellable = true)
    private void isNetheriteBundleImmune(Explosion explosion, CallbackInfoReturnable<Boolean> cir) {
        if (this.getItem().is(ModItems.NETHERITE_BUNDLE)) {
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

        // 1. Suche in den HÄNDEN (höchste Priorität)
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack heldItem = player.getItemInHand(hand);
            if (tryPickupWithBundle(heldItem, itemOnGround, player)) {
                handlePickupSuccess(player, itemOnGround, ci);
                return;
            }
        }

        // 2. Suche im INVENTAR (nur wenn pickupDelay abgelaufen ist)
        if (this.pickupDelay == 0) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack inventoryStack = player.getInventory().getItem(i);
                if (tryPickupWithBundle(inventoryStack, itemOnGround, player)) {
                    handlePickupSuccess(player, itemOnGround, ci);
                    return;
                }
            }
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

    @Unique
    private void handlePickupSuccess(Player player, ItemStack itemOnGround, CallbackInfo ci) {
        // Visuelles Feedback und Statistik
        player.take(this, itemOnGround.getCount());
        player.awardStat(Stats.ITEM_PICKED_UP.get(itemOnGround.getItem()), itemOnGround.getCount());

        // Wenn das Item komplett aufgesaugt wurde
        if (itemOnGround.isEmpty()) {
            this.discard(); // Entity aus der Welt entfernen
            ci.cancel();    // Das Vanilla-Event abbrechen, damit es nicht doppelt aufgehoben wird
        }
    }


}