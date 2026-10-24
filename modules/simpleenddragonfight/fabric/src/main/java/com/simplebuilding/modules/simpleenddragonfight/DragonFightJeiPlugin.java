package com.simplebuilding.modules.simpleenddragonfight;

import java.util.List;
import java.util.Map;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Optional JEI plugin of Simple End Dragon Fight (JEI24, Fabric entrypoint {@code jei_mod_plugin}): {@link DragonFightJeiInfo}. */
public final class DragonFightJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(DragonFightRegistry.ID, "jei");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (Map.Entry<String, List<Item>> page : DragonFightJeiInfo.pages().entrySet()) {
            registration.addItemStackInfo(page.getValue().stream().map(ItemStack::new).toList(), Component.translatable(page.getKey()));
        }
    }
}
