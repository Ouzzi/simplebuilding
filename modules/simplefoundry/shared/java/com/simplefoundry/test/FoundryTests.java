package com.simplefoundry.test;

import com.simplefoundry.FoundryRegistry;
import com.simplefoundry.FoundryScan;
import com.simplefoundry.Fuels;
import com.simplefoundry.Heat;
import com.simplefoundry.Layers;
import com.simplefoundry.Melts;
import com.simplefoundry.Metal;
import com.simplefoundry.SimpleFoundry;
import com.simplefoundry.block.DeskBlock;
import com.simplefoundry.block.DeskBlockEntity;
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
            check(h, Melts.of(new ItemStack(Items.GOLD_NUGGET)).units() == 1 && Melts.of(new ItemStack(Items.COPPER_BLOCK)).units() == 9 * Metal.INGOT, "nugget 1, block 81");
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
                be.fuel().shrink(4);
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
        ALL.put("capacity_and_save", h -> {
            try {
                BlockPos desk = build(h, 1);
                DeskBlockEntity be = (DeskBlockEntity) h.getLevel().getBlockEntity(desk);
                check(h, be.insert(new ItemStack(Items.IRON_BLOCK, 64)) == 3 && be.used() == 3 * 81, "one chamber holds 27 ingots = three blocks of 81 units, took " + be.used());
                check(h, be.insert(new ItemStack(Items.IRON_INGOT, 1)) == 0, "full tank refuses more");
                check(h, be.capacity() == Metal.CHAMBER_UNITS, "capacity of one chamber");
                be.addFuel(new ItemStack(Items.COAL, 3));
                var tag = be.saveWithoutMetadata(h.getLevel().registryAccess());
                check(h, tag.getIntOr("Pending_iron", 0) == 243 && tag.contains("Fuel"), "state is written");
            } finally {
                clear(h);
            }
            h.succeed();
        });
    }

    private FoundryTests() {}
}
