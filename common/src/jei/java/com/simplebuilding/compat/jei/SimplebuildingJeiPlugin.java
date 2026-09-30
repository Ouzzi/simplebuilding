package com.simplebuilding.compat.jei;

import com.simplebuilding.compat.InWorldRecipeCatalog;
import com.simplebuilding.compat.MobDropCatalog;
import com.simplebuilding.compat.RecipelessJeiInfo;
import com.simplebuilding.recipe.CountBasedSmithingRecipe;
import com.simplebuilding.recipe.UpgradeSmithingRecipe;
import java.util.ArrayList;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import com.simplebuilding.tweaks.item.TweaksJeiInfo;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JEI support: one category per in-world transformation (machine upgrade, reshaping, diamond block,
 * chisel, shears on wool, trim template in an item frame, washing an octant in a cauldron) and the
 * count-based smithing recipes in JEI's smithing category, plus "Mob drops" ({@link MobDropCatalog}:
 * heads from a charged creeper's explosion, music discs from a creeper a skeleton killed).
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
        registration.addRecipeCategories(new MobDropCategory(gui));
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getSmithingCategory().addExtension(CountBasedSmithingRecipe.class, new CountBasedSmithingExtension());
        registration.getSmithingCategory().addExtension(UpgradeSmithingRecipe.class, new TrimUpgradeSmithingExtension());
        // Flypad I: die Elytra muss Reparatur tragen - JEI zeigt sie verzaubert.
        registration.getCraftingCategory().addExtension(com.simplebuilding.recipe.EnchantedShapelessRecipe.class,
                new EnchantedShapelessExtension());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (InWorldRecipeCatalog.Kind kind : InWorldRecipeCatalog.Kind.values()) {
            registration.addRecipes(InWorldCategory.recipeType(kind), catalog().of(kind));
        }
        registration.addRecipes(MobDropCategory.TYPE, MobDropCatalog.drops());
        // Besatz-Aufwertungen am Schmiedetisch mit der Ruestung als Ergebnis (die Platzhalter-Rezepte
        // nennen die Vorlage; sie werden in onRuntimeAvailable ausgeblendet).
        List<RecipeHolder<SmithingRecipe>> trimUpgrades = new ArrayList<>();
        for (UpgradeSmithingRecipe recipe : TrimUpgradeSmithingExtension.displayRecipes()) {
            ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE,
                    Identifier.fromNamespaceAndPath("simplebuilding", "jei/trim_upgrade_" + trimUpgrades.size()));
            trimUpgrades.add(new RecipeHolder<>(key, recipe));
        }
        registration.addRecipes(RecipeTypes.SMITHING, trimUpgrades);
        // Infoseiten der aus Simple Tweaks uebernommenen Pads, Platten und Werkzeuge.
        for (Map.Entry<String, List<ItemLike>> family : TweaksJeiInfo.families().entrySet()) {
            List<ItemStack> stacks = family.getValue().stream().map(ItemStack::new).toList();
            // Dazu, was dieser Server eingestellt hat (Reiter Server & Modpack Tuning), wo der Text Zahlen nennt.
            List<Component> lines = new java.util.ArrayList<>();
            lines.add(Component.translatable(TweaksJeiInfo.KEY_PREFIX + family.getKey()));
            lines.addAll(com.simplebuilding.config.ServerTuningInfo.jeiLines(family.getKey()));
            registration.addItemStackInfo(stacks, lines.toArray(new Component[0]));
        }
        // Infoseiten der Gegenstaende ohne JEI-sichtbares Rezept (Loot, Erzabbau, Altbestand).
        var infoPages = new java.util.LinkedHashMap<>(RecipelessJeiInfo.pages());
        infoPages.putAll(RecipelessJeiInfo.supplementalPages());
        for (Map.Entry<String, List<ItemLike>> page : infoPages.entrySet()) {
            List<ItemStack> stacks = page.getValue().stream().map(ItemStack::new).toList();
            registration.addItemStackInfo(stacks, Component.translatable(RecipelessJeiInfo.KEY_PREFIX + page.getKey()));
        }
    }

    /** Die Platzhalter-Rezepte der Besatz-Aufwertungen ausblenden (Ergebnis dort: die Vorlage). */
    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        IRecipeManager recipes = runtime.getRecipeManager();
        List<RecipeHolder<SmithingRecipe>> dummies = recipes.createRecipeLookup(RecipeTypes.SMITHING).get()
                .filter(holder -> TrimUpgradeSmithingExtension.DUMMIES.contains(holder.id().identifier()))
                .toList();
        if (!dummies.isEmpty()) {
            recipes.hideRecipes(RecipeTypes.SMITHING, dummies);
        }
        // Das Admin-Buch stellen nur Operatoren her (GuideBooks.operatorOnly): alle anderen sehen sein Rezept nicht.
        if (!com.simplebuilding.guide.GuideBooks.isOperator(net.minecraft.client.Minecraft.getInstance().player)) {
            List<RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>> operatorOnly = recipes.createRecipeLookup(RecipeTypes.CRAFTING).get()
                    .filter(holder -> com.simplebuilding.guide.GuideBooks.isOperatorOnlyRecipe(holder.id().identifier()))
                    .toList();
            if (!operatorOnly.isEmpty()) {
                recipes.hideRecipes(RecipeTypes.CRAFTING, operatorOnly);
            }
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(MobDropCategory.TYPE, net.minecraft.world.item.Items.CREEPER_SPAWN_EGG);
        for (InWorldRecipeCatalog.Kind kind : InWorldRecipeCatalog.Kind.values()) {
            for (Item tool : catalog().toolsOf(kind)) {
                registration.addCraftingStation(InWorldCategory.recipeType(kind), tool);
            }
        }
    }
}
