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

    public SmithingScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context, ForgingSlotsManager slotsManager) {
        super(type, syncId, playerInventory, context, slotsManager);
    }

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$customSmithingLogic(CallbackInfo ci) {
        // Zugriff auf die Eingabe-Slots
        ItemStack templateStack = this.input.getStack(0);
        ItemStack armorStack = this.input.getStack(1);
        ItemStack materialStack = this.input.getStack(2);

        // Prüfen, ob unsere spezifische Kombination vorliegt für glowing trim upgrade
        if (templateStack.isOf(ModItems.GLOWING_TRIM_TEMPLATE) && materialStack.isOf(Items.GLOW_INK_SAC)) {
            boolean isValidArmor = isValidArmor(armorStack);

            if (isValidArmor) {
                int currentLevel = GlowingTrimUtils.getGlowLevel(armorStack);
                if (currentLevel < 5) {
                    ItemStack outputStack = armorStack.copy();
                    // Erhöht den Glow-Level im NBT des Output-Stacks
                    GlowingTrimUtils.incrementGlowLevel(outputStack);
                    // Wir setzen 1 Item als Output (die Menge der Rüstung ist meist 1)
                    outputStack.setCount(1);
                    // Das Ergebnis in den Output-Slot setzen
                    this.output.setStack(0, outputStack);
                    ci.cancel();
                } else {
                    // Wenn Level 5 erreicht ist, kein Output (oder man erlaubt es, aber erhöht nicht mehr)
                    this.output.setStack(0, ItemStack.EMPTY);
                    ci.cancel();
                }
            }
        }

        // Prüfen, ob unsere spezifische Kombination vorliegt für emitting trim upgrade
        if (templateStack.isOf(ModItems.EMITTING_TRIM_TEMPLATE) && materialStack.isOf(Items.GLOWSTONE_DUST)) {
            boolean isValidArmor = isValidArmor(armorStack);

            if (isValidArmor) {
                int currentLevel = GlowingTrimUtils.getGlowLevel(armorStack);
                if (currentLevel < 5) {
                    ItemStack outputStack = armorStack.copy();
                    // Erhöht den Emitting-Level im NBT des Output-Stacks
                    GlowingTrimUtils.incrementGlowLevel(outputStack);
                    // Wir setzen 1 Item als Output (die Menge der Rüstung ist meist 1)
                    outputStack.setCount(1);
                    // Das Ergebnis in den Output-Slot setzen
                    this.output.setStack(0, outputStack);
                    ci.cancel();
                } else {
                    // Wenn Level 5 erreicht ist, kein Output (oder man erlaubt es, aber erhöht nicht mehr)
                    this.output.setStack(0, ItemStack.EMPTY);
                    ci.cancel();
                }
            }
        }

    }

    private static boolean isValidArmor(ItemStack armorStack) {
        return !armorStack.isEmpty() && (armorStack.isIn(ItemTags.TRIMMABLE_ARMOR) || armorStack.get(DataComponentTypes.EQUIPPABLE) != null);
    }

}