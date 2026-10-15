package com.simplewanderers.test;

import com.mojang.serialization.JsonOps;
import com.simplewanderers.SimpleWanderers;
import com.simplewanderers.WandererRegistry;
import com.simplewanderers.config.WandererConfig;
import com.simplewanderers.entity.VillageWanderer;
import com.simplewanderers.entity.WandererHorse;
import com.simplewanderers.logic.WandererSpawner;
import com.simplewanderers.logic.WandererTrades;
import com.simplewanderers.logic.WandererVillages;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Module GameTests (owner answers W1-W10). All on the 15 x 12 x 15 arena; the village is a stub {@link WandererSpawner.Locator}
 * (a real village cannot be generated in a test), the clock is explicit: rotation and respawn take a game time, so
 * "3 days" and "1.5 days" are shortened to a function call instead of 72000 ticks of waiting.
 */
public final class WandererTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    public static final Map<String, Integer> MAX_TICKS = new LinkedHashMap<>();
    private static long nextVillage = System.nanoTime();

    static {
        ALL.put("registered", h -> {
            for (String id : List.of("village_wanderer", "wanderer_horse")) {
                h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(SimpleWanderers.id(id)), "entity type " + id);
            }
            h.assertTrue(BuiltInRegistries.ITEM.containsKey(SimpleWanderers.id("village_wanderer_spawn_egg")), "spawn egg");
            h.assertTrue(DefaultAttributes.hasSupplier(WandererRegistry.WANDERER) && DefaultAttributes.hasSupplier(WandererRegistry.HORSE), "attributes");
            h.assertTrue(WandererConfig.respawnDays == 7.0 && WandererConfig.rerollDays == 3.0 && WandererConfig.restockDays == 1.5
                    && WandererConfig.villageRadius == 48 && WandererConfig.homeRadius == 24 && WandererConfig.horse && WandererConfig.raidBlocksSpawn
                    && WandererConfig.biomeAssortment && WandererConfig.enabled, "config defaults");
            h.assertTrue(WandererConfig.rerollTicks() == 72000L && WandererConfig.restockTicks() == 36000L && WandererConfig.respawnTicks() == 168000L, "day maths");
            h.succeed();
        });
        ALL.put("config_caps", h -> {
            try {
                var json = com.google.gson.JsonParser.parseString("{\"respawnDays\":9999,\"rerollDays\":0.001,\"restockDays\":-4,\"villageRadius\":2,\"homeRadius\":9999,\"horse\":\"x\"}").getAsJsonObject();
                WandererConfig.apply(json);
                h.assertTrue(WandererConfig.respawnDays == 60.0 && WandererConfig.rerollDays == 1.0 && WandererConfig.restockDays == 0.5, "days clamp");
                h.assertTrue(WandererConfig.villageRadius == 16 && WandererConfig.homeRadius == 48 && WandererConfig.horse, "radius clamp, bad bool = default");
                WandererConfig.apply(com.google.gson.JsonParser.parseString("{\"rerollDays\":2,\"restockDays\":9}").getAsJsonObject());
                h.assertTrue(WandererConfig.restockDays == 2.0, "restock never exceeds reroll");
                WandererConfig.apply(com.google.gson.JsonParser.parseString("{\"respawnDays\":\"a\",\"rerollDays\":null}").getAsJsonObject());
                h.assertTrue(WandererConfig.respawnDays == 7.0 && WandererConfig.rerollDays == 3.0, "garbage = default");
                h.assertTrue(WandererConfig.KEYS.equals(WandererConfig.toJson().keySet().stream().toList()), "KEYS lists the file's keys in order");
            } finally {
                WandererConfig.reset();
            }
            h.succeed();
        });
        ALL.put("sub_mod_switch", h -> {
            boolean before = com.simplebuilding.framework.api.SubMods.enabled(SimpleWanderers.MOD_ID);
            try {
                village(h, "plains");
                com.simplebuilding.framework.api.SubMods.set(SimpleWanderers.MOD_ID, false);
                h.assertTrue(!SimpleWanderers.on() && WandererSpawner.visit(h.getLevel(), bell(h), h.getLevel().getGameTime()) == WandererSpawner.Result.DISABLED, "off: no spawn");
            } finally {
                com.simplebuilding.framework.api.SubMods.set(SimpleWanderers.MOD_ID, before);
                WandererSpawner.reset();
            }
            h.succeed();
        });

        // --- exactly one per village ------------------------------------------------------------------------------------------
        ALL.put("spawn_one_per_village", h -> {
            try {
                ServerLevel level = h.getLevel();
                long id = village(h, "plains");
                long now = level.getGameTime();
                h.assertTrue(WandererSpawner.visit(level, bell(h), now) == WandererSpawner.Result.SPAWNED, "first visit spawns");
                h.assertTrue(WandererSpawner.visit(level, bell(h), now + 200) == WandererSpawner.Result.EXISTS, "second visit: exists");
                h.assertTrue(WandererSpawner.visit(level, bell(h), now + 400) == WandererSpawner.Result.EXISTS, "third visit: exists");
                List<VillageWanderer> found = wanderers(h, id);
                h.assertTrue(found.size() == 1, "exactly one wanderer in the village: " + found.size());
                VillageWanderer w = found.get(0);
                h.assertTrue(w.getHomePosition() != null && !w.isInvisible() && w.getDespawnDelay() == 0 && !w.removeWhenFarAway(1.0E9), "stays at home, visible, never despawns");
                h.assertTrue(w.villagerType().equals("plains") && WandererVillages.get(level.getServer()).get(WandererVillages.key(level, id)).wanderer().get().equals(w.getUUID()), "record points at him");
                // Another village gets its own.
                long other = village(h, "desert");
                h.assertTrue(WandererSpawner.visit(level, bell(h), now + 600) == WandererSpawner.Result.SPAWNED && wanderers(h, other).size() == 1 && wanderers(h, id).size() == 1,
                        "a second village has its own wanderer; the first is untouched");
            } finally {
                WandererSpawner.reset();
            }
            h.succeed();
        });
        ALL.put("duplicate_removed_on_join", h -> {
            ServerLevel level = h.getLevel();
            long id = village(h, "plains");
            WandererSpawner.visit(level, bell(h), level.getGameTime());
            VillageWanderer first = wanderers(h, id).get(0);
            VillageWanderer second = WandererRegistry.WANDERER.spawn(level, h.absolutePos(new BlockPos(3, 1, 3)), EntitySpawnReason.EVENT);
            second.bind(id, "plains", bell(h));
            h.assertTrue(wanderers(h, id).size() == 2, "two loaded for a moment");
            h.runAfterDelay(5, () -> {
                try {
                    List<VillageWanderer> found = wanderers(h, id);
                    h.assertTrue(found.size() == 1 && found.get(0) == first && second.isRemoved(), "the duplicate removed itself, the recorded one stayed: " + found.size());
                    h.assertTrue(!WandererVillages.get(level.getServer()).get(WandererVillages.key(level, id)).alive() == false, "record still alive");
                    h.succeed();
                } finally {
                    WandererSpawner.reset();
                }
            });
        });
        MAX_TICKS.put("duplicate_removed_on_join", 100);
        ALL.put("spawn_without_village_is_ignored", h -> {
            // Spawn egg / summon: no village id, never recorded, never removed as a duplicate.
            ServerLevel level = h.getLevel();
            VillageWanderer a = WandererRegistry.WANDERER.spawn(level, h.absolutePos(new BlockPos(3, 1, 3)), EntitySpawnReason.SPAWN_ITEM_USE);
            VillageWanderer b = WandererRegistry.WANDERER.spawn(level, h.absolutePos(new BlockPos(5, 1, 3)), EntitySpawnReason.SPAWN_ITEM_USE);
            h.assertTrue(a.villageId() == VillageWanderer.NO_VILLAGE && b.villageId() == VillageWanderer.NO_VILLAGE, "no village");
            h.runAfterDelay(5, () -> {
                h.assertTrue(!a.isRemoved() && !b.isRemoved() && !a.getOffers().isEmpty(), "both stay and trade");
                h.succeed();
            });
        });

        // --- respawn ----------------------------------------------------------------------------------------------------------
        ALL.put("respawn_after_days_only_when_visited", h -> {
            try {
                ServerLevel level = h.getLevel();
                long id = village(h, "plains");
                long t0 = level.getGameTime();
                WandererSpawner.visit(level, bell(h), t0);
                VillageWanderer w = wanderers(h, id).get(0);
                WandererHorse horse = (WandererHorse) level.getEntity(w.horseId());
                w.kill(level);
                WandererVillages.Entry dead = WandererVillages.get(level.getServer()).get(WandererVillages.key(level, id));
                h.assertTrue(!dead.alive() && dead.deadSince() >= t0, "killing him records the death (W5: no protection)");
                h.assertTrue(horse.isAlive() && horse.getLeashHolder() == null, "the horse is free, not lost");
                long died = dead.deadSince();
                long week = 7 * SimpleWanderers.DAY;
                h.assertTrue(WandererSpawner.visit(level, bell(h), died + week - 1) == WandererSpawner.Result.WAITING, "6 days 23 hours: waiting");
                h.assertTrue(wanderers(h, id).stream().noneMatch(Entity::isAlive), "nobody appears by itself");
                h.assertTrue(WandererSpawner.visit(level, bell(h), died + week) == WandererSpawner.Result.SPAWNED, "after 7 days a visit respawns him");
                h.assertTrue(wanderers(h, id).stream().filter(Entity::isAlive).count() == 1, "exactly one again");
                // Respawn off: waiting forever.
                VillageWanderer again = wanderers(h, id).stream().filter(Entity::isAlive).findFirst().get();
                again.kill(level);
                WandererConfig.respawnDays = 0.0;
                long died2 = WandererVillages.get(level.getServer()).get(WandererVillages.key(level, id)).deadSince();
                h.assertTrue(WandererSpawner.visit(level, bell(h), died2 + 100 * week) == WandererSpawner.Result.WAITING, "respawnDays 0: never");
            } finally {
                WandererConfig.reset();
                WandererSpawner.reset();
            }
            h.succeed();
        });
        ALL.put("lost_record_recovers", h -> {
            try {
                ServerLevel level = h.getLevel();
                long id = village(h, "plains");
                WandererVillages data = WandererVillages.get(level.getServer());
                data.setAlive(WandererVillages.key(level, id), UUID.randomUUID());
                long now = level.getGameTime();
                for (int i = 0; i < WandererSpawner.LOST_AFTER; i++) h.assertTrue(WandererSpawner.visit(level, bell(h), now + i) == WandererSpawner.Result.EXISTS, "record alive, not found yet");
                WandererVillages.Entry e = data.get(WandererVillages.key(level, id));
                h.assertTrue(!e.alive() && e.deadSince() == now + WandererSpawner.LOST_AFTER - 1, "never found with a player at the bell: lost, the 7 days start");
                // An old wanderer entity of that village that shows up is adopted, not duplicated.
                VillageWanderer stray = WandererRegistry.WANDERER.spawn(level, h.absolutePos(new BlockPos(4, 1, 4)), EntitySpawnReason.EVENT);
                stray.bind(id, "plains", bell(h));
                h.assertTrue(WandererSpawner.visit(level, bell(h), now + 10) == WandererSpawner.Result.EXISTS && wanderers(h, id).size() == 1
                        && data.get(WandererVillages.key(level, id)).wanderer().get().equals(stray.getUUID()), "stray adopted");
            } finally {
                WandererSpawner.reset();
            }
            h.succeed();
        });
        ALL.put("village_records_round_trip", h -> {
            WandererVillages data = new WandererVillages();
            UUID u = UUID.randomUUID();
            data.setAlive("minecraft:overworld/1", u);
            data.setAlive("minecraft:overworld/2", UUID.randomUUID());
            data.markDead("minecraft:overworld/2", data.get("minecraft:overworld/2").wanderer().get(), 12345L);
            h.assertTrue(!data.markDead("minecraft:overworld/1", UUID.randomUUID(), 5L), "a stranger never kills the record");
            var json = WandererVillages.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
            WandererVillages back = WandererVillages.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
            h.assertTrue(back.size() == 2 && back.get("minecraft:overworld/1").wanderer().get().equals(u) && back.get("minecraft:overworld/2").deadSince() == 12345L
                    && !back.get("minecraft:overworld/2").alive(), "records survive the save");
            h.succeed();
        });

        // --- horse ------------------------------------------------------------------------------------------------------------
        ALL.put("horse_leashed_and_capturable", h -> {
            try {
                ServerLevel level = h.getLevel();
                long id = village(h, "plains");
                WandererSpawner.visit(level, bell(h), level.getGameTime());
                VillageWanderer w = wanderers(h, id).get(0);
                Entity e = level.getEntity(w.horseId());
                h.assertTrue(e instanceof WandererHorse horse && horse.getLeashHolder() == w && horse.isPersistenceRequired(), "a persistent horse hangs on his leash");
                WandererHorse horse = (WandererHorse) e;
                h.assertTrue(horse.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.BODY).is(Items.LEATHER_HORSE_ARMOR), "in ochre leather armor");
                ServerPlayer player = player(h);
                player.setShiftKeyDown(false);
                InteractionResult plain = horse.mobInteract(player, InteractionHand.MAIN_HAND);
                h.assertTrue(plain.consumesAction() && player.getVehicle() == null && horse.getLeashHolder() == w, "right-click: not rideable, still leashed");
                h.assertTrue(horse.getLeashHolder() == w, "still his");
                player.setShiftKeyDown(true);
                h.assertTrue(horse.mobInteract(player, InteractionHand.MAIN_HAND).consumesAction() && horse.getLeashHolder() == player, "sneak + empty hand: the leash is yours");
                player.setShiftKeyDown(false);
                // The wanderer notices and starts the replacement clock; a new horse only after respawnDays.
                w.horseUpkeep(level);
                h.assertTrue(w.horseId() == null && w.horseGoneSince() >= 0, "he knows the horse is gone");
                w.horseUpkeep(level);
                h.assertTrue(w.horseId() == null, "not replaced at once");
                w.horseLostAt(level.getGameTime() - WandererConfig.respawnTicks());
                h.assertTrue(w.spawnHorse(level) && level.getEntity(w.horseId()) instanceof WandererHorse fresh && fresh != horse && fresh.getLeashHolder() == w, "a new horse after the wait");
                // Setting horse = false: no horse at all.
                WandererConfig.horse = false;
                long other = village(h, "plains");
                WandererSpawner.visit(level, bell(h), level.getGameTime());
                h.assertTrue(wanderers(h, other).get(0).horseId() == null, "horse setting off: none");
            } finally {
                WandererConfig.reset();
                WandererSpawner.reset();
            }
            h.succeed();
        });

        // --- rotation ---------------------------------------------------------------------------------------------------------
        ALL.put("rotation_reroll_every_period_restock_every_half", h -> {
            try {
                rotation(h, 72000L, 36000L);
                // Shortened times through the config: 1 day / 0.5 day behave the same way.
                WandererConfig.rerollDays = 1.0;
                WandererConfig.restockDays = 0.5;
                rotation(h, 24000L, 12000L);
            } finally {
                WandererConfig.reset();
            }
            h.succeed();
        });
        ALL.put("rotation_waits_for_trading_and_never_bursts", h -> {
            ServerLevel level = h.getLevel();
            long id = nextVillage++;
            VillageWanderer w = bound(h, id, "plains", new BlockPos(7, 1, 7));
            long t = 10 * 72000L;
            w.tickRotation(t);
            w.getOffers();
            ServerPlayer player = player(h);
            w.setTradingPlayer(player);
            List<Item> before = results(w);
            h.assertTrue(w.tickRotation(t + 72000L) == VillageWanderer.Rotation.NONE && results(w).equals(before), "no change while somebody trades");
            w.setTradingPlayer(null);
            h.assertTrue(w.tickRotation(t + 72000L) == VillageWanderer.Rotation.REROLL, "it changes when the trade is over");
            h.assertTrue(w.tickRotation(t + 72000L) == VillageWanderer.Rotation.NONE, "once");
            // 50 periods of absence: one reroll, then quiet.
            h.assertTrue(w.tickRotation(t + 50 * 72000L) == VillageWanderer.Rotation.REROLL && w.tickRotation(t + 50 * 72000L) == VillageWanderer.Rotation.NONE, "no catch-up burst");
            // A half period with a restock in between a mid-trade uses reset.
            w.getOffers().get(0).increaseUses();
            w.setTradingPlayer(player);
            w.tickRotation(t + 51 * 72000L + 36000L);
            h.assertTrue(w.getOffers().get(0).getUses() == 1, "restock waits for the trade too");
            w.setTradingPlayer(null);
            h.assertTrue(w.tickRotation(t + 51 * 72000L + 36000L) != VillageWanderer.Rotation.NONE, "and happens after it");
            h.succeed();
        });
        ALL.put("rotation_persists_across_reload", h -> {
            ServerLevel level = h.getLevel();
            long id = nextVillage++;
            VillageWanderer w = bound(h, id, "desert", new BlockPos(7, 1, 7));
            w.tickRotation(3 * 72000L);
            List<Item> offers = results(w);
            TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            w.saveWithoutId(out);
            VillageWanderer copy = bound(h, id + 1, "plains", new BlockPos(9, 1, 7));
            copy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
            h.assertTrue(copy.villageId() == id && copy.villagerType().equals("desert") && results(copy).equals(offers), "village, type and offers survive");
            h.assertTrue(copy.tickRotation(3 * 72000L + 100) == VillageWanderer.Rotation.NONE, "a reload within the period rolls nothing");
            h.assertTrue(copy.tickRotation(4 * 72000L) == VillageWanderer.Rotation.REROLL, "the next period still comes");
            h.succeed();
        });

        // --- goods ------------------------------------------------------------------------------------------------------------
        ALL.put("trades_only_from_wanderer_pools", h -> {
            ServerLevel level = h.getLevel();
            Set<String> allowed = new LinkedHashSet<>();
            for (String set : List.of("wanderer/buying", "wanderer/common", "wanderer/uncommon", "wanderer/rare", "wanderer/biome/desert", "wanderer/biome/swamp")) {
                allowed.addAll(poolKeys(level, "simplewanderers:" + set));
            }
            h.assertTrue(!allowed.isEmpty(), "pools are not empty");
            Set<Item> poolItems = new LinkedHashSet<>();
            for (String type : WandererTrades.TYPES) {
                VillageWanderer w = bound(h, nextVillage++, type, new BlockPos(7, 1, 7));
                for (String set : WandererTrades.SETS) pool(level, w, set, poolItems);
                pool(level, w, WandererTrades.biomeSet(type), poolItems);
            }
            for (int i = 0; i < 40; i++) {
                VillageWanderer w = bound(h, nextVillage++, WandererTrades.TYPES.get(i % WandererTrades.TYPES.size()), new BlockPos(7, 1, 7));
                w.tickRotation(i * 72000L);
                List<MerchantOffer> offers = w.getOffers();
                h.assertTrue(offers.size() >= 6 && offers.size() <= 10, "6-10 offers: " + offers.size());
                Set<Item> seen = new LinkedHashSet<>();
                for (MerchantOffer o : offers) {
                    h.assertTrue(poolItems.contains(o.getResult().getItem()) || o.getResult().is(Items.EMERALD), "an offer from outside the wanderer's pools: " + o.getResult());
                    h.assertTrue(seen.add(o.getResult().getItem()), "an item twice: " + o.getResult());
                    String path = BuiltInRegistries.ITEM.getKey(o.getResult().getItem()).getPath();
                    h.assertTrue(!path.endsWith("_core") && !path.contains("enderite") && !path.contains("netherite") && !path.contains("astral"),
                            "nothing very rare at the wanderer: " + path);
                }
            }
            h.succeed();
        });
        MAX_TICKS.put("trades_only_from_wanderer_pools", 200);
        ALL.put("vanilla_common_partition_complete", h -> {
            ServerLevel level = h.getLevel();
            Set<String> vanilla = new LinkedHashSet<>();
            for (String k : poolKeys(level, "minecraft:wandering_trader/common")) if (k.startsWith("minecraft:")) vanilla.add(k);
            Set<String> dw = new LinkedHashSet<>(), wt = new LinkedHashSet<>();
            for (String k : poolKeys(level, "simplewanderers:wanderer/common")) if (k.startsWith("minecraft:")) dw.add(k);
            for (String k : poolKeys(level, "simplewanderers:wandering_trader/common")) if (k.startsWith("minecraft:")) wt.add(k);
            h.assertTrue(vanilla.size() == 78 && dw.size() == 47 && wt.size() == 31, "78 = 47 + 31: " + vanilla.size() + "/" + dw.size() + "/" + wt.size());
            Set<String> union = new LinkedHashSet<>(dw);
            union.addAll(wt);
            h.assertTrue(union.equals(vanilla), "complete: " + union.size());
            h.assertTrue(dw.stream().noneMatch(wt::contains), "no good at both");
            // The Wandering Trader really draws from his half now (the Vanilla trade set was repointed).
            for (String set : List.of("common", "buying")) {
                TradeSet trade = level.registryAccess().lookupOrThrow(Registries.TRADE_SET).getValueOrThrow(
                        net.minecraft.resources.ResourceKey.create(Registries.TRADE_SET, Identifier.parse("minecraft:wandering_trader/" + set)));
                String key = trade.trades() instanceof net.minecraft.core.HolderSet.Named<VillagerTrade> named ? named.key().location().toString() : "?";
                h.assertTrue(key.equals("simplewanderers:wandering_trader/" + set), "traveling trader draws " + set + " from " + key);
            }
            // Buying: buckets, potato, hay at the wanderer; bottle and spider eye stay.
            Set<String> buyDw = poolKeys(level, "simplewanderers:wanderer/buying"), buyWt = poolKeys(level, "simplewanderers:wandering_trader/buying");
            h.assertTrue(buyDw.contains("minecraft:wandering_trader/water_bucket_emerald") && buyWt.contains("minecraft:wandering_trader/water_bottle_emerald")
                    && !buyDw.contains("minecraft:wandering_trader/water_bottle_emerald") && !buyWt.contains("minecraft:wandering_trader/water_bucket_emerald"), "buying split");
            h.succeed();
        });
        ALL.put("no_tier4_mod_item_and_cores_stay_at_the_trader", h -> {
            ServerLevel level = h.getLevel();
            for (String set : List.of("wanderer/buying", "wanderer/common", "wanderer/uncommon", "wanderer/rare")) {
                for (String key : poolKeys(level, "simplewanderers:" + set)) {
                    boolean ok = key.startsWith("minecraft:") || key.equals("simplebuilding:wandering_trader/octant_emerald") || key.equals("simplebuilding:wandering_trader/reinforced_bundle_emerald")
                            || key.equals("simplebuilding:wandering_trader/emerald_octant") || key.equals("simplebuilding:wandering_trader/emerald_reinforced_bundle")
                            || key.equals("simplebuilding:wandering_trader/emerald_diamond_pebbles") || key.equals("simplemoney:wandering_trader/40") || key.equals("simplemoney:wandering_trader/41")
                            || (key.startsWith("simplemoney:links/simplemobheads/") && key.endsWith("_head"));
                    h.assertTrue(ok && !key.contains("core"), "not for the wanderer: " + key);
                }
            }
            if (net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(Identifier.parse("simplebuilding:copper_core"))) {
                Set<String> unc = poolKeys(level, "minecraft:wandering_trader/uncommon");
                for (String core : List.of("emerald_copper_cores", "emerald_iron_cores", "emerald_gold_core", "emerald_diamond_core", "emerald_wand_book", "emerald_octant", "emerald_reinforced_bundle")) {
                    h.assertTrue(unc.contains("simplebuilding:wandering_trader/" + core), "the traveling trader still sells " + core);
                }
            }
            h.succeed();
        });
        ALL.put("biome_assortment", h -> {
            ServerLevel level = h.getLevel();
            Map<String, Set<Item>> pools = new LinkedHashMap<>();
            for (String type : WandererTrades.TYPES) {
                VillageWanderer w = bound(h, nextVillage++, type, new BlockPos(7, 1, 7));
                Set<Item> items = new LinkedHashSet<>();
                WandererTrades.poolOffers(level, w, WandererTrades.setKey(WandererTrades.biomeSet(type))).forEach(o -> items.add(o.getResult().getItem()));
                h.assertTrue(items.size() >= 4, type + " has a pool of at least 4: " + items.size());
                pools.put(type, items);
                // Every wanderer of that type offers at least one of its biome goods (two are drawn, duplicates skipped).
                for (int i = 0; i < 8; i++) {
                    VillageWanderer v = bound(h, nextVillage++, type, new BlockPos(7, 1, 7));
                    v.tickRotation(i * 72000L);
                    h.assertTrue(v.getOffers().stream().anyMatch(o -> items.contains(o.getResult().getItem())), type + " offers a biome good (period " + i + ")");
                }
            }
            h.assertTrue(!pools.get("desert").containsAll(pools.get("snow")) && pools.get("desert").contains(Items.CACTUS) && pools.get("snow").contains(Items.PACKED_ICE)
                    && pools.get("taiga").contains(Items.PODZOL) && pools.get("jungle").contains(Items.JUNGLE_LOG), "the pools differ by village type");
            h.assertTrue(WandererTrades.biomeSet("mushroom_island").equals("wanderer/biome/plains") && WandererTrades.biomeSet("desert").equals("wanderer/biome/desert"), "unknown types are plains");
            // Switch: off = no biome offers beyond the shared pools (the biome set is not drawn).
            try {
                WandererConfig.biomeAssortment = false;
                VillageWanderer v = bound(h, nextVillage++, "desert", new BlockPos(7, 1, 7));
                v.tickRotation(0L);
                v.getOffers();
                h.assertTrue(v.getOffers().size() <= 8, "biomeAssortment off: at most 8 offers");
            } finally {
                WandererConfig.reset();
            }
            h.succeed();
        });
        ALL.put("works_standalone", h -> {
            ServerLevel level = h.getLevel();
            boolean partners = net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(Identifier.parse("simplebuilding:octant"))
                    || BuiltInRegistries.ITEM.containsKey(Identifier.parse("simplemoney:money_bill"));
            int mod = 0;
            for (int i = 0; i < 30; i++) {
                VillageWanderer w = bound(h, nextVillage++, WandererTrades.TYPES.get(i % 7), new BlockPos(7, 1, 7));
                w.tickRotation(i * 72000L);
                h.assertTrue(w.getOffers().size() >= 6, "offers exist: " + w.getOffers().size());
                for (MerchantOffer o : w.getOffers()) if (!BuiltInRegistries.ITEM.getKey(o.getResult().getItem()).getNamespace().equals("minecraft")) mod++;
            }
            h.assertTrue(partners || mod == 0, "alone (only Vanilla): every offer is a Vanilla item, " + mod + " were not");
            h.succeed();
        });
        ALL.put("guide_book", com.simplewanderers.guide.WanderersGuide::gameTest);
        ALL.put("discover_recipe_unlocks", com.simplewanderers.discover.WanderersDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simplewanderers.discover.WanderersDiscover::behaviourHints);
    }

    // --- helpers ----------------------------------------------------------------------------------------------------------------

    /** Installs the stub village locator: the arena's middle is the bell, the id is unique per call. Returns the id. */
    static long village(GameTestHelper h, String type) {
        long id = nextVillage++;
        BlockPos bell = bell(h);
        WandererSpawner.locator = (level, near, radius) -> Optional.of(new WandererSpawner.Village(id, bell, type));
        h.runBeforeTestEnd(() -> {
            for (VillageWanderer w : wanderers(h, id)) w.discard();
            for (WandererHorse horse : h.getLevel().getEntitiesOfClass(WandererHorse.class, area(h))) horse.discard();
            WandererSpawner.reset();
        });
        return id;
    }

    static BlockPos bell(GameTestHelper h) {
        return h.absolutePos(new BlockPos(7, 1, 7));
    }

    static AABB area(GameTestHelper h) {
        return new AABB(h.absolutePos(new BlockPos(0, 0, 0))).inflate(0).expandTowards(15, 12, 15).inflate(24);
    }

    static List<VillageWanderer> wanderers(GameTestHelper h, long id) {
        return h.getLevel().getEntitiesOfClass(VillageWanderer.class, area(h), w -> w.villageId() == id);
    }

    /** A wanderer bound to a village, spawned the way the spawner does, but without recording it. */
    static VillageWanderer bound(GameTestHelper h, long id, String type, BlockPos relative) {
        VillageWanderer w = WandererRegistry.WANDERER.spawn(h.getLevel(), h.absolutePos(relative), EntitySpawnReason.EVENT);
        w.setNoAi(true);
        w.bind(id, type, bell(h));
        h.runBeforeTestEnd(w::discard);
        return w;
    }

    static ServerPlayer player(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        h.runBeforeTestEnd(() -> h.getLevel().getServer().getPlayerList().remove(p));
        return p;
    }

    static List<Item> results(VillageWanderer w) {
        List<Item> out = new ArrayList<>();
        for (MerchantOffer o : w.getOffers()) out.add(o.getResult().getItem());
        return out;
    }

    /** Ids of the trades in a villager_trade tag, e.g. {@code simplewanderers:wanderer/common}. */
    static Set<String> poolKeys(ServerLevel level, String tag) {
        Set<String> keys = new LinkedHashSet<>();
        var registry = level.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
        registry.get(TagKey.create(Registries.VILLAGER_TRADE, Identifier.parse(tag))).ifPresent(named -> named.forEach((Holder<VillagerTrade> holder) ->
                holder.unwrapKey().ifPresent(k -> keys.add(k.identifier().toString()))));
        return keys;
    }

    static void pool(ServerLevel level, VillageWanderer w, String set, Set<Item> into) {
        WandererTrades.poolOffers(level, w, WandererTrades.setKey(set)).forEach(o -> into.add(o.getResult().getItem()));
    }

    /** Period maths of the rotation with explicit game times: reroll at the period, restock at the half. */
    static void rotation(GameTestHelper h, long reroll, long restock) {
        long id = nextVillage++;
        VillageWanderer w = bound(h, id, "plains", new BlockPos(7, 1, 7));
        long t0 = 5 * reroll;
        h.assertTrue(w.tickRotation(t0) == VillageWanderer.Rotation.NONE, "first call only learns the period");
        List<Item> a = results(w);
        h.assertTrue(!a.isEmpty(), "offers drawn");
        w.getOffers().get(0).increaseUses();
        w.getOffers().get(1).increaseUses();
        h.assertTrue(w.tickRotation(t0 + restock - 1) == VillageWanderer.Rotation.NONE && w.getOffers().get(0).getUses() == 1, "before the half: nothing");
        h.assertTrue(w.tickRotation(t0 + restock) == VillageWanderer.Rotation.RESTOCK, "restock at the half period");
        h.assertTrue(w.getOffers().get(0).getUses() == 0 && w.getOffers().get(1).getUses() == 0 && results(w).equals(a), "uses reset, same offers");
        h.assertTrue(w.tickRotation(t0 + reroll - 1) == VillageWanderer.Rotation.NONE, "still the same period");
        h.assertTrue(w.tickRotation(t0 + reroll) == VillageWanderer.Rotation.REROLL, "a new stock at the period");
        List<Item> b = results(w);
        h.assertTrue(!b.equals(a), "period B differs from period A");
        // Deterministic: another wanderer of the same village in the same period draws the same list; another village does not.
        VillageWanderer twin = bound(h, id, "plains", new BlockPos(9, 1, 7));
        twin.tickRotation(t0 + reroll);
        h.assertTrue(results(twin).equals(b), "same village + period = same stock for everybody");
        VillageWanderer stranger = bound(h, id + 7, "plains", new BlockPos(9, 1, 9));
        stranger.tickRotation(t0 + reroll);
        h.assertTrue(!results(stranger).equals(b), "another village has its own stock");
        h.assertTrue(w.tickRotation(t0 + reroll + restock) == VillageWanderer.Rotation.RESTOCK, "and the half after that restocks again");
    }

    private WandererTests() {}
}
