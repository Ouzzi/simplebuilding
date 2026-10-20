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
     * Death handling order with every module loaded (gate 9f0aa3003): Simple Tools Soulbound owns its stacks (taken out
     * first, respawn copy with durability capped at 50), Simple Tweaks' partial keep inventory (T27, default hotbar + armor)
     * keeps only what is left of its slots, the rest drops. Respawning moves everything once: the dead player keeps no copy
     * and a second respawn from the same death gives nothing.
     */
    @GameTest(maxTicks = 100)
    public void soulboundKeepInventoryAndVaultSideBySide(GameTestHelper helper) {
        var level = helper.getLevel();
        if (level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY)) { helper.succeed(); return; }
        var soulbound = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT,
                        Identifier.fromNamespaceAndPath("simpletools", "soulbound")));
        helper.assertTrue(soulbound.isPresent(), "simpletools:soulbound registered");
        var p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var at = helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 2, 2.5));
        p.snapTo(at.x, at.y, at.z, 0, 0);
        try {
            var sword = new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
            sword.enchant(soulbound.get(), 1);
            p.getInventory().setItem(3, sword);
            p.getInventory().setItem(5, new ItemStack(net.minecraft.world.item.Items.DIRT, 10));
            p.getInventory().setItem(12, new ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 20));
            p.setHealth(0.0F);
            p.die(level.damageSources().genericKill());
            var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(p.blockPosition()).inflate(4));
            helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(net.minecraft.world.item.Items.COBBLESTONE)), "main inventory dropped");
            helper.assertTrue(drops.stream().noneMatch(e -> e.getItem().is(net.minecraft.world.item.Items.DIRT) || e.getItem().is(net.minecraft.world.item.Items.DIAMOND_SWORD)),
                    "neither the Soulbound sword nor the kept hotbar dirt became an item entity: " + drops);
            var fresh = (net.minecraft.server.level.ServerPlayer) helper.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            fresh.restoreFrom(p, false);
            var back = fresh.getInventory().getItem(3);
            helper.assertTrue(back.is(net.minecraft.world.item.Items.DIAMOND_SWORD) && back.getMaxDamage() - back.getDamageValue() == 50,
                    "the sword comes back through Soulbound (durability capped at 50), not as an untouched T27 copy: " + back);
            helper.assertTrue(fresh.getInventory().getItem(5).is(net.minecraft.world.item.Items.DIRT) && fresh.getInventory().getItem(5).getCount() == 10
                    && fresh.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND_SWORD) == 1, "T27 dirt back, exactly one sword");
            helper.assertTrue(p.getInventory().isEmpty(), "the dead player keeps no copy");
            var again = (net.minecraft.server.level.ServerPlayer) helper.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            again.restoreFrom(p, false);
            helper.assertTrue(again.getInventory().isEmpty(), "a second respawn from the same death gives nothing");
            drops.forEach(net.minecraft.world.entity.Entity::discard);
        } finally {
            level.getServer().getPlayerList().remove(p);
        }
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
        dumpCreativeTabs(System.getenv("SB_CREATIVE_TAB_DUMP"));
        helper.assertTrue(problems.isEmpty(), "creative tab coverage (" + problems.size() + "): " + problems);
        helper.succeed();
    }

    /**
     * b-texfix20 (19.10.): with SB_CREATIVE_TAB_DUMP set, the rebuilt tabs go to that file as JSON
     * ({@code [{"tab": id, "items": [{"id": item, "model": item_model}]}]}) for tools/textures/placeholder_audit.py, which
     * renders a contact sheet per tab offline. Unset (every normal run): nothing is written.
     */
    private static void dumpCreativeTabs(String target) {
        if (target == null || target.isBlank()) {
            return;
        }
        var tabs = new com.google.gson.JsonArray();
        for (Identifier tabId : BuiltInRegistries.CREATIVE_MODE_TAB.keySet()) {
            var items = new com.google.gson.JsonArray();
            for (ItemStack stack : BuiltInRegistries.CREATIVE_MODE_TAB.getValue(tabId).getDisplayItems()) {
                var entry = new com.google.gson.JsonObject();
                entry.addProperty("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                Identifier model = stack.get(net.minecraft.core.component.DataComponents.ITEM_MODEL);
                if (model != null) {
                    entry.addProperty("model", model.toString());
                }
                items.add(entry);
            }
            var tab = new com.google.gson.JsonObject();
            tab.addProperty("tab", tabId.toString());
            tab.add("items", items);
            tabs.add(tab);
        }
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of(target), tabs.toString());
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException("creative tab dump " + target, e);
        }
    }

    /**
     * Module items that are no creative item on purpose: the Trick Mirror is switched off behind a config (owner E11),
     * the Dragonmite spawn egg is offered only with {@code exposeSpawnEgg}, the light-blue portal is the legacy block of
     * old worlds, and the wiring example mod is a developer sample.
     */
    private static final Set<String> HIDDEN_BY_DESIGN = Set.of("simplemobs:trick_mirror",
            "simpleenddragonfight:dragonmite_spawn_egg", "simpledimension:light_blue_portal", "wiringexample:token",
            // Only made by burning an uploaded track onto a blank disc; an empty one would be meaningless.
            "simplemusic:custom_disc");

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
