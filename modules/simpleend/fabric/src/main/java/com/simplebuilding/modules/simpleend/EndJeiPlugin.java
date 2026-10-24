package com.simplebuilding.modules.simpleend;

import java.util.List;
import java.util.Map;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Optional JEI plugin of Simple End (JEI24, Fabric entrypoint {@code jei_mod_plugin}): the mob drop pages of {@link EndJeiInfo}. */
public final class EndJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath("simpleend", "jei");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (Map.Entry<String, List<Item>> page : EndJeiInfo.pages().entrySet()) {
            registration.addItemStackInfo(page.getValue().stream().map(ItemStack::new).toList(), Component.translatable(page.getKey()));
        }
    }
}
