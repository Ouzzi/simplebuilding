package com.simplebuilding.mixin;

import com.simplebuilding.util.AstralStorage;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEnderChestContainer.class)
public abstract class AstralStorageMixin implements AstralStorage {
    @Unique private final SimpleContainer simplebuilding$extra = new SimpleContainer(27);
    @Override public SimpleContainer simplebuilding$astralStorage() { return simplebuilding$extra; }

    @Inject(method = "fromSlots", at = @At("TAIL"))
    private void simplebuilding$load(ValueInput.TypedInputList<ItemStackWithSlot> slots, CallbackInfo ci) {
        if (!com.simplebuilding.version.McVersion.END_SYSTEMS) return;
        simplebuilding$extra.clearContent();
        for (ItemStackWithSlot slot : slots) if (slot.slot() >= 27 && slot.slot() < 54)
            simplebuilding$extra.setItem(slot.slot() - 27, slot.stack());
    }

    @Inject(method = "storeAsSlots", at = @At("TAIL"))
    private void simplebuilding$save(ValueOutput.TypedOutputList<ItemStackWithSlot> slots, CallbackInfo ci) {
        if (!com.simplebuilding.version.McVersion.END_SYSTEMS) return;
        for (int i = 0; i < 27; i++) if (!simplebuilding$extra.getItem(i).isEmpty())
            slots.add(new ItemStackWithSlot(i + 27, simplebuilding$extra.getItem(i)));
    }
}
