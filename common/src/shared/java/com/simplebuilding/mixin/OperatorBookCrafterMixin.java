package com.simplebuilding.mixin;

import com.simplebuilding.guide.GuideBooks;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.CrafterBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Der Crafter hat keinen Spieler, also auch keine Operatorrechte: nur-Operator-Buecher stellt er nie her. */
@Mixin(CrafterBlock.class)
public abstract class OperatorBookCrafterMixin {

    @Inject(method = "getPotentialResults", at = @At("RETURN"), cancellable = true)
    private static void simplebuilding$noAdminBookFromCrafters(ServerLevel level, CraftingInput input,
                                                               CallbackInfoReturnable<Optional<RecipeHolder<CraftingRecipe>>> cir) {
        Optional<RecipeHolder<CraftingRecipe>> result = cir.getReturnValue();
        if (result.isPresent() && GuideBooks.isOperatorOnlyRecipe(result.get().id().identifier())) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
