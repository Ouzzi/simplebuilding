package com.simplebuilding.mixin;

import com.simplebuilding.blueprint.BlueprintCartography;
import com.simplebuilding.blueprint.BlueprintScanner;
import com.simplebuilding.items.custom.BlueprintItem;
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
 * Kartentisch als Blaupausen-Scanner: Oktant mit Auswahl oben, leere/unsignierte Blaupause
 * unten, gefuellte Blaupause rechts. Logik in {@link BlueprintCartography}/{@link BlueprintScanner}.
 */
@Mixin(CartographyTableMenu.class)
public abstract class CartographyTableMenuMixin extends AbstractContainerMenu {
    @Shadow @Final private ContainerLevelAccess access;
    @Shadow @Final private ResultContainer resultContainer;

    @Unique
    private Player simplebuilding$player;

    protected CartographyTableMenuMixin(MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    private void simplebuilding$wrapSlots(int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
        this.simplebuilding$player = inventory.player;
        BlueprintCartography.wrapSlots(this, this.slots, access);
    }

    @Inject(method = "setupResultSlot", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$scanBlueprint(ItemStack map, ItemStack additional, ItemStack result, CallbackInfo ci) {
        if (!BlueprintCartography.isScanSetup(map, additional)) {
            return;
        }
        ci.cancel();
        this.access.execute((level, pos) -> {
            ItemStack out = BlueprintCartography.result(level, pos, map, additional, this.simplebuilding$player);
            if (!ItemStack.matches(out, result)) {
                this.resultContainer.setItem(BlueprintCartography.RESULT_SLOT, out);
                this.broadcastChanges();
            }
        });
    }

    /** Umschalt-Klick legt Oktant und Blaupause in ihre Tisch-Slots statt in die Hotbar. */
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$quickMoveBlueprintItems(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        if (slotIndex <= BlueprintCartography.RESULT_SLOT || slotIndex >= this.slots.size()) {
            return;
        }
        Slot slot = this.slots.get(slotIndex);
        ItemStack stack = slot.getItem();
        int target = BlueprintScanner.isOctant(stack) ? BlueprintCartography.MAP_SLOT
                : stack.getItem() instanceof BlueprintItem ? BlueprintCartography.ADDITIONAL_SLOT : -1;
        if (target < 0) {
            return;
        }
        ItemStack before = stack.copy();
        if (!this.moveItemStackTo(stack, target, target + 1, false)) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        cir.setReturnValue(before);
    }
}
