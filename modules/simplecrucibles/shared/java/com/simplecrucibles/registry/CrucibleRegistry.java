package com.simplecrucibles.registry;

import com.simplecrucibles.SimpleCrucibles;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** Loader-neutral registration order: components, blocks, items, block entity (per loader), menus, creative tab. */
public final class CrucibleRegistry {
    public static void tab() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SimpleCrucibles.id("main"),
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                        .title(Component.translatable("itemgroup.simplecrucibles.main"))
                        .icon(() -> new ItemStack(CrucibleItems.IRON_CRUCIBLE))
                        .displayItems((params, out) -> {
                            tabStacks().forEach(out::accept);
                            bucketStacks().forEach(out::accept);
                            if (SimpleCrucibles.on()) out.accept(com.simplecrucibles.guide.CruciblesGuide.stack());
                        })
                        .build());
    }

    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(CrucibleItems.IRON_CRUCIBLE));
        out.add(new ItemStack(CrucibleItems.REINFORCED_CRUCIBLE));
        out.add(new ItemStack(CrucibleItems.NETHERITE_CRUCIBLE));
        out.add(new ItemStack(CrucibleItems.ENDERITE_CRUCIBLE));
        out.add(new ItemStack(CrucibleItems.COPPER_BARREL));
        out.add(new ItemStack(CrucibleItems.REINFORCED_BARREL));
        out.add(new ItemStack(CrucibleItems.NETHERITE_BARREL));
        out.add(new ItemStack(CrucibleItems.ENDERITE_BARREL));
        out.add(new ItemStack(CrucibleItems.REINFORCED_CAULDRON));
        return out;
    }

    /**
     * The buckets a creative player picks (split stage 5b): copper, soul lava, Enderite, milk, ceramic - without the worn
     * ceramic stages and the full Enderite buckets (only reached by use). Vanilla's tools tab gets them after the powder
     * snow bucket (per-mod switch); SimpleBuilding's functional tab lists them by id in its own row layout.
     */
    public static List<ItemStack> bucketStacks() {
        List<ItemStack> out = new ArrayList<>();
        com.simplecrucibles.fluid.CrucibleFluids.creativeBuckets().forEach(item -> out.add(new ItemStack(item)));
        return out;
    }

    /** SimpleBuilding's own functional tab: the crucibles etc. also belong there when that mod is installed. */
    public static final net.minecraft.resources.Identifier SB_FUNCTIONAL_TAB =
            net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", "functional");

    /** Whether {@code tab} should receive {@link #tabStacks()}: Vanilla functional blocks (if allowed) or SimpleBuilding's. */
    public static boolean wantsStacks(net.minecraft.resources.ResourceKey<CreativeModeTab> tab, boolean vanillaAllowed) {
        if (tab.identifier().equals(SB_FUNCTIONAL_TAB)) return true;
        return vanillaAllowed && tab.equals(net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS);
    }

    private CrucibleRegistry() {}
}
