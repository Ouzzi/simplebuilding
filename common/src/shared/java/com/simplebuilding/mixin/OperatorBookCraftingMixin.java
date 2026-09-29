package com.simplebuilding.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simplebuilding.guide.GuideBooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Das Admin-Buch koennen nur Operatoren herstellen ({@link GuideBooks#operatorOnly}): fuer alle
 * anderen bleibt das Ergebnisfeld leer, als gaebe es das Rezept nicht. Greift in der gemeinsamen
 * Ergebnisberechnung von Werkbank und Inventar-Raster ({@code CraftingMenu#slotChangedCraftingGrid}),
 * also auch, wenn das Rezeptbuch die Zutaten einlegt.
 */
@Mixin(CraftingMenu.class)
public abstract class OperatorBookCraftingMixin {

    @WrapOperation(method = "slotChangedCraftingGrid", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/ResultContainer;setRecipeUsed(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/crafting/RecipeHolder;)Z"))
    private static boolean simplebuilding$onlyOperatorsCraftTheAdminBook(ResultContainer container, ServerPlayer player, RecipeHolder<?> recipe,
                                                                         Operation<Boolean> original) {
        if (!GuideBooks.mayCraft(player, recipe)) {
            return false;
        }
        return original.call(container, player, recipe);
    }
}
