package com.simplebuilding.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Fletching Table's arrows of the required, bundled sub-mod Simple Arrows by id (DW04C-4 / b-arrows03 04.11., like
 * {@link EnchantingRefs}): SimpleBuilding imports none of its classes (principle 3). The test centre takes the arrows, with
 * their parts, from the module's one tab {@code simplearrows:arrows}; without the module (NeoForge/Forge 26.3, 26.2) there are none.
 */
public final class ArrowRefs {
    public static final String NAMESPACE = "simplearrows";
    public static final Identifier TAB = Identifier.fromNamespaceAndPath(NAMESPACE, "arrows");

    private ArrowRefs() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NAMESPACE, path);
    }

    /** The module's item, air when it is not registered. */
    public static Item item(String path) {
        return BuiltInRegistries.ITEM.getValue(id(path));
    }

    /** The crafted arrow item ({@code simplearrows:crafted_arrow}), air without Simple Arrows. */
    public static Item craftedArrow() {
        return item("crafted_arrow");
    }

    /** The module tab's stacks (built for {@code lookup}), empty without Simple Arrows. */
    public static List<ItemStack> tabStacks(HolderLookup.Provider lookup) {
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(TAB);
        if (tab == null) return List.of();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.VANILLA_SET, false, lookup));
        return List.copyOf(tab.getDisplayItems());
    }

    /** Every Fletching Table arrow of this installation (one per part combination, tip by tip), {@code count} each. */
    public static List<ItemStack> craftedArrows(HolderLookup.Provider lookup, int count) {
        Item arrow = craftedArrow();
        List<ItemStack> out = new ArrayList<>();
        if (arrow == Items.AIR) return out;
        for (ItemStack stack : tabStacks(lookup)) {
            if (stack.is(arrow)) out.add(stack.copyWithCount(count));
        }
        return out;
    }
}
