package com.simplebuilding.tweaks.mixin;

import com.simplebuilding.tweaks.easter.EasterEggs;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Die Easter-Kette ({@code com.simplebuilding.tweaks.easter.EasterEggs}) kostet je Schritt genau die
 * Zutaten der normalen Aufwertung - ein Easter-Pad passt damit auch in das normale Rezept, und welches
 * von beiden der Schmiedetisch nimmt, haengt an der Rezeptreihenfolge. Darum nimmt kein
 * Umwandlungsrezept eine Basis mit Easter-Stufe: die Kette laeuft allein ueber
 * {@code EasterSmithingRecipe}, ein Easter-Pad wird nie still zu einem normalen Pad mit falschem Namen.
 *
 * <p>{@link SmithingTransformRecipe} erbt {@code matches} als Default von {@link SmithingRecipe} (auf
 * 1.21.11, 26.2 und 26.3); dieses Mixin fuegt der Klasse eine eigene Fassung hinzu, die bis auf die
 * Easter-Pruefung dem Default entspricht.
 */
@Mixin(SmithingTransformRecipe.class)
public abstract class SmithingTransformEasterGuardMixin implements SmithingRecipe {

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        if (EasterEggs.stageOf(input.base()) > 0) {
            return false;
        }
        return Ingredient.testOptionalIngredient(this.templateIngredient(), input.template())
                && this.baseIngredient().test(input.base())
                && Ingredient.testOptionalIngredient(this.additionIngredient(), input.addition());
    }
}
