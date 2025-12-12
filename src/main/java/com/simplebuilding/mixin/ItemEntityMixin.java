package com.simplebuilding.mixin;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
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

    public ItemEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow public abstract ItemStack getStack();

    @Inject(method = "onPlayerCollision", at = @At("HEAD"))
    private void onPlayerCollision(PlayerEntity player, CallbackInfo ci) {
        System.out.println("onPlayerCollision triggered");

        if (this.getEntityWorld().isClient()) return;

        if (!player.isSneaking()) return;

        ItemStack itemOnGround = this.getStack();
        if (itemOnGround.isEmpty()) return;
        
        System.out.println("item detected");

        for (Hand hand : Hand.values()) {
            ItemStack heldItem = player.getStackInHand(hand);

            if (tryPickupWithBundle(heldItem, itemOnGround, player)) {
                if (itemOnGround.isEmpty()) {
                    this.discard();
                }
                return;
            }
        }
    }

    @Unique
    private boolean tryPickupWithBundle(ItemStack bundleStack, ItemStack itemToPickup, PlayerEntity player) {
        if (!(bundleStack.getItem() instanceof ReinforcedBundleItem)) return false;

        var registry = player.getEntityWorld().getRegistryManager();
        var enchantments = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var funnel = enchantments.getOptional(ModEnchantments.FUNNEL);

        if (funnel.isEmpty() || EnchantmentHelper.getLevel(funnel.get(), bundleStack) <= 0) {
            System.out.println("no funnel enchantment");
            return false;
        }

        boolean success = ((ReinforcedBundleItem) bundleStack.getItem()).tryInsertStackFromWorld(bundleStack, itemToPickup, player);

        if (success) {
            player.sendPickup(this, itemToPickup.getCount());
        }
        System.out.println("bundle tryes to pick up. response -> " + success);
        return success;
    }

    @Inject(method = "isImmuneToExplosion", at = @At("HEAD"), cancellable = true)
    private void isNetheriteBundleImmune(Explosion explosion, CallbackInfoReturnable<Boolean> cir) {
        if (this.getStack().isOf(ModItems.NETHERITE_BUNDLE)) {
            cir.setReturnValue(true);
        }
    }

    @Shadow private int pickupDelay;

    @Inject(method = "onPlayerCollision", at = @At(value = "HEAD"), cancellable = true)
    private void onPlayerPickup(PlayerEntity player, CallbackInfo ci) {
        if (this.getEntityWorld().isClient()) return;

        // Pickup Delay prüfen (wie in Vanilla)
        if (this.pickupDelay == 0) {
            ItemStack stackOnGround = this.getStack();

            // Inventar des Spielers durchsuchen
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack inventoryStack = player.getInventory().getStack(i);

                // Prüfen ob das Item im Inventar ein ReinforcedBundle ist (oder davon erbt, wie Quiver)
                if (inventoryStack.getItem() instanceof ReinforcedBundleItem bundleItem) {

                    // Prüfen auf FUNNEL Enchantment
                    if (hasFunnelEnchantment(inventoryStack, player)) {

                        // Versuchen das Item einzufügen
                        boolean inserted = bundleItem.tryInsertStackFromWorld(inventoryStack, stackOnGround, player);

                        if (inserted) {
                            // Wenn etwas eingefügt wurde, Statistik erhöhen (optional)
                            player.increaseStat(Stats.PICKED_UP.getOrCreateStat(stackOnGround.getItem()), stackOnGround.getCount());

                            // Wenn der Stack leer ist (alles eingesaugt), Entity entfernen
                            if (stackOnGround.isEmpty()) {
                                this.discard();
                                ci.cancel(); // Vanilla Pickup abbrechen
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean hasFunnelEnchantment(ItemStack stack, PlayerEntity player) {
        var registry = player.getEntityWorld().getRegistryManager();
        var enchantments = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var funnel = enchantments.getOptional(ModEnchantments.FUNNEL);

        return funnel.isPresent() && EnchantmentHelper.getLevel(funnel.get(), stack) > 0;
    }
}