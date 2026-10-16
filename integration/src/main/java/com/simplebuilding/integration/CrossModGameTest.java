package com.simplebuilding.integration;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Only public registry ids and vanilla interfaces cross the mod boundary. */
public final class CrossModGameTest {
    @GameTest(maxTicks = 100)
    public void exampleItemFitsSimpleBuildingStorage(GameTestHelper helper) {
        helper.assertTrue(FabricLoader.getInstance().isModLoaded("simplebuilding")
                && FabricLoader.getInstance().isModLoaded("wiringexample"), "Both mods must be loaded");
        var item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("wiringexample", "token"));
        var block = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("simplebuilding", "reinforced_hopper"));
        var pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, block);
        var entity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(entity instanceof Container, "SimpleBuilding hopper exposes vanilla Container");
        var container = (Container) entity;
        container.setItem(0, new ItemStack(item, 3));
        helper.assertTrue(container.getItem(0).is(item) && container.getItem(0).getCount() == 3,
                "SimpleBuilding storage must accept the example mod's item");
        helper.assertTrue(container.removeItem(0, 2).getCount() == 2 && container.getItem(0).getCount() == 1,
                "The foreign item can be retrieved without loss");
        helper.succeed();
    }

    /**
     * Nachtrag 65: with every module loaded, each registered non-Vanilla item stands in at least one creative tab
     * (own module tab or SimpleBuilding / Vanilla tab) and no mod tab offers one stack twice. Legacy ids that
     * exist only so old worlds still load are the only exceptions.
     */
    @GameTest(maxTicks = 100)
    public void everyModuleItemIsInSomeCreativeTab(GameTestHelper helper) {
        CreativeModeTabs.tryRebuildTabContents(helper.getLevel().enabledFeatures(), true, helper.getLevel().registryAccess());
        List<String> problems = new ArrayList<>();
        Set<net.minecraft.world.item.Item> offered = new HashSet<>();
        for (Identifier tabId : BuiltInRegistries.CREATIVE_MODE_TAB.keySet()) {
            CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(tabId);
            Set<String> seen = new HashSet<>();
            if (!"minecraft".equals(tabId.getNamespace()) && !tabId.getPath().equals("enchanted_dev") && tab.getDisplayItems().isEmpty()
                    && !tabId.getPath().equals("arrows")) {
                problems.add(tabId + " is a mod tab without any item");
            }
            for (ItemStack stack : tab.getDisplayItems()) {
                offered.add(stack.getItem());
                String key = BuiltInRegistries.ITEM.getKey(stack.getItem()) + stack.getComponentsPatch().toString();
                if (!"minecraft".equals(tabId.getNamespace()) && !seen.add(key)) {
                    problems.add(tabId + " offers " + key + " twice");
                }
            }
        }
        for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
            if ("minecraft".equals(id.getNamespace()) || LEGACY_OR_HIDDEN.contains(id.getPath()) || HIDDEN_BY_DESIGN.contains(id.toString())) {
                continue;
            }
            if (!offered.contains(BuiltInRegistries.ITEM.getValue(id))) {
                problems.add(id + " stands in no creative tab");
            }
        }
        // Owner N65: a module that has its own tab lists every one of its items there (SimpleBuilding has several tabs and its
        // own test, DataIntegrityTests#everyModItemIsInExactlyOneCreativeTab).
        Set<String> ownTabNamespaces = new HashSet<>();
        for (Identifier tabId : BuiltInRegistries.CREATIVE_MODE_TAB.keySet()) {
            if (!"minecraft".equals(tabId.getNamespace()) && !"simplebuilding".equals(tabId.getNamespace())) {
                ownTabNamespaces.add(tabId.getNamespace());
            }
        }
        for (String namespace : List.of("simpleshops", "simplebeacons", "simpletrims", "simplemaps", "simpletweaks", "simplemobs")) {
            if (!ownTabNamespaces.contains(namespace)) {
                problems.add(namespace + " has no creative tab of its own");
            }
        }
        for (String namespace : ownTabNamespaces) {
            if (namespace.equals("simplelib")) {
                continue; // its shared materials stand in SimpleBuilding's materials tab and Vanilla's ingredients by design
            }
            Set<net.minecraft.world.item.Item> inOwnTab = new HashSet<>();
            for (Identifier tabId : BuiltInRegistries.CREATIVE_MODE_TAB.keySet()) {
                if (tabId.getNamespace().equals(namespace)) {
                    BuiltInRegistries.CREATIVE_MODE_TAB.getValue(tabId).getDisplayItems().forEach(stack -> inOwnTab.add(stack.getItem()));
                }
            }
            for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
                if (id.getNamespace().equals(namespace) && !LEGACY_OR_HIDDEN.contains(id.getPath()) && !HIDDEN_BY_DESIGN.contains(id.toString())
                        && !inOwnTab.contains(BuiltInRegistries.ITEM.getValue(id))) {
                    problems.add(id + " is missing from the " + namespace + " tab");
                }
            }
        }
        helper.assertTrue(problems.isEmpty(), "creative tab coverage (" + problems.size() + "): " + problems);
        helper.succeed();
    }

    /**
     * Module items that are no creative item on purpose: the Trick Mirror is switched off behind a config (owner E11),
     * the Dragonmite spawn egg is offered only with {@code exposeSpawnEgg}, the light-blue portal is the legacy block of
     * old worlds, and the wiring example mod is a developer sample.
     */
    private static final Set<String> HIDDEN_BY_DESIGN = Set.of("simplemobs:trick_mirror",
            "simpleenddragonfight:dragonmite_spawn_egg", "simpledimension:light_blue_portal", "wiringexample:token");

    /** Items kept out of every tab on purpose (see SimpleBuilding's DataIntegrityTests list). */
    private static final Set<String> LEGACY_OR_HIDDEN = Set.of(
            "creative_spacer", "stone_spatula", "copper_spatula", "iron_spatula", "gold_spatula", "diamond_spatula",
            "netherite_spatula", "spawn_teleporter_tier_3", "spawn_teleporter_tier_4", "netherite_flypad",
            "enderite_flypad", "reinforced_elytra_pad", "fine_elytra_pad", "funny_stick",
            "chipped_ceramic_bucket", "chipped_ceramic_water_bucket", "chipped_ceramic_lava_bucket",
            "cracked_ceramic_bucket", "cracked_ceramic_water_bucket", "cracked_ceramic_lava_bucket",
            "brittle_ceramic_bucket", "brittle_ceramic_water_bucket", "brittle_ceramic_lava_bucket",
            "enderite_water_bucket_full", "enderite_lava_bucket_full", "enderite_soul_lava_bucket_full",
            "enderite_milk_bucket_full");
}
