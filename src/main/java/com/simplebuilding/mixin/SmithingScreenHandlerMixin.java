package com.simplebuilding.mixin;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.GlowingTrimUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SmithingScreenHandler;
import net.minecraft.screen.slot.ForgingSlotsManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmithingScreenHandler.class)
public abstract class SmithingScreenHandlerMixin extends ForgingScreenHandler {

    // Konstruktor-Fix für 1.21 (5 Argumente)
    public SmithingScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context, ForgingSlotsManager slotsManager) {
        super(type, syncId, playerInventory, context, slotsManager);
    }

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$customSmithingLogic(CallbackInfo ci) {
        // Slot 0: Template, Slot 1: Armor, Slot 2: Material
        ItemStack templateStack = this.input.getStack(0);
        ItemStack armorStack = this.input.getStack(1);
        ItemStack materialStack = this.input.getStack(2);

        // Check: Ist es unser Glowing Template + Glow Ink Sac?
        if (templateStack.isOf(ModItems.GLOWING_TRIM_TEMPLATE) && materialStack.isOf(Items.GLOW_INK_SAC)) {

            // Check: Ist es eine Rüstung?
            boolean isArmor = armorStack.isIn(ItemTags.TRIMMABLE_ARMOR)
                    || armorStack.get(DataComponentTypes.EQUIPPABLE) != null;

            if (isArmor && !armorStack.isEmpty()) {
                int currentLevel = GlowingTrimUtils.getGlowLevel(armorStack);

                // Upgrade bis Level 5
                if (currentLevel < 5) {
                    ItemStack outputStack = armorStack.copy();
                    GlowingTrimUtils.incrementGlowLevel(outputStack);

                    this.output.setStack(0, outputStack);
                    ci.cancel(); // Wichtig: Vanilla Logik stoppen
                }
            }
        }
    }
}