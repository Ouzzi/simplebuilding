package com.simplebuilding.modules.simpletools.strip;

import com.simplebuilding.modules.simpletools.SimpleTools;
import com.simplebuilding.modules.simpletools.ToolsConfig;
import com.simplebuilding.modules.simpletools.ToolsRegistry;
import com.simplelib.api.AxeStrips;
import com.simplelib.test.PartnerChecks;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** IG31-59 strip station GameTests; Fabric wraps them in ModuleGameTest, NeoForge/Forge register them via ToolsTests.ALL. */
public final class StripTests {
    private static final BlockPos CAGE = new BlockPos(2, 2, 2);
    /** Test-only extra axe way (stone -> smooth stone), active only while a test runs: stands for the wood of a mod. */
    private static boolean modWay;
    private static boolean modWayRegistered;

    private StripTests() {}

    public static void addAll(Map<String, Consumer<GameTestHelper>> all) {
        all.put("strip_station_registered_with_recipe", StripTests::registeredWithRecipe);
        all.put("strip_station_takes_a_whole_stack_and_gives_it_back", StripTests::takesAndGives);
        all.put("strip_station_strips_a_whole_stack_for_one_durability_each", StripTests::stripsWholeStack);
        all.put("strip_station_copper_unwaxes_then_scrapes_step_by_step", StripTests::copperSteps);
        all.put("strip_station_axe_stays_at_one_durability", StripTests::durabilityLimit);
        all.put("strip_station_hoppers_fill_from_above_and_empty_below", StripTests::hoppers);
        all.put("strip_station_options_switch_it_off", StripTests::switchedOff);
        all.put("strip_station_works_wood_of_other_mods", StripTests::otherMods);
    }

    // ------------------------------------------------------------ helpers

    /** A survival player (mock players are creative; without instabuild the axe wears) with the stack in the main hand. */
    private static ServerPlayer player(GameTestHelper h, ItemStack main) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.getAbilities().instabuild = false;
        Vec3 at = h.absoluteVec(new Vec3(2.5, 2.0, 0.5));
        p.setPos(at.x, at.y, at.z);
        p.setItemInHand(InteractionHand.MAIN_HAND, main);
        p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        return p;
    }

    private static StripStationBlockEntity cage(GameTestHelper h) {
        h.setBlock(CAGE, ToolsRegistry.STRIP_STATION);
        return (StripStationBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(CAGE));
    }

    private static void click(GameTestHelper h, ServerPlayer p) {
        BlockPos abs = h.absolutePos(CAGE);
        p.gameMode.useItemOn(p, h.getLevel(), p.getMainHandItem(), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
    }

    private static void withConfig(GameTestHelper h, Consumer<ToolsConfig> change, Runnable body) {
        ToolsConfig old = SimpleTools.CONFIG;
        try {
            SimpleTools.CONFIG = new ToolsConfig();
            change.accept(SimpleTools.CONFIG);
            body.run();
        } finally {
            SimpleTools.CONFIG = old;
        }
    }

    private static ItemStack axe(Item item, int damage) {
        ItemStack axe = new ItemStack(item);
        axe.setDamageValue(damage);
        return axe;
    }

    // ------------------------------------------------------------ tests

    private static void registeredWithRecipe(GameTestHelper h) {
        Identifier id = ToolsRegistry.STRIP_STATION_ID;
        h.assertTrue(BuiltInRegistries.BLOCK.getOptional(id).isPresent() && BuiltInRegistries.ITEM.getOptional(id).isPresent()
                && BuiltInRegistries.BLOCK_ENTITY_TYPE.getOptional(id).isPresent(), "block, item and block entity are registered");
        ItemStack planks = new ItemStack(Items.OAK_PLANKS), bars = new ItemStack(Items.IRON_BARS);
        CraftingInput grid = CraftingInput.of(3, 3, List.of(planks, bars, planks, bars, new ItemStack(Items.IRON_AXE), bars, planks, bars, planks));
        ItemStack made = h.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, h.getLevel())
                .map(r -> r.value().assemble(grid)).orElse(ItemStack.EMPTY);
        h.assertTrue(made.is(ToolsRegistry.STRIP_STATION_ITEM), "4 iron bars and 4 planks around an iron axe make the station, got " + made);
        h.assertTrue(h.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, id)).isPresent(), "recipe id " + id);
        h.succeed();
    }

    private static void takesAndGives(GameTestHelper h) {
        var cage = cage(h);
        ServerPlayer p = player(h, new ItemStack(Items.OAK_LOG, 64));
        click(h, p);
        h.assertTrue(cage.input().is(Items.OAK_LOG) && cage.input().getCount() == 64 && p.getMainHandItem().isEmpty(), "the whole stack went in: " + cage.input());
        click(h, p);
        h.assertTrue(cage.isEmpty() && p.getInventory().countItem(Items.OAK_LOG) == 64, "empty hand takes the stack out again: " + p.getInventory().countItem(Items.OAK_LOG));
        // something else is refused while the cage holds a different item
        cage.insert(new ItemStack(Items.OAK_LOG, 3));
        ItemStack birch = new ItemStack(Items.BIRCH_LOG, 5);
        h.assertTrue(cage.insert(birch) == 0 && birch.getCount() == 5, "a different item does not go in");
        h.assertTrue(cage.insert(new ItemStack(Items.STONE, 1)) == 0, "stone has no axe work, so it does not go in");
        h.succeed();
    }

    private static void stripsWholeStack(GameTestHelper h) {
        var cage = cage(h);
        cage.insert(new ItemStack(Items.OAK_LOG, 64));
        ServerPlayer p = player(h, axe(Items.IRON_AXE, 0));
        click(h, p);
        h.assertTrue(cage.input().isEmpty() && cage.output().is(Items.STRIPPED_OAK_LOG) && cage.output().getCount() == 64, "one click strips all 64: " + cage.output());
        h.assertTrue(p.getMainHandItem().getDamageValue() == 64, "1 durability per block, was " + p.getMainHandItem().getDamageValue());
        // the stripped logs are done: another click does nothing and costs nothing
        click(h, p);
        h.assertTrue(p.getMainHandItem().getDamageValue() == 64 && cage.output().getCount() == 64, "nothing more to strip");
        h.succeed();
    }

    private static void copperSteps(GameTestHelper h) {
        var cage = cage(h);
        cage.insert(new ItemStack(Items.COPPER_BLOCK.waxed().oxidized(), 10));
        ServerPlayer p = player(h, axe(Items.DIAMOND_AXE, 0));
        var copper = Items.COPPER_BLOCK.weathering();
        Item[] expected = {copper.oxidized(), copper.weathered(), copper.exposed(), copper.unaffected()};
        String[] names = {"wax off first", "then oxidized to weathered", "weathered to exposed", "exposed to plain"};
        for (int i = 0; i < expected.length; i++) {
            click(h, p);
            h.assertTrue(cage.output().is(expected[i]) && cage.output().getCount() == 10 && cage.input().isEmpty(), names[i] + ": " + cage.output());
        }
        click(h, p);
        h.assertTrue(cage.output().is(copper.unaffected()) && p.getMainHandItem().getDamageValue() == 40, "plain copper has no further step, damage " + p.getMainHandItem().getDamageValue());
        h.succeed();
    }

    private static void durabilityLimit(GameTestHelper h) {
        var cage = cage(h);
        cage.insert(new ItemStack(Items.OAK_LOG, 20));
        int max = new ItemStack(Items.IRON_AXE).getMaxDamage();
        ServerPlayer p = player(h, axe(Items.IRON_AXE, max - 10));
        click(h, p);
        ItemStack held = p.getMainHandItem();
        h.assertTrue(cage.output().getCount() == 9 && cage.input().getCount() == 11, "only 9 can be paid: out " + cage.output().getCount() + ", in " + cage.input().getCount());
        h.assertTrue(held.is(Items.IRON_AXE) && held.getDamageValue() == max - 1, "the axe is left at 1 durability, damage " + held.getDamageValue());
        click(h, p);
        h.assertTrue(p.getMainHandItem().is(Items.IRON_AXE) && p.getMainHandItem().getDamageValue() == max - 1 && cage.output().getCount() == 9, "a worn axe works nothing and does not break");
        h.succeed();
    }

    private static void hoppers(GameTestHelper h) {
        var cage = cage(h);
        BlockPos above = CAGE.above();
        h.setBlock(above, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        ((HopperBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(above))).setItem(0, new ItemStack(Items.OAK_LOG, 3));
        h.runAfterDelay(25, () -> {
            h.assertTrue(cage.input().is(Items.OAK_LOG) && cage.input().getCount() >= 1, "a hopper above fills the cage: " + cage.input());
            h.setBlock(above, Blocks.AIR);
            // below: only the worked blocks leave, never the input
            cage.setItem(StripStationBlockEntity.INPUT, new ItemStack(Items.BIRCH_LOG, 4));
            cage.setItem(StripStationBlockEntity.OUTPUT, new ItemStack(Items.STRIPPED_OAK_LOG, 3));
            BlockPos below = CAGE.below();
            h.setBlock(below, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
            h.runAfterDelay(25, () -> {
                var hopper = (HopperBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(below));
                int taken = 0;
                for (int i = 0; i < hopper.getContainerSize(); i++) {
                    ItemStack s = hopper.getItem(i);
                    h.assertTrue(s.isEmpty() || s.is(Items.STRIPPED_OAK_LOG), "the hopper below got only worked blocks, not " + s);
                    taken += s.getCount();
                }
                h.assertTrue(taken >= 1 && cage.input().is(Items.BIRCH_LOG) && cage.input().getCount() == 4, "worked blocks left, the input stayed: " + taken);
                h.succeed();
            });
        });
    }

    private static void switchedOff(GameTestHelper h) {
        var cage = cage(h);
        withConfig(h, c -> c.enableStripStation = false, () -> {
            ServerPlayer p = player(h, new ItemStack(Items.OAK_LOG, 8));
            click(h, p);
            h.assertTrue(cage.isEmpty(), "switched off: nothing goes in");
        });
        withConfig(h, c -> c.stripStationHopper = false, () -> {
            h.assertTrue(cage.getSlotsForFace(Direction.UP).length == 0 && !cage.canPlaceItemThroughFace(0, new ItemStack(Items.OAK_LOG), Direction.UP),
                    "hoppers option off: the cage has no sided slots");
        });
        h.assertTrue(cage.getSlotsForFace(Direction.UP).length == 1 && cage.getSlotsForFace(Direction.DOWN).length == 1 && cage.getSlotsForFace(Direction.NORTH).length == 0,
                "default: top fills, bottom empties, sides closed");
        h.succeed();
    }

    private static void otherMods(GameTestHelper h) {
        // The hook mods use for their own wood (Simple End registers its logs through it).
        if (!modWayRegistered) {
            modWayRegistered = true;
            AxeStrips.register(state -> modWay && state.is(Blocks.STONE) ? Blocks.SMOOTH_STONE.defaultBlockState() : null);
        }
        modWay = true;
        try {
            var cage = cage(h);
            h.assertTrue(cage.insert(new ItemStack(Items.STONE, 12)) == 12, "a block with an extra axe way goes in");
            ServerPlayer p = player(h, axe(Items.IRON_AXE, 0));
            click(h, p);
            h.assertTrue(cage.output().is(Items.SMOOTH_STONE) && cage.output().getCount() == 12, "worked through the hook: " + cage.output());
        } finally {
            modWay = false;
        }
        // Simple End's own wood, when it is loaded
        if (PartnerChecks.isModLoaded("simpleend")) {
            Item log = BuiltInRegistries.ITEM.keySet().stream().filter(i -> i.getNamespace().equals("simpleend") && i.getPath().endsWith("_log") && !i.getPath().startsWith("stripped_"))
                    .map(BuiltInRegistries.ITEM::getValue).findFirst().orElse(null);
            h.assertTrue(log != null, "Simple End has a log");
            StripWork.Step step = StripWork.step(new ItemStack(Items.IRON_AXE), new ItemStack(log));
            h.assertTrue(step != null && step.result().getItem().toString().contains("stripped_"), "an End log strips in the station: " + step);
        }
        h.succeed();
    }
}
