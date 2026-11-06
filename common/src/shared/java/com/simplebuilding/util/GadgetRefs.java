package com.simplebuilding.util;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.version.McVersion;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The gadgets that left SimpleBuilding, by id (OW24-2, docs/ai/PLAN-B-GADGETS24-2026-10-24.md): stage G1 moved the Gauge
 * ({@code velocity_gauge}) stage G2 the Transposer ({@code rotator}) and the Resonance Rod ({@code amethyst_lens}) and stage G4 the Echo Sounder ({@code echo_sounder}) to the
 * required, bundled sub-mod Simple Gadgets ({@code simplegadgets:*}, 26.3 only, {@link McVersion#GADGETS_IN_SB}). SimpleBuilding imports none of its classes (principle 3): tabs, the search tab, the
 * tooltips, the HUD stack and the tests find the item in the registry. On 26.2 the same calls give SimpleBuilding's own
 * item. Call after registration; an unregistered id gives air.
 */
public final class GadgetRefs {
    public static final String VELOCITY_GAUGE = "velocity_gauge";
    public static final String ROTATOR = "rotator";
    public static final String AMETHYST_LENS = "amethyst_lens";
    public static final String ECHO_SOUNDER = "echo_sounder";

    private GadgetRefs() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(McVersion.GADGETS_NAMESPACE, path);
    }

    public static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, id(path));
    }

    public static Item item(String path) {
        return BuiltInRegistries.ITEM.getValue(id(path));
    }

    /** The Gauge: SimpleBuilding's own on 26.2, Simple Gadgets' on 26.3. */
    public static Item gauge() {
        return McVersion.GADGETS_IN_SB ? ModItems.VELOCITY_GAUGE : item(VELOCITY_GAUGE);
    }

    /** The Transposer: SimpleBuilding's own on 26.2, Simple Gadgets' on 26.3. */
    public static Item rotator() {
        return McVersion.GADGETS_IN_SB ? ModItems.ROTATOR : item(ROTATOR);
    }

    /** The Resonance Rod: SimpleBuilding's own on 26.2, Simple Gadgets' on 26.3. */
    public static Item rod() {
        return McVersion.GADGETS_IN_SB ? com.simplebuilding.tweaks.item.TweaksItems.LASER_POINTER : item(AMETHYST_LENS);
    }

    /** The Echo Sounder: SimpleBuilding's own on 26.2, Simple Gadgets' on 26.3 (OW24-2 G4). */
    public static Item echo() {
        return McVersion.GADGETS_IN_SB ? com.simplebuilding.tweaks.item.TweaksItems.ECHO_COMPASS : item(ECHO_SOUNDER);
    }

    public static boolean isEcho(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id(ECHO_SOUNDER));
    }

    public static boolean isRod(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id(AMETHYST_LENS));
    }

    public static boolean isRotator(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id(ROTATOR));
    }

    public static boolean isGauge(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id(VELOCITY_GAUGE));
    }
}
