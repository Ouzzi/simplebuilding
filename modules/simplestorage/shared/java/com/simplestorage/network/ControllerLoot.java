package com.simplestorage.network;

import com.simplestorage.chest.TieredChestBlockEntity;
import com.simplestorage.config.StorageConfig;
import com.simplestorage.registry.StorageItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * Controller chests as a find (BS29b-3): a better structure chest ({@code BetterChests}: Reinforced in strongholds, Netherite in
 * bastions and nether fortresses, Enderite in end cities and end ships) holds, with {@code controllerLootPercent} (default 10 %, at
 * most 50 %), a controller chest of its own step plus two interfaces and four cables, on top of its double loot. The roll comes from
 * the chest's loot seed and position, so it does not change when the chest is opened again.
 */
public final class ControllerLoot {
    private static final long SALT = 0x6C0D_7A11_5EED_B10CL;

    private ControllerLoot() {
    }

    /** Which controller step a better chest of {@code tier} holds. */
    public static ControllerLevel stepOf(com.simplestorage.chest.ChestTier tier) {
        return switch (tier) {
            case REINFORCED -> ControllerLevel.REINFORCED;
            case NETHERITE -> ControllerLevel.NETHERITE;
            case ENDERITE -> ControllerLevel.ENDERITE;
        };
    }

    /** What the find holds. */
    public static List<ItemStack> find(ControllerLevel step) {
        return List.of(new ItemStack(StorageItems.CONTROLLERS.get(step)), new ItemStack(StorageItems.CHEST_INTERFACE, 2), new ItemStack(StorageItems.COPPER_CABLE, 4));
    }

    /** Rolls the find for {@code chest}, after its loot table was rolled (server only). */
    public static void maybeAdd(TieredChestBlockEntity chest, long seed) {
        if (chest instanceof ControllerChestBlockEntity || !NetworkSubstitutes.enabled() || chest.getLevel() == null) return;
        double chance = StorageConfig.controllerLootChance();
        if (chance <= 0.0) return;
        RandomSource random = seed == 0L ? chest.getLevel().getRandom() : RandomSource.create(seed ^ (chest.getBlockPos().asLong() * 0x9E37_79B9_7F4A_7C15L) ^ SALT);
        if (random.nextDouble() >= chance) return;
        add(chest, stepOf(chest.tier()), random);
    }

    /** Puts the find into random empty slots of {@code chest} (what finds no slot is left out). */
    public static void add(TieredChestBlockEntity chest, ControllerLevel step, RandomSource random) {
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < chest.getContainerSize(); i++) {
            if (chest.getItem(i).isEmpty()) free.add(i);
        }
        for (ItemStack stack : find(step)) {
            if (free.isEmpty()) return;
            chest.setItem(free.remove(random.nextInt(free.size())), stack);
        }
    }
}
