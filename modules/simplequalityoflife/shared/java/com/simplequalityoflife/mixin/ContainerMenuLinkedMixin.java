package com.simplequalityoflife.mixin;

import com.simplequalityoflife.container.LinkedContainers;
import com.simplequalityoflife.container.LinkedMenu;
import com.simplequalityoflife.container.PortableContainers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Linked GUIs and Easy Shulkers: extra slots, shift-click routing and the inventory right-click. */
@Mixin(AbstractContainerMenu.class)
public abstract class ContainerMenuLinkedMixin implements LinkedMenu {
    @Shadow protected abstract Slot addSlot(Slot slot);

    @Shadow protected abstract boolean moveItemStackTo(ItemStack stack, int start, int end, boolean backwards);

    @Unique private LinkedContainers.@Nullable Session qol$session;
    @Unique private @Nullable Object qol$panel;

    @Override
    public void qol$addSlot(Slot slot) {
        this.addSlot(slot);
    }

    @Override
    public boolean qol$move(ItemStack stack, int start, int end, boolean backwards) {
        return this.moveItemStackTo(stack, start, end, backwards);
    }

    @Override
    public LinkedContainers.@Nullable Session qol$session() {
        return this.qol$session;
    }

    @Override
    public void qol$session(LinkedContainers.@Nullable Session session) {
        this.qol$session = session;
    }

    @Override
    public @Nullable Object qol$panel() {
        return this.qol$panel;
    }

    @Override
    public void qol$panel(@Nullable Object panel) {
        this.qol$panel = panel;
    }

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void qol$clicked(int slotIndex, int button, ContainerInput input, Player player, CallbackInfo ci) {
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        if (PortableContainers.interceptClick(self, slotIndex, button, input, player) || LinkedContainers.interceptClick(self, slotIndex, input, player)) ci.cancel();
    }
}
