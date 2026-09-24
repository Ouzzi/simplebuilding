package com.simplebuilding.client.gui.tooltip;

import com.simplebuilding.items.tooltip.ReinforcedBundleTooltipData;
import com.simplebuilding.util.BundleTooltipAccessor;
import com.simplebuilding.util.DyedStorage;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;

/**
 * Baut das Client-Tooltip eines Buendels der Mod - fuer alle Loader dieselbe Fabrik: Vanillas
 * {@link ClientBundleTooltip} mit auf die Kapazitaet skaliertem Fuellbalken und, bei einem
 * gefaerbten Buendel, leicht in dessen Farbe getoenten Feldern.
 */
public final class ReinforcedBundleTooltips {
    private ReinforcedBundleTooltips() {
    }

    public static ClientBundleTooltip create(ReinforcedBundleTooltipData data) {
        ClientBundleTooltip component = new ClientBundleTooltip(data.contents());
        BundleTooltipAccessor accessor = (BundleTooltipAccessor) component;
        accessor.simplebuilding$setCapacityScale((float) data.maxCapacity() / 64.0f);
        accessor.simplebuilding$setSlotTint(DyedStorage.spriteTint(data.dyeColor()));
        return component;
    }
}
