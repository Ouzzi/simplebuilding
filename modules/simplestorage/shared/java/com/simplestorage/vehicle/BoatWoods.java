package com.simplestorage.vehicle;

import com.simplestorage.registry.StorageComponents;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The wood of a tiered chest boat (Queue N19, moved from SimpleBuilding in split stage 8b-2): one entity type and one
 * item per tier, the wood rides along as the component {@code simplestorage:boat_wood} (was {@code simplebuilding:boat_wood},
 * id move) on the item and as synced data on the boat. Bamboo is the raft.
 */
public final class BoatWoods {
    /** Vanilla's boat woods of 26.3, in the order of the creative tab. */
    public static final List<String> VANILLA = List.of("oak", "spruce", "birch", "jungle", "acacia", "cherry", "dark_oak",
            "pale_oak", "mangrove", "poplar", "bamboo");
    /** Simple End's boat woods (its own boat items, {@code simpleend:<wood>_boat}); only while Simple End is loaded. */
    public static final List<String> END = List.of("ashwood", "duskwood");
    public static final String SIMPLE_END = "simpleend";
    public static final String DEFAULT = "oak";
    private static List<String> all = VANILLA;

    private BoatWoods() {
    }

    /** Set once by the block registration (the loader knows whether Simple End is there). */
    public static void useEndWoods(boolean simpleEnd) {
        all = simpleEnd ? Stream.concat(VANILLA.stream(), END.stream()).toList() : VANILLA;
    }

    /** Every boat wood of this game: Vanilla, then the End woods with Simple End. */
    public static List<String> all() {
        return all;
    }

    /** A known wood, or oak. */
    public static String normalize(@Nullable String wood) {
        return wood != null && all.contains(wood) ? wood : DEFAULT;
    }

    public static boolean isRaft(String wood) {
        return "bamboo".equals(wood);
    }

    /** The wood a stack carries (oak without the component). */
    public static String of(ItemStack stack) {
        return normalize(stack.get(StorageComponents.BOAT_WOOD));
    }

    /** The stack of {@code item} with {@code wood}. */
    public static ItemStack stack(Item item, String wood) {
        ItemStack stack = new ItemStack(item);
        stack.set(StorageComponents.BOAT_WOOD, normalize(wood));
        return stack;
    }

    /** The namespace of the wood's own boats: {@code minecraft}, or {@code simpleend} for the End woods. */
    public static String namespace(String wood) {
        return END.contains(wood) ? SIMPLE_END : "minecraft";
    }

    /** The wood's plain boat (or raft) - the crafting base. */
    public static Item boat(String wood) {
        return item(wood, isRaft(wood) ? "bamboo_raft" : wood + "_boat");
    }

    /** The wood's chest boat (or chest raft) - its name and item sprite. */
    public static Item chestBoat(String wood) {
        return item(wood, isRaft(wood) ? "bamboo_chest_raft" : wood + "_chest_boat");
    }

    private static Item item(String wood, String path) {
        return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(namespace(wood), path));
    }
}
