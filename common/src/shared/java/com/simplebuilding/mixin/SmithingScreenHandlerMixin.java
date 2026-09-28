package com.simplebuilding.mixin;

import com.simplebuilding.recipe.CountBasedSmithingRecipe;
import com.simplebuilding.util.TrimUpgrades;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

@Mixin(SmithingMenu.class)
public abstract class SmithingScreenHandlerMixin extends ItemCombinerMenu {

    public SmithingScreenHandlerMixin(@Nullable MenuType<?> type, int syncId, Inventory playerInventory, ContainerLevelAccess context, ItemCombinerMenuSlotDefinition slotsManager) {
        super(type, syncId, playerInventory, context, slotsManager);
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void onTakeOutputCustom(Player player, ItemStack stack, CallbackInfo ci) {
        Level world = player.level();

        // Logik nur auf dem Server ausführen (ServerRecipeManager existiert nur dort)
        if (world instanceof ServerLevel serverWorld) {

            // Wir casten den Manager zur Server-Implementation, die 'getFirstMatch' besitzt
            if (serverWorld.recipeAccess() instanceof RecipeManager serverRecipeManager) {

                SmithingRecipeInput input = new SmithingRecipeInput(
                        this.inputSlots.getItem(0),
                        this.inputSlots.getItem(1),
                        this.inputSlots.getItem(2)
                );

                // The recipe is registered with type SMITHING (SmithingRecipe#getType), not under
                // the mod's own COUNT_BASED_SMITHING type - asking for that type never matched and
                // the extra additions were never taken (audit 2026-09-26 #12).
                Optional<CountBasedSmithingRecipe> match = serverRecipeManager
                        .getRecipeFor(RecipeType.SMITHING, input, world)
                        .map(RecipeHolder::value)
                        .filter(CountBasedSmithingRecipe.class::isInstance)
                        .map(CountBasedSmithingRecipe.class::cast);

                if (match.isPresent()) {
                    CountBasedSmithingRecipe recipe = match.get();
                    int countToConsume = recipe.getAdditionCount();

                    // Wenn wir mehr als 1 Item verbrauchen müssen (Vanilla zieht 1 automatisch ab)
                    if (countToConsume > 1) {
                        ItemStack additionStack = this.inputSlots.getItem(2);
                        if (additionStack.getCount() >= countToConsume - 1) {
                             // Hier decrement wir manuell den Rest
                             additionStack.shrink(countToConsume - 1);
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$customSmithingLogic(CallbackInfo ci) {
        // Glowing (Leuchttinte), Emitting (Glowstonestaub) und Pulsating (Echoscherbe): die Regeln
        // stehen in TrimUpgrades, damit JEI dasselbe Ergebnis zeigt. null = nicht unsere Kombination,
        // dann rechnet Vanilla; EMPTY = unsere Kombination, aber die Obergrenze ist erreicht.
        ItemStack result = TrimUpgrades.result(this.inputSlots.getItem(0), this.inputSlots.getItem(1), this.inputSlots.getItem(2));
        if (result != null) {
            this.resultSlots.setItem(0, result);
            ci.cancel();
        }
    }

}