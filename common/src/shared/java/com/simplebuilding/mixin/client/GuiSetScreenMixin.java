package com.simplebuilding.mixin.client;

import com.simplebuilding.client.gui.RecipeBookSmithingScreen;
import com.simplebuilding.version.McVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Tauscht den Vanilla-Schmiedebildschirm gegen {@link RecipeBookSmithingScreen} (gleiches Menue, plus Rezeptbuch).
 * Nur bei genau {@link SmithingScreen}: Unterklassen anderer Mods bleiben unberuehrt. Ein Eingriff hier statt in
 * {@code MenuScreens} funktioniert auf allen Loadern gleich.
 */
@Mixin(Gui.class)
public abstract class GuiSetScreenMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen simplebuilding$smithingRecipeBook(Screen screen) {
        if (McVersion.SMITHING_RECIPE_BOOK && screen != null && screen.getClass() == SmithingScreen.class
                && Minecraft.getInstance().player != null) {
            SmithingScreen smithing = (SmithingScreen) screen;
            return new RecipeBookSmithingScreen(smithing.getMenu(), Minecraft.getInstance().player.getInventory(), smithing.getTitle());
        }
        return screen;
    }
}
