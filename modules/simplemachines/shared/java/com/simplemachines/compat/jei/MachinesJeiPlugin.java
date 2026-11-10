package com.simplemachines.compat.jei;

import com.simplemachines.SimpleMachines;
import com.simplemachines.smither.Smithers;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.resources.Identifier;

/**
 * Optional JEI plugin of Simple Machines (MACH32-MOD stage 4): the Auto Smither runs Vanilla's smithing recipes, so it is a
 * catalyst of JEI's smithing category next to the smithing table. Only JEI loads it (Fabric: entrypoint {@code jei_mod_plugin}).
 */
@JeiPlugin
public final class MachinesJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return SimpleMachines.id("jei");
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (Smithers.ITEM != null) registration.addCraftingStation(RecipeTypes.SMITHING, Smithers.ITEM);
    }
}
