package com.simplebuilding.modules.simpledimensions.guide;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * W5 (IG31-64): the Simple Dimensions chapter of the Simple Modpack guide is easy to get - crafted (book + crafting table,
 * SimpleLib), by {@code /simpledimension guide}, and handed out on a player's first journey into a dimension. Hand-written;
 * {@link DimensionsGuide} is generated and must not be edited. The book is SimpleLib's; without it nothing is given.
 */
public final class DimensionsGuideAccess {
    /** Marks the player as welcomed (an impossible-trigger advancement, awarded only by this class). */
    public static final Identifier WELCOME = Identifier.fromNamespaceAndPath(DimensionsGuide.NAMESPACE, "guide/welcome");

    private DimensionsGuideAccess() {
    }

    /** Opens the chapter and puts the Simple Modpack guide into the player's inventory (dropped if full). False without SimpleLib. */
    public static boolean give(ServerPlayer player) {
        Item guide = BuiltInRegistries.ITEM.getOptional(DimensionsGuide.MODPACK_GUIDE).orElse(null);
        if (guide == null) return false;
        award(player, DimensionsGuide.CHAPTER);
        ItemStack stack = new ItemStack(guide);
        if (!player.getInventory().add(stack)) player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
        return true;
    }

    /** First journey into a dimension: the guide once per player. True when it was handed out. */
    public static boolean welcome(ServerPlayer player) {
        var holder = player.level().getServer().getAdvancements().get(WELCOME);
        if (holder == null || player.getAdvancements().getOrStartProgress(holder).isDone()) return false;
        award(player, WELCOME);
        return give(player);
    }

    private static void award(ServerPlayer player, Identifier id) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(id);
        if (holder == null) return;
        for (String criterion : holder.value().criteria().keySet()) player.getAdvancements().award(holder, criterion);
    }
}
