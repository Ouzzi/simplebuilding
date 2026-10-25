package com.simplegadgets;

import com.simplegadgets.gauge.VelocityGaugeItem;
import com.simplegadgets.rod.ResonanceRodItem;
import com.simplegadgets.rotator.RotatorItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Everything Simple Gadgets registers (Fabric: directly in the initializer), in this order: items, creative tab. Ids and
 * properties as in SimpleBuilding before the move.
 */
public final class GadgetsRegistry {
    /** Gauge (Messuhr): speed, altimeter, auto-walk; id {@code velocity_gauge} as in SimpleBuilding. */
    public static Item VELOCITY_GAUGE;
    /** Transposer (Transponierer): turns blocks, swaps blocks; id {@code rotator} as in SimpleBuilding. */
    public static Item ROTATOR;
    /** Resonance Rod (Resonanzstab): beam dot, melts, ignites, measures; id {@code amethyst_lens} as in SimpleBuilding. */
    public static Item AMETHYST_LENS;
    public static CreativeModeTab TAB;

    /** Enchantability of the netherite tier, as SimpleBuilding gave its gadgets. */
    private static final int ENCHANTABILITY = 15;

    private GadgetsRegistry() {}

    public static void registerItems() {
        GadgetComponents.register();
        VELOCITY_GAUGE = item("velocity_gauge", p -> new VelocityGaugeItem(p.stacksTo(1)));
        ROTATOR = item("rotator", p -> new RotatorItem(p.durability(RotatorItem.MAX_CHARGE).stacksTo(1).enchantable(ENCHANTABILITY)));
        AMETHYST_LENS = item("amethyst_lens", p -> new ResonanceRodItem(p.durability(ResonanceRodItem.MAX_CHARGE)));
        com.simplelib.api.GadgetActions.registerItemHint(ROTATOR, context -> ((RotatorItem) ROTATOR).canTransformTarget(context));
        com.simplegadgets.guide.GadgetsGuide.register();
    }

    public static void registerTab() {
        TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SimpleGadgets.id("gadgets"), CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemgroup.simplegadgets.gadgets"))
                .icon(() -> new ItemStack(VELOCITY_GAUGE))
                .displayItems((parameters, output) -> tabStacks().forEach(output::accept))
                .build());
    }

    /** The gadgets in tab order. */
    public static List<Item> gadgets() {
        return VELOCITY_GAUGE == null ? List.of() : List.of(VELOCITY_GAUGE, ROTATOR, AMETHYST_LENS);
    }

    /** Every gadget, then the guide (empty while the module is switched off). */
    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = gadgetStacks();
        if (!out.isEmpty()) out.add(com.simplegadgets.guide.GadgetsGuide.stack());
        return out;
    }

    /** The gadgets only, for Vanilla's tools tab (empty while the module is switched off). */
    public static List<ItemStack> gadgetStacks() {
        List<ItemStack> out = new ArrayList<>();
        if (SimpleGadgets.on()) gadgets().forEach(item -> out.add(new ItemStack(item)));
        return out;
    }

    private static Item item(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleGadgets.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
    }
}
