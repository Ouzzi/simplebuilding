package com.simplebuilding.version;

import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SmithingMenu;

/** 26.3: Bildschirme melden belegte Texteingaben ({@code isInputCaptured}); das Rezeptbuch-Suchfeld gehoert dazu. */
public abstract class RecipeBookSmithingScreenBase extends SmithingScreen {
    protected RecipeBookSmithingScreenBase(SmithingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    /** Das Rezeptbuch des Bildschirms. */
    protected abstract net.minecraft.client.gui.screens.recipebook.RecipeBookComponent<?> recipeBook();

    @Override
    public boolean isInputCaptured() {
        return super.isInputCaptured() || recipeBook().capturesInput();
    }
}
