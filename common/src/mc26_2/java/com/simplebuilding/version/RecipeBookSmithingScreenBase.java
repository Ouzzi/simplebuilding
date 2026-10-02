package com.simplebuilding.version;

import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SmithingMenu;

/** 26.2: kein {@code isInputCaptured} - Tastendruecke faengt das Rezeptbuch ueber keyPressed ab. Zwilling in mc26_3/overlay. */
public abstract class RecipeBookSmithingScreenBase extends SmithingScreen {
    protected RecipeBookSmithingScreenBase(SmithingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    /** Das Rezeptbuch des Bildschirms. */
    protected abstract net.minecraft.client.gui.screens.recipebook.RecipeBookComponent<?> recipeBook();
}
