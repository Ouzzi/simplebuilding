package com.simplebuilding.compat;

import com.simplebuilding.compat.accessory.AccessorySlots;
import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketsApi;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Trinkets Updated (Fabric, MC 26.2 and 26.3; mod id {@value #MOD_ID}, API package
 * {@code eu.pb4.trinkets}): backpacks and quivers in the {@code chest/back} and {@code legs/belt}
 * slots count as worn ({@link AccessorySlots}).
 *
 * <p>Optional: compiled against the Trinkets API ({@code compileOnly}), and only
 * {@code Simplebuilding#onInitialize} refers to this class, inside an
 * {@code isModLoaded("trinkets_updated")} branch. Without Trinkets Updated the class is never
 * loaded. The slots come from data: {@code data/trinkets/entities/simplebuilding.json} enables the
 * two slots for players, {@code data/trinkets/tags/item/chest/back.json} and {@code legs/belt.json}
 * say which items fit.
 */
public final class TrinketsCompat implements AccessorySlots.Hook {
    public static final String MOD_ID = "trinkets_updated";

    private TrinketsCompat() {
    }

    public static void register() {
        AccessorySlots.register(new TrinketsCompat());
    }

    @Override
    public List<ItemStack> worn(LivingEntity entity) {
        TrinketAttachment attachment = TrinketsApi.getAttachment(entity);
        if (attachment == null) {
            return List.of();
        }
        List<ItemStack> stacks = new ArrayList<>();
        attachment.forEach((access, stack) -> {
            // Cosmetic slots only change the look; the real accessory sits in the normal slot.
            if (!access.cosmetic() && !stack.isEmpty()) {
                stacks.add(stack);
            }
        });
        return stacks;
    }

    @Override
    public List<ItemStack> visible(LivingEntity entity) {
        TrinketAttachment attachment = TrinketsApi.getAttachment(entity);
        if (attachment == null) {
            return List.of();
        }
        List<ItemStack> stacks = new ArrayList<>();
        attachment.forEachVisible((access, stack) -> {
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        });
        return stacks;
    }
}
