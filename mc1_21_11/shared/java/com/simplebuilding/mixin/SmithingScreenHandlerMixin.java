package com.simplebuilding.mixin;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.recipe.CountBasedSmithingRecipe;
import com.simplebuilding.recipe.ModRecipes;
import com.simplebuilding.util.GlowingTrimUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
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

                // Jetzt können wir getFirstMatch aufrufen
                Optional<RecipeHolder<CountBasedSmithingRecipe>> match = serverRecipeManager
                        .getRecipeFor(ModRecipes.COUNT_BASED_SMITHING, input, world);

                if (match.isPresent()) {
                    CountBasedSmithingRecipe recipe = match.get().value();
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
        // Zugriff auf die Eingabe-Slots
        ItemStack templateStack = this.inputSlots.getItem(0);
        ItemStack armorStack = this.inputSlots.getItem(1);
        ItemStack materialStack = this.inputSlots.getItem(2);

        // Prüfen, ob unsere spezifische Kombination vorliegt für glowing trim upgrade
        if (templateStack.is(ModItems.GLOWING_TRIM_TEMPLATE) && materialStack.is(Items.GLOW_INK_SAC)) {
            boolean isValidArmor = isValidArmor(armorStack);

            if (isValidArmor) {
                int currentLevel = GlowingTrimUtils.getGlowLevel(armorStack);
                if (currentLevel < 2) {
                    ItemStack outputStack = armorStack.copy();
                    GlowingTrimUtils.setGlowLevel(outputStack, currentLevel + 1);
                    outputStack.setCount(1);
                    this.resultSlots.setItem(0, outputStack);
                    ci.cancel();
                    return;
                } else {
                    this.resultSlots.setItem(0, ItemStack.EMPTY);
                    ci.cancel();
                }
            }
        }

        // Prüfen, ob unsere spezifische Kombination vorliegt für emitting trim upgrade
        if (templateStack.is(ModItems.EMITTING_TRIM_TEMPLATE) && materialStack.is(Items.GLOWSTONE_DUST)) {
            boolean isValidArmor = isValidArmor(armorStack);

            if (isValidArmor) {
                int currentLevel = GlowingTrimUtils.getEmissionLevel(armorStack);
                if (currentLevel < 5) {
                    ItemStack outputStack = armorStack.copy();
                    // Erhöht den Glow-Level im NBT des Output-Stacks
                    GlowingTrimUtils.incrementEmissionLevel(outputStack);
                    // Wir setzen 1 Item als Output (die Menge der Rüstung ist meist 1)
                    outputStack.setCount(1);
                    // Das Ergebnis in den Output-Slot setzen
                    this.resultSlots.setItem(0, outputStack);
                    ci.cancel();
                } else {
                    // Wenn Level 5 erreicht ist, kein Output (oder man erlaubt es, aber erhöht nicht mehr)
                    this.resultSlots.setItem(0, ItemStack.EMPTY);
                    ci.cancel();
                }
            }
        }

    }

    private static boolean isValidArmor(ItemStack armorStack) {
        return !armorStack.isEmpty() && (armorStack.is(ItemTags.TRIMMABLE_ARMOR) || armorStack.get(DataComponents.EQUIPPABLE) != null);
    }

}