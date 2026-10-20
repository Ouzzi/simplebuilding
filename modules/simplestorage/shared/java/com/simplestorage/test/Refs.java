package com.simplestorage.test;

import com.simplestorage.SimpleStorage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** The module's chests and boxes by id, with the names SimpleBuilding's {@code StorageRefs} uses (the tests came from there). */
final class Refs {
    private Refs() {
    }

    static Block block(String path) {
        return BuiltInRegistries.BLOCK.getValue(SimpleStorage.id(path));
    }

    static Item item(String path) {
        return BuiltInRegistries.ITEM.getValue(SimpleStorage.id(path));
    }

    static Block chestBlock(String tier) {
        return block(tier + "_chest");
    }

    static Item chest(String tier) {
        return item(tier + "_chest");
    }

    static Block trappedChestBlock(String tier) {
        return block(tier + "_trapped_chest");
    }

    static Item trappedChest(String tier) {
        return item(tier + "_trapped_chest");
    }

    static Block shulkerBoxBlock(String tier) {
        return block(tier + "_shulker_box");
    }

    static Item shulkerBox(String tier) {
        return item(tier + "_shulker_box");
    }

    static List<Item> trappedCopperChests() {
        List<Item> out = new ArrayList<>();
        for (String waxed : List.of("", "waxed_")) {
            for (String stage : SimpleStorage.COPPER_STAGES) out.add(item(waxed + stage + "trapped_copper_chest"));
        }
        return out;
    }

    /** A SimpleLib shared material ({@code cracked_diamond}, {@code netherite_nugget}, {@code enderite_nugget}). */
    static Item material(String path) {
        return com.simplelib.api.SimpleLibApi.materialItem(path);
    }

    /**
     * Split stage 8c-1: SimpleBuilding's Master Builder, Color Palette and Constructor's Touch only exist with SimpleBuilding;
     * a test about them passes without it (standalone) and says so. True when the test must stop here.
     */
    static boolean withoutSimpleBuildingEnchantments(net.minecraft.gametest.framework.GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        if (registry.get(com.simplestorage.bundle.BundleEnchantments.MASTER_BUILDER).isPresent()) return false;
        SimpleStorage.LOG.info("SimpleBuilding's enchantments are not loaded (standalone), nothing to check");
        helper.succeed();
        return true;
    }
}
