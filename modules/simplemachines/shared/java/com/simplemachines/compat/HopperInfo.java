package com.simplemachines.compat;

import com.simplemachines.hopper.HopperFilterMode;
import com.simplemachines.hopper.TierHopperBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * What the optional Jade plugin of Simple Machines shows for a tier hopper (b-jadehop26, MACH32 stage 3 rest: was
 * SimpleBuilding's topic "hopper filter", which on 26.3 only knew the 26.2 hopper), without any Jade class so the server
 * GameTests can check it: the filter mode and, unless the filter is off, the filter items (filter principle: the real item
 * in a slot is its filter). The server writes {@link #serverData}, the client turns it into {@link #lines}.
 */
public final class HopperInfo {
    /** Filter mode ordinal in Jade's server data; absent for anything that is no tier hopper. */
    public static final String MODE = "SimpleMachinesHopperMode";
    /** Description ids of the filter items, joined by {@link #SEPARATOR}. */
    public static final String ITEMS = "SimpleMachinesHopperItems";
    private static final String SEPARATOR = "\n";
    private static final String KEY = "jade.simplemachines.hopper.";

    private HopperInfo() {
    }

    /** Writes mode and filter items of a tier hopper into {@code data}; does nothing for any other block entity. */
    public static void serverData(BlockEntity be, CompoundTag data) {
        if (!(be instanceof TierHopperBlockEntity hopper)) return;
        data.putInt(MODE, hopper.getFilterMode().ordinal());
        List<String> items = new ArrayList<>();
        for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
            ItemStack filter = hopper.getItem(slot);
            if (!filter.isEmpty()) items.add(filter.getItem().getDescriptionId());
        }
        data.putString(ITEMS, String.join(SEPARATOR, items));
    }

    /** The Jade lines of {@link #serverData}: "Filter: mode", then the items or "accepts nothing"; empty without hopper data. */
    public static List<Component> lines(CompoundTag data) {
        List<Component> lines = new ArrayList<>();
        int ordinal = data.getIntOr(MODE, -1);
        if (ordinal < 0) return lines;
        HopperFilterMode mode = HopperFilterMode.of(ordinal);
        lines.add(Component.translatable(KEY + "mode", mode.getText()));
        if (mode == HopperFilterMode.NONE) return lines;
        String items = data.getStringOr(ITEMS, "");
        if (items.isEmpty()) {
            lines.add(Component.translatable(KEY + "nothing"));
            return lines;
        }
        MutableComponent list = Component.empty();
        String[] ids = items.split(SEPARATOR);
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) list.append(", ");
            list.append(Component.translatable(ids[i]));
        }
        lines.add(Component.translatable(KEY + "items", list));
        return lines;
    }
}
