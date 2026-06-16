package com.simplebuilding.mixin;

import com.simplebuilding.items.custom.QuiverItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BowItem.class)
public class BowItemMixin {

    // ThreadLocal verhindert Konflikte und merkt sich den Status pro Schuss-Vorgang
    @Unique
    private final ThreadLocal<Boolean> usedQuiver = ThreadLocal.withInitial(() -> false);

    // 1. "use": Prüft zuerst den Köcher (Priorität!)
    @Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getProjectile(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack checkQuiverOnUse(Player player, ItemStack stack) {
        ItemStack quiverArrow = QuiverItem.findProjectileForBow(player);
        if (!quiverArrow.isEmpty()) {
            return quiverArrow;
        }
        return player.getProjectile(stack);
    }

    // 2. "onStoppedUsing": Prüft zuerst den Köcher und setzt das Flag
    @Redirect(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getProjectile(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack checkQuiverOnStop(Player player, ItemStack stack) {
        usedQuiver.set(false); // Reset

        ItemStack quiverArrow = QuiverItem.findProjectileForBow(player);
        if (!quiverArrow.isEmpty()) {
            usedQuiver.set(true); // Wir benutzen den Köcher!
            return quiverArrow;
        }

        return player.getProjectile(stack);
    }

    // 3. Verbrauch: Entfernt den Pfeil NUR, wenn das Flag gesetzt ist
    @Inject(method = "releaseUsing", at = @At("RETURN"))
    private void consumeArrowFromQuiver(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
        // Abbruch wenn Schuss fehlgeschlagen oder Creative Mode
        if (!cir.getReturnValue() || !(user instanceof Player player) || player.getAbilities().instabuild) {
            usedQuiver.set(false);
            return;
        }

        // Nur entfernen, wenn wir vorher entschieden haben, den Köcher zu nutzen
        if (usedQuiver.get()) {
            QuiverItem.consumeProjectileForBow(player);
        }

        usedQuiver.set(false);
    }
}