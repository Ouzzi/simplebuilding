package com.simplelib.registry;

import com.simplelib.SimpleLib;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** Loader-neutral registration order: components, blocks, items, block entity (per loader), menus, creative tab. */
public final class LibRegistry {
    public static void tab() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SimpleLib.id("main"),
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                        .title(Component.translatable("itemgroup.simplelib.main"))
                        .icon(() -> new ItemStack(LibItems.IRON_CRUCIBLE))
                        .displayItems((params, out) -> tabStacks().forEach(out::accept))
                        .build());
    }

    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(LibItems.IRON_CRUCIBLE));
        out.add(new ItemStack(LibItems.REINFORCED_CRUCIBLE));
        out.add(new ItemStack(LibItems.NETHERITE_CRUCIBLE));
        out.add(new ItemStack(LibItems.COPPER_BARREL));
        out.add(new ItemStack(LibItems.REINFORCED_BARREL));
        out.add(new ItemStack(LibItems.NETHERITE_BARREL));
        out.add(new ItemStack(LibItems.REINFORCED_CAULDRON));
        return out;
    }

    private LibRegistry() {}
}
