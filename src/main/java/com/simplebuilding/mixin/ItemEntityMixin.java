package com.simplebuilding.mixin;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
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

        // --- WICHTIGE ÄNDERUNG ---
        // Wenn der Spieler NICHT schleicht, brechen wir hier sofort ab.
        // Das Mixin macht dann nichts, und das normale Minecraft-Aufheben (Vanilla) passiert.
        if (!player.isSneaking()) return;

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
            // Prüfen, ob das FUNNEL Enchantment vorhanden ist
            if (hasFunnelEnchantment(bundleStack, player)) {
                // Versuchen einzufügen
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

    @Unique
    private boolean hasFunnelEnchantment(ItemStack stack, PlayerEntity player) {
        var registry = player.getEntityWorld().getRegistryManager();
        var enchantments = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var funnel = enchantments.getOptional(ModEnchantments.FUNNEL);
        return funnel.isPresent() && EnchantmentHelper.getLevel(funnel.get(), stack) > 0;
    }
}