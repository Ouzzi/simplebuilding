package com.simplelib.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import net.minecraft.resources.Identifier;

/**
 * Optional JEI plugin of SimpleLib: the shared tab for in-world upgrades ({@link InWorldUpgradeCategory}); the modules
 * add their entries from their own plugins. Only JEI loads it (Fabric: entrypoint {@code jei_mod_plugin}; NeoForge: the
 * annotation); Forge builds without it.
 */
@JeiPlugin
public final class SimpleLibJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath("simplelib", "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new InWorldUpgradeCategory(registration.getJeiHelpers().getGuiHelper()));
    }
}
