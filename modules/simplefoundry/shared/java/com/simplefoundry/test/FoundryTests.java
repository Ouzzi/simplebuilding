package com.simplefoundry.test;

import com.simplefoundry.FoundryRegistry;
import com.simplefoundry.FoundryScan;
import com.simplefoundry.Fuels;
import com.simplefoundry.Heat;
import com.simplefoundry.CastTarget;
import com.simplefoundry.Layers;
import com.simplefoundry.Tier;
import com.simplefoundry.Melts;
import com.simplefoundry.Metal;
import com.simplefoundry.SimpleFoundry;
import com.simplefoundry.block.DeskBlock;
import com.simplefoundry.block.ChamberBlock;
import com.simplefoundry.block.DeskBlockEntity;
import com.simplefoundry.block.FoundryMenu;
import com.simplefoundry.block.GrateBlock;
import com.simplefoundry.block.SpoutBlock;
import com.simplefoundry.block.TrayBlockEntity;
import com.simplefoundry.config.FoundryConfig;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Module GameTests (plan docs/ai/PLAN-B-FOUNDRY32-2026-10-31.md): registry, config, layer maths, item tables, the multiblock check and a
 * whole melt-and-pour run. The run drives {@code DeskBlockEntity.tick} by hand with the melt and cast intervals set to 1 tick, because
 * the game clock does not advance inside one test call. All on the 16 x 8 x 16 stone arena ({@code simplefoundry:arena}).
 */
public final class FoundryTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();

    private static void check(GameTestHelper h, boolean ok, String message) {
        h.assertTrue(ok, message);
    }

    /** Shell of bricks (x 2..4, z 2..4, y 1..hgt+2) around a one-block-wide tank of {@code hgt} chambers at x 3, z 3; desk north, grate west. */
    private static BlockPos build(GameTestHelper h, int hgt) {
        ServerLevel level = h.getLevel();
        for (int x = 2; x <= 4; x++) {
            for (int z = 2; z <= 4; z++) {
                for (int y = 1; y <= hgt + 2; y++) {
                    boolean inside = x == 3 && z == 3 && y >= 2 && y <= hgt + 1;
                    level.setBlockAndUpdate(h.absolutePos(new BlockPos(x, y, z)), (inside ? FoundryRegistry.MELTING_CHAMBER : FoundryRegistry.FOUNDRY_BRICK).defaultBlockState());
                }
            }
        }
        BlockPos desk = h.absolutePos(new BlockPos(3, 2, 2));
        level.setBlockAndUpdate(desk, FoundryRegistry.DESK.defaultBlockState().setValue(DeskBlock.FACING, Direction.NORTH));
        level.setBlockAndUpdate(h.absolutePos(new BlockPos(2, 2, 3)), FoundryRegistry.FIRE_GRATE.defaultBlockState());
        return desk;
    }

    /** A spout at tank level {@code level} on the south wall with a tray in front of it. */
    private static BlockPos spout(GameTestHelper h, int tankLevel) {
        ServerLevel level = h.getLevel();
        level.setBlockAndUpdate(h.absolutePos(new BlockPos(3, 2 + tankLevel, 4)), FoundryRegistry.FOUNDRY_SPOUT.defaultBlockState().setValue(SpoutBlock.FACING, Direction.SOUTH));
        BlockPos tray = h.absolutePos(new BlockPos(3, 2 + tankLevel, 5));
        level.setBlockAndUpdate(tray, FoundryRegistry.CASTING_TRAY.defaultBlockState());
        return tray;
    }

    private static void clear(GameTestHelper h) {
        for (int x = 1; x <= 5; x++) for (int z = 1; z <= 5; z++) for (int y = 1; y <= 6; y++) {
            h.getLevel().setBlockAndUpdate(h.absolutePos(new BlockPos(x, y, z)), Blocks.AIR.defaultBlockState());
        }
    }

    private static void fast() {
        FoundryConfig.meltTicksPerUnit = 1;
        FoundryConfig.castTicks = 1;
    }

    private static void run(GameTestHelper h, BlockPos desk, int ticks) {
        ServerLevel level = h.getLevel();
        DeskBlockEntity be = (DeskBlockEntity) level.getBlockEntity(desk);
        for (int i = 0; i < ticks; i++) DeskBlockEntity.tick(level, desk, level.getBlockState(desk), be);
    }

    static {
        ALL.put("registered", h -> {
            FoundryRegistry.BLOCKS.keySet().forEach(p -> check(h, BuiltInRegistries.BLOCK.containsKey(SimpleFoundry.id(p)), "block " + p));
            FoundryRegistry.ITEMS.keySet().forEach(p -> check(h, BuiltInRegistries.ITEM.containsKey(SimpleFoundry.id(p)), "item " + p));
            check(h, FoundryRegistry.BLOCKS.size() == 6 && FoundryRegistry.ITEMS.size() == 6, "six blocks and items");
            check(h, FoundryRegistry.DESK_BE != null && FoundryRegistry.TRAY_BE != null, "block entity types");
            check(h, FoundryConfig.enabled && FoundryConfig.KEYS.equals(List.of("enabled", "maxChambers", "meltTicksPerUnit", "castTicks"))
                    && FoundryConfig.KEYS.size() == FoundryConfig.toJson().size(), "config defaults and keys");
            h.succeed();
        });
        ALL.put("config_clamps", h -> {
            try {
                var json = new com.google.gson.JsonObject();
                json.addProperty("maxChambers", 100000);
                json.addProperty("meltTicksPerUnit", 0);
                json.addProperty("castTicks", "x");
                FoundryConfig.apply(json);
                check(h, FoundryConfig.maxChambers == FoundryConfig.MAX_CHAMBERS_LIMIT, "chamber cap clamped to the hard limit");
                check(h, FoundryConfig.meltTicksPerUnit == 1 && FoundryConfig.castTicks == FoundryConfig.CAST_DEFAULT, "low value clamped, wrong type = default");
            } finally {
                FoundryConfig.reset();
            }
            h.succeed();
        });
        ALL.put("sub_mod_switch", h -> {
            boolean before = com.simplebuilding.framework.api.SubMods.enabled(SimpleFoundry.MOD_ID);
            try {
                check(h, SimpleFoundry.on(), "on by default");
                com.simplebuilding.framework.api.SubMods.set(SimpleFoundry.MOD_ID, false);
                check(h, !SimpleFoundry.on(), "off by the super-mod switch");
                check(h, BuiltInRegistries.BLOCK.containsKey(SimpleFoundry.id("foundry_desk")), "off: blocks still exist");
            } finally {
                com.simplebuilding.framework.api.SubMods.set(SimpleFoundry.MOD_ID, before);
                FoundryConfig.reset();
            }
            h.succeed();
        });
        ALL.put("layers", h -> {
            int cap = 2 * Metal.CHAMBER_UNITS;
            int[] gold = new int[3];
            gold[Metal.GOLD.ordinal()] = 81;
            check(h, Layers.at(gold, cap, 2, 0) == Metal.GOLD, "gold lies in the low level");
            check(h, Layers.at(gold, cap, 2, 1) == null, "nothing in the upper level");
            int[] mixed = new int[3];
            mixed[Metal.GOLD.ordinal()] = 300;
            mixed[Metal.IRON.ordinal()] = 100;
            check(h, Layers.at(mixed, cap, 2, 0) == Metal.GOLD, "gold only below its border");
            check(h, Layers.at(mixed, cap, 2, 1) == Metal.IRON, "the upper layer is drawn where two meet: iron in level 1 (243..486)");
            int[] copper = new int[3];
            copper[Metal.COPPER.ordinal()] = 100;
            copper[Metal.IRON.ordinal()] = 200;
            check(h, Layers.at(copper, cap, 2, 0) == Metal.IRON, "iron on top of copper reaches into level 0 (copper 0..100, iron 100..300): upper first");
            check(h, Layers.at(copper, 0, 2, 0) == null && Layers.at(copper, cap, 2, 5) == null && Layers.at(copper, cap, 0, 0) == null, "bad arguments give nothing");
            h.succeed();
        });
        ALL.put("tables", h -> {
            Melts.Melt rawIron = Melts.of(new ItemStack(Items.RAW_IRON));
            check(h, rawIron != null && rawIron.metal() == Metal.IRON && rawIron.units() == Metal.INGOT, "raw iron = one ingot (1:1, no ore bonus)");
            check(h, Melts.of(new ItemStack(Items.GOLD_NUGGET)).units() == 1 && Melts.of(new ItemStack(Items.COPPER_BLOCK.weathering().unaffected())).units() == 9 * Metal.INGOT, "nugget 1, block 81");
            check(h, Melts.of(new ItemStack(Items.STONE)) == null && Melts.of(ItemStack.EMPTY) == null, "stone does not melt");
            check(h, Metal.IRON.heat == Heat.HIGH && Metal.COPPER.heat == Heat.MEDIUM && Metal.GOLD.heat == Heat.MEDIUM, "heat per metal");
            check(h, Fuels.of(new ItemStack(Items.COAL)).heat() == Heat.MEDIUM && Fuels.of(new ItemStack(Items.COAL_BLOCK)).heat() == Heat.HIGH
                    && Fuels.of(new ItemStack(Items.OAK_LOG)).heat() == Heat.MEDIUM && Fuels.of(new ItemStack(Items.STICK)) == null, "fuel heat");
            check(h, !Heat.MEDIUM.atLeast(Heat.HIGH) && Heat.HIGH.atLeast(Heat.MEDIUM), "heat order");
            h.succeed();
        });
        ALL.put("scan", h -> {
            try {
                BlockPos desk = build(h, 2);
                spout(h, 0);
                FoundryScan.Result r = FoundryScan.scan(h.getLevel(), desk);
                check(h, r.valid() && r.chambers().size() == 2 && r.levels() == 2 && r.grates().size() == 1 && r.spouts().size() == 1, "valid foundry: " + r.reason());
                check(h, r.spouts().getFirst().level() == 0 && r.spouts().getFirst().facing() == Direction.SOUTH, "spout reads its tank level");
                // a hole in the hull
                BlockPos roof = h.absolutePos(new BlockPos(3, 4, 3));
                BlockState brick = h.getLevel().getBlockState(roof);
                h.getLevel().setBlockAndUpdate(roof, Blocks.AIR.defaultBlockState());
                check(h, "open_hull".equals(FoundryScan.scan(h.getLevel(), desk).reason()), "a hole in the roof is refused");
                h.getLevel().setBlockAndUpdate(roof, brick);
                // vanilla glass closes the hull
                BlockPos window = h.absolutePos(new BlockPos(4, 3, 3));
                h.getLevel().setBlockAndUpdate(window, Blocks.GLASS.defaultBlockState());
                check(h, FoundryScan.scan(h.getLevel(), desk).valid(), "glass is a window");
                // no grate
                h.getLevel().setBlockAndUpdate(h.absolutePos(new BlockPos(2, 2, 3)), FoundryRegistry.FOUNDRY_BRICK.defaultBlockState());
                check(h, "no_grate".equals(FoundryScan.scan(h.getLevel(), desk).reason()), "a foundry needs a fire grate");
                h.getLevel().setBlockAndUpdate(h.absolutePos(new BlockPos(2, 2, 3)), FoundryRegistry.FIRE_GRATE.defaultBlockState());
                // chamber cap
                FoundryConfig.maxChambers = 1;
                check(h, "too_big".equals(FoundryScan.scan(h.getLevel(), desk).reason()), "two chambers over a cap of one");
                FoundryConfig.maxChambers = 2;
                check(h, FoundryScan.scan(h.getLevel(), desk).valid(), "two chambers under a cap of two");
                // a second desk
                h.getLevel().setBlockAndUpdate(h.absolutePos(new BlockPos(4, 2, 3)), FoundryRegistry.DESK.defaultBlockState());
                check(h, "second_desk".equals(FoundryScan.scan(h.getLevel(), desk).reason()), "one desk per foundry");
                // no tank next to the desk
                clear(h);
                h.getLevel().setBlockAndUpdate(desk, FoundryRegistry.DESK.defaultBlockState());
                check(h, "no_chamber".equals(FoundryScan.scan(h.getLevel(), desk).reason()), "a lone desk has no tank");
            } finally {
                FoundryConfig.reset();
                clear(h);
            }
            h.succeed();
        });
        ALL.put("melt_and_pour", h -> {
            try {
                fast();
                BlockPos desk = build(h, 2);
                BlockPos low = spout(h, 0);
                BlockPos high = spout(h, 1);
                DeskBlockEntity be = (DeskBlockEntity) h.getLevel().getBlockEntity(desk);
                ItemStack raw = new ItemStack(Items.RAW_IRON, 9);
                check(h, be.insert(raw) == 9 && be.pending(Metal.IRON) == 81, "nine raw iron wait as 81 units");
                run(h, desk, 20);
                check(h, be.melt(Metal.IRON) == 0 && be.pending(Metal.IRON) == 81, "no fuel: nothing melts");
                check(h, be.addFuel(new ItemStack(Items.COAL, 4)) == 4, "coal accepted");
                run(h, desk, 20);
                check(h, be.pending(Metal.IRON) == 81 && !be.lit(), "coal is not hot enough for iron: still waiting, not burning");
                be.clearFuel();
                check(h, be.addFuel(new ItemStack(Items.COAL_BLOCK, 1)) == 1, "coal block accepted");
                run(h, desk, 4);
                check(h, be.lit() && h.getLevel().getBlockState(h.absolutePos(new BlockPos(2, 2, 3))).getValue(GrateBlock.LIT), "burning: grate is lit");
                run(h, desk, 300);
                TrayBlockEntity lowTray = (TrayBlockEntity) h.getLevel().getBlockEntity(low);
                TrayBlockEntity highTray = (TrayBlockEntity) h.getLevel().getBlockEntity(high);
                check(h, lowTray.metal() == Metal.IRON && lowTray.count() == 9, "nine iron ingots in the low tray, got " + lowTray.count());
                check(h, highTray.count() == 0, "the high spout draws nothing: the iron lies low");
                check(h, be.melt(Metal.IRON) == 0 && be.pending(Metal.IRON) == 0, "all melted and poured");
                check(h, !lowTray.take(h.getLevel(), low), "the ingots are still hot in the same tick");
                lowTray.drop(h.getLevel(), low);
                check(h, lowTray.count() == 0, "drop empties the tray");
            } finally {
                FoundryConfig.reset();
                clear(h);
            }
            h.succeed();
        });
        ALL.put("tiers", h -> {
            try {
                fast();
                check(h, Tier.STONE.slots == 3 && Tier.IRON.slots == 6 && Tier.DIAMOND.slots == 9 && Tier.NETHERITE.slots == 12, "SA3: 3/6/9/12 slots");
                check(h, Tier.STONE.next() == Tier.IRON && Tier.NETHERITE.next() == null && Tier.byIndex(99) == Tier.NETHERITE && Tier.byIndex(-1) == Tier.STONE, "tier order and clamping");
                BlockPos desk = build(h, 2);
                DeskBlockEntity be = (DeskBlockEntity) h.getLevel().getBlockEntity(desk);
                check(h, be.tier() == Tier.STONE, "a new desk is the first tier");
                ItemStack wrong = new ItemStack(Items.DIAMOND_BLOCK);
                check(h, !be.upgrade(wrong) && be.tier() == Tier.STONE && wrong.getCount() == 1, "a tier cannot be skipped");
                check(h, be.upgrade(new ItemStack(Items.IRON_BLOCK)) && be.tier() == Tier.IRON, "iron block lifts to tier 2");
                // parallel lanes: tier 1 melts 1 unit per interval, tier 2 two
                be.insert(new ItemStack(Items.GOLD_INGOT, 2));
                be.addFuel(new ItemStack(Items.COAL, 2));
                run(h, desk, 2);
                check(h, be.melt(Metal.GOLD) == 4 && be.pending(Metal.GOLD) == 14, "tier 2 melts two units per interval: " + be.melt(Metal.GOLD));
                var tag = be.saveWithoutMetadata(h.getLevel().registryAccess());
                check(h, tag.getIntOr("Tier", -1) == Tier.IRON.ordinal(), "tier is saved");
                check(h, be.upgrade(new ItemStack(Items.DIAMOND_BLOCK)) && be.upgrade(new ItemStack(Items.NETHERITE_INGOT)) && be.tier() == Tier.NETHERITE && !be.upgrade(new ItemStack(Items.NETHERITE_INGOT)), "up to tier 4, no further");
            } finally {
                FoundryConfig.reset();
                clear(h);
            }
            h.succeed();
        });
        ALL.put("layer_select", h -> {
            try {
                BlockPos desk = build(h, 3);
                DeskBlockEntity be = (DeskBlockEntity) h.getLevel().getBlockEntity(desk);
                be.revalidate();
                check(h, be.structure().levels() == 3 && be.selectedLevel() == 0, "three levels, bottom selected");
                check(h, be.cycleLevel() == 2 && be.cycleLevel() == 1 && be.cycleLevel() == 0 && be.cycleLevel() == 2, "SA4: the arrow goes down and wraps from the bottom to the top");
                check(h, be.selectedMetal() == null, "nothing melted: empty layer");
                be.insert(new ItemStack(Items.GOLD_BLOCK, 1));
                be.addFuel(new ItemStack(Items.COAL, 2));
                fast();
                run(h, desk, 100);
                while (be.selectedLevel() != 0) be.cycleLevel();
                check(h, be.selectedMetal() == Metal.GOLD, "the lowest layer shows the gold, got " + be.selectedMetal());
            } finally {
                FoundryConfig.reset();
                clear(h);
            }
            h.succeed();
        });
        ALL.put("gear_and_target", h -> {
            try {
                ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
                Melts.Melt whole = Melts.of(pick);
                check(h, whole != null && whole.metal() == Metal.IRON && whole.units() == 3 * Metal.INGOT, "SA7: an iron pickaxe is three ingots");
                pick.setDamageValue(pick.getMaxDamage() / 2);
                Melts.Melt worn = Melts.of(pick);
                check(h, worn != null && worn.units() < whole.units() && worn.units() > 0, "worn gear gives less");
                pick.setDamageValue(pick.getMaxDamage());
                check(h, Melts.of(pick) == null, "a broken piece gives nothing");
                check(h, Melts.of(new ItemStack(Items.GOLDEN_CHESTPLATE)).units() == 8 * Metal.INGOT && Melts.of(new ItemStack(Items.GOLDEN_CHESTPLATE)).metal() == Metal.GOLD, "golden chestplate = eight gold ingots");
                check(h, Melts.of(new ItemStack(Items.DIAMOND_PICKAXE)) == null && Melts.of(new ItemStack(Items.NETHERITE_SWORD)) == null, "no melting of diamond or netherite gear yet");
                // the yield of every piece is at most its cost: a fresh piece gives exactly the ingots, never more
                for (var item : List.of(Items.IRON_SWORD, Items.IRON_HELMET, Items.GOLDEN_BOOTS, Items.GOLDEN_HOE, Items.IRON_LEGGINGS)) {
                    Melts.Melt m = Melts.of(new ItemStack(item));
                    check(h, m != null && m.units() % Metal.INGOT == 0, "whole ingots for " + item);
                }
                // SA5 = C: a spout pours into anything that is a cast target
                check(h, CastTarget.class.isAssignableFrom(TrayBlockEntity.class), "the tray is a cast target");
            } finally {
                clear(h);
            }
            h.succeed();
        });
        ALL.put("fuel_tank", h -> {
            try {
                BlockPos desk = build(h, 2);
                DeskBlockEntity be = (DeskBlockEntity) h.getLevel().getBlockEntity(desk);
                check(h, be.fuelTotal() == 0 && be.fuelLevel() == 0 && h.getLevel().getBlockState(desk).getValue(DeskBlock.FUEL) == 0, "empty tank, gauge 0");
                check(h, be.addFuel(new ItemStack(Items.COAL, 3)) == 3 && be.fuelTotal() == 4800 && be.fuelLevel() == 1, "coal fills the tank, first gauge step");
                check(h, h.getLevel().getBlockState(desk).getValue(DeskBlock.FUEL) == 1, "the gauge is written into the desk block");
                check(h, be.addFuel(new ItemStack(Items.LAVA_BUCKET, 1)) == 1 && be.fuelTotal() == 24800 && be.fuelLevel() == 3, "lava fills 20000 ticks, gauge 3");
                check(h, h.getLevel().getBlockState(desk).getValue(DeskBlock.FUEL) == 3, "block state follows");
                check(h, be.addFuel(new ItemStack(Items.LAVA_BUCKET, 1)) == 0, "a second bucket does not fit (24800 + 20000 > 40000)");
                check(h, be.addFuel(new ItemStack(Items.COAL_BLOCK, 1)) == 0 && be.addFuel(new ItemStack(Items.COAL, 9)) == 9, "block refused, nine coal fit (15200 left)");
                check(h, be.fuelLevel() == 4 && be.fuelTotal() == 39200, "nearly full = gauge 4");
                var tag = be.saveWithoutMetadata(h.getLevel().registryAccess());
                check(h, tag.getIntOr("FuelHigh", 0) == 20000 && tag.getIntOr("FuelMedium", 0) == 19200, "tank is written in two heat parts");
                be.clearFuel();
                check(h, be.fuelLevel() == 0 && h.getLevel().getBlockState(desk).getValue(DeskBlock.FUEL) == 0, "emptied");
                // main way: right click with the lava bucket pours it in and hands the empty bucket back
                ServerPlayer player = h.makeMockServerPlayerInLevel();
                player.setGameMode(GameType.SURVIVAL);
                ItemStack lava = new ItemStack(Items.LAVA_BUCKET);
                player.setItemInHand(InteractionHand.MAIN_HAND, lava);
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(desk), Direction.UP, desk, false);
                player.gameMode.useItemOn(player, h.getLevel(), lava, InteractionHand.MAIN_HAND, hit);
                check(h, be.fuelTotal() == 20000, "lava bucket poured into the tank: " + be.fuelTotal());
                check(h, player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET), "the empty bucket comes back");
                // side way: coal from the hand
                ItemStack coal = new ItemStack(Items.COAL, 2);
                player.setItemInHand(InteractionHand.MAIN_HAND, coal);
                player.gameMode.useItemOn(player, h.getLevel(), coal, InteractionHand.MAIN_HAND, hit);
                check(h, be.fuelTotal() == 23200 && coal.getCount() == 0, "coal poured in: " + be.fuelTotal());
            } finally {
                FoundryConfig.reset();
                clear(h);
            }
            h.succeed();
        });
        ALL.put("fuel_burn", h -> {
            try {
                fast();
                BlockPos desk = build(h, 2);
                DeskBlockEntity be = (DeskBlockEntity) h.getLevel().getBlockEntity(desk);
                be.insert(new ItemStack(Items.GOLD_INGOT, 4));
                be.addFuel(new ItemStack(Items.COAL, 2));
                int before = be.fuelTotal();
                run(h, desk, 10);
                check(h, be.lit() && be.fuelTotal() < before && be.fuelTotal() > 0, "coal burns out of the tank step by step, total " + be.fuelTotal());
                run(h, desk, 40);
                check(h, be.melt(Metal.GOLD) == 36, "four gold ingots melted with medium heat");
                check(h, h.getLevel().getBlockState(desk).getValue(DeskBlock.FUEL) == be.fuelLevel(), "gauge stays in step");
                // iron needs the high part: coal alone leaves it waiting, lava starts it
                be.insert(new ItemStack(Items.IRON_INGOT, 2));
                run(h, desk, 10);
                check(h, be.pending(Metal.IRON) == 18, "medium fuel cannot melt iron");
                be.addFuel(new ItemStack(Items.LAVA_BUCKET, 1));
                run(h, desk, 20);
                check(h, be.pending(Metal.IRON) == 0 && be.melt(Metal.IRON) == 18, "lava melts the iron: " + be.melt(Metal.IRON));
            } finally {
                FoundryConfig.reset();
                clear(h);
            }
            h.succeed();
        });
        ALL.put("layer_view", h -> {
            try {
                fast();
                BlockPos desk = build(h, 3);
                DeskBlockEntity be = (DeskBlockEntity) h.getLevel().getBlockEntity(desk);
                // pure data: 729 units, 3 levels of 243; 81 gold and 162 iron lie at 0..81 and 81..243
                int[] units = {81, 0, 162};
                check(h, Layers.atUnit(units, 0) == Metal.GOLD && Layers.atUnit(units, 80) == Metal.GOLD && Layers.atUnit(units, 81) == Metal.IRON
                        && Layers.atUnit(units, 242) == Metal.IRON && Layers.atUnit(units, 243) == null, "atUnit stacks bottom to top");
                check(h, Layers.windowPart(units, 729, 3, 0, 0, 4) == Metal.GOLD && Layers.windowPart(units, 729, 3, 0, 1, 4) == Metal.IRON
                        && Layers.windowPart(units, 729, 3, 0, 3, 4) == Metal.IRON && Layers.windowPart(units, 729, 3, 1, 0, 4) == null, "window parts of level 0");
                // in the world: melt, then the chambers show it
                be.insert(new ItemStack(Items.GOLD_BLOCK, 1));
                be.insert(new ItemStack(Items.RAW_COPPER_BLOCK, 1));
                be.addFuel(new ItemStack(Items.COAL, 5));
                run(h, desk, 100);
                check(h, be.melt(Metal.GOLD) == 81 && be.melt(Metal.COPPER) == 81, "melted: " + be.melt(Metal.GOLD) + "/" + be.melt(Metal.COPPER));
                be.refreshChambers(h.getLevel());
                BlockState low = h.getLevel().getBlockState(h.absolutePos(new BlockPos(3, 2, 3)));
                BlockState mid = h.getLevel().getBlockState(h.absolutePos(new BlockPos(3, 3, 3)));
                BlockState top = h.getLevel().getBlockState(h.absolutePos(new BlockPos(3, 4, 3)));
                check(h, low.getValue(ChamberBlock.S0) == ChamberBlock.Fill.GOLD && low.getValue(ChamberBlock.S1) == ChamberBlock.Fill.COPPER
                        && low.getValue(ChamberBlock.S2) == ChamberBlock.Fill.COPPER && low.getValue(ChamberBlock.S3) == ChamberBlock.Fill.NONE, "bottom chamber: gold at the very bottom, copper above it, empty on top: " + low);
                check(h, mid.getValue(ChamberBlock.S0) == ChamberBlock.Fill.NONE && top.getValue(ChamberBlock.S0) == ChamberBlock.Fill.NONE, "the upper chambers stay empty");
                // the window data the menu draws
                ServerPlayer player = h.makeMockServerPlayerInLevel();
                FoundryMenu menu = new FoundryMenu(7, player.getInventory(), be);
                check(h, menu.capacity() == 729 && menu.levels() == 3 && menu.melt()[Metal.GOLD.ordinal()] == 81 && menu.melt()[Metal.COPPER.ordinal()] == 81
                        && menu.status().equals("ok") && menu.fuel() > 0, "menu data: capacity, levels, melts, status, fuel");
                int selected = menu.selected();
                check(h, menu.clickMenuButton(player, FoundryMenu.ARROW_BUTTON) && menu.selected() != selected, "arrow button moves the layer");
                check(h, menu.clickMenuButton(player, FoundryMenu.LEVEL_BUTTON + 1) && be.selectedLevel() == 1, "a click on a level selects it");
                // emptying the tank clears the display
                be.clearFuel();
                be.clearMelt();
                be.refreshChambers(h.getLevel());
                check(h, h.getLevel().getBlockState(h.absolutePos(new BlockPos(3, 2, 3))).getValue(ChamberBlock.S0) == ChamberBlock.Fill.NONE, "empty tank shows no melt");
            } finally {
                FoundryConfig.reset();
                clear(h);
            }
            h.succeed();
        });
        ALL.put("window_slots", h -> {
            try {
                fast();
                BlockPos desk = build(h, 2);
                DeskBlockEntity be = (DeskBlockEntity) h.getLevel().getBlockEntity(desk);
                be.revalidate();
                be.items.setItem(0, new ItemStack(Items.GOLD_INGOT, 3));
                be.items.setItem(5, new ItemStack(Items.GOLD_INGOT, 3));
                be.items.setItem(DeskBlockEntity.FUEL_SLOT, new ItemStack(Items.LAVA_BUCKET));
                run(h, desk, 60);
                check(h, be.items.getItem(0).isEmpty(), "the first place fed the melt");
                check(h, be.items.getItem(5).getCount() == 3, "place 6 is locked on the first tier");
                check(h, be.items.getItem(DeskBlockEntity.FUEL_SLOT).is(Items.BUCKET) && be.fuelTotal() > 0, "the fuel slot poured the lava and holds the empty bucket");
                check(h, be.melt(Metal.GOLD) == 27, "three ingots melted: " + be.melt(Metal.GOLD));
                be.upgrade(new ItemStack(Items.IRON_BLOCK));
                run(h, desk, 60);
                check(h, be.items.getItem(5).isEmpty() && be.melt(Metal.GOLD) == 54, "after the upgrade place 6 works: " + be.melt(Metal.GOLD));
                var tag = be.saveWithoutMetadata(h.getLevel().registryAccess());
                check(h, tag.contains("Slot" + DeskBlockEntity.FUEL_SLOT), "slots are saved");
                ServerPlayer player = h.makeMockServerPlayerInLevel();
                FoundryMenu menu = new FoundryMenu(3, player.getInventory(), be);
                check(h, menu.slots.size() == 13 + 36, "13 desk slots and the player inventory");
                check(h, menu.slots.get(2).isActive() && menu.slots.get(5).isActive() && !menu.slots.get(6).isActive(), "tier 2: six places active");
                check(h, !menu.slots.get(6).mayPlace(new ItemStack(Items.IRON_INGOT)) && menu.slots.get(5).mayPlace(new ItemStack(Items.IRON_INGOT)) && !menu.slots.get(5).mayPlace(new ItemStack(Items.DIRT)), "places take only meltable items");
                check(h, menu.slots.get(DeskBlockEntity.FUEL_SLOT).mayPlace(new ItemStack(Items.COAL)) && !menu.slots.get(DeskBlockEntity.FUEL_SLOT).mayPlace(new ItemStack(Items.DIRT)), "fuel slot takes fuel only");
                be.dropContents(h.getLevel(), desk);
                check(h, be.items.isEmpty(), "breaking the desk empties the slots");
            } finally {
                FoundryConfig.reset();
                clear(h);
            }
            h.succeed();
        });
        ALL.put("capacity_and_save", h -> {
            try {
                BlockPos desk = build(h, 1);
                DeskBlockEntity be = (DeskBlockEntity) h.getLevel().getBlockEntity(desk);
                check(h, be.insert(new ItemStack(Items.IRON_BLOCK, 64)) == 3 && be.used() == 3 * 81, "one chamber holds 27 ingots = three blocks of 81 units, took " + be.used());
                check(h, be.insert(new ItemStack(Items.IRON_INGOT, 1)) == 0, "full tank refuses more");
                check(h, be.capacity() == Metal.CHAMBER_UNITS, "capacity of one chamber");
                be.addFuel(new ItemStack(Items.COAL, 3));
                var tag = be.saveWithoutMetadata(h.getLevel().registryAccess());
                check(h, tag.getIntOr("Pending_iron", 0) == 243 && tag.getIntOr("FuelMedium", 0) == 4800, "state is written");
            } finally {
                clear(h);
            }
            h.succeed();
        });
    }

    private FoundryTests() {}
}
