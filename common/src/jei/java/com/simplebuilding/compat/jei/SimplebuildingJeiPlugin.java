package com.simplebuilding.compat.jei;

import com.simplebuilding.compat.InWorldRecipeCatalog;
import com.simplebuilding.recipe.CountBasedSmithingRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JEI support: one category per in-world transformation (machine upgrade, reshaping, diamond block,
 * chisel, shears on wool, trim template in an item frame, washing an octant in a cauldron) and the
 * count-based smithing recipes in JEI's smithing category.
 *
 * <p><b>Only loaded by JEI.</b> NeoForge finds this class through the {@link JeiPlugin} annotation,
 * Fabric through the {@code jei_mod_plugin} entrypoint in {@code fabric.mod.json}; nothing in the mod
 * references it, so without JEI it is never loaded and the JEI API (compileOnly) is never needed.
 *
 * <p>All data comes from {@link InWorldRecipeCatalog}, which reads the same export as the wiki.
 */
@JeiPlugin
public final class SimplebuildingJeiPlugin implements IModPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("simplebuilding/jei");
    private static final Identifier UID = Identifier.fromNamespaceAndPath("simplebuilding", "jei_plugin");

    private InWorldRecipeCatalog.Catalog catalog;

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    private InWorldRecipeCatalog.Catalog catalog() {
        if (catalog == null) {
            catalog = InWorldRecipeCatalog.build();
            if (!catalog.problems().isEmpty()) {
                LOGGER.warn("In-world JEI catalog skipped {} entries: {}", catalog.problems().size(), catalog.problems());
            }
        }
        return catalog;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        // JEI registers again on every world join and reload; build the catalog afresh each time.
        catalog = null;
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        for (InWorldRecipeCatalog.Kind kind : InWorldRecipeCatalog.Kind.values()) {
            registration.addRecipeCategories(new InWorldCategory(kind, gui, catalog().of(kind)));
        }
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getSmithingCategory().addExtension(CountBasedSmithingRecipe.class, new CountBasedSmithingExtension());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (InWorldRecipeCatalog.Kind kind : InWorldRecipeCatalog.Kind.values()) {
            registration.addRecipes(InWorldCategory.recipeType(kind), catalog().of(kind));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (InWorldRecipeCatalog.Kind kind : InWorldRecipeCatalog.Kind.values()) {
            for (Item tool : catalog().toolsOf(kind)) {
                registration.addCraftingStation(InWorldCategory.recipeType(kind), tool);
            }
        }
    }
}
