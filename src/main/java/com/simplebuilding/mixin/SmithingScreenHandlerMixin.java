package com.simplebuilding.mixin;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.recipe.CountBasedSmithingRecipe;
import com.simplebuilding.recipe.ModRecipes;
import com.simplebuilding.util.GlowingTrimUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.recipe.input.SmithingRecipeInput;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SmithingScreenHandler;
import net.minecraft.screen.slot.ForgingSlotsManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(SmithingScreenHandler.class)
public abstract class SmithingScreenHandlerMixin extends ForgingScreenHandler {

    public SmithingScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context, ForgingSlotsManager slotsManager) {
        super(type, syncId, playerInventory, context, slotsManager);
    }

    @Inject(method = "onTakeOutput", at = @At("HEAD"))
    private void onTakeOutputCustom(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        World world = player.getEntityWorld();

        // Logik nur auf dem Server ausführen (ServerRecipeManager existiert nur dort)
        if (world instanceof ServerWorld serverWorld) {

            // Wir casten den Manager zur Server-Implementation, die 'getFirstMatch' besitzt
            if (serverWorld.getRecipeManager() instanceof ServerRecipeManager serverRecipeManager) {

                SmithingRecipeInput input = new SmithingRecipeInput(
                        this.input.getStack(0),
                        this.input.getStack(1),
                        this.input.getStack(2)
                );

                // Jetzt können wir getFirstMatch aufrufen
                Optional<RecipeEntry<CountBasedSmithingRecipe>> match = serverRecipeManager
                        .getFirstMatch(ModRecipes.COUNT_BASED_SMITHING, input, world);

                if (match.isPresent()) {
                    CountBasedSmithingRecipe recipe = match.get().value();
                    int countToConsume = recipe.getAdditionCount();

                    // Wenn wir mehr als 1 Item verbrauchen müssen (Vanilla zieht 1 automatisch ab)
                    if (countToConsume > 1) {
                        ItemStack additionStack = this.input.getStack(2);
                        if (additionStack.getCount() >= countToConsume - 1) {
                             // Hier decrement wir manuell den Rest
                             additionStack.decrement(countToConsume - 1);
                        }
                    }
                }
            }
        }
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
                if (currentLevel < 2) {
                    ItemStack outputStack = armorStack.copy();
                    GlowingTrimUtils.setGlowLevel(outputStack, currentLevel + 1);
                    outputStack.setCount(1);
                    this.output.setStack(0, outputStack);
                    ci.cancel();
                    return;
                } else {
                    this.output.setStack(0, ItemStack.EMPTY);
                    ci.cancel();
                }
            }
        }

        // Prüfen, ob unsere spezifische Kombination vorliegt für emitting trim upgrade
        if (templateStack.isOf(ModItems.EMITTING_TRIM_TEMPLATE) && materialStack.isOf(Items.GLOWSTONE_DUST)) {
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