package com.simplebuilding.mixin;

import com.simplebuilding.items.custom.QuiverItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Der Koecher versorgt auch die Armbrust (Besitzer 2026-09-28), mit derselben Suche wie der Bogen
 * ({@link QuiverItem#findProjectileForCrossbow}; gehaltene Munition wie eine Rakete geht vor).
 *
 * <p>Geladen wird in {@code tryLoadProjectiles}: Vanillas {@code draw} zieht die Kosten
 * ({@code useAmmo}, also Unendlichkeit, Mehrfachschuss und Kreativ eingerechnet) vom uebergebenen
 * Stapel ab. Der Koecher reicht eine Kopie; was {@code draw} von ihr abgezogen hat, wird danach
 * genau so oft aus dem Koecher genommen - die Armbrust zahlt also exakt, was Vanilla fuer lose
 * Pfeile verlangt haette.
 */
@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {

    // static: siehe BowItemMixin - Initialisierer von @Unique-Instanzfeldern kommen nicht sicher an.
    @Unique
    private static final ThreadLocal<ItemStack> simplebuilding$quiverArrow = ThreadLocal.withInitial(() -> ItemStack.EMPTY);
    @Unique
    private static final ThreadLocal<Integer> simplebuilding$countBefore = ThreadLocal.withInitial(() -> 0);

    @Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getProjectile(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack simplebuilding$quiverOnUse(Player player, ItemStack crossbow) {
        ItemStack arrow = QuiverItem.findProjectileForCrossbow(player, crossbow);
        return arrow.isEmpty() ? player.getProjectile(crossbow) : arrow;
    }

    @Redirect(method = "tryLoadProjectiles", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getProjectile(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack simplebuilding$quiverOnLoad(LivingEntity shooter, ItemStack crossbow) {
        simplebuilding$quiverArrow.set(ItemStack.EMPTY);
        if (shooter instanceof Player player) {
            ItemStack arrow = QuiverItem.findProjectileForCrossbow(player, crossbow);
            if (!arrow.isEmpty()) {
                simplebuilding$quiverArrow.set(arrow);
                simplebuilding$countBefore.set(arrow.getCount());
                return arrow;
            }
        }
        return shooter.getProjectile(crossbow);
    }

    @Inject(method = "tryLoadProjectiles", at = @At("RETURN"))
    private static void simplebuilding$billQuiver(LivingEntity shooter, ItemStack crossbow, CallbackInfoReturnable<Boolean> cir) {
        ItemStack arrow = simplebuilding$quiverArrow.get();
        simplebuilding$quiverArrow.set(ItemStack.EMPTY);
        if (arrow.isEmpty() || !cir.getReturnValue() || !(shooter instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        int spent = simplebuilding$countBefore.get() - arrow.getCount();
        for (int i = 0; i < spent; i++) {
            QuiverItem.consumeProjectileForBow(player);
        }
    }
}
