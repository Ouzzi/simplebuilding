package com.simplebuilding.modules.simplestorage;

import com.simplelib.api.AccessorySlots;
import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketsApi;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Trinkets Updated (Fabric; mod id {@value #MOD_ID}): backpacks and quivers in the {@code chest/back} and {@code legs/belt}
 * slots count as worn (SimpleLib {@link AccessorySlots}). Split stage 8c-2: was SimpleBuilding's {@code TrinketsCompat}.
 * Optional: compileOnly API, only {@code StorageFabric} refers to this class, inside an {@code isModLoaded} branch. The slots
 * come from data: {@code data/trinkets/entities/simplestorage.json} and the {@code chest/back}, {@code legs/belt} item tags.
 */
final class TrinketsHook implements AccessorySlots.Hook {
    static final String MOD_ID = "trinkets_updated";

    private TrinketsHook() {
    }

    static void register() {
        AccessorySlots.register(new TrinketsHook());
    }

    @Override
    public List<ItemStack> worn(LivingEntity entity) {
        TrinketAttachment attachment = TrinketsApi.getAttachment(entity);
        if (attachment == null) return List.of();
        List<ItemStack> stacks = new ArrayList<>();
        // Cosmetic slots only change the look; the real accessory sits in the normal slot.
        attachment.forEach((access, stack) -> {
            if (!access.cosmetic() && !stack.isEmpty()) stacks.add(stack);
        });
        return stacks;
    }

    @Override
    public List<ItemStack> visible(LivingEntity entity) {
        TrinketAttachment attachment = TrinketsApi.getAttachment(entity);
        if (attachment == null) return List.of();
        List<ItemStack> stacks = new ArrayList<>();
        attachment.forEachVisible((access, stack) -> {
            if (!stack.isEmpty()) stacks.add(stack);
        });
        return stacks;
    }
}
