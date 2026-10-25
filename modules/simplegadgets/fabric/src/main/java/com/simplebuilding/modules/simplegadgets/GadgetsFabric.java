package com.simplebuilding.modules.simplegadgets;

import com.simplegadgets.GadgetsRegistry;
import com.simplegadgets.SimpleGadgets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class GadgetsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleGadgets.init(FabricLoader.getInstance().getConfigDir());
        GadgetsRegistry.registerItems();
        GadgetsRegistry.registerTab();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleGadgets.on()) return;
            // With SimpleBuilding its search tab chain puts them into its gadget row (by id); alone behind the recovery compass.
            if (!FabricLoader.getInstance().isModLoaded(SimpleGadgets.OLD_NAMESPACE)
                    && com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FabricLoader.getInstance().getConfigDir(), SimpleGadgets.MOD_ID)) {
                var stacks = GadgetsRegistry.gadgetStacks().stream().filter(s -> out.getDisplayStacks().stream().noneMatch(d -> ItemStack.isSameItemSameComponents(d, s))).toList();
                if (!stacks.isEmpty()) {
                    if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.RECOVERY_COMPASS))) out.insertAfter(Items.RECOVERY_COMPASS, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                    else stacks.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
                }
            }
            var book = java.util.List.of(com.simplegadgets.guide.GadgetsGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> com.simplegadgets.rotator.RotatorItem.onDisconnect(handler.player));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> com.simplegadgets.rotator.RotatorItem.clearAll());
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simplegadgets.guide.GadgetsGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }
}
