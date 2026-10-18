package com.simplehammers.compat.jei;

import com.simplehammers.SimpleHammers;
import com.simplehammers.registry.HammerRegistry;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Optional JEI plugin of Simple Hammers: info pages for what no recipe shows - the area break, reshaping and carving,
 * crushing diamond and quartz blocks and the in-world upgrades. With SimpleBuilding its JEI category lists every
 * in-world way of the hammer in detail. Only JEI loads it (Fabric: entrypoint {@code jei_mod_plugin}; NeoForge: the
 * annotation); Forge builds without it.
 */
@JeiPlugin
public final class HammersJeiPlugin implements IModPlugin {
    public static final String INFO_PREFIX = "jei.simplehammers.info.";

    @Override
    public Identifier getPluginUid() {
        return SimpleHammers.id("jei");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<ItemStack> hammers = new ArrayList<>();
        HammerRegistry.HAMMERS.values().forEach(item -> hammers.add(new ItemStack(item)));
        if (hammers.isEmpty()) return;
        registration.addItemStackInfo(hammers, Component.translatable(INFO_PREFIX + "area"),
                Component.translatable(INFO_PREFIX + "reshape"), Component.translatable(INFO_PREFIX + "crush"),
                Component.translatable(INFO_PREFIX + "upgrade"));
    }
}
