package com.simplebuilding.entity.vehicle;

import com.simplebuilding.component.ModDataComponentTypes;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The wood of a tiered chest boat (Queue N19): one entity type and one item per tier, the wood rides along as the
 * component {@code simplebuilding:boat_wood} on the item and as synced data on the boat. Bamboo is the raft.
 */
public final class BoatWoods {
    /** Vanilla's boat woods of 26.3, in the order of the creative tab. */
    public static final List<String> ALL = List.of("oak", "spruce", "birch", "jungle", "acacia", "cherry", "dark_oak",
            "pale_oak", "mangrove", "poplar", "bamboo");
    public static final String DEFAULT = "oak";

    private BoatWoods() {
    }

    /** A known wood, or oak. */
    public static String normalize(@Nullable String wood) {
        return wood != null && ALL.contains(wood) ? wood : DEFAULT;
    }

    public static boolean isRaft(String wood) {
        return "bamboo".equals(wood);
    }

    /** The wood a stack carries (oak without the component). */
    public static String of(ItemStack stack) {
        return normalize(stack.get(ModDataComponentTypes.BOAT_WOOD));
    }

    /** The stack of {@code item} with {@code wood}. */
    public static ItemStack stack(Item item, String wood) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponentTypes.BOAT_WOOD, normalize(wood));
        return stack;
    }

    /** Vanilla's plain boat (or raft) of the wood - the crafting base. */
    public static Item boat(String wood) {
        return vanilla(isRaft(wood) ? "bamboo_raft" : wood + "_boat");
    }

    /** Vanilla's chest boat (or chest raft) of the wood - its name and item sprite. */
    public static Item chestBoat(String wood) {
        return vanilla(isRaft(wood) ? "bamboo_chest_raft" : wood + "_chest_boat");
    }

    private static Item vanilla(String path) {
        return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(path));
    }
}
