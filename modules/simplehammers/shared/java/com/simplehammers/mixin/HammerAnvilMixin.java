package com.simplehammers.mixin;

import com.simplehammers.engine.HammerAnvil;
import com.simplehammers.item.SledgehammerItem;
import net.minecraft.core.component.DataComponents;
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
 * the cost is always at least one level.
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
        this.resultSlots.setItem(0, result.stack());
        this.cost.set(result.cost());
        this.repairItemCountCost = result.materials();
        ci.cancel();
    }

    /** Whatever made a hammer result (vanilla included): it never raises the repair cost and is never "too expensive". */
    @Inject(method = "createResult", at = @At("RETURN"))
    private void simplehammers$hammerRepairCost(CallbackInfo ci) {
        ItemStack output = this.resultSlots.getItem(0);
        if (output.isEmpty() || !SledgehammerItem.isHammer(output)) return;
        output.set(DataComponents.REPAIR_COST, 0);
        if (this.cost.get() >= 40) this.cost.set(39);
    }
}
