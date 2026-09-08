package com.simplebuilding.mixin;

import com.simplebuilding.items.custom.QuiverItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
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
    // static, nicht Instanzfeld: Mixin uebernimmt Initialisierer von @Unique-INSTANZfeldern
    // nicht zuverlaessig ins Ziel - das zweite Feld unten hat genau das ausgeloest, und
    // usedQuiver kam als null im Spiel an. Ein statischer Initialisierer landet im <clinit>
    // und wird sicher uebernommen. Inhaltlich aendert das nichts: ThreadLocal traegt seinen
    // Zustand ohnehin pro Thread, und BowItem ist ein Singleton.
    private static final ThreadLocal<Boolean> usedQuiver = ThreadLocal.withInitial(() -> false);

    // Der Pfeil, den der Köcher geliefert hat. Der Verbrauch unten braucht ihn, weil
    // ammo_use-Verzauberungen (Unendlichkeit) auf die Pfeilsorte schauen.
    @Unique
    private static final ThreadLocal<ItemStack> quiverProjectile = ThreadLocal.withInitial(() -> ItemStack.EMPTY);

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
        quiverProjectile.set(ItemStack.EMPTY);

        ItemStack quiverArrow = QuiverItem.findProjectileForBow(player);
        if (!quiverArrow.isEmpty()) {
            usedQuiver.set(true); // Wir benutzen den Köcher!
            quiverProjectile.set(quiverArrow);
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
            quiverProjectile.set(ItemStack.EMPTY);
            return;
        }

        // Nur entfernen, wenn wir vorher entschieden haben, den Köcher zu nutzen - und nur dann,
        // wenn Vanilla im selben Schuss ebenfalls bezahlt haette. ProjectileWeaponItem#useAmmo
        // rechnet den Verbrauch mit EnchantmentHelper.processAmmoUse aus; kommt dabei 0 heraus
        // (Unendlichkeit auf einem einfachen Pfeil), markiert Vanilla den abgeschossenen Pfeil als
        // INTANGIBLE_PROJECTILE und zieht nichts ab - dann darf auch der Köcher nichts zahlen.
        // processAmmoUse statt einer festen Unendlichkeits-Abfrage: es holt die Bedingung aus
        // infinity.json (nur minecraft:arrow, Spektral- und getränkte Pfeile kosten weiter) und
        // traegt jede datengetriebene ammo_use-Verzauberung anderer Mods automatisch mit.
        // Der ServerLevel-Zweig deckt sich mit Vanilla, das clientseitig ohnehin nichts verbraucht.
        if (usedQuiver.get() && world instanceof ServerLevel serverLevel
                && EnchantmentHelper.processAmmoUse(serverLevel, stack, quiverProjectile.get(), 1) > 0) {
            QuiverItem.consumeProjectileForBow(player);
        }

        usedQuiver.set(false);
        quiverProjectile.set(ItemStack.EMPTY);
    }
}