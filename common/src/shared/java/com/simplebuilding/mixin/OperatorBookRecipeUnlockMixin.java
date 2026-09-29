package com.simplebuilding.mixin;

import com.simplebuilding.guide.GuideBooks;
import java.util.Collection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Nur Operatoren bekommen das Rezept des Admin-Buchs ins Rezeptbuch: jede Freischaltung
 * (Advancement-Belohnung, {@code /recipe give}, Beitritt) laeuft ueber {@code awardRecipes}, und dort
 * faellt es fuer alle anderen heraus. Den Abgleich nach {@code /op}/{@code /deop} macht der Beitritt
 * ({@link GuideBooks#syncOperatorRecipes}).
 */
@Mixin(ServerPlayer.class)
public abstract class OperatorBookRecipeUnlockMixin {

    @ModifyVariable(method = "awardRecipes", at = @At("HEAD"), argsOnly = true)
    private Collection<RecipeHolder<?>> simplebuilding$hideAdminBookRecipe(Collection<RecipeHolder<?>> recipes) {
        return GuideBooks.filterUnlocks((ServerPlayer) (Object) this, recipes);
    }
}
