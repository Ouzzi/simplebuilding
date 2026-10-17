package com.simplebuilding.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplebuilding.crucible.CrucibleCompat;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.util.SledgehammerUpgrades;
import com.simplebuilding.version.McVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Crucible P5 in SimpleBuilding (docs/ai/PLAN-CRUCIBLE-P5-2026-10-05.md): the Enderite tiers, the sledgehammer ways,
 * the cauldron/crucible JEI catalog, the milk cauldron's Jade lines and the quartz crush. Soul lava, the buckets, the
 * crucible JEI category and Jade pages are Simple Crucibles' (split stage 5b, its tests in com.simplecrucibles.test).
 * On lines without {@link McVersion#CRUCIBLE} every test passes at once.
 */
public final class CrucibleTests {
    private CrucibleTests() {
    }



    /**
     * Jade for the milk cauldron (content, ripeness, what to do), computed from the block state alone and found by
     * registry id. Expected strings are this test's own. The reinforced cauldron has Simple Crucibles' plugin (5b).
     */
    public static void milkAndReinforcedCauldronsShowTheirContents(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        Block milk = BuiltInRegistries.BLOCK.getValue(Identifier.parse("simplesandwiches:milk_cauldron"));
        // The SimpleBuilding test run does not load SimpleSandwiches: the milk states are only checked where the module is present.
        if (milk != Blocks.AIR) milkCauldron(helper, milk);
        reinforcedCauldron(helper);
        helper.succeed();
    }

    private static void milkCauldron(GameTestHelper helper, Block milk) {
        String content = "jade.simplebuilding.cauldron.content";
        cauldron(helper, milk, new String[] {"content", "milk", "stage", "0"},
                java.util.List.of(content + " [" + content + ".milk]", "jade.simplebuilding.cauldron.ripeness [1, 4]"));
        cauldron(helper, milk, new String[] {"content", "milk", "stage", "2"},
                java.util.List.of(content + " [" + content + ".milk]", "jade.simplebuilding.cauldron.ripeness [3, 4]"));
        cauldron(helper, milk, new String[] {"content", "butter", "stage", "0"},
                java.util.List.of(content + " [" + content + ".butter]", "jade.simplebuilding.cauldron.ready []"));
        cauldron(helper, milk, new String[] {"content", "cheese", "stage", "0"},
                java.util.List.of(content + " [" + content + ".cheese]", "jade.simplebuilding.cauldron.ready []"));
    }

    /** Split stage 5b: the reinforced cauldron's Jade lines are Simple Crucibles' own plugin; SimpleBuilding's topic leaves it alone. */
    private static void reinforcedCauldron(GameTestHelper helper) {
        Block reinforced = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("simplecrucibles", "reinforced_cauldron"));
        cauldron(helper, reinforced, new String[] {"content", "water"}, java.util.List.of());
        cauldron(helper, Blocks.CAULDRON, new String[0], java.util.List.of());
        cauldron(helper, Blocks.WATER_CAULDRON, new String[0], java.util.List.of());
        helper.assertTrue(com.simplebuilding.compat.BlockInfo.isModCauldron("simplesandwiches:milk_cauldron")
                && !com.simplebuilding.compat.BlockInfo.isModCauldron("simplecrucibles:reinforced_cauldron")
                && !com.simplebuilding.compat.BlockInfo.isModCauldron("minecraft:cauldron"), "Jade's fluid line is replaced for the milk cauldron only");
    }

    private static void cauldron(GameTestHelper helper, Block block, String[] properties, java.util.List<String> expected) {
        BlockState state = block.defaultBlockState();
        for (int i = 0; i < properties.length; i += 2) {
            state = withProperty(state, properties[i], properties[i + 1]);
        }
        java.util.List<String> actual = new java.util.ArrayList<>();
        for (var line : com.simplebuilding.compat.BlockInfo.stateLines(com.simplebuilding.compat.BlockInfo.Topic.CAULDRON, state)) {
            actual.add(line.key() + " " + line.argTexts());
        }
        helper.assertTrue(actual.equals(expected), java.util.Arrays.toString(properties) + " on " + BuiltInRegistries.BLOCK.getKey(block)
                + ": expected " + expected + ", was " + actual);
    }

    private static BlockState withProperty(BlockState state, String name, String value) {
        net.minecraft.world.level.block.state.properties.Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        return parsed(state, property, value);
    }

    private static <T extends Comparable<T>> BlockState parsed(BlockState state, net.minecraft.world.level.block.state.properties.Property<T> property, String value) {
        return state.setValue(property, property.getValue(value).orElseThrow());
    }

    /**
     * The JEI category "Cauldron and crucible": the crucible build, the barrel, the cauldron upgrade (which the machine
     * upgrade category does not list) and butter and cheese. Numbers are this test's own: 6 strikes, 4 of them walls,
     * 6 attach strikes, 9 barrel fields, 8 cracked diamonds.
     */
    public static void cauldronWorldCatalogMatchesTheRules(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        var entries = new java.util.LinkedHashMap<String, com.simplebuilding.compat.CauldronWorldCatalog.Entry>();
        com.simplebuilding.compat.CauldronWorldCatalog.entries().forEach(entry -> entries.put(entry.id(), entry));
        boolean sandwiches = item("simplesandwiches:butter_block") != Items.AIR && item("simplesandwiches:cheese_block") != Items.AIR;
        var expected = new java.util.HashSet<>(java.util.Set.of("crucible_build", "barrel_attach", "cauldron_reinforce",
                "hammer_upgrade/simplecrucibles:iron_crucible", "hammer_upgrade/simplecrucibles:reinforced_crucible", "hammer_upgrade/simplecrucibles:netherite_crucible",
                "hammer_upgrade/simplecrucibles:copper_barrel", "hammer_upgrade/simplecrucibles:reinforced_barrel", "hammer_upgrade/simplecrucibles:netherite_barrel"));
        if (sandwiches) expected.addAll(java.util.Set.of("milk_butter", "milk_cheese"));
        helper.assertTrue(entries.keySet().equals(expected), "catalog entries " + entries.keySet());
        // 2026-10-06: the crucible/barrel hammer upgrades are in JEI too (2 material, from the upgrade table).
        var ironUp = entries.get("hammer_upgrade/simplecrucibles:iron_crucible");
        helper.assertTrue(ironUp.inputs().get(0).items().equals(java.util.List.of(item("simplecrucibles:iron_crucible")))
                && ironUp.inputs().get(1).items().equals(java.util.List.of(ModItems.CRACKED_DIAMOND)) && ironUp.inputs().get(1).count() == 4
                && ironUp.output().items().equals(java.util.List.of(item("simplecrucibles:reinforced_crucible"))), "iron -> reinforced crucible entry");

        var build = entries.get("crucible_build");
        helper.assertTrue(build.inputs().get(0).items().equals(java.util.List.of(Items.CAULDRON)) && build.inputs().get(0).count() == 1, "an empty cauldron (N51)");
        helper.assertTrue(build.inputs().get(1).count() == 12 && build.inputs().get(1).items().contains(Items.IRON_INGOT), "12 ingots for plates and rim (N51)");
        helper.assertTrue(build.inputs().get(2).count() == 4 && build.inputs().get(2).items().contains(Items.IRON_INGOT)
                && build.inputs().get(2).items().contains(ModItems.IRON_POLE), "4 handle strikes: iron ingot or iron rod");
        helper.assertTrue(build.output().items().equals(java.util.List.of(item("simplecrucibles:iron_crucible"))), "the iron crucible");
        helper.assertTrue(build.tools().contains(ModItems.IRON_SLEDGEHAMMER) && build.tools().contains(ModItems.ENDERITE_SLEDGEHAMMER), "every sledgehammer");
        helper.assertTrue(noteArgs(build, 0).equals(java.util.List.of(16)) && noteArgs(build, 1).equals(java.util.List.of(12, 4)), "notes name 16 strikes, 12 plate/rim and 4 handle ingots");

        var barrel = entries.get("barrel_attach");
        helper.assertTrue(barrel.inputs().get(0).items().contains(item("simplecrucibles:copper_barrel"))
                && barrel.inputs().get(0).items().contains(item("simplecrucibles:enderite_barrel")), "barrels");
        helper.assertTrue(barrel.inputs().get(1).items().contains(item("simplecrucibles:iron_crucible")), "next to a crucible");
        helper.assertTrue(noteArgs(barrel, 0).equals(java.util.List.of(6)) && noteArgs(barrel, 1).equals(java.util.List.of("6/12/18/30")), "notes name 6 strikes, 6/12/18/30 fields (N12c: as many as the crucible; enderite 30 since N37)");

        var cauldron = entries.get("cauldron_reinforce");
        helper.assertTrue(cauldron.inputs().get(0).items().equals(java.util.List.of(Items.CAULDRON)), "a cauldron");
        helper.assertTrue(cauldron.inputs().get(1).items().equals(java.util.List.of(ModItems.CRACKED_DIAMOND)) && cauldron.inputs().get(1).count() == 8, "8 cracked diamonds");
        helper.assertTrue(cauldron.output().items().equals(java.util.List.of(item("simplecrucibles:reinforced_cauldron"))), "the reinforced cauldron");
        helper.assertTrue(cauldron.tools().contains(ModItems.STONE_SLEDGEHAMMER), "every hammer works (rank 0)");
        helper.assertTrue(cauldron.durationTicks() == SledgehammerUpgrades.upgradeTicks(SledgehammerUpgrades.upgradeOf(Blocks.CAULDRON)), "duration of the upgrade");
        helper.assertTrue(com.simplebuilding.compat.InWorldRecipeCatalog.build().entries().stream()
                .noneMatch(entry -> entry.id().equals("machine_upgrade/minecraft:cauldron")),
                "the cauldron upgrade is not in the machine upgrade category (else drop it here)");

        if (sandwiches) {
        var butter = entries.get("milk_butter");
        helper.assertTrue(butter.inputs().get(1).items().equals(java.util.List.of(Items.MILK_BUCKET)) && butter.tools().isEmpty()
                && butter.output().items().equals(java.util.List.of(item("simplesandwiches:butter_block"))), "milk bucket in a cauldron gives butter");
        var cheese = entries.get("milk_cheese");
        helper.assertTrue(cheese.inputs().size() == 2 && cheese.inputs().get(1).items().equals(java.util.List.of(Items.MILK_BUCKET))
                && cheese.output().items().equals(java.util.List.of(item("simplesandwiches:cheese_block"))), "milk alone gives cheese (no spider eye)");
        }

        var catalysts = com.simplebuilding.compat.CauldronWorldCatalog.catalysts();
        helper.assertTrue(catalysts.contains(Items.CAULDRON) && catalysts.contains(item("simplecrucibles:reinforced_cauldron"))
                && catalysts.contains(ModItems.DIAMOND_SLEDGEHAMMER) && catalysts.contains(item("simplecrucibles:enderite_crucible"))
                && catalysts.contains(item("simplecrucibles:iron_crucible")), "catalysts: both cauldrons, the hammers, the crucibles");
        helper.succeed();
    }

    private static java.util.List<Object> noteArgs(com.simplebuilding.compat.CauldronWorldCatalog.Entry entry, int note) {
        return java.util.List.of(((net.minecraft.network.chat.contents.TranslatableContents) entry.notes().get(note).getContents()).getArgs());
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    private static Block lib(String path) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("simplelib", path));
    }

    private static ServerPlayer player(GameTestHelper helper, ItemStack main, ItemStack off) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
        player.setPos(at.x, at.y, at.z);
        player.setItemInHand(InteractionHand.MAIN_HAND, main);
        player.setItemInHand(InteractionHand.OFF_HAND, off);
        return player;
    }

    private static UseOnContext click(GameTestHelper helper, ServerPlayer player, BlockPos rel) {
        BlockPos abs = helper.absolutePos(rel);
        return new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false));
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    }

    /** Enderite crucible: 30 slots (27 before N37; saves load by index, so old contents stay) and double stacks; Enderite barrel double stacks too (owner F4/F10, 59). */
    public static void enderiteTiersHaveTwentySevenSlotsAndDoubleStacks(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        Block crucible = CrucibleCompat.enderiteCrucible();
        helper.assertTrue(crucible != null && CrucibleCompat.crucibleSlots(crucible) == 30, "enderite crucible needs 30 slots (5x6, N37)");
        helper.assertTrue(CrucibleCompat.stackMultiplier(crucible) == 2, "enderite crucible holds double stacks");
        helper.assertTrue(CrucibleCompat.stackMultiplier(CrucibleCompat.enderiteBarrel()) == 2, "enderite barrel holds double stacks");
        helper.setBlock(new BlockPos(1, 1, 1), crucible);
        helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 1))) != null, "enderite crucible has a block entity");
        helper.assertTrue(!CrucibleCompat.axeWaysEnabled(), "SimpleBuilding switches the axe ways off (principle 5a)");
        helper.succeed();
    }

    /** Owner N15: a barrel attached to the Enderite crucible holds the crucible's double stacks. */
    public static void aBarrelAttachedToTheEnderiteCrucibleHoldsItsDoubleStacks(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        Block barrel = BuiltInRegistries.BLOCK.getValue(Identifier.parse("simplecrucibles:copper_barrel"));
        BlockPos at = new BlockPos(2, 1, 2), beside = at.east();
        helper.setBlock(at, CrucibleCompat.enderiteCrucible());
        helper.setBlock(beside, barrel);
        ItemStack cobble = new ItemStack(Items.COBBLESTONE);
        var loose = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(helper.absolutePos(beside));
        helper.assertTrue(loose.getMaxStackSize(cobble) == 64, "a loose copper barrel holds normal stacks, got " + loose.getMaxStackSize(cobble));
        BlockState attached = helper.getBlockState(beside);
        for (var property : attached.getProperties()) {
            if (property.getName().equals("attached")) attached = attached.setValue((net.minecraft.world.level.block.state.properties.BooleanProperty) property, true);
            if (property.getName().equals("facing")) attached = attached.setValue((net.minecraft.world.level.block.state.properties.EnumProperty<Direction>) property, Direction.WEST);
        }
        helper.setBlock(beside, attached);
        var container = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(helper.absolutePos(beside));
        helper.assertTrue(CrucibleCompat.isAttached(helper.getBlockState(beside)), "barrel attached to the crucible");
        helper.assertTrue(container.getMaxStackSize(cobble) == 128, "attached, the barrel inherits the double stacks, got " + container.getMaxStackSize(cobble));
        helper.assertTrue(container.getMaxStackSize(new ItemStack(Items.IRON_SWORD)) == 1, "unstackable items stay single");
        // Owner N37: attached, the copper barrel (27 of its own) has the Enderite crucible's 30 slots.
        helper.assertTrue(container.getContainerSize() == 30, "attached copper barrel has the crucible's 30 slots, got " + container.getContainerSize());
        for (int i = 0; i < 27; i++) if (i != 5) container.setItem(i, new ItemStack(Items.DIRT));
        container.setItem(27, new ItemStack(Items.APPLE, 3));
        container.setItem(29, new ItemStack(Items.STICK, 2));
        helper.assertTrue(container.getItem(29).is(Items.STICK), "slot 30 holds items while attached");
        // Crucible gone: back to 27; the extra slots move into the free slot, what does not fit drops - nothing is lost.
        helper.setBlock(at, Blocks.AIR);
        helper.assertTrue(!CrucibleCompat.isAttached(helper.getBlockState(beside)) && container.getContainerSize() == 27,
                "detached: a 27-slot copper barrel again, got " + container.getContainerSize());
        helper.assertTrue(container.getItem(5).is(Items.APPLE) && container.getItem(5).getCount() == 3, "the apples moved into the free slot 6");
        var dropped = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(helper.absolutePos(beside)).inflate(2), e -> e.getItem().is(Items.STICK));
        helper.assertTrue(dropped.size() == 1 && dropped.get(0).getItem().getCount() == 2, "the sticks that did not fit dropped, not lost: " + dropped.size());
        helper.succeed();
    }

    /**
     * Owner N15: a loose Enderite barrel keeps its double stacks everywhere - its menu slot takes 128, a hopper tops a
     * slot of 127 up to 128 and moves on, and a slot of 64 is not "full" for a hopper.
     */
    public static void aLooseEnderiteBarrelKeepsDoubleStacksInMenuAndHopper(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, CrucibleCompat.enderiteBarrel());
        var be = helper.getLevel().getBlockEntity(helper.absolutePos(at));
        var barrel = (net.minecraft.world.Container) be;
        ServerPlayer player = player(helper, ItemStack.EMPTY, ItemStack.EMPTY);
        var menu = ((net.minecraft.world.MenuProvider) be).createMenu(0, player.getInventory(), player);
        ItemStack cobble = new ItemStack(Items.COBBLESTONE);
        helper.assertTrue(menu != null && menu.slots.get(0).getMaxStackSize(cobble) == 128,
                "the loose enderite barrel's menu slot takes 128, got " + (menu == null ? "no menu" : menu.slots.get(0).getMaxStackSize(cobble)));
        barrel.setItem(0, new ItemStack(Items.COBBLESTONE, 127));
        ItemStack rest = net.minecraft.world.level.block.entity.HopperBlockEntity.addItem(null, barrel, new ItemStack(Items.COBBLESTONE, 2), Direction.UP);
        String line = barrel.getItem(0).getCount() + "+" + barrel.getItem(1).getCount() + " rest " + rest.getCount();
        helper.assertTrue(line.equals("128+1 rest 0"), "hopper into a loose enderite barrel slot of 127 plus 2: " + line);
        helper.succeed();
    }

    /**
     * Owner N51: sledgehammer on an empty Vanilla cauldron, 16 strikes with one iron ingot each - 8 for the side plates (half, then
     * full), 4 for the handles (iron poles count), 4 for the rim; each strike is its own blank stage; 2 durability each.
     */
    public static void sledgehammerBuildsTheIronCrucible(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, Blocks.CAULDRON);
        ItemStack hammer = new ItemStack(ModItems.IRON_SLEDGEHAMMER);
        ServerPlayer player = player(helper, hammer, new ItemStack(Items.IRON_INGOT, 8));
        helper.assertTrue(player.getOffhandItem().is(TagKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath("simplecrucibles", "crucible_walls"))), "SimpleLib plate material (iron ingot) loaded");
        helper.assertTrue(new ItemStack(ModItems.IRON_POLE).is(TagKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath("simplecrucibles", "crucible_handles"))), "SimpleBuilding handle materials loaded");
        Block blank = lib("crucible_blank");
        var stage = (net.minecraft.world.level.block.state.properties.IntegerProperty) blank.getStateDefinition().getProperty("stage");
        int strikes = CrucibleCompat.buildStrikes();
        helper.assertTrue(strikes == 16 && CrucibleCompat.buildWallStrikes() == 12, "16 strikes, 12 of them plates and rim: " + strikes);
        for (int strike = 1; strike <= strikes; strike++) {
            if (strike == 9) player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.IRON_POLE, 4));
            if (strike == 13) player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.IRON_INGOT, 4));
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(hammer));
            helper.assertTrue(hammer.getItem().useOn(click(helper, player, pos)).consumesAction(), "strike " + strike + " not taken");
            helper.assertTrue(hammer.getDamageValue() == strike * CrucibleCompat.HAMMER_DAMAGE_PER_STRIKE,
                    "strike " + strike + " must build and damage the hammer");
            BlockState now = helper.getBlockState(pos);
            helper.assertTrue(strike < strikes ? now.is(blank) && now.getValue(stage) == strike : now.is(lib("iron_crucible")),
                    "after strike " + strike + ": " + now);
        }
        helper.assertTrue(player.getOffhandItem().isEmpty(), "every ingot used");
        // Broken half-built (stage 11): the cauldron and 11 iron ingots come back.
        BlockState half = blank.defaultBlockState().setValue(stage, 11);
        half = withValue(half, "cauldron", "true");
        java.util.Map<Item, Integer> drops = new java.util.HashMap<>();
        for (ItemStack stack : Block.getDrops(half, helper.getLevel(), helper.absolutePos(new BlockPos(5, 1, 2)), null)) {
            drops.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        helper.assertTrue(drops.equals(java.util.Map.of(Items.CAULDRON, 1, Items.IRON_INGOT, 11)), "blank stage 11 gives back the cauldron and 11 ingots: " + drops);
        // A filled cauldron is not taken (empty it first): the hammer does nothing there.
        BlockPos water = new BlockPos(5, 1, 4);
        helper.setBlock(water, Blocks.WATER_CAULDRON);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.IRON_INGOT, 1));
        player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(hammer));
        hammer.getItem().useOn(click(helper, player, water));
        helper.assertTrue(helper.getBlockState(water).is(Blocks.WATER_CAULDRON) && player.getOffhandItem().getCount() == 1, "a water cauldron stays untouched");
        helper.succeed();
    }

    /**
     * Owner N51: a crucible upgrade by sledgehammer takes 12 blows; every third blow strikes one unit in (4 cracked
     * diamonds), shown as a band (state {@code upgrade}). Contents and the attached barrel with its contents stay.
     */
    public static void hammerUpgradeGoesStrikeByStrike(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        BlockPos pos = new BlockPos(2, 1, 2);
        BlockPos barrelPos = new BlockPos(2, 1, 3);
        helper.setBlock(pos, lib("iron_crucible"));
        BlockState barrelState = lib("copper_barrel").defaultBlockState();
        barrelState = withValue(barrelState, "attached", "true");
        barrelState = withValue(barrelState, "facing", "north");
        helper.setBlock(barrelPos, barrelState);
        ((net.minecraft.world.Container) helper.getLevel().getBlockEntity(helper.absolutePos(pos))).setItem(0, new ItemStack(Items.RAW_IRON, 5));
        ((net.minecraft.world.Container) helper.getLevel().getBlockEntity(helper.absolutePos(barrelPos))).setItem(2, new ItemStack(Items.COAL, 9));
        var upgrade = SledgehammerUpgrades.upgradeOf(lib("iron_crucible"));
        int blows = SledgehammerUpgrades.blows(upgrade);
        helper.assertTrue(blows == 12 && upgrade.materialCost() == 4 && SledgehammerUpgrades.staged(upgrade), "12 blows, 4 units: " + blows);
        ItemStack hammer = new ItemStack(ModItems.IRON_SLEDGEHAMMER);
        ServerPlayer player = smith(helper, hammer, new ItemStack(ModItems.CRACKED_DIAMOND, 5), pos);
        helper.assertTrue(SledgehammerUpgrades.hasJob(player) && player.isUsingItem(), "the click started the hammer");
        var units = (net.minecraft.world.level.block.state.properties.IntegerProperty) lib("iron_crucible").getStateDefinition().getProperty("upgrade");
        int ticks = blows * SledgehammerUpgrades.HIT_INTERVAL;
        for (int tick = 1; tick < ticks; tick++) {
            player.connection.tick();
            int done = tick / SledgehammerUpgrades.HIT_INTERVAL;
            BlockState now = helper.getBlockState(pos);
            helper.assertTrue(now.is(lib("iron_crucible")) && now.getValue(units) == done / 3, "after " + done + " blows: " + now);
            helper.assertTrue(player.getOffhandItem().getCount() == 5 - done / 3, "units used after " + done + " blows: " + player.getOffhandItem().getCount());
        }
        player.connection.tick();
        helper.assertBlockPresent(lib("reinforced_crucible"), pos);
        helper.assertTrue(player.getOffhandItem().getCount() == 1, "four cracked diamonds used: " + player.getOffhandItem().getCount());
        var crucible = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(crucible.getContainerSize() == 12 && crucible.getItem(0).is(Items.RAW_IRON) && crucible.getItem(0).getCount() == 5, "crucible contents kept");
        BlockState barrelNow = helper.getBlockState(barrelPos);
        var barrel = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(helper.absolutePos(barrelPos));
        helper.assertTrue(CrucibleCompat.isAttached(barrelNow) && barrel.getItem(2).is(Items.COAL) && barrel.getItem(2).getCount() == 9,
                "barrel stays attached with its contents: " + barrelNow);
        helper.succeed();
    }

    /** Owner N51: a crucible broken mid-upgrade gives back the units struck in so far; nothing is lost on release. */
    public static void brokenMidUpgradeReturnsTheUnits(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, lib("reinforced_crucible"));
        ItemStack hammer = new ItemStack(ModItems.DIAMOND_SLEDGEHAMMER);
        ServerPlayer player = smith(helper, hammer, new ItemStack(ModItems.NETHERITE_NUGGET, 4), pos);
        for (int tick = 1; tick <= 7 * SledgehammerUpgrades.HIT_INTERVAL; tick++) player.connection.tick();
        player.releaseUsingItem();
        var units = (net.minecraft.world.level.block.state.properties.IntegerProperty) lib("reinforced_crucible").getStateDefinition().getProperty("upgrade");
        helper.assertTrue(helper.getBlockState(pos).getValue(units) == 2 && player.getOffhandItem().getCount() == 2,
                "7 blows: 2 netherite nuggets in, 2 left: " + helper.getBlockState(pos) + " / " + player.getOffhandItem().getCount());
        helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
        int nuggets = 0;
        for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(2))) {
            if (item.getItem().is(ModItems.NETHERITE_NUGGET)) nuggets += item.getItem().getCount();
        }
        helper.assertTrue(nuggets == 2, "breaking it returns the 2 struck-in nuggets, got " + nuggets);
        helper.succeed();
    }

    private static <T extends Comparable<T>> BlockState withValue(BlockState state, String name, String value) {
        @SuppressWarnings("unchecked")
        Property<T> property = (Property<T>) state.getBlock().getStateDefinition().getProperty(name);
        return property == null ? state : property.getValue(value).map(v -> state.setValue(property, v)).orElse(state);
    }

    /** A mock smith standing on {@code machine}, looking straight down, who right-clicks it once (vanilla's use path). */
    @SuppressWarnings("removal")
    private static ServerPlayer smith(GameTestHelper helper, ItemStack hammer, ItemStack material, BlockPos machine) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setShiftKeyDown(false);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        player.setItemInHand(InteractionHand.OFF_HAND, material);
        Vec3 feet = helper.absoluteVec(new Vec3(machine.getX() + 0.5, machine.getY() + 1.0, machine.getZ() + 0.5));
        player.snapTo(feet.x, feet.y, feet.z, 0.0F, 90.0F);
        BlockPos abs = helper.absolutePos(machine);
        player.gameMode.useItemOn(player, helper.getLevel(), hammer, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs).add(0.0, 0.5, 0.0), Direction.UP, abs, false));
        return player;
    }

    /** Crucible and barrel upgrades cost twice a furnace upgrade; the chain ends at Enderite (owner F9-F11, 56, 59). */
    public static void sledgehammerUpgradesCostDouble(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        var iron = SledgehammerUpgrades.upgradeOf(lib("iron_crucible"));
        var netherite = SledgehammerUpgrades.upgradeOf(lib("netherite_crucible"));
        var barrel = SledgehammerUpgrades.upgradeOf(lib("reinforced_barrel"));
        var netheriteBarrel = SledgehammerUpgrades.upgradeOf(lib("netherite_barrel"));
        var cauldron = SledgehammerUpgrades.upgradeOf(Blocks.CAULDRON);
        helper.assertTrue(iron != null && iron.to() == lib("reinforced_crucible") && iron.nugget() == ModItems.CRACKED_DIAMOND
                && iron.materialCost() == 4 && SledgehammerUpgrades.blows(iron) == 12, "iron -> reinforced: 4 cracked diamonds, 12 strikes (owner N51)");
        helper.assertTrue(netherite != null && netherite.to() == CrucibleCompat.enderiteCrucible() && netherite.nugget() == ModItems.ENDERITE_NUGGET
                && netherite.minHammerRank() == SledgehammerUpgrades.RANK_NETHERITE, "netherite -> enderite with enderite nuggets, netherite hammer");
        helper.assertTrue(barrel != null && barrel.to() == lib("netherite_barrel") && barrel.nugget() == ModItems.NETHERITE_NUGGET
                && barrel.materialCost() == 2, "reinforced -> netherite barrel with netherite nuggets");
        helper.assertTrue(netheriteBarrel != null && netheriteBarrel.to() == CrucibleCompat.enderiteBarrel() && netheriteBarrel.materialCost() == 2,
                "netherite -> enderite barrel");
        helper.assertTrue(cauldron != null && cauldron.to() == CrucibleCompat.reinforcedCauldron() && cauldron.materialCost() == 8,
                "cauldron -> reinforced cauldron with 8 cracked diamonds");
        // The upgrade keeps the contents (owner 11).
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, lib("netherite_crucible"));
        BlockPos abs = helper.absolutePos(pos);
        var be = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(abs);
        be.setItem(0, new ItemStack(Items.RAW_IRON, 5));
        CrucibleCompat.upgradeInPlace(helper.getLevel(), abs, CrucibleCompat.enderiteCrucible());
        helper.assertBlockPresent(CrucibleCompat.enderiteCrucible(), pos);
        var fresh = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(abs);
        helper.assertTrue(fresh.getContainerSize() == 30 && fresh.getItem(0).is(Items.RAW_IRON) && fresh.getItem(0).getCount() == 5,
                "upgrade kept the contents");
        helper.succeed();
    }







    /** SimpleBuilding's additions to the module's extreme-heat tag: layered raw enderite and cracked diamond (soul lava heat: Simple Crucibles' tests). */
    public static void soulLavaHeatsExtremeFlowingHigh(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        TagKey<Item> extreme = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("simplecrucibles", "needs_extreme_heat"));
        helper.assertTrue(new ItemStack(ModItems.LAYERED_RAW_ENDERITE).is(extreme) && new ItemStack(ModItems.CRACKED_DIAMOND).is(extreme),
                "layered raw enderite and cracked diamond need extreme heat");
        helper.succeed();
    }

















    /** Sledgehammer on a quartz block: 4 quartz (owner 54). */
    public static void sledgehammerCrushesQuartzBlockIntoFourQuartz(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, Blocks.QUARTZ_BLOCK);
        ItemStack hammer = new ItemStack(ModItems.IRON_SLEDGEHAMMER);
        ServerPlayer player = player(helper, hammer, ItemStack.EMPTY);
        SledgehammerItem.crushQuartzBlock(helper.getLevel(), helper.absolutePos(pos), player, hammer);
        helper.assertBlockPresent(Blocks.AIR, pos);
        int quartz = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(2))
                .stream().filter(e -> e.getItem().is(Items.QUARTZ)).mapToInt(e -> e.getItem().getCount()).sum();
        helper.assertTrue(quartz == SledgehammerItem.QUARTZ_BLOCK_QUARTZ, "quartz dropped: " + quartz);
        helper.succeed();
    }


    /**
     * Owner tweaks P6: the crucible is walk-in like a cauldron. Nothing collides above the belly wall
     * (9/16 = 0.5625, less than the 0.6 step height), the interior column is empty from the floor up
     * and the neck stays open; the walls and the floor themselves are still solid.
     */
    public static void crucibleCollisionAllowsWalkingInAndKeepsTheInteriorEmpty(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        VoxelShape collision = lib("iron_crucible").defaultBlockState().getCollisionShape(helper.getLevel(), BlockPos.ZERO, CollisionContext.empty());
        var boxes = collision.toAabbs();
        helper.assertTrue(boxes.stream().allMatch(box -> box.maxY <= 9.0 / 16.0 + 1.0E-4), "nothing blocks above the 9/16 belly wall");
        helper.assertTrue(boxes.stream().noneMatch(box -> contains(box, 0.5, 0.5, 0.5)), "the interior is empty at belly height");
        helper.assertTrue(boxes.stream().noneMatch(box -> contains(box, 0.5, 0.75, 0.5)), "the neck above the interior stays open");
        helper.assertTrue(boxes.stream().anyMatch(box -> box.minX <= 0.125 && box.maxX >= 0.125 && box.maxY >= 9.0 / 16.0 - 1.0E-4),
                "the belly rim still reaches the walk-in height");
        helper.succeed();
    }

    private static boolean contains(AABB box, double x, double y, double z) {
        return x >= box.minX && x <= box.maxX && y >= box.minY && y <= box.maxY && z >= box.minZ && z <= box.maxZ;
    }

    /**
     * Owner tweaks P6: stepping on a high-heat crucible burns like magma (1.0 hotFloor damage, lava below),
     * while medium heat (campfire) and sneaking spare the entity - the same gate as vanilla {@code MagmaBlock}.
     * The heat assertions go through {@link CrucibleCompat} (the only SimpleLib bridge), because SimpleLib is
     * not built on 26.2; there every test passes at once behind {@link McVersion#CRUCIBLE}.
     */
    public static void crucibleStepOnHurtsOnlyOnHighHeatAndNotWhenSneaking(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        ServerLevel level = helper.getLevel();
        BlockPos hot = new BlockPos(2, 1, 2);
        helper.setBlock(hot.below(), Blocks.LAVA);
        helper.setBlock(hot, lib("iron_crucible"));
        BlockPos hotAbs = helper.absolutePos(hot);
        BlockState hotState = level.getBlockState(hotAbs);
        BlockPos warm = new BlockPos(5, 1, 2);
        helper.setBlock(warm.below(), Blocks.CAMPFIRE);
        helper.setBlock(warm, lib("iron_crucible"));
        BlockPos warmAbs = helper.absolutePos(warm);
        BlockState warmState = level.getBlockState(warmAbs);
        // The crucible's own server tick computes the heat (heatDirty starts true); one tick is enough.
        helper.runAfterDelay(1, () -> {
            helper.assertTrue("high".equals(CrucibleCompat.heatAt(level, hotAbs)), "lava below: high heat");
            helper.assertTrue("medium".equals(CrucibleCompat.heatAt(level, warmAbs)), "campfire below: medium heat");
            Pig victim = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, hot.above());
            float before = victim.getHealth();
            hotState.getBlock().stepOn(level, hotAbs, hotState, victim);
            helper.assertTrue(victim.getHealth() < before, "high heat burns: " + victim.getHealth());
            Pig safe = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, warm.above());
            float safeBefore = safe.getHealth();
            warmState.getBlock().stepOn(level, warmAbs, warmState, safe);
            helper.assertTrue(safe.getHealth() == safeBefore, "medium heat does not burn");
            ServerPlayer sneaker = helper.makeMockServerPlayerInLevel();
            sneaker.setShiftKeyDown(true);
            float sneakerBefore = sneaker.getHealth();
            hotState.getBlock().stepOn(level, hotAbs, hotState, sneaker);
            helper.assertTrue(sneaker.getHealth() == sneakerBefore, "sneaking spares the heat");
            helper.succeed();
        });
    }

    /**
     * Owner N15: the attached barrel's outline follows its model - the body element of
     * {@code *_barrel_attached.json} turned by the y-rotation of its blockstate variant, for every facing and every
     * barrel tier. The expected box is read from the blockstate and the model on the classpath, so moving the model
     * moves this test with it; the loose barrel keeps the full cube it has today.
     */
    public static void attachedBarrelHitboxFollowsItsModel(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        AABB cube = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
        for (String[] barrel : barrels()) {
            Block block = barrelBlock(barrel);
            if (block == null) continue;
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                BlockState attached = with(with(block.defaultBlockState(), "attached", true), "facing", facing);
                AABB expected = attachedModelBox(helper, barrel, facing);
                AABB shape = attached.getShape(helper.getLevel(), BlockPos.ZERO, CollisionContext.empty()).bounds();
                AABB collision = attached.getCollisionShape(helper.getLevel(), BlockPos.ZERO, CollisionContext.empty()).bounds();
                helper.assertTrue(sameBox(shape, expected),
                        barrel[1] + " " + facing + ": outline " + shape + " instead of the model box " + expected);
                helper.assertTrue(sameBox(collision, expected),
                        barrel[1] + " " + facing + ": collision " + collision + " instead of the model box " + expected);
            }
            BlockState loose = block.defaultBlockState();
            helper.assertTrue(sameBox(loose.getShape(helper.getLevel(), BlockPos.ZERO, CollisionContext.empty()).bounds(), cube),
                    barrel[1] + ": the loose barrel must keep the full cube");
        }
        helper.succeed();
    }

    /**
     * Owner N15 (X-Ray): an attached barrel must not occlude with a full block shape anywhere, or the renderer culls
     * the neighbouring wall face ({@code Block.shouldRenderFace} turns false) and the room behind the wall shows
     * through. The loose barrel is the control: full occlusion, neighbour faces stay culled as they do today.
     */
    public static void attachedBarrelDoesNotOccludeItsNeighbours(GameTestHelper helper) {
        if (!McVersion.CRUCIBLE) { helper.succeed(); return; }
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (String[] barrel : barrels()) {
            Block block = barrelBlock(barrel);
            if (block == null) continue;
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                BlockState attached = with(with(block.defaultBlockState(), "attached", true), "facing", facing);
                helper.assertTrue(!Block.isShapeFullBlock(attached.getOcclusionShape()),
                        barrel[1] + " " + facing + ": the attached barrel must not occlude as a full block");
                for (Direction direction : Direction.values()) {
                    helper.assertTrue(Block.shouldRenderFace(stone, attached, direction),
                            barrel[1] + " " + facing + ": the wall face towards " + direction + " would be culled (X-Ray)");
                }
            }
            BlockState loose = block.defaultBlockState();
            helper.assertTrue(Block.isShapeFullBlock(loose.getOcclusionShape()),
                    barrel[1] + ": the loose barrel keeps its full occlusion shape");
            for (Direction direction : Direction.values()) {
                helper.assertTrue(!Block.shouldRenderFace(stone, loose, direction),
                        barrel[1] + ": the loose barrel still culls the wall face towards " + direction);
            }
        }
        helper.succeed();
    }

    // --- helpers of the barrel hitbox tests ---

    /** Every barrel with an attached model: namespace and path; enderite is only registered on 26.3. */
    private static String[][] barrels() {
        return new String[][] {{"simplecrucibles", "copper_barrel"}, {"simplecrucibles", "reinforced_barrel"},
                {"simplecrucibles", "netherite_barrel"}, {"simplecrucibles", "enderite_barrel"}};
    }

    private static Block barrelBlock(String[] barrel) {
        return "simplelib".equals(barrel[0]) ? lib(barrel[1]) : CrucibleCompat.enderiteBarrel();
    }

    /**
     * The state with one property set by name: the barrel's own Property instances are created inside SimpleLib and
     * are not reachable from this source tree, and StateHolder compares properties by identity, so the vanilla
     * constants of the same name would not match.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState with(BlockState base, String property, T value) {
        Property<T> found = (Property<T>) base.getBlock().getStateDefinition().getProperty(property);
        if (found == null) throw new IllegalStateException(base + " has no property " + property);
        return base.setValue(found, value);
    }

    /** Blockstate variant of this barrel at that facing -> its model -> the first element, turned by the variant's y. */
    private static AABB attachedModelBox(GameTestHelper helper, String[] barrel, Direction facing) {
        String blockstatePath = "assets/" + barrel[0] + "/blockstates/" + barrel[1] + ".json";
        JsonObject variants = jsonResource(helper, blockstatePath).getAsJsonObject("variants");
        JsonObject variant = null;
        for (String key : variants.keySet()) {
            if (key.contains("attached=true") && key.contains("facing=" + facing.getName()) && key.contains("open=false")) {
                variant = variants.getAsJsonObject(key);
                break;
            }
        }
        helper.assertTrue(variant != null, blockstatePath + " has no attached variant for " + facing);
        String model = variant.get("model").getAsString();
        int split = model.indexOf(':');
        String modelPath = "assets/" + model.substring(0, split) + "/models/" + model.substring(split + 1) + ".json";
        JsonArray elements = jsonResource(helper, modelPath).getAsJsonArray("elements");
        helper.assertTrue(elements.size() > 0, modelPath + " has no elements to take the outline from");
        JsonObject body = elements.get(0).getAsJsonObject();
        helper.assertTrue(!body.has("rotation"),
                modelPath + " rotates its first element, which this test does not model - adapt the test first");
        JsonArray from = body.getAsJsonArray("from");
        JsonArray to = body.getAsJsonArray("to");
        return turnedBox(from.get(0).getAsDouble(), from.get(2).getAsDouble(), to.get(0).getAsDouble(), to.get(2).getAsDouble(),
                from.get(1).getAsDouble(), to.get(1).getAsDouble(), variant.has("y") ? variant.get("y").getAsInt() : 0);
    }

    /** The element footprint in pixels, turned about the block centre the way the blockstate turns the model. */
    private static AABB turnedBox(double x1, double z1, double x2, double z2, double minY, double maxY, int y) {
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (double[] corner : new double[][] {{x1, z1}, {x1, z2}, {x2, z1}, {x2, z2}}) {
            double[] turned = turn(corner[0], corner[1], y);
            minX = Math.min(minX, turned[0]);
            maxX = Math.max(maxX, turned[0]);
            minZ = Math.min(minZ, turned[1]);
            maxZ = Math.max(maxZ, turned[1]);
        }
        return new AABB(minX / 16.0, minY / 16.0, minZ / 16.0, maxX / 16.0, maxY / 16.0, maxZ / 16.0);
    }

    /** y=90 sends model north (-z) to east (+x): (x,z) becomes (16-z,x), the blockstate rotation about the centre. */
    private static double[] turn(double x, double z, int y) {
        return switch (Math.floorMod(y, 360)) {
            case 90 -> new double[] {16.0 - z, x};
            case 180 -> new double[] {16.0 - x, 16.0 - z};
            case 270 -> new double[] {z, 16.0 - x};
            default -> new double[] {x, z};
        };
    }

    private static boolean sameBox(AABB a, AABB b) {
        return Math.abs(a.minX - b.minX) < 1.0E-6 && Math.abs(a.minY - b.minY) < 1.0E-6 && Math.abs(a.minZ - b.minZ) < 1.0E-6
                && Math.abs(a.maxX - b.maxX) < 1.0E-6 && Math.abs(a.maxY - b.maxY) < 1.0E-6 && Math.abs(a.maxZ - b.maxZ) < 1.0E-6;
    }

    private static JsonObject jsonResource(GameTestHelper helper, String path) {
        try (java.io.InputStream in = CrucibleTests.class.getClassLoader().getResourceAsStream(path)) {
            helper.assertTrue(in != null, path + " is not on the classpath");
            return JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("cannot read " + path, e);
        }
    }
}
