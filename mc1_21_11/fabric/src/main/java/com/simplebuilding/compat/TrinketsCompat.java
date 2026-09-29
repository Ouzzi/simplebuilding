package com.simplebuilding.compat;

import com.simplebuilding.compat.accessory.AccessorySlots;
import dev.emi.trinkets.api.TrinketsApi;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Trinkets (Fabric, MC 1.21.11; mod id {@value #MOD_ID}, API package {@code dev.emi.trinkets} - on
 * this Minecraft version only the Trinkets Updated fork 3.11 ships a build): backpacks and quivers in
 * the {@code chest/back} and {@code legs/belt} slots count as worn ({@link AccessorySlots}).
 *
 * <p>Optional: compiled against the Trinkets API ({@code modCompileOnly}), and only
 * {@code Simplebuilding#onInitialize} refers to this class, inside an {@code isModLoaded("trinkets")}
 * branch. Without Trinkets the class is never loaded. Slot data as on 26.2:
 * {@code data/trinkets/entities/simplebuilding.json} plus the item tags {@code chest/back} and
 * {@code legs/belt}.
 */
public final class TrinketsCompat implements AccessorySlots.Hook {
    public static final String MOD_ID = "trinkets";

    private TrinketsCompat() {
    }

    public static void register() {
        AccessorySlots.register(new TrinketsCompat());
    }

    @Override
    public List<ItemStack> worn(LivingEntity entity) {
        List<ItemStack> stacks = new ArrayList<>();
        TrinketsApi.getTrinketComponent(entity).ifPresent(component -> component.forEach((slot, stack) -> {
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }));
        return stacks;
    }
}
