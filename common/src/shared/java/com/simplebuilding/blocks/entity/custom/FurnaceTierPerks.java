package com.simplebuilding.blocks.entity.custom;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.util.ModTags;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Upper-tier furnaces count eligible recipes twice to pay double experience.
 * Output counts always come from the recipe. Cracked diamonds are excluded from
 * extra experience because their lossless crafting cycle would create an XP farm.
 */
public final class FurnaceTierPerks {

    private FurnaceTierPerks() {
    }

    /** Netherit- oder Enderit-Stufe einer der drei Familien. */
    public static boolean isUpperTier(BlockState state) {
        return state.is(ModBlocks.NETHERITE_FURNACE) || state.is(ModBlocks.ENDERITE_FURNACE)
                || state.is(ModBlocks.NETHERITE_SMOKER) || state.is(ModBlocks.ENDERITE_SMOKER)
                || state.is(ModBlocks.NETHERITE_BLAST_FURNACE) || state.is(ModBlocks.ENDERITE_BLAST_FURNACE);
    }

    /** Ob dieser Schmelzvorgang doppelt zaehlt. */
    public static boolean doublesExperience(BlockState state, @Nullable RecipeHolder<?> recipe) {
        return recipe != null && isUpperTier(state) && !inputTouches(recipe, ModTags.Items.FURNACE_BONUS_EXCLUDED);
    }

    /**
     * Zusaetzliche Kochticks je Spieltick: Verstaerkt +1, Netherit +3, Enderit +7 - also die zwei-,
     * vier- und achtfache Geschwindigkeit der Vanilla-Maschine derselben Familie (Ofen, Raeucherofen,
     * Schmelzofen), ohne Brennstoffkosten. 0 fuer jeden anderen Block.
     *
     * <p>Dieselben Zahlen, die die {@code tick}-Methoden der drei Block-Entities selbst tragen (dort
     * bleiben sie stehen, die Mutationskataloge haengen an genau diesen Zeilen); gelesen nur fuer die
     * Anzeige ({@code compat.BlockInfo}, Jade). {@code FurnaceTests} prueft das Tempo im Spiel,
     * {@code BlockInfoTests#furnaceSpeedFollowsTheTier} diese Tabelle.
     */
    public static int extraCookTicks(BlockState state) {
        if (state.is(ModBlocks.REINFORCED_FURNACE) || state.is(ModBlocks.REINFORCED_SMOKER) || state.is(ModBlocks.REINFORCED_BLAST_FURNACE)) {
            return 1;
        }
        if (state.is(ModBlocks.NETHERITE_FURNACE) || state.is(ModBlocks.NETHERITE_SMOKER) || state.is(ModBlocks.NETHERITE_BLAST_FURNACE)) {
            return 3;
        }
        if (state.is(ModBlocks.ENDERITE_FURNACE) || state.is(ModBlocks.ENDERITE_SMOKER) || state.is(ModBlocks.ENDERITE_BLAST_FURNACE)) {
            return 7;
        }
        return 0;
    }

    /** Geschwindigkeit gegenueber der Vanilla-Maschine (2, 4, 8); 1 fuer jeden anderen Block. */
    public static int speedFactor(BlockState state) {
        return 1 + extraCookTicks(state);
    }

    @SuppressWarnings("deprecation")
    private static boolean inputTouches(RecipeHolder<?> recipe, TagKey<Item> tag) {
        if (!(recipe.value() instanceof AbstractCookingRecipe cooking)) {
            return false;
        }
        return cooking.input().items().anyMatch((Holder<Item> holder) -> holder.is(tag));
    }
}
