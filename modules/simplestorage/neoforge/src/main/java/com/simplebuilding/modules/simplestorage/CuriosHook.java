package com.simplebuilding.modules.simplestorage;

import com.simplelib.api.AccessorySlots;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

/**
 * Curios (NeoForge; mod id {@value #MOD_ID}): backpacks and quivers in the {@code back} and {@code belt} slots count as worn
 * (SimpleLib {@link AccessorySlots}). Split stage 8c-2: was SimpleBuilding's {@code CuriosCompat}. Optional: compileOnly API,
 * only {@code StorageNeoForge} refers to this class, inside an {@code isLoaded("curios")} branch. The slots come from data:
 * {@code data/simplestorage/curios/entities/simplestorage.json}, the {@code curios:back} and {@code curios:belt} item tags.
 */
final class CuriosHook implements AccessorySlots.Hook {
    static final String MOD_ID = "curios";

    private CuriosHook() {
    }

    static void register() {
        AccessorySlots.register(new CuriosHook());
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
            // findCurios hands out the stacks the slots hold (not copies): callers change the contents in place.
            for (SlotResult result : handler.findCurios(stack -> !stack.isEmpty())) {
                if (result.slotContext().cosmetic() || (visibleOnly && !result.slotContext().visible())) continue;
                stacks.add(result.stack());
            }
            return stacks;
        }).orElse(List.of());
    }
}
