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
 * Was die oberen Ofenstufen ueber die Geschwindigkeit hinaus belohnt. Wird aus
 * {@code setRecipeUsed} der drei Block-Entities gerufen - einmal je fertig geschmolzenem Gegenstand,
 * direkt nachdem Vanillas {@code serverTick} das Ergebnis in den Ausgabeslot gelegt hat.
 *
 * <ul>
 *   <li><b>Doppelte Erfahrung</b> in jedem Netherit- und Enderit-Ofen, -Raeucherofen und
 *       -Schmelzofen: das Rezept wird zweimal gezaehlt, die Erfahrung beim Herausnehmen verdoppelt
 *       sich damit genau.</li>
 *   <li><b>Mehr Ausbeute</b> im Netherit-Schmelzofen (jeder {@value #NETHERITE_BONUS_PERIOD}. Schmelzvorgang
 *       +1, also +25 %) und im Enderit-Schmelzofen (jeder {@value #ENDERITE_BONUS_PERIOD}., +50 %),
 *       aber nur fuer Rezepte, deren Zutat ausschliesslich aus {@code simplebuilding:blast_furnace_bonus}
 *       besteht - den Rohmetallen.</li>
 * </ul>
 * Beides entfaellt fuer Zutaten in {@code simplebuilding:furnace_bonus_excluded}: der rissige Diamant
 * liesse sich sonst verlustfrei im Kreis fuehren (Diamantblock -&gt; 81 Diamantsplitter -&gt; 9 rissige
 * Diamanten -&gt; 9 Diamanten), und jeder Bonus darauf waere eine Endlosquelle.
 */
public final class FurnaceTierPerks {

    public static final int NETHERITE_BONUS_PERIOD = 4;
    public static final int ENDERITE_BONUS_PERIOD = 2;
    /** Wo der Schmelzofen mitzaehlt, wie weit er bis zum naechsten Bonus ist. */
    public static final String BONUS_PROGRESS_KEY = "simplebuilding:bonus_progress";

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

    /** Jeder wievielte passende Schmelzvorgang einen Gegenstand mehr bringt; 0 = gar keiner. */
    public static int bonusPeriod(BlockState state) {
        if (state.is(ModBlocks.ENDERITE_BLAST_FURNACE)) {
            return ENDERITE_BONUS_PERIOD;
        }
        if (state.is(ModBlocks.NETHERITE_BLAST_FURNACE)) {
            return NETHERITE_BONUS_PERIOD;
        }
        return 0;
    }

    /** Ob die Zutat des Rezepts nur aus Bonus-Gegenstaenden besteht (und keiner ausgeschlossen ist). */
    @SuppressWarnings("deprecation") // Ingredient#items(): der einzige Blick auf die Zutaten
    public static boolean earnsOutputBonus(@Nullable RecipeHolder<?> recipe) {
        if (recipe == null || !(recipe.value() instanceof AbstractCookingRecipe cooking)) {
            return false;
        }
        var items = cooking.input().items().toList();
        return !items.isEmpty()
                && items.stream().allMatch(holder -> holder.is(ModTags.Items.BLAST_FURNACE_BONUS))
                && !inputTouches(recipe, ModTags.Items.FURNACE_BONUS_EXCLUDED);
    }

    @SuppressWarnings("deprecation")
    private static boolean inputTouches(RecipeHolder<?> recipe, TagKey<Item> tag) {
        if (!(recipe.value() instanceof AbstractCookingRecipe cooking)) {
            return false;
        }
        return cooking.input().items().anyMatch((Holder<Item> holder) -> holder.is(tag));
    }
}
