package com.simplebuilding.mixin;

import com.simplebuilding.items.custom.QuiverItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BowItem.class)
public class BowItemMixin {

    // 1. Damit der Bogen überhaupt anfängt zu ziehen (use), gaukeln wir ihm vor, wir hätten einen Pfeil,
    // falls wir einen im Köcher finden.
    @Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getProjectileType(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"))
    private ItemStack checkQuiverOnUse(PlayerEntity player, ItemStack stack) {
        ItemStack vanillaProjectile = player.getProjectileType(stack);
        if (!vanillaProjectile.isEmpty()) return vanillaProjectile;

        // Check Quiver (Offhand/Chest)
        ItemStack quiverArrow = QuiverItem.findProjectileForBow(player);
        if (!quiverArrow.isEmpty()) {
            return quiverArrow; // Gibt eine Kopie zurück, damit der Check !isEmpty() besteht
        }
        return ItemStack.EMPTY;
    }

    // 2. Beim Loslassen (onStoppedUsing) prüft Vanilla erneut auf Pfeile.
    // Wir leiten dies um, um den Köcher-Pfeil zurückzugeben, damit der Schuss ausgelöst wird.
    @Redirect(method = "onStoppedUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getProjectileType(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"))
    private ItemStack checkQuiverOnStop(PlayerEntity player, ItemStack stack) {
        ItemStack vanillaProjectile = player.getProjectileType(stack);
        if (!vanillaProjectile.isEmpty()) return vanillaProjectile;

        return QuiverItem.findProjectileForBow(player);
    }

    // 3. Verbrauch des Pfeils aus dem Köcher.
    @Inject(method = "onStoppedUsing", at = @At("RETURN"))
    private void consumeArrowFromQuiver(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
        // Nur weitermachen, wenn der Schuss erfolgreich war (true zurückgegeben wurde)
        if (!cir.getReturnValue()) return;

        if (!(user instanceof PlayerEntity player) || player.getAbilities().creativeMode) return;

        // Wenn Vanilla einen Pfeil gefunden hätte, wäre er bereits verbraucht worden.
        ItemStack vanillaProjectile = player.getProjectileType(stack);

        // Wenn Vanilla NICHTS findet, aber die Methode erfolgreich war (durch unseren Redirect oben),
        // dann müssen wir den Pfeil manuell aus dem Köcher entfernen.
        if (vanillaProjectile.isEmpty()) {
            QuiverItem.consumeProjectileForBow(player);
        }
    }
}