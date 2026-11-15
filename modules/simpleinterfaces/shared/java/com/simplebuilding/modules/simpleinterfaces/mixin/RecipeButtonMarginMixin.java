package com.simplebuilding.modules.simpleinterfaces.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simplebuilding.modules.simpleinterfaces.client.StyledScreens;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * N64 (A7 container margin): the screens with Vanilla's recipe book button grow with the margin; Vanilla places the button at
 * {@code height / 2 - k} (for its own image height), so it would slide down into the box frame (player inventory: onto the
 * container box's bottom edge). {@link StyledScreens#recipeButton} keeps it at its Vanilla place in the image.
 */
@Mixin({InventoryScreen.class, CraftingScreen.class, AbstractFurnaceScreen.class})
public abstract class RecipeButtonMarginMixin extends AbstractContainerScreen<AbstractContainerMenu> {
    private RecipeButtonMarginMixin(AbstractContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @ModifyReturnValue(method = "getRecipeBookButtonPosition", at = @At("RETURN"))
    private ScreenPosition simpleinterfaces$recipeButton(ScreenPosition pos) {
        return StyledScreens.recipeButton(this, pos, this.height, this.imageHeight, this.topPos);
    }
}
