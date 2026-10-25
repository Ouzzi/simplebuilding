package com.simplegadgets;

import com.simplegadgets.rod.LensMeasurement;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * The data components of Simple Gadgets. {@code lens_measurement} (the Resonance Rod's last measurement) moved from SimpleBuilding's
 * {@code simplebuilding:lens_measurement}; SimpleLib's id move lets old stacks keep it. Registered once from the loader's entry.
 */
public final class GadgetComponents {
    /** Resonance Rod with Constructor's Touch: the last measured distance (tooltip). */
    public static DataComponentType<LensMeasurement> LENS_MEASUREMENT;

    private GadgetComponents() {}

    public static void register() {
        if (LENS_MEASUREMENT != null) return;
        LENS_MEASUREMENT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SimpleGadgets.id("lens_measurement"),
                DataComponentType.<LensMeasurement>builder()
                        .persistent(LensMeasurement.CODEC)
                        .networkSynchronized(LensMeasurement.STREAM_CODEC)
                        // Written every second while beaming; without this Vanilla lowered the rod in the hand and raised it each
                        // time (owner 2026-09-29: "the item must not move"). Like DAMAGE.
                        .ignoreSwapAnimation()
                        .build());
    }
}
