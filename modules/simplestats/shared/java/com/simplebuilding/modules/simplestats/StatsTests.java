package com.simplebuilding.modules.simplestats;

import com.simplebuilding.modules.simplestats.data.StatData;
import com.simplebuilding.modules.simplestats.data.StatData.Delta;
import com.simplebuilding.modules.simplestats.data.StatKeys;
import io.netty.buffer.Unpooled;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.stats.Stats;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Shared test bodies; Fabric wraps them in {@code ModuleGameTest}, NeoForge registers {@link #ALL}. Namespace {@code simplestats:}. */
public final class StatsTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    static {
        ALL.put("launch", StatsTests::launch);
        ALL.put("codec_roundtrip", StatsTests::codecRoundtrip);
        ALL.put("foreign_version", StatsTests::foreignVersion);
        ALL.put("mining", StatsTests::mining);
        ALL.put("mining_bundling", StatsTests::miningBundling);
        ALL.put("kill_variants", StatsTests::killVariants);
        ALL.put("player_kills", StatsTests::playerKills);
        ALL.put("arrow_after_hand_swap", StatsTests::arrowAfterHandSwap);
        ALL.put("armor_absorbed", StatsTests::armorAbsorbed);
        ALL.put("fall_boots", StatsTests::fallBoots);
        ALL.put("shield_blocked", StatsTests::shieldBlocked);
        ALL.put("elytra_and_boots", StatsTests::elytraAndBoots);
        ALL.put("fishing", StatsTests::fishing);
        ALL.put("tool_uses", StatsTests::toolUses);
        ALL.put("shearing", StatsTests::shearing);
        ALL.put("brush", StatsTests::brush);
        ALL.put("creative_and_machines", StatsTests::creativeAndMachines);
        ALL.put("switch_off", StatsTests::switchOff);
        ALL.put("smithing_inherits", StatsTests::smithingInherits);
        ALL.put("reset_command", StatsTests::resetCommand);
        ALL.put("names", StatsTests::names);
        ALL.put("guide_book", com.simplebuilding.modules.simplestats.guide.StatsGuide::gameTest);
        // ALL-END
    }

    private StatsTests() {}

    // ------------------------------------------------------------------ helpers

    private static void with(GameTestHelper h, Consumer<StatsConfig> tweak, Runnable body) {
        StatsConfig old = SimpleStats.CONFIG;
        StatsConfig config = new StatsConfig();
        config.countFakePlayers = true; // the gametest players are subclasses of ServerPlayer, like machine players
        tweak.accept(config);
        SimpleStats.CONFIG = config;
        StatsRuntime.reset();
        var server = h.getLevel().getServer();
        boolean pvp = h.getLevel().getGameRules().get(GameRules.PVP);
        h.getLevel().getGameRules().set(GameRules.PVP, true, server);
        try {
            body.run();
        } finally {
            h.getLevel().getGameRules().set(GameRules.PVP, pvp, server);
            SimpleStats.CONFIG = old;
            StatsRuntime.reset();
        }
    }

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer p = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 2, 2))));
        return p;
    }

    /** A survival player placed in the world with a connection: only such a player can be hurt (ServerPlayer#isInvulnerableTo reads the connection). */
    @SuppressWarnings("removal")
    private static ServerPlayer placed(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket()); // else the player stays invulnerable
        p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 2, 2))));
        p.getAbilities().invulnerable = false;
        p.getAbilities().instabuild = false;
        p.setHealth(p.getMaxHealth());
        return p;
    }

    /** A command source acting as the player but answering to the server (a mock player has no connection to send messages to). */
    private static net.minecraft.commands.CommandSourceStack source(GameTestHelper h, ServerPlayer p, PermissionSet permissions) {
        return h.getLevel().getServer().createCommandSourceStack().withEntity(p).withPermission(permissions);
    }

    private static BlockPos pos(GameTestHelper h) { return h.absolutePos(new BlockPos(2, 2, 2)); }

    private static String id(Block block) { return BuiltInRegistries.BLOCK.getKey(block).toString(); }

    private static StatData stats(ItemStack stack) { return StatsComponents.of(stack); }

    private static void eq(GameTestHelper h, long expected, long actual, String what) { h.assertTrue(expected == actual, what + ": expected " + expected + " but was " + actual); }

    private static ItemStack enchanted(GameTestHelper h, ItemStack stack, ResourceKey<Enchantment> key, int level) {
        stack.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key), level);
        return stack;
    }

    /** Sum and "other" of every list must equal its total. */
    private static void invariant(GameTestHelper h, StatData data) {
        for (var list : data.details().entrySet()) {
            String total = StatKeys.LIST_TOTAL.get(list.getKey());
            if (total != null) eq(h, data.total(total), list.getValue().sum(), "list " + list.getKey() + " adds up to " + total);
        }
    }

    /** Uses the tool in the player's main hand on the block, like a right click. */
    private static void use(ServerPlayer p, ItemStack tool, BlockHitResult hit) {
        p.setItemSlot(EquipmentSlot.MAINHAND, tool);
        tool.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, hit));
    }

    // ------------------------------------------------------------------ tests

    public static void launch(GameTestHelper h) {
        h.assertTrue(StatsComponents.ITEM_STATS != null, "component type registered");
        h.assertTrue(BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(SimpleStats.id("item_stats")) == StatsComponents.ITEM_STATS, "registered under simplestats:item_stats");
        h.assertTrue(h.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("simplestats") != null, "/simplestats registered");
        h.succeed();
    }

    /** Component survives item serialization and the network codec; the byte budget holds for 1,000 different blocks. */
    public static void codecRoundtrip(GameTestHelper h) {
        Delta d = new Delta();
        for (int i = 0; i < 1000; i++) d.add(StatKeys.BLOCKS_MINED, StatKeys.L_BLOCKS, "mod" + (i % 7) + ":block_number_" + i, 1 + i % 5);
        d.record(StatKeys.R_BIGGEST_HIT, 1234);
        StatData data = StatData.EMPTY.apply(d, StatData.Budget.DEFAULT, 20000).withUid(42L);
        h.assertTrue(data.bytes() <= StatData.Budget.DEFAULT.maxBytes(), "estimated size within the budget: " + data.bytes());
        invariant(h, data);
        ItemStack stack = new ItemStack(Items.IRON_PICKAXE);
        stack.set(StatsComponents.ITEM_STATS, data);
        var ops = h.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var tag = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
        ItemStack back = ItemStack.CODEC.parse(ops, tag).getOrThrow();
        h.assertTrue(stats(back).equals(data), "item NBT roundtrip keeps the statistics");
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        StatsComponents.STREAM_CODEC.encode(buf, data);
        h.assertTrue(buf.readableBytes() <= StatData.Budget.DEFAULT.maxBytes(), "network size within the budget: " + buf.readableBytes());
        h.assertTrue(StatsComponents.STREAM_CODEC.decode(buf).equals(data), "network roundtrip keeps the statistics");
        h.succeed();
    }

    /** A value of a newer schema is read and shown but never overwritten. */
    public static void foreignVersion(GameTestHelper h) {
        with(h, c -> {}, () -> {
            ServerPlayer p = player(h);
            ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
            StatData future = StatData.sanitize(99, 7L, 1L, Map.of(StatKeys.BLOCKS_MINED, 5L), Map.of(), Map.of(), StatData.Budget.DEFAULT);
            pick.set(StatsComponents.ITEM_STATS, future);
            p.setItemSlot(EquipmentSlot.MAINHAND, pick);
            pick.mineBlock(h.getLevel(), Blocks.STONE.defaultBlockState(), pos(h), p);
            StatsRuntime.flush();
            h.assertTrue(stats(pick).equals(future), "newer version left unchanged");
        });
        h.succeed();
    }

    /** Hardness 0 blocks count too (owner decision S9); totals and the per-block list agree. */
    public static void mining(GameTestHelper h) {
        with(h, c -> {}, () -> {
            ServerPlayer p = player(h);
            ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
            p.setItemSlot(EquipmentSlot.MAINHAND, pick);
            pick.mineBlock(h.getLevel(), Blocks.STONE.defaultBlockState(), pos(h), p);
            pick.mineBlock(h.getLevel(), Blocks.STONE.defaultBlockState(), pos(h), p);
            pick.mineBlock(h.getLevel(), Blocks.SHORT_GRASS.defaultBlockState(), pos(h), p);
            StatsRuntime.flush();
            StatData d = stats(pick);
            eq(h, 3, d.total(StatKeys.BLOCKS_MINED), "blocks mined");
            eq(h, 2, d.detail(StatKeys.L_BLOCKS).entries().get(id(Blocks.STONE)), "stone");
            eq(h, 1, d.detail(StatKeys.L_BLOCKS).entries().get(id(Blocks.SHORT_GRASS)), "grass (hardness 0)");
            h.assertTrue(d.since() > 0 && d.uid() != 0, "date and id set");
            invariant(h, d);
            ItemStack stick = new ItemStack(Items.STICK);
            stick.mineBlock(h.getLevel(), Blocks.STONE.defaultBlockState(), pos(h), p);
            StatsRuntime.flush();
            h.assertTrue(!stick.has(StatsComponents.ITEM_STATS), "a stick is no tool: no statistics");
        });
        h.succeed();
    }

    /** A hammer breaking 25 blocks and 10,000 mined blocks cost one write each (bundled per tick) and little time. */
    public static void miningBundling(GameTestHelper h) {
        with(h, c -> {}, () -> {
            ServerPlayer p = player(h);
            ItemStack hammer = new ItemStack(Items.IRON_PICKAXE);
            hammer.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
            p.setItemSlot(EquipmentSlot.MAINHAND, hammer);
            for (int i = 0; i < 25; i++) hammer.mineBlock(h.getLevel(), Blocks.STONE.defaultBlockState(), pos(h), p);
            eq(h, 0, StatsRuntime.writes, "no write before the end of the tick");
            StatsRuntime.flush();
            eq(h, 1, StatsRuntime.writes, "25 blocks = one write");
            eq(h, 25, stats(hammer).total(StatKeys.BLOCKS_MINED), "25 counted");
            long start = System.nanoTime();
            for (int i = 0; i < 10_000; i++) hammer.mineBlock(h.getLevel(), Blocks.STONE.defaultBlockState(), pos(h), p);
            StatsRuntime.flush();
            long millis = (System.nanoTime() - start) / 1_000_000;
            eq(h, 10_025, stats(hammer).total(StatKeys.BLOCKS_MINED), "10,000 more counted");
            h.assertTrue(StatsRuntime.writes <= 10, "at most 10 writes, was " + StatsRuntime.writes);
            h.assertTrue(millis < 2000, "10,000 blocks took " + millis + " ms");
        });
        h.succeed();
    }

    /** Kill counts the mob and its variant (red sheep, baby zombie); the damage dealt and the biggest hit are recorded. */
    public static void killVariants(GameTestHelper h) {
        with(h, c -> {}, () -> {
            ServerPlayer p = player(h);
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            p.setItemSlot(EquipmentSlot.MAINHAND, sword);
            Sheep sheep = h.spawn(EntityTypes.SHEEP, new BlockPos(3, 2, 3));
            sheep.setColor(DyeColor.RED);
            Zombie zombie = h.spawn(EntityTypes.ZOMBIE, new BlockPos(4, 2, 4));
            zombie.setBaby(true);
            sheep.hurtServer(h.getLevel(), p.damageSources().playerAttack(p), 100F);
            zombie.hurtServer(h.getLevel(), p.damageSources().playerAttack(p), 100F);
            StatsRuntime.flush();
            StatData d = stats(sword);
            eq(h, 2, d.total(StatKeys.KILLS), "kills");
            eq(h, 1, d.detail(StatKeys.L_KILLS).entries().get("minecraft:sheep"), "sheep");
            eq(h, 1, d.detail(StatKeys.L_KILL_VARIANTS).entries().get("minecraft:sheep#red"), "red sheep variant");
            eq(h, 1, d.detail(StatKeys.L_KILL_VARIANTS).entries().get("minecraft:zombie#baby"), "baby zombie variant");
            h.assertTrue(d.total(StatKeys.DAMAGE_DEALT) > 0 && d.record(StatKeys.R_BIGGEST_HIT) > 0, "damage dealt and biggest hit");
            invariant(h, d);
        });
        h.succeed();
    }

    /** Killing a player counts as a number only (kind player, no name); switchable. */
    public static void playerKills(GameTestHelper h) {
        with(h, c -> {}, () -> {
            ServerPlayer p = player(h), victim = placed(h);
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            p.setItemSlot(EquipmentSlot.MAINHAND, sword);
            victim.hurtServer(h.getLevel(), p.damageSources().playerAttack(p), 1000F);
            StatsRuntime.flush();
            StatData d = stats(sword);
            eq(h, 1, d.total(StatKeys.KILLS), "player kill counted");
            eq(h, 1, d.detail(StatKeys.L_KILLS).entries().get("minecraft:player"), "listed as player only");
            h.assertTrue(d.detail(StatKeys.L_KILLS).entries().size() == 1 && d.detail(StatKeys.L_KILL_VARIANTS).entries().keySet().equals(java.util.Set.of("minecraft:player")), "no name, no variant");
        });
        with(h, c -> c.trackPlayerKills = false, () -> {
            ServerPlayer p = player(h), victim = placed(h);
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            p.setItemSlot(EquipmentSlot.MAINHAND, sword);
            victim.hurtServer(h.getLevel(), p.damageSources().playerAttack(p), 1000F);
            StatsRuntime.flush();
            eq(h, 0, stats(sword).total(StatKeys.KILLS), "player kills switched off");
        });
        h.succeed();
    }

    /** A bow's arrow only holds a copy of the bow; the kill still goes to the real bow after it moved to another slot. */
    public static void arrowAfterHandSwap(GameTestHelper h) {
        with(h, c -> {}, () -> {
            ServerPlayer p = player(h);
            ItemStack bow = new ItemStack(Items.BOW);
            p.setItemSlot(EquipmentSlot.MAINHAND, bow);
            StatsRuntime.onShoot(p, bow, List.of(new ItemStack(Items.ARROW)));
            Arrow arrow = new Arrow(h.getLevel(), p, new ItemStack(Items.ARROW), bow.copy());
            p.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            p.getInventory().setItem(5, bow);
            Sheep sheep = h.spawn(EntityTypes.SHEEP, new BlockPos(6, 2, 6));
            sheep.hurtServer(h.getLevel(), p.damageSources().arrow(arrow, p), 100F);
            StatsRuntime.flush();
            StatData d = stats(bow);
            eq(h, 1, d.total(StatKeys.ARROWS_FIRED), "arrows fired");
            eq(h, 1, d.total(StatKeys.KILLS), "arrow kill belongs to the bow");
            eq(h, 1, d.total(StatKeys.HITS), "hit");
            h.assertTrue(d.record(StatKeys.R_LONGEST_SHOT) >= 1, "longest shot recorded");
        });
        h.succeed();
    }

    /** The worn pieces share what the armor took away, in proportion to their armor points; the parts add up exactly. */
    public static void armorAbsorbed(GameTestHelper h) {
        with(h, c -> c.countCreative = true, () -> {
            ServerPlayer p = placed(h);
            ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE), helmet = new ItemStack(Items.IRON_HELMET);
            p.setItemSlot(EquipmentSlot.CHEST, chest);
            p.setItemSlot(EquipmentSlot.HEAD, helmet);
            p.doTick(); // applies the armor attributes (equipment changes are detected in the player's own tick)
            float before = p.getHealth();
            p.hurtServer(h.getLevel(), p.damageSources().cactus(), 8F); // not "generic": that bypasses armor
            float lost = before - p.getHealth();
            StatsRuntime.flush();
            long expected = Math.round((8F - lost) * 100.0);
            long c = stats(chest).total(StatKeys.DAMAGE_ABSORBED), hm = stats(helmet).total(StatKeys.DAMAGE_ABSORBED);
            h.assertTrue(Math.abs(c + hm - expected) <= 1 && expected > 0, "parts " + c + "+" + hm + " add up to " + expected);
            h.assertTrue(c > hm && Math.abs(c - 3 * hm) <= 3, "chestplate (6 points) takes three times the helmet (2 points): " + c + " / " + hm);
            eq(h, 1, stats(chest).total(StatKeys.HITS_TAKEN), "hit taken");
            h.assertTrue(stats(chest).detail(StatKeys.L_ABSORBED).entries().keySet().iterator().next().startsWith("minecraft:"), "by damage type");
            invariant(h, stats(chest));
        });
        h.succeed();
    }

    /** Fall damage belongs to the boots only. */
    public static void fallBoots(GameTestHelper h) {
        with(h, c -> c.countCreative = true, () -> {
            ServerPlayer p = placed(h);
            ItemStack boots = enchanted(h, new ItemStack(Items.IRON_BOOTS), Enchantments.FEATHER_FALLING, 4), chest = new ItemStack(Items.IRON_CHESTPLATE);
            p.setItemSlot(EquipmentSlot.FEET, boots);
            p.setItemSlot(EquipmentSlot.CHEST, chest);
            p.doTick();
            float before = p.getHealth();
            p.hurtServer(h.getLevel(), p.damageSources().fall(), 6F);
            StatsRuntime.flush();
            h.assertTrue(stats(boots).total(StatKeys.DAMAGE_ABSORBED) > 0, "boots absorbed fall damage (health lost " + (before - p.getHealth()) + " of 6)");
            h.assertTrue(!chest.has(StatsComponents.ITEM_STATS), "other armor untouched by a fall");
        });
        h.succeed();
    }

    /** A blocked attack counts for the shield with the attacker's kind. */
    public static void shieldBlocked(GameTestHelper h) {
        with(h, c -> {}, () -> {
            ServerPlayer p = player(h);
            ItemStack shield = new ItemStack(Items.SHIELD);
            Zombie zombie = h.spawn(EntityTypes.ZOMBIE, new BlockPos(5, 2, 5));
            StatsRuntime.onBlocked(p, shield, p.damageSources().mobAttack(zombie), 4F);
            StatsRuntime.flush();
            StatData d = stats(shield);
            eq(h, 1, d.total(StatKeys.ATTACKS_BLOCKED), "attacks blocked");
            eq(h, 400, d.total(StatKeys.DAMAGE_BLOCKED), "damage blocked (hundredths)");
            eq(h, 400, d.detail(StatKeys.L_BLOCKED_BY).entries().get("minecraft:zombie"), "by attacker kind");
        });
        h.succeed();
    }

    /** Distances come from the vanilla movement statistics: elytra flight (with its record) and the boots' walking paths. */
    public static void elytraAndBoots(GameTestHelper h) {
        with(h, c -> {}, () -> {
            ServerPlayer p = player(h);
            ItemStack elytra = new ItemStack(Items.ELYTRA), boots = new ItemStack(Items.LEATHER_BOOTS);
            p.setItemSlot(EquipmentSlot.CHEST, elytra);
            p.setItemSlot(EquipmentSlot.FEET, boots);
            StatsRuntime.startFlight(p);
            p.awardStat(Stats.AVIATE_ONE_CM, 12345);
            StatsRuntime.stopFlight(p);
            p.awardStat(Stats.WALK_ONE_CM, 1000);
            p.awardStat(Stats.SPRINT_ONE_CM, 500);
            p.awardStat(Stats.CROUCH_ONE_CM, 200);
            p.awardStat(Stats.SWIM_ONE_CM, 300);
            StatsRuntime.accrue(p);
            StatsRuntime.flush();
            StatData e = stats(elytra), b = stats(boots);
            eq(h, 12345, e.total(StatKeys.DISTANCE_FLOWN), "distance flown");
            eq(h, 1, e.total(StatKeys.FLIGHTS), "flights");
            eq(h, 123, e.record(StatKeys.R_LONGEST_FLIGHT), "longest flight in blocks");
            eq(h, 1000, b.total(StatKeys.DISTANCE_WALKED), "walked");
            eq(h, 500, b.total(StatKeys.DISTANCE_SPRINTED), "sprinted");
            eq(h, 200, b.total(StatKeys.DISTANCE_SNEAKED), "sneaked");
            eq(h, 300, b.total(StatKeys.DISTANCE_SWUM), "swum");
        });
        h.succeed();
    }

    /** Casting and catching through the real hook points; fish, treasure and junk are told apart. */
    public static void fishing(GameTestHelper h) {
        with(h, c -> {}, () -> {
            ServerPlayer p = player(h);
            ItemStack rod = new ItemStack(Items.FISHING_ROD);
            p.setItemSlot(EquipmentSlot.MAINHAND, rod);
            FishingHook hook = new FishingHook(p, h.getLevel(), 0, 0);
            Projectile.spawnProjectile(hook, h.getLevel(), rod);
            hook.discard();
            try {
                CriteriaTriggers.FISHING_ROD_HOOKED.trigger(p, rod, new FishingHook(p, h.getLevel(), 0, 0),
                        List.of(new ItemStack(Items.COD), new ItemStack(Items.SADDLE), new ItemStack(Items.STICK)));
            } catch (RuntimeException ignored) {
                // advancement bookkeeping of a player without a connection; the statistics hook runs first
            }
            StatsRuntime.flush();
            StatData d = stats(rod);
            eq(h, 1, d.total(StatKeys.CASTS), "casts");
            eq(h, 3, d.total(StatKeys.CATCHES), "catches");
            eq(h, 1, d.detail(StatKeys.L_CATCH_KINDS).entries().get("fish"), "fish");
            eq(h, 1, d.detail(StatKeys.L_CATCH_KINDS).entries().get("treasure"), "treasure");
            eq(h, 1, d.detail(StatKeys.L_CATCH_KINDS).entries().get("junk"), "junk");
            invariant(h, d);
        });
        h.succeed();
    }

    /** Stripping a log, tilling soil and lighting with flint and steel through the real item use. */
    public static void toolUses(GameTestHelper h) {
        with(h, c -> {}, () -> {
            ServerPlayer p = player(h);
            BlockPos target = new BlockPos(2, 1, 2);
            BlockPos abs = h.absolutePos(target);
            var hit = new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), net.minecraft.core.Direction.UP, abs, false);
            ItemStack axe = new ItemStack(Items.IRON_AXE), hoe = new ItemStack(Items.IRON_HOE), lighter = new ItemStack(Items.FLINT_AND_STEEL);
            h.setBlock(target, Blocks.OAK_LOG);
            use(p, axe, hit);
            h.setBlock(target, Blocks.DIRT);
            use(p, hoe, hit);
            h.setBlock(target, Blocks.STONE);
            use(p, lighter, hit);
            StatsRuntime.flush();
            eq(h, 1, stats(axe).total(StatKeys.LOGS_STRIPPED), "log stripped");
            eq(h, 1, stats(hoe).total(StatKeys.TILLED), "soil tilled");
            eq(h, 1, stats(lighter).total(StatKeys.IGNITED), "ignited");
        });
        h.succeed();
    }

    /** Shearing a sheep counts with its color. */
    public static void shearing(GameTestHelper h) {
        with(h, c -> {}, () -> {
            ServerPlayer p = player(h);
            ItemStack shears = new ItemStack(Items.SHEARS);
            p.setItemSlot(EquipmentSlot.MAINHAND, shears);
            Sheep sheep = h.spawn(EntityTypes.SHEEP, new BlockPos(3, 2, 3));
            sheep.setColor(DyeColor.BLUE);
            p.interactOn(sheep, InteractionHand.MAIN_HAND, Vec3.ZERO);
            StatsRuntime.flush();
            StatData d = stats(shears);
            eq(h, 1, d.total(StatKeys.SHEARED), "sheared");
            eq(h, 1, d.detail(StatKeys.L_SHEARED).entries().get("minecraft:sheep#blue"), "blue sheep");
        });
        h.succeed();
    }

    /** A suspicious block brushed away counts once, for the brush. */
    public static void brush(GameTestHelper h) {
        with(h, c -> {}, () -> {
            ServerPlayer p = player(h);
            ItemStack brush = new ItemStack(Items.BRUSH);
            BlockPos target = new BlockPos(2, 1, 2);
            h.setBlock(target, Blocks.SUSPICIOUS_SAND);
            var be = (BrushableBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(target));
            long time = h.getLevel().getGameTime();
            for (int i = 0; i < 10; i++) be.brush(time + i * 11L, h.getLevel(), p, net.minecraft.core.Direction.UP, brush);
            StatsRuntime.flush();
            eq(h, 1, stats(brush).total(StatKeys.BRUSHED), "brushed");
            eq(h, 1, stats(brush).detail(StatKeys.L_BRUSHED).entries().get(id(Blocks.SUSPICIOUS_SAND)), "suspicious sand");
        });
        h.succeed();
    }

    /** Creative and spectator players and machine players do not count (owner decision S8); the settings open them up. */
    public static void creativeAndMachines(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 2, 2);
        with(h, c -> c.countFakePlayers = false, () -> {
            ServerPlayer machine = player(h);
            ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
            pick.mineBlock(h.getLevel(), Blocks.STONE.defaultBlockState(), h.absolutePos(at), machine);
            StatsRuntime.flush();
            h.assertTrue(!pick.has(StatsComponents.ITEM_STATS), "machine players do not count");
        });
        with(h, c -> {}, () -> {
            ServerPlayer creative = (ServerPlayer) h.makeMockServerPlayer(GameType.CREATIVE);
            ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
            pick.mineBlock(h.getLevel(), Blocks.STONE.defaultBlockState(), h.absolutePos(at), creative);
            StatsRuntime.flush();
            h.assertTrue(!pick.has(StatsComponents.ITEM_STATS), "creative players do not count");
        });
        with(h, c -> c.countCreative = true, () -> {
            ServerPlayer creative = (ServerPlayer) h.makeMockServerPlayer(GameType.CREATIVE);
            ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
            pick.mineBlock(h.getLevel(), Blocks.STONE.defaultBlockState(), h.absolutePos(at), creative);
            StatsRuntime.flush();
            eq(h, 1, stats(pick).total(StatKeys.BLOCKS_MINED), "countCreative opens it up");
        });
        h.succeed();
    }

    /** Switched off: nothing is counted and no component appears. */
    public static void switchOff(GameTestHelper h) {
        with(h, c -> c.enabled = false, () -> {
            ServerPlayer p = player(h);
            ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
            pick.mineBlock(h.getLevel(), Blocks.STONE.defaultBlockState(), pos(h), p);
            StatsRuntime.flush();
            h.assertTrue(!pick.has(StatsComponents.ITEM_STATS), "no component while off");
        });
        h.succeed();
    }

    /** The smithing upgrade keeps the statistics of the old item (owner decision S6). */
    public static void smithingInherits(GameTestHelper h) {
        ItemStack old = new ItemStack(Items.DIAMOND_PICKAXE);
        StatData data = StatData.EMPTY.apply(new Delta().add(StatKeys.BLOCKS_MINED, StatKeys.L_BLOCKS, "minecraft:stone", 9), StatData.Budget.DEFAULT, 5).withUid(3L);
        old.set(StatsComponents.ITEM_STATS, data);
        var input = new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), old, new ItemStack(Items.NETHERITE_INGOT));
        var recipe = h.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, h.getLevel());
        h.assertTrue(recipe.isPresent(), "smithing recipe found");
        ItemStack result = recipe.get().value().assemble(input);
        h.assertTrue(result.is(Items.NETHERITE_PICKAXE), "netherite pickaxe");
        h.assertTrue(stats(result).equals(data), "statistics inherited");
        h.succeed();
    }

    /** {@code /simplestats reset} needs permission and is the only way to clear an item. */
    public static void resetCommand(GameTestHelper h) {
        with(h, c -> {}, () -> {
            ServerPlayer p = player(h);
            ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
            p.setItemSlot(EquipmentSlot.MAINHAND, pick);
            pick.mineBlock(h.getLevel(), Blocks.STONE.defaultBlockState(), pos(h), p);
            StatsRuntime.flush();
            var dispatcher = h.getLevel().getServer().getCommands().getDispatcher();
            boolean refused = false;
            try {
                dispatcher.execute("simplestats reset", source(h, p, PermissionSet.NO_PERMISSIONS));
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                refused = true;
            }
            h.assertTrue(refused && pick.has(StatsComponents.ITEM_STATS), "without permission nothing happens");
            try {
                dispatcher.execute("simplestats reset", source(h, p, PermissionSet.ALL_PERMISSIONS));
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                h.fail("reset failed: " + e.getMessage());
            }
            h.assertTrue(!pick.has(StatsComponents.ITEM_STATS), "reset clears the statistics");
        });
        h.succeed();
    }

    /** Names of keys are built from translation keys, variants are split into parts. */
    public static void names(GameTestHelper h) {
        h.assertTrue(key(StatNames.entry(StatKeys.L_BLOCKS, "minecraft:stone")).equals("block.minecraft.stone"), "block key");
        h.assertTrue(key(StatNames.entry(StatKeys.L_KILLS, "somemod:odd_thing")).equals("entity.somemod.odd_thing"), "foreign mob key");
        h.assertTrue(key(StatNames.mob("minecraft:sheep#red+baby")).equals("stat.simplestats.variant"), "variant name");
        h.assertTrue(key(StatNames.mob("minecraft:sheep")).equals("entity.minecraft.sheep"), "plain mob name");
        h.succeed();
    }

    private static String key(net.minecraft.network.chat.Component c) {
        return c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t ? t.getKey() : "<" + c.getString() + ">";
    }
}
