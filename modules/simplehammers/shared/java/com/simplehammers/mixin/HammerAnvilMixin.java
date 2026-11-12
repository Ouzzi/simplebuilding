package com.simplehammers.mixin;

import com.simplehammers.engine.HammerAnvil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sledgehammers at the anvil ({@link HammerAnvil}: hammer + hammer, hammer + material); moved from SimpleBuilding's
 * {@code AnvilScreenHandlerMixin} (REV24-F-18), whose hammer part only runs on 26.2 ({@code McVersion.HAMMERS_IN_SB}),
 * so with or without SimpleBuilding exactly this mixin handles hammers. Vanilla's {@code mayPickup} takes the result,
 * the cost is always at least one level. PLAN40 simplehammers-2 (owner A): the prior work penalty counts like Vanilla's,
 * and from 40 levels on the result is "too expensive" outside creative, as Vanilla's own {@code createResult} does.
 */
@Mixin(AnvilMenu.class)
public abstract class HammerAnvilMixin extends ItemCombinerMenu {
    @Shadow private int repairItemCountCost;
    @Shadow @Final private DataSlot cost;
    @Shadow @Nullable private String itemName;

    protected HammerAnvilMixin(@Nullable MenuType<?> type, int syncId, Inventory inventory, ContainerLevelAccess access,
                               ItemCombinerMenuSlotDefinition slots) {
        super(type, syncId, inventory, access, slots);
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void simplehammers$hammerResult(CallbackInfo ci) {
        HammerAnvil.Result result = HammerAnvil.result(this.inputSlots.getItem(0), this.inputSlots.getItem(1), this.itemName);
        if (result == null) return;
        this.cost.set(result.cost());
        this.repairItemCountCost = result.materials();
        boolean tooExpensive = result.cost() >= 40 && !this.player.hasInfiniteMaterials(); // Vanilla's "too expensive" limit
        this.resultSlots.setItem(0, tooExpensive ? ItemStack.EMPTY : result.stack());
        ci.cancel();
    }
}
