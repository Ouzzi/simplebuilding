package com.simplebuilding.mixin;

import com.simplebuilding.dummy.SmallArmorStand;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.EquipmentDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Kleiner Ruestungsstaender (Nachtrag 29): ein Spender legt ein Ruestungsteil oder eine Tier-Ruestung auf einen leeren
 * kleinen Staender vor sich. Vanilla erlaubt Pferde-/Wolfs-/Nautilus-Ruestung nur ihren Tieren, deshalb ein eigener Weg
 * vor Vanillas Suche; alles andere (Tiere, Spieler, andere Staender) bleibt Vanilla.
 */
@Mixin(EquipmentDispenseItemBehavior.class)
public abstract class EquipmentDispenseStandMixin {
    @Inject(method = "dispenseEquipment", at = @At("HEAD"), cancellable = true)
    private static void simplebuilding$smallStand(BlockSource source, ItemStack dispensed, CallbackInfoReturnable<Boolean> cir) {
        if (!SmallArmorStand.accepts(dispensed)) {
            return;
        }
        BlockPos pos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
        List<SmallArmorStand> stands = source.level().getEntitiesOfClass(SmallArmorStand.class, new AABB(pos),
                stand -> stand.isAlive() && stand.shownSlot() == null);
        if (!stands.isEmpty() && stands.getFirst().put(dispensed)) {
            dispensed.shrink(1);
            cir.setReturnValue(true);
        }
    }
}
