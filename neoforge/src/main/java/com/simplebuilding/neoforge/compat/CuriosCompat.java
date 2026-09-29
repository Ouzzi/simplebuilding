package com.simplebuilding.neoforge.compat;

import com.simplebuilding.compat.accessory.AccessorySlots;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

/**
 * Curios (NeoForge; mod id {@value #MOD_ID}): backpacks and quivers in the {@code back} and
 * {@code belt} slots count as worn ({@link AccessorySlots}). The same source compiles against
 * Curios 16 (MC 26.2), 17 (26.3) and 14 (1.21.11, a copy in mc1_21_11/neoforge).
 *
 * <p>Optional: compiled against the Curios API jar ({@code compileOnly}), and only the mod
 * constructor refers to this class, inside an {@code isLoaded("curios")} branch. Without Curios the
 * class is never loaded. The slots come from data: {@code data/simplebuilding/curios/entities/simplebuilding.json}
 * enables {@code back} and {@code belt} for players, {@code data/curios/tags/item/back.json} and
 * {@code belt.json} say which items fit.
 */
public final class CuriosCompat implements AccessorySlots.Hook {
    public static final String MOD_ID = "curios";

    private CuriosCompat() {
    }

    public static void register() {
        AccessorySlots.register(new CuriosCompat());
    }

    @Override
    public List<ItemStack> worn(LivingEntity entity) {
        return collect(entity, false);
    }

    @Override
    public List<ItemStack> visible(LivingEntity entity) {
        return collect(entity, true);
    }

    private static List<ItemStack> collect(LivingEntity entity, boolean visibleOnly) {
        return CuriosApi.getCuriosInventory(entity).map(handler -> {
            List<ItemStack> stacks = new ArrayList<>();
            // findCurios hands out the stacks the slots hold (not copies), which the callers need:
            // they change the backpack or quiver contents in place.
            for (SlotResult result : handler.findCurios(stack -> !stack.isEmpty())) {
                if (result.slotContext().cosmetic() || (visibleOnly && !result.slotContext().visible())) {
                    continue;
                }
                stacks.add(result.stack());
            }
            return stacks;
        }).orElse(List.of());
    }
}
