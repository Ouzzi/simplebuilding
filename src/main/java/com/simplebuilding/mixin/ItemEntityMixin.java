package com.simplebuilding.mixin;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {

    @Shadow public abstract ItemStack getStack();
    @Shadow private int pickupDelay;

    public ItemEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "isImmuneToExplosion", at = @At("HEAD"), cancellable = true)
    private void isNetheriteBundleImmune(Explosion explosion, CallbackInfoReturnable<Boolean> cir) {
        if (this.getStack().isOf(ModItems.NETHERITE_BUNDLE)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void onPlayerCollision(PlayerEntity player, CallbackInfo ci) {
        if (this.getEntityWorld().isClient()) return;

        // Wenn du möchtest, dass es auch beim Fliegen funktioniert (ohne Sneaken),
        // müsstest du diese Zeile entfernen oder anpassen:
        if (player.isSneaking()) return;

        ItemStack itemOnGround = this.getStack();
        if (itemOnGround.isEmpty()) return;

        // 1. Suche in den HÄNDEN (höchste Priorität)
        for (Hand hand : Hand.values()) {
            ItemStack heldItem = player.getStackInHand(hand);
            if (tryPickupWithBundle(heldItem, itemOnGround, player)) {
                handlePickupSuccess(player, itemOnGround, ci);
                return;
            }
        }

        // 2. Suche im INVENTAR (nur wenn pickupDelay abgelaufen ist)
        if (this.pickupDelay == 0) {
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack inventoryStack = player.getInventory().getStack(i);
                if (tryPickupWithBundle(inventoryStack, itemOnGround, player)) {
                    handlePickupSuccess(player, itemOnGround, ci);
                    return;
                }
            }
        }
    }

    @Unique
    private boolean tryPickupWithBundle(ItemStack bundleStack, ItemStack itemToPickup, PlayerEntity player) {
        // Prüfen, ob es ein ReinforcedBundle ist
        if (bundleStack.getItem() instanceof ReinforcedBundleItem bundleItem) {

            // --- NEUE LOGIK ---
            // Wir nutzen die Methode aus dem Item, die Level 1 (Filter) und Level 2 (Alles) unterscheidet.
            if (bundleItem.canAutoPickup(bundleStack, itemToPickup, player.getEntityWorld())) {

                // Wenn erlaubt, versuchen wir das Item einzufügen (Drawer-Logik passiert hier drin)
                return bundleItem.tryInsertStackFromWorld(bundleStack, itemToPickup, player);
            }
        }
        return false;
    }

    @Unique
    private void handlePickupSuccess(PlayerEntity player, ItemStack itemOnGround, CallbackInfo ci) {
        // Visuelles Feedback und Statistik
        player.sendPickup(this, itemOnGround.getCount());
        player.increaseStat(Stats.PICKED_UP.getOrCreateStat(itemOnGround.getItem()), itemOnGround.getCount());

        // Wenn das Item komplett aufgesaugt wurde
        if (itemOnGround.isEmpty()) {
            this.discard(); // Entity aus der Welt entfernen
            ci.cancel();    // Das Vanilla-Event abbrechen, damit es nicht doppelt aufgehoben wird
        }
    }


    @Inject(method = "tick", at = @At("HEAD"))
    private void floatInVoid(CallbackInfo ci) {
        // Prüfe ob es ein Enderite Item ist
        if (isEnderiteItem(this.getStack().getItem())) {
            // Wenn wir im Void sind (z.B. unter Y = -64 oder World Bottom)
            if (this.getY() < this.getEntityWorld().getBottomY()) {

                // Setze Bewegung auf 0 und schwebe langsam nach oben oder bleibe stehen
                Vec3d velocity = this.getVelocity();
                this.setVelocity(velocity.x * 0.9, 0.1, velocity.z * 0.9); // Schwebt langsam hoch

                // Setze Position hard, falls zu tief, damit es nicht despawned (Despawn ist meist bei -64 - 64)
                if (this.getY() < this.getEntityWorld().getBottomY() - 10) {
                    this.setPosition(this.getX(), this.getEntityWorld().getBottomY() + 5, this.getZ());
                    this.setVelocity(0, 0, 0);
                }

                // Verhindere Despawn Timer
                this.setNoGravity(true);
            } else {
                this.setNoGravity(false);
            }
        }
    }

    @Unique
    private boolean isEnderiteItem(Item item) {
        // Liste aller Enderite Items
        return item == ModItems.ENDERITE_INGOT || item == ModItems.ENDERITE_SCRAP
                || item == ModItems.ENDERITE_SWORD || item == ModItems.ENDERITE_PICKAXE
                /* ... Armor etc ... TODO */;
    }
}