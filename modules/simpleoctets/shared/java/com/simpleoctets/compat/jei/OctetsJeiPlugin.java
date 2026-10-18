package com.simpleoctets.compat.jei;

import com.simpleoctets.SimpleOctets;
import com.simpleoctets.registry.OctetRegistry;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Optional JEI plugin of Simple Octets: info pages for what the saw/stonecutter recipes do not show - how octets are placed
 * and mixed, and the melon slice that becomes a melon octet. Only JEI loads it (Fabric: entrypoint {@code jei_mod_plugin};
 * NeoForge: the annotation); Forge builds without it.
 */
@JeiPlugin
public final class OctetsJeiPlugin implements IModPlugin {
    public static final String INFO_PREFIX = "jei.simpleoctets.info.";

    @Override
    public Identifier getPluginUid() {
        return SimpleOctets.id("jei");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<ItemStack> octets = new ArrayList<>();
        OctetRegistry.WOOD_OCTET_ITEMS.forEach(item -> octets.add(new ItemStack(item)));
        OctetRegistry.MATERIAL_OCTET_ITEMS.forEach(item -> octets.add(new ItemStack(item)));
        if (!octets.isEmpty()) registration.addItemStackInfo(octets, Component.translatable(INFO_PREFIX + "octets"));
        if (OctetRegistry.MELON_OCTET != null) {
            registration.addItemStackInfo(List.of(new ItemStack(Items.MELON_SLICE)), Component.translatable(INFO_PREFIX + "melon_octet"));
        }
    }
}
