package com.simpleblocks.compat.jei;

import com.simpleblocks.SimpleBlocks;
import com.simpleblocks.saw.Sawing;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;

/**
 * Optional JEI plugin of Simple Blocks: the Shaping Saw category (cuts only the saw has) and the saw as a station of
 * JEI's stonecutting category too (it cuts all of those). The cuts come from the server's list
 * ({@link Sawing.Payload}), which may arrive after JEI started, so they are added as soon as they are there.
 * Only JEI loads it (Fabric entrypoint {@code jei_mod_plugin}, NeoForge annotation); Forge builds without it.
 */
@JeiPlugin
public final class BlocksJeiPlugin implements IModPlugin {
    private static IJeiRuntime runtime;
    private static boolean added;
    private static boolean listening;

    @Override
    public Identifier getPluginUid() {
        return SimpleBlocks.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new SawCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    /** The in-world upgrade (stonecutter + axe or hammer + 4 iron nuggets) as an info page on both blocks. */
    @Override
    public void registerRecipes(mezz.jei.api.registration.IRecipeRegistration registration) {
        registration.addItemStackInfo(java.util.List.of(new net.minecraft.world.item.ItemStack(Sawing.SAW_ITEM),
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONECUTTER)),
                net.minecraft.network.chat.Component.translatable("jei.simpleblocks.info.shaping_saw"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(SawCategory.TYPE, Sawing.SAW_ITEM);
        registration.addCraftingStation(RecipeTypes.STONECUTTING, Sawing.SAW_ITEM);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        added = false;
        if (!listening) {
            listening = true;
            Sawing.CLIENT_LISTENERS.add(BlocksJeiPlugin::addCuts);
        }
        addCuts();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    private static void addCuts() {
        if (runtime == null || added) return;
        var cuts = Sawing.clientEntries().stream().filter(Sawing.Entry::sawOnly).toList();
        if (cuts.isEmpty()) return;
        runtime.getRecipeManager().addRecipes(SawCategory.TYPE, cuts);
        added = true;
    }
}
