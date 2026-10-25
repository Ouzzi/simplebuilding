package com.simplelib.mixin;

import com.simplelib.api.AnvilRechargeable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Recharging a charged gadget ({@link AnvilRechargeable}) in the anvil: costs no levels, uses only what the gadget needs
 * to be full (same rules as SimpleBuilding's {@code AnvilScreenHandlerMixin} for its own interface; the two never both apply).
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilRechargeMixin extends ItemCombinerMenu {
    @Shadow private int repairItemCountCost;
    @Shadow @Final private DataSlot cost;
    @Shadow @Nullable private String itemName;

    protected AnvilRechargeMixin(@Nullable MenuType<?> type, int syncId, Inventory inventory, ContainerLevelAccess access,
                                 ItemCombinerMenuSlotDefinition slots) {
        super(type, syncId, inventory, access, slots);
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void simplelib$rechargeCharged(CallbackInfo ci) {
        ItemStack left = this.inputSlots.getItem(0);
        ItemStack right = this.inputSlots.getItem(1);
        if (!(left.getItem() instanceof AnvilRechargeable rechargeable) || !rechargeable.isRechargeMaterial(right)) return;
        int used = rechargeable.rechargeMaterialNeeded(left, right.getCount());
        if (used <= 0) {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            this.cost.set(0);
            this.repairItemCountCost = 0;
            ci.cancel();
            return;
        }
        ItemStack result = rechargeable.rechargedWith(left, used);
        simplelib$rename(left, result);
        this.resultSlots.setItem(0, result);
        this.cost.set(0);
        this.repairItemCountCost = used;
        ci.cancel();
    }

    /** Vanilla never hands out a result at 0 levels ({@code cost > 0}); recharging is free. */
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void simplelib$freeRecharge(Player player, boolean hasItem, CallbackInfoReturnable<Boolean> cir) {
        if (this.inputSlots.getItem(0).getItem() instanceof AnvilRechargeable rechargeable
                && rechargeable.isRechargeMaterial(this.inputSlots.getItem(1))
                && !this.resultSlots.getItem(0).isEmpty()) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void simplelib$rename(ItemStack original, ItemStack result) {
        if (this.itemName != null && !StringUtil.isBlank(this.itemName)) {
            if (!this.itemName.equals(original.getHoverName().getString())) result.set(DataComponents.CUSTOM_NAME, Component.literal(this.itemName));
        } else if (original.has(DataComponents.CUSTOM_NAME)) {
            result.remove(DataComponents.CUSTOM_NAME);
        }
    }
}
