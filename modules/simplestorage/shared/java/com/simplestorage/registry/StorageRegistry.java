package com.simplestorage.registry;

import com.simplestorage.SimpleStorage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Loader-neutral registration order: blocks, items, block entities (per loader), menus, creative tab. */
public final class StorageRegistry {
    public static CreativeModeTab TAB;

    public static void registerTab() {
        TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SimpleStorage.id("storage"), CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemgroup.simplestorage.storage"))
                .icon(() -> new ItemStack(StorageItems.CRATES.get(com.simplestorage.crate.CrateWood.OAK)))
                .displayItems((parameters, output) -> tabStacks().forEach(output::accept))
                .build());
    }

    /** The table, the crates, the guide (empty while the module is switched off: Vanilla hides an empty tab). */
    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = new ArrayList<>();
        if (!SimpleStorage.on()) return out;
        out.add(new ItemStack(StorageItems.STORAGE_CRAFTING_TABLE));
        out.addAll(crateStacks());
        out.add(com.simplestorage.guide.StorageGuide.stack());
        return out;
    }

    public static List<ItemStack> crateStacks() {
        List<ItemStack> out = new ArrayList<>();
        for (Item crate : StorageItems.CRATES.values()) out.add(new ItemStack(crate));
        return out;
    }

    /**
     * Whether the module fills Vanilla's tabs itself: only on its own (SimpleBuilding's search-tab chains place the
     * crates behind each wood and the table behind the crafting table by id), only while switched on and allowed.
     */
    public static boolean fillsVanillaTabs(boolean simpleBuildingLoaded, boolean vanillaTabsAllowed) {
        return !simpleBuildingLoaded && vanillaTabsAllowed && SimpleStorage.on();
    }

    private StorageRegistry() {}
}
