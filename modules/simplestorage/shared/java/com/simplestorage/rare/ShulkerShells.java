package com.simplestorage.rare;

import com.simplestorage.chest.ChestTier;
import com.simplestorage.registry.StorageItems;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Tier shulker shells (owner 2026-10-02, split stage 8b-3): Reinforced, Netherite, Enderite. Rare shulkers drop the shell of
 * their tier ({@link RareShulkers#dropShells}); a tier shell, any copper chest and a Vanilla shulker shell craft the shulker box of
 * that tier. The chain Shulker Shell + Iron Nugget -&gt; Reinforced, + Netherite Nugget -&gt; Netherite, + Enderite Nugget -&gt;
 * Enderite (SimpleLib's nuggets) also lifts a living shulker one tier ({@link RareShulkers#upgradeOf}); with SimpleBuilding a
 * shell placed on the ground takes the same nuggets (its pile, by id).
 */
public final class ShulkerShells {
    private ShulkerShells() {
    }

    /** One step of the chain: shell + nugget -&gt; next shell. */
    public record Step(Item shell, Item nugget, Item result) {
    }

    /** The chain, bottom up. */
    public static List<Step> steps() {
        return List.of(
                new Step(Items.SHULKER_SHELL, Items.IRON_NUGGET, StorageItems.REINFORCED_SHULKER_SHELL),
                new Step(StorageItems.REINFORCED_SHULKER_SHELL, com.simplelib.api.SimpleLibApi.materialItem("netherite_nugget"), StorageItems.NETHERITE_SHULKER_SHELL),
                new Step(StorageItems.NETHERITE_SHULKER_SHELL, com.simplelib.api.SimpleLibApi.materialItem("enderite_nugget"), StorageItems.ENDERITE_SHULKER_SHELL));
    }

    /** The tier shell of a chest/shulker tier. */
    public static Item shellOf(ChestTier tier) {
        return switch (tier) {
            case REINFORCED -> StorageItems.REINFORCED_SHULKER_SHELL;
            case NETHERITE -> StorageItems.NETHERITE_SHULKER_SHELL;
            case ENDERITE -> StorageItems.ENDERITE_SHULKER_SHELL;
        };
    }

    /** The three tier shells in tier order. */
    public static List<Item> tierShells() {
        return List.of(StorageItems.REINFORCED_SHULKER_SHELL, StorageItems.NETHERITE_SHULKER_SHELL, StorageItems.ENDERITE_SHULKER_SHELL);
    }
}
