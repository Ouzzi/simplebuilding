package com.simplelib.compat.jei;

import com.simplelib.api.InWorldUpgrades;
import com.simplelib.api.RecipeViewer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Optional JEI plugin of SimpleLib: the shared tabs "In-World Upgrades", "Special Ways" and "Mob Drops"
 * ({@link InWorldUpgradeCategory}, {@link MobDropCategory}) with everything the modules announced through
 * {@link RecipeViewer} (JEI24) and the in-world upgrade table. The table can reach the client after JEI started; then
 * it is added once it arrives ({@link InWorldUpgrades#onApply}). Only JEI loads it (Fabric: entrypoint
 * {@code jei_mod_plugin}; NeoForge: the annotation); Forge builds without it.
 */
@JeiPlugin
public final class SimpleLibJeiPlugin implements IModPlugin {
    private static volatile IJeiRuntime runtime;
    private static volatile boolean tableAdded;
    private static boolean listening;

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath("simplelib", "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new InWorldUpgradeCategory(gui),
                new InWorldUpgradeCategory(gui, InWorldUpgradeCategory.SPECIAL, Items.CRAFTER),
                new MobDropCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(InWorldUpgradeCategory.TYPE, RecipeViewer.inWorld());
        tableAdded = false;
        if (InWorldUpgrades.isLoaded()) {
            registration.addRecipes(InWorldUpgradeCategory.TYPE, RecipeViewer.tableEntries());
            tableAdded = true;
        }
        registration.addRecipes(InWorldUpgradeCategory.SPECIAL, RecipeViewer.special());
        registration.addRecipes(MobDropCategory.TYPE, RecipeViewer.mobDrops());
        for (RecipeViewer.Info page : RecipeViewer.info()) {
            if (!page.items().isEmpty()) registration.addItemStackInfo(page.items(), page.lines().toArray(new net.minecraft.network.chat.Component[0]));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        catalysts(registration, InWorldUpgradeCategory.TYPE, RecipeViewer.inWorld());
        if (InWorldUpgrades.isLoaded()) catalysts(registration, InWorldUpgradeCategory.TYPE, RecipeViewer.tableEntries());
        catalysts(registration, InWorldUpgradeCategory.SPECIAL, RecipeViewer.special());
    }

    private static void catalysts(IRecipeCatalystRegistration registration, IRecipeType<InWorldUpgrade> type, List<InWorldUpgrade> entries) {
        Set<Item> tools = new LinkedHashSet<>();
        entries.forEach(entry -> tools.addAll(entry.tools()));
        tools.forEach(tool -> registration.addCraftingStation(type, tool));
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        if (!listening) {
            listening = true;
            InWorldUpgrades.onApply(() -> net.minecraft.client.Minecraft.getInstance().execute(SimpleLibJeiPlugin::addTableLate));
        }
        addTableLate();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    private static void addTableLate() {
        IJeiRuntime current = runtime;
        if (current == null || tableAdded || !InWorldUpgrades.isLoaded()) return;
        List<InWorldUpgrade> entries = RecipeViewer.tableEntries();
        if (entries.isEmpty()) return;
        current.getRecipeManager().addRecipes(InWorldUpgradeCategory.TYPE, entries);
        tableAdded = true;
    }
}
