package com.simplebuilding.modules.simpletweaks.mixin;

import com.simplebuilding.modules.simpletweaks.deed.DeedCartography;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Claim deeds at the cartography table (SP22-8 §5, rules in {@link DeedCartography}). Priority 1300: applied after
 * Simple Maps (1000), SimpleBuilding's blueprint (1100) and Simple Music's sheets (1200), so it wraps their slots.
 */
@Mixin(value = CartographyTableMenu.class, priority = 1300)
public abstract class CartographyDeedMixin extends AbstractContainerMenu {
    @Shadow @Final private ContainerLevelAccess access;
    @Shadow @Final private ResultContainer resultContainer;
    /** The player at the table: a menu belongs to one player, who is the one taking (and so bound to) the deed. */
    @Unique private Player simpletweaks$player;

    protected CartographyDeedMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    private void simpletweaks$wrapDeedSlots(int id, Inventory inventory, ContainerLevelAccess levelAccess, CallbackInfo ci) {
        simpletweaks$player = inventory.player;
        DeedCartography.wrapSlots(this, this.slots, levelAccess);
    }

    @Inject(method = "setupResultSlot", at = @At("HEAD"), cancellable = true)
    private void simpletweaks$deedResult(ItemStack map, ItemStack additional, ItemStack current, CallbackInfo ci) {
        if (!com.simplebuilding.modules.simpletweaks.claims.Claims.on() || !DeedCartography.involves(map, additional)) return;
        ci.cancel();
        access.execute((level, pos) -> {
            ItemStack result = DeedCartography.result(simpletweaks$player, level.getServer(), map, additional);
            if (!ItemStack.matches(result, resultContainer.getItem(DeedCartography.RESULT_SLOT))) resultContainer.setItem(DeedCartography.RESULT_SLOT, result);
            broadcastChanges();
        });
    }

    /** Shift-click puts deeds into the table's slots instead of the hotbar. */
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void simpletweaks$quickMoveDeed(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index <= DeedCartography.RESULT_SLOT || index >= this.slots.size()) return;
        Slot slot = this.slots.get(index);
        ItemStack stack = slot.getItem();
        int target = DeedCartography.quickMoveTarget(stack, this.slots.get(DeedCartography.MAP_SLOT).getItem());
        if (target < 0) return;
        ItemStack before = stack.copy();
        if (!this.moveItemStackTo(stack, target, target + 1, false)) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        cir.setReturnValue(before);
    }
}
