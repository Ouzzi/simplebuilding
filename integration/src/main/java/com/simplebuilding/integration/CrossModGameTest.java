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

    /**
     * S1a measurement (Simple Optimization, plan a-opt45b): idle server tick cost of every ticking block entity outside
     * {@code minecraft} (all modules loaded) next to Vanilla hopper/furnace/blast furnace as reference. All are placed
     * at once (flag 2, no neighbour updates) and timed over real game ticks, so tickers gated on {@code getGameTime() % n}
     * count too. Logged as {@code SB-BENCH s1a ...}; asserted only that every idle ticker runs without an exception.
     */
    @GameTest(maxTicks = 260)
    public void familyIdleTickCost(GameTestHelper helper) {
        var level = helper.getLevel();
        var reference = Set.of("minecraft:hopper", "minecraft:furnace", "minecraft:blast_furnace");
        record Probe(String id, BlockPos pos, net.minecraft.world.level.block.state.BlockState state,
                     net.minecraft.world.level.block.entity.BlockEntityTicker<net.minecraft.world.level.block.entity.BlockEntity> ticker,
                     long[] ns) {}
        var probes = new ArrayList<Probe>();
        var skipped = new ArrayList<String>();
        var errors = new ArrayList<String>();
        var slots = new ArrayList<BlockPos>();
        for (int y = 1; y <= 5; y += 2) for (int x = 0; x <= 6; x += 2) for (int z = 0; z <= 6; z += 2) slots.add(new BlockPos(x, y, z));
        for (var type : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
            var key = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
            if (key == null || key.getNamespace().equals("minecraft") && !reference.contains(key.toString())) continue;
            var block = BuiltInRegistries.BLOCK.stream().filter(b -> type.isValid(b.defaultBlockState())).findFirst();
            if (block.isEmpty()) { skipped.add(key + " (no block)"); continue; }
            var state = block.get().defaultBlockState();
            var ticker = state.getTicker(level, type);
            if (ticker == null) continue; // client-only or no ticker: nothing runs on the server
            if (probes.size() >= slots.size()) { skipped.add(key + " (no slot)"); continue; }
            var pos = helper.absolutePos(slots.get(probes.size()));
            try {
                level.setBlock(pos, state, 2);
                @SuppressWarnings("unchecked")
                var t = (net.minecraft.world.level.block.entity.BlockEntityTicker<net.minecraft.world.level.block.entity.BlockEntity>) (Object) level.getBlockState(pos).getTicker(level, type);
                if (t == null || level.getBlockEntity(pos) == null) { skipped.add(key + " (not placed)"); continue; }
                probes.add(new Probe(key.toString(), pos, level.getBlockState(pos), t, new long[2]));
            } catch (Throwable e) {
                errors.add(key + " place: " + e);
            }
        }
        final int warmup = 40, ticks = 200, calls = 5;
        for (int tick = 1; tick <= warmup + ticks; tick++) {
            boolean measure = tick > warmup;
            helper.runAfterDelay(tick, () -> {
                for (var p : probes) {
                    var be = level.getBlockEntity(p.pos());
                    if (be == null || p.ns()[1] < 0) continue;
                    try {
                        long t0 = System.nanoTime();
                        for (int i = 0; i < calls; i++) p.ticker().tick(level, p.pos(), p.state(), be);
                        if (measure) { p.ns()[0] += System.nanoTime() - t0; p.ns()[1] += calls; }
                    } catch (Throwable e) {
                        errors.add(p.id() + " tick: " + e);
                        p.ns()[1] = -1;
                    }
                }
            });
        }
        helper.runAfterDelay(warmup + ticks + 2, () -> {
            var log = org.slf4j.LoggerFactory.getLogger("sbintegration");
            var sorted = new ArrayList<>(probes);
            sorted.removeIf(p -> p.ns()[1] <= 0);
            sorted.sort(java.util.Comparator.comparingLong((Probe p) -> p.ns()[0] / p.ns()[1]).reversed());
            for (var p : sorted)
                log.info("SB-BENCH s1a {} ns_per_tick={}{}", p.id(), p.ns()[0] / p.ns()[1], reference.contains(p.id()) ? " (reference)" : "");
            log.info("SB-BENCH s1a measured={} skipped={} errors={}", sorted.size(), skipped, errors);
            helper.assertTrue(errors.isEmpty(), "idle tickers must not throw: " + errors);
            helper.assertTrue(sorted.stream().filter(p -> reference.contains(p.id())).count() == reference.size(), "reference measured");
            helper.assertTrue(sorted.size() > reference.size(), "family block entities measured");
            helper.succeed();
        });
    }
}
