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
    /** Rezeptbuch-Kategorien (N16): Spitze, Schaft, Befiederung, je ein Reiter; IDs {@link #categoryId}. */
    public static RecipeBookCategory TIP_CATEGORY;
    public static RecipeBookCategory SHAFT_CATEGORY;
    public static RecipeBookCategory FLETCHING_CATEGORY;

    private FletchingRecipes() {
    }

    /** Registry-Pfad der Buch-Kategorie einer Teil-Art, z. B. {@code fletching_tip}. */
    public static String categoryId(FletchingRecipe.Kind kind) {
        return ID + "_" + kind.getSerializedName();
    }

    public static RecipeBookCategory category(FletchingRecipe.Kind kind) {
        return switch (kind) {
            case TIP -> TIP_CATEGORY;
            case SHAFT -> SHAFT_CATEGORY;
            case FLETCHING -> FLETCHING_CATEGORY;
        };
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
     * Schaltet dem Spieler alle Befiederungsrezepte frei (beim Oeffnen des Tisches): das Rezeptbuch zeigt je Kategorie
     * alle Teile, fehlende rot hinterlegt. Bereits bekannte bleiben unveraendert.
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
