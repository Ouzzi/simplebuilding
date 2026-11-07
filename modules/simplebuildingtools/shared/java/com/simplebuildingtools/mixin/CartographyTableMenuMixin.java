package com.simplebuildingtools.mixin;

import com.simplebuildingtools.blueprint.BlueprintCartography;
import com.simplebuildingtools.blueprint.BlueprintScanner;
import com.simplebuildingtools.items.custom.BlueprintItem;
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
// Hoehere Prioritaet = spaeter angewandt: wickelt auch Slots anderer Mods (Simple Maps) statt von ihnen ersetzt zu werden.
@Mixin(value = CartographyTableMenu.class, priority = 1100)
public abstract class CartographyTableMenuMixin extends AbstractContainerMenu implements com.simplebuildingtools.blueprint.CartographyScanTicker {
    @Shadow @Final private ContainerLevelAccess access;
    @Shadow @Final private ResultContainer resultContainer;

    @Shadow @Final public net.minecraft.world.Container container;

    @Unique
    private Player simplebuildingtools$player;

    @Unique
    private final BlueprintCartography.TableScan simplebuildingtools$scan = new BlueprintCartography.TableScan();

    protected CartographyTableMenuMixin(MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    private void simplebuildingtools$wrapSlots(int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
        this.simplebuildingtools$player = inventory.player;
        BlueprintCartography.wrapSlots(this, this.slots, access);
    }

    @Inject(method = "setupResultSlot", at = @At("HEAD"), cancellable = true)
    private void simplebuildingtools$scanBlueprint(ItemStack map, ItemStack additional, ItemStack result, CallbackInfo ci) {
        if (!BlueprintCartography.isScanSetup(map, additional)) {
            return;
        }
        ci.cancel();
        this.access.execute((level, pos) -> this.simplebuildingtools$scan.inputsChanged(level, pos, map, additional,
                this.simplebuildingtools$player, this::simplebuildingtools$setResult));
    }

    /**
     * Laeuft jeden Server-Tick fuer das offene Menue (aus {@code AbstractContainerMenuMixin}, nicht als Override: SimpleBuilding
     * ueberschreibt broadcastChanges hier schon fuer seine 26.2-Blaupausen): fuehrt einen grossen Scan fort.
     */
    @Override
    public void simplebuildingtools$tickScan() {
        if (this.simplebuildingtools$scan.running()) {
            this.access.execute((level, pos) -> this.simplebuildingtools$scan.tick(level, this.container.getItem(0),
                    this.container.getItem(1), this.simplebuildingtools$player, this::simplebuildingtools$setResult));
        }
    }

    @Unique
    private void simplebuildingtools$setResult(ItemStack out) {
        if (!ItemStack.matches(out, this.resultContainer.getItem(BlueprintCartography.RESULT_SLOT))) {
            this.resultContainer.setItem(BlueprintCartography.RESULT_SLOT, out);
        }
    }

    /** Umschalt-Klick legt Oktant und Blaupause in ihre Tisch-Slots statt in die Hotbar. */
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void simplebuildingtools$quickMoveBlueprintItems(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        if (slotIndex <= BlueprintCartography.RESULT_SLOT || slotIndex >= this.slots.size()) {
            return;
        }
        Slot slot = this.slots.get(slotIndex);
        ItemStack stack = slot.getItem();
        int target = BlueprintScanner.isOctant(stack) || BlueprintCartography.isSigned(stack) ? BlueprintCartography.MAP_SLOT
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
