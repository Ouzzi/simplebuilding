package com.simplebuilding.fletching;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Rezepttyp und Rezeptbuch-Kategorie des Befiederungstischs. Jeder Loader registriert sie unter
 * {@code simplebuilding:fletching} (nur mit {@code McVersion.FLETCHING}) und setzt die Felder.
 */
public final class FletchingRecipes {
    public static final String ID = "fletching";

    public static RecipeType<FletchingRecipe> TYPE;
    public static RecipeBookCategory CATEGORY;

    private FletchingRecipes() {
    }

    /** Der Rezepttyp, wie ihn Vanilla-Typen bauen (toString = ID). */
    public static RecipeType<FletchingRecipe> newType() {
        return new RecipeType<>() {
            @Override
            public String toString() {
                return ID;
            }
        };
    }

    /** Alle geladenen Befiederungsrezepte. */
    public static List<RecipeHolder<?>> all(ServerPlayer player) {
        List<RecipeHolder<?>> out = new ArrayList<>();
        if (TYPE == null) {
            return out;
        }
        for (RecipeHolder<?> holder : player.level().getServer().getRecipeManager().getRecipes()) {
            if (holder.value().getType() == TYPE) {
                out.add(holder);
            }
        }
        return out;
    }

    /**
     * Schaltet dem Spieler alle Befiederungsrezepte frei (beim Oeffnen des Tisches): das Rezeptbuch zeigt wie das
     * fruehere Material-Panel alle Kombinationen, nicht herstellbare rot hinterlegt. Bereits bekannte bleiben unveraendert.
     */
    public static void unlockAll(ServerPlayer player) {
        List<RecipeHolder<?>> missing = new ArrayList<>();
        for (RecipeHolder<?> holder : all(player)) {
            if (!player.getRecipeBook().contains(holder.id())) {
                missing.add(holder);
            }
        }
        if (!missing.isEmpty()) {
            player.awardRecipes(missing);
        }
    }
}
