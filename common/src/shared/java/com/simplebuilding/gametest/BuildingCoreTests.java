package com.simplebuilding.gametest;

import com.mojang.authlib.GameProfile;
import com.simplebuilding.blocks.ModBlocks;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.BuildingCoreItem;
import com.simplebuilding.items.custom.CoreOreTransmutation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The six building cores (owner, 2026-09-28): they do not stack, every recipe that takes a core still
 * crafts with one core per slot, and a right click plays one of three animations rolled 70/20/10 -
 * server-timed over a few ticks, then a one-second cooldown. Since 2026-09-29 a click on a block that
 * ores generate in has a tiny, tier-dependent chance to turn it into one of that block's ores
 * ({@link CoreOreTransmutation}).
 */
public final class BuildingCoreTests {

    private BuildingCoreTests() {
    }

    private static List<Item> cores() {
        return List.of(ModItems.COPPER_CORE, ModItems.IRON_CORE, ModItems.GOLD_CORE,
                ModItems.DIAMOND_CORE, ModItems.NETHERITE_CORE, ModItems.ENDERITE_CORE);
    }

    private static boolean isCore(Holder<Item> item) {
        return cores().contains(item.value());
    }

    /**
     * Every core has a maximum stack size of 1, so two cores take two slots.
     *
     * <p><strong>What breaks this test:</strong> any core registered with a stack size above 1
     * (they stacked to 16 before 2026-09-28).
     */
    public static void buildingCoresAreNotStackable(GameTestHelper helper) {
        for (Item core : cores()) {
            ItemStack stack = new ItemStack(core);
            helper.assertValueEqual(stack.getMaxStackSize(), 1, BuiltInRegistries.ITEM.getKey(core) + " max stack size");
            helper.assertFalse(stack.isStackable(), BuiltInRegistries.ITEM.getKey(core) + " still stacks");
        }
        helper.succeed();
    }

    /**
     * Every shaped crafting and smithing recipe that takes a core matches with exactly one item per
     * slot - the core included - and every recipe that makes a core makes one. The easter recipes
     * are left out: they only exist on their dates.
     *
     * <p><strong>What breaks this test:</strong> a recipe that would need two cores in one slot (a
     * count-based smithing addition above one), a recipe that yields a stack of cores, or a core
     * recipe that stops matching; and fewer than the sixteen core recipes known on 2026-10-02 (seventeen until the
     * Infused Potion Pad III switched from the enderite core to the enderite pressure plate)
     * (then the test would check nothing).
     */
    public static void everyCoreRecipeCraftsWithOneCorePerSlot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> checked = new ArrayList<>();
        for (RecipeHolder<?> holder : level.getServer().getRecipeManager().getRecipes()) {
            String id = holder.id().identifier().toString();
            if (id.contains("easter/")) {
                continue;
            }
            Recipe<?> recipe = holder.value();
            if (recipe instanceof ShapedRecipe shaped) {
                List<Optional<Ingredient>> ingredients = shaped.getIngredients();
                if (ingredients.stream().noneMatch(i -> i.isPresent() && i.get().items().anyMatch(BuildingCoreTests::isCore))) {
                    continue;
                }
                List<ItemStack> grid = new ArrayList<>();
                for (Optional<Ingredient> ingredient : ingredients) {
                    grid.add(ingredient.map(BuildingCoreTests::one).orElse(ItemStack.EMPTY));
                }
                CraftingInput input = CraftingInput.of(shaped.getWidth(), shaped.getHeight(), grid);
                helper.assertTrue(shaped.matches(input, level), id + " does not match with one item per slot");
                helper.assertFalse(shaped.assemble(input).isEmpty(), id + " crafts nothing");
                checked.add(id);
            } else if (recipe instanceof SmithingRecipe smithing) {
                boolean baseIsCore = smithing.baseIngredient().items().anyMatch(BuildingCoreTests::isCore);
                boolean additionIsCore = smithing.additionIngredient().map(i -> i.items().anyMatch(BuildingCoreTests::isCore)).orElse(false);
                if (!baseIsCore && !additionIsCore) {
                    continue;
                }
                SmithingRecipeInput input = new SmithingRecipeInput(
                        smithing.templateIngredient().map(BuildingCoreTests::one).orElse(ItemStack.EMPTY),
                        one(smithing.baseIngredient()),
                        smithing.additionIngredient().map(BuildingCoreTests::one).orElse(ItemStack.EMPTY));
                helper.assertTrue(smithing.matches(input, level), id + " does not match with one core");
                ItemStack result = smithing.assemble(input);
                helper.assertFalse(result.isEmpty(), id + " forges nothing");
                helper.assertTrue(result.getCount() <= result.getMaxStackSize(), id + " forges " + result + ", more than a stack");
                checked.add(id);
            }
        }
        helper.assertTrue(checked.size() >= 16, "only " + checked.size() + " core recipes were found: " + checked);
        helper.succeed();
    }

    /**
     * Hand motions of the core (Nachtrag 11): the weights add up to 100 and fall as the motions get cooler
     * (pulse > spin > rise > boomerang), the ore motion is never rolled but is the longest; every motion starts and
     * ends at rest (no snap), and over 20000 seeded rolls each share lands within two points of its weight.
     */
    public static void coreHandMotionsAreRarerTheCoolerAndStartAndEndAtRest(GameTestHelper helper) {
        com.simplebuilding.items.custom.CoreHandMotion.Motion[] motions = com.simplebuilding.items.custom.CoreHandMotion.Motion.values();
        int total = 0;
        for (int i = 0; i < motions.length; i++) {
            total += motions[i].weight;
            if (i > 0 && motions[i].weight > 0) { // FORGE and the sage orb's ORB are never rolled
                helper.assertTrue(motions[i].weight < motions[i - 1].weight, motions[i] + " is not rarer than " + motions[i - 1]);
            }
            helper.assertTrue(motions[i].ticks <= com.simplebuilding.items.custom.CoreHandMotion.Motion.FORGE.ticks, motions[i] + " outlasts the ore motion");
            for (float t : new float[] {0.0F, 0.9999F}) {
                float[] pose = com.simplebuilding.items.custom.CoreHandMotion.pose(motions[i], t);
                for (int k = 0; k < 6; k++) {
                    double rest = Math.abs(pose[k]) % 360.0;
                    rest = Math.min(rest, 360.0 - rest);
                    helper.assertTrue(k < 3 ? Math.abs(pose[k]) < 0.02 : rest < 2.5, motions[i] + " not at rest at t=" + t + ", value " + k + " = " + pose[k]);
                }
                helper.assertTrue(Math.abs(pose[6] - 1.0F) < 0.02F, motions[i] + " scale at t=" + t + " = " + pose[6]);
            }
        }
        helper.assertTrue(total == 100, "motion weights add up to " + total);
        helper.assertTrue(com.simplebuilding.items.custom.CoreHandMotion.fromRoll(99) != com.simplebuilding.items.custom.CoreHandMotion.Motion.FORGE, "FORGE is rolled");
        java.util.Map<com.simplebuilding.items.custom.CoreHandMotion.Motion, Integer> seen = new java.util.EnumMap<>(com.simplebuilding.items.custom.CoreHandMotion.Motion.class);
        RandomSource random = RandomSource.create(42L);
        for (int i = 0; i < 20000; i++) seen.merge(com.simplebuilding.items.custom.CoreHandMotion.roll(random), 1, Integer::sum);
        for (com.simplebuilding.items.custom.CoreHandMotion.Motion motion : motions) {
            double share = 100.0 * seen.getOrDefault(motion, 0) / 20000;
            helper.assertTrue(Math.abs(share - motion.weight) <= 2.0, motion + " share " + share);
        }
        helper.succeed();
    }

    /**
     * The animation roll: 0-69 glow, 70-89 orbit, 90-99 burst, and over 20000 seeded rolls the
     * shares land within two points of 70/20/10. The same seed gives the same sequence.
     *
     * <p><strong>What breaks this test:</strong> changed weights or boundaries, or a roll that does
     * not come from the random source it is handed.
     */
    public static void coreAnimationRollFollowsTheSeventyTwentyTenWeights(GameTestHelper helper) {
        helper.assertTrue(BuildingCoreItem.fromRoll(0) == BuildingCoreItem.Animation.GLOW, "roll 0");
        helper.assertTrue(BuildingCoreItem.fromRoll(69) == BuildingCoreItem.Animation.GLOW, "roll 69");
        helper.assertTrue(BuildingCoreItem.fromRoll(70) == BuildingCoreItem.Animation.ORBIT, "roll 70");
        helper.assertTrue(BuildingCoreItem.fromRoll(89) == BuildingCoreItem.Animation.ORBIT, "roll 89");
        helper.assertTrue(BuildingCoreItem.fromRoll(90) == BuildingCoreItem.Animation.BURST, "roll 90");
        helper.assertTrue(BuildingCoreItem.fromRoll(99) == BuildingCoreItem.Animation.BURST, "roll 99");

        int[] counts = new int[3];
        RandomSource random = RandomSource.create(20260928L);
        RandomSource twin = RandomSource.create(20260928L);
        int rolls = 20000;
        for (int i = 0; i < rolls; i++) {
            BuildingCoreItem.Animation animation = BuildingCoreItem.roll(random);
            helper.assertTrue(animation == BuildingCoreItem.roll(twin), "the same seed rolled differently at " + i);
            counts[animation.ordinal()]++;
        }
        int[] expected = {70, 20, 10};
        for (int i = 0; i < 3; i++) {
            double share = 100.0 * counts[i] / rolls;
            helper.assertTrue(Math.abs(share - expected[i]) < 2.0,
                    BuildingCoreItem.Animation.values()[i] + " came up " + share + " % instead of about " + expected[i] + " %");
        }
        helper.succeed();
    }

    /**
     * A right click with a core, through the server's use path ({@code ServerPlayerGameMode#useItem}),
     * plays an animation (the click succeeds), keeps the core and starts the cooldown; a second click
     * during the cooldown does nothing.
     *
     * <p><strong>What breaks this test:</strong> a core that passes the click on (plain items do),
     * consumes itself, or has no cooldown.
     */
    public static void rightClickingTheCorePlaysAnAnimationAndStartsTheCooldown(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        for (Item core : cores()) {
            ItemStack stack = new ItemStack(core);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            InteractionResult first = player.gameMode.useItem(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND);
            helper.assertTrue(first instanceof InteractionResult.Success, BuiltInRegistries.ITEM.getKey(core) + ": the right click was not taken, " + first);
            helper.assertTrue(player.getCooldowns().isOnCooldown(stack), BuiltInRegistries.ITEM.getKey(core) + ": no cooldown after the animation");
            helper.assertValueEqual(player.getMainHandItem().getCount(), 1, BuiltInRegistries.ITEM.getKey(core) + ": cores in hand after the click");
            InteractionResult second = player.gameMode.useItem(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND);
            helper.assertFalse(second instanceof InteractionResult.Success, BuiltInRegistries.ITEM.getKey(core) + ": a click during the cooldown played again");
        }
        helper.succeed();
    }

    /** Ticks the animation test may run: the longest animation plus a margin for the tick hook. */
    public static final int ANIMATION_TEST_MAX_TICKS = 40;

    /**
     * A right click on a block goes through the server's block path ({@code ServerPlayerGameMode#useItemOn}):
     * the click is taken, the core is kept, the cooldown runs exactly {@link BuildingCoreItem#COOLDOWN_TICKS}
     * ticks (one second since 2026-09-29), a second click during it does nothing, and the stone either
     * stays stone or became one of the stone ores (the chance is real, just tiny).
     *
     * <p><strong>What breaks this test:</strong> a core that only reacts to air clicks, consumes
     * itself, a changed cooldown, or a transmutation into a block the stone table does not hold.
     */
    public static void rightClickingStoneWithTheCoreStartsTheOneSecondCooldown(GameTestHelper helper) {
        helper.assertValueEqual(BuildingCoreItem.COOLDOWN_TICKS, 20, "core cooldown in ticks");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockPos absolute = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).add(0, 0.5, 0), Direction.UP, absolute, false);
        List<Block> stoneTable = CoreOreTransmutation.ores(CoreOreTransmutation.Host.STONE).stream()
                .map(CoreOreTransmutation.WeightedOre::ore).toList();
        for (Item core : cores()) {
            String id = BuiltInRegistries.ITEM.getKey(core).toString();
            helper.setBlock(pos, Blocks.STONE);
            ItemStack stack = new ItemStack(core);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            InteractionResult first = player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(first instanceof InteractionResult.Success, id + ": the click on stone was not taken, " + first);
            helper.assertValueEqual(player.getMainHandItem().getCount(), 1, id + ": cores in hand after the click");
            Block after = helper.getBlockState(pos).getBlock();
            helper.assertTrue(after == Blocks.STONE || stoneTable.contains(after), id + ": stone became " + after);
            InteractionResult second = player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND, hit);
            helper.assertFalse(second instanceof InteractionResult.Success, id + ": a click during the cooldown was taken");
            for (int tick = 1; tick < BuildingCoreItem.COOLDOWN_TICKS; tick++) {
                player.getCooldowns().tick();
            }
            helper.assertTrue(player.getCooldowns().isOnCooldown(stack), id + ": the cooldown ended before " + BuildingCoreItem.COOLDOWN_TICKS + " ticks");
            player.getCooldowns().tick();
            helper.assertFalse(player.getCooldowns().isOnCooldown(stack), id + ": still cooling down after " + BuildingCoreItem.COOLDOWN_TICKS + " ticks");
        }
        helper.succeed();
    }

    /**
     * The animations are server-timed: each one schedules its later steps, every step has run by
     * {@link BuildingCoreItem#duration} ticks, and every animation ends before the cooldown does, so two
     * never overlap. Runs through the loader's server tick hook (Fabric END_SERVER_TICK, NeoForge
     * ServerTickEvent.Post).
     *
     * <p><strong>What breaks this test:</strong> a missing tick hook (the steps would wait forever), an
     * animation longer than the cooldown, or all steps played at once (nothing scheduled).
     */
    public static void coreAnimationsAreServerTimedAndEndWithinTheCooldown(GameTestHelper helper) {
        int longest = 0;
        for (BuildingCoreItem.Animation animation : BuildingCoreItem.Animation.values()) {
            int duration = BuildingCoreItem.duration(animation);
            helper.assertTrue(duration > 0 && duration < BuildingCoreItem.COOLDOWN_TICKS,
                    animation + " lasts " + duration + " ticks, the cooldown only " + BuildingCoreItem.COOLDOWN_TICKS);
            longest = Math.max(longest, duration);
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        for (BuildingCoreItem.Animation animation : BuildingCoreItem.Animation.values()) {
            int before = BuildingCoreItem.pendingSteps(player);
            BuildingCoreItem.play(helper.getLevel(), player, animation, 0xA57DE9);
            helper.assertTrue(BuildingCoreItem.pendingSteps(player) > before, animation + " scheduled no later steps");
        }
        int wait = longest + 2;
        helper.runAfterDelay(wait, () -> {
            helper.assertValueEqual(BuildingCoreItem.pendingSteps(player), 0, "animation steps still waiting after " + wait + " ticks");
            helper.succeed();
        });
    }

    /**
     * Which blocks are hosts: the stone ore replaceables (stone, granite, diorite, andesite), deepslate
     * and tuff, netherrack, end stone - and nothing else (cobblestone, smooth stone, cobbled deepslate,
     * blackstone, basalt, dirt, the mod's nihil end stone are not).
     *
     * <p><strong>What breaks this test:</strong> a host missing or a block that no ore generates in
     * counted as a host.
     */
    public static void coreOreHostsAreTheBlocksOresGenerateIn(GameTestHelper helper) {
        Block[][] expected = {
                {Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE},
                {Blocks.DEEPSLATE, Blocks.TUFF},
                {Blocks.NETHERRACK},
                {Blocks.END_STONE}};
        for (CoreOreTransmutation.Host host : CoreOreTransmutation.Host.values()) {
            for (Block block : expected[host.ordinal()]) {
                helper.assertTrue(CoreOreTransmutation.hostOf(block.defaultBlockState()).orElse(null) == host,
                        BuiltInRegistries.BLOCK.getKey(block) + " should be a " + host + " host");
            }
        }
        for (Block block : List.of(Blocks.COBBLESTONE, Blocks.SMOOTH_STONE, Blocks.COBBLED_DEEPSLATE, Blocks.BLACKSTONE,
                Blocks.BASALT, Blocks.DIRT, Blocks.END_STONE_BRICKS, Blocks.COAL_ORE, ModBlocks.NIHIL_END_STONE)) {
            helper.assertTrue(CoreOreTransmutation.hostOf(block.defaultBlockState()).isEmpty(),
                    BuiltInRegistries.BLOCK.getKey(block) + " must not be a host");
        }
        helper.succeed();
    }

    /**
     * The ore tables per host, with a seeded random source: every host's weights add up to 100, the
     * roll boundaries land on the first and last ore, only ores of the host come out (stone: plain
     * vanilla ores; deepslate: deepslate ores; netherrack: nether quartz, nether gold, ancient debris;
     * end stone: the mod's nihilith and astralit ore), and over 100000 picks each ore's share lies
     * within one point of its weight - with ancient debris, emerald and astralit the rare ones.
     *
     * <p><strong>What breaks this test:</strong> a stone ore offered in deepslate or the other way
     * round, an ore of the wrong dimension, changed weights, or picks that ignore the weights.
     */
    public static void coreOreTablesOnlyHoldTheHostsOresAndFollowTheirWeights(GameTestHelper helper) {
        RandomSource random = RandomSource.create(20260929L);
        for (CoreOreTransmutation.Host host : CoreOreTransmutation.Host.values()) {
            List<CoreOreTransmutation.WeightedOre> table = CoreOreTransmutation.ores(host);
            int total = CoreOreTransmutation.totalWeight(host);
            helper.assertValueEqual(total, 100, host + " weights");
            helper.assertTrue(CoreOreTransmutation.oreForRoll(host, 0) == table.get(0).ore(), host + ": roll 0");
            helper.assertTrue(CoreOreTransmutation.oreForRoll(host, total - 1) == table.get(table.size() - 1).ore(), host + ": last roll");
            Map<Block, Integer> counts = new HashMap<>();
            for (CoreOreTransmutation.WeightedOre entry : table) {
                String id = BuiltInRegistries.BLOCK.getKey(entry.ore()).toString();
                helper.assertTrue(entry.weight() > 0, id + " has no weight in " + host);
                helper.assertTrue(counts.put(entry.ore(), 0) == null, id + " twice in " + host);
                boolean valid = switch (host) {
                    case STONE -> id.startsWith("minecraft:") && id.endsWith("_ore") && !id.contains("deepslate") && !id.contains("nether");
                    case DEEPSLATE -> id.startsWith("minecraft:deepslate_") && id.endsWith("_ore");
                    case NETHERRACK -> id.equals("minecraft:nether_quartz_ore") || id.equals("minecraft:nether_gold_ore") || id.equals("minecraft:ancient_debris");
                    case END_STONE -> id.equals("simplebuilding:nihilith_ore") || id.equals("simplebuilding:astralit_ore");
                };
                helper.assertTrue(valid, id + " is no ore of " + host);
            }
            int picks = 100000;
            for (int i = 0; i < picks; i++) {
                Block ore = CoreOreTransmutation.pickOre(host, random);
                helper.assertTrue(counts.containsKey(ore), host + " picked " + ore + ", which is not in its table");
                counts.merge(ore, 1, Integer::sum);
            }
            for (CoreOreTransmutation.WeightedOre entry : table) {
                double share = 100.0 * counts.get(entry.ore()) / picks;
                helper.assertTrue(Math.abs(share - entry.weight()) < 1.0,
                        BuiltInRegistries.BLOCK.getKey(entry.ore()) + " came up " + share + " % in " + host + " instead of about " + entry.weight() + " %");
            }
        }
        helper.assertTrue(CoreOreTransmutation.ores(CoreOreTransmutation.Host.NETHERRACK).getLast().ore() == Blocks.ANCIENT_DEBRIS,
                "ancient debris should be the rarest nether ore");
        helper.succeed();
    }

    /**
     * The chance ladder: 1 in 10000 for the copper core down to 1 in 2000 for enderite, strictly
     * better with every tier, each core item carrying its constant; "1 in 1" always hits, "1 in 0"
     * never, and 1 in 2000 over two million seeded draws hits about a thousand times.
     *
     * <p><strong>What breaks this test:</strong> a core wired to another tier's chance, a ladder that
     * is not increasing, or a chance roll that does not come from the random source it is handed.
     */
    public static void coreOreChanceClimbsFromCopperToEnderite(GameTestHelper helper) {
        int[] ladder = {CoreOreTransmutation.COPPER_CORE_ORE_CHANCE, CoreOreTransmutation.IRON_CORE_ORE_CHANCE,
                CoreOreTransmutation.GOLD_CORE_ORE_CHANCE, CoreOreTransmutation.DIAMOND_CORE_ORE_CHANCE,
                CoreOreTransmutation.NETHERITE_CORE_ORE_CHANCE, CoreOreTransmutation.ENDERITE_CORE_ORE_CHANCE};
        List<Item> cores = cores();
        for (int i = 0; i < ladder.length; i++) {
            BuildingCoreItem core = (BuildingCoreItem) cores.get(i);
            helper.assertValueEqual(core.oreChanceOneIn(), ladder[i], BuiltInRegistries.ITEM.getKey(core) + " ore chance (1 in N)");
            if (i > 0) {
                helper.assertTrue(ladder[i] < ladder[i - 1], BuiltInRegistries.ITEM.getKey(core) + " is not likelier than the tier below");
            }
        }
        helper.assertTrue(ladder[0] >= 5000, "the copper core should stay a curiosity, 1 in " + ladder[0]);
        helper.assertTrue(ladder[ladder.length - 1] >= 1000, "the enderite core should stay rare, 1 in " + ladder[ladder.length - 1]);
        RandomSource random = RandomSource.create(7L);
        for (int i = 0; i < 100; i++) {
            helper.assertTrue(CoreOreTransmutation.chanceHits(1, random), "1 in 1 missed");
            helper.assertFalse(CoreOreTransmutation.chanceHits(0, random), "1 in 0 hit");
        }
        RandomSource seeded = RandomSource.create(20260929L);
        int hits = 0;
        int draws = 2_000_000;
        for (int i = 0; i < draws; i++) {
            if (CoreOreTransmutation.chanceHits(2000, seeded)) {
                hits++;
            }
        }
        helper.assertTrue(Math.abs(hits - draws / 2000) < 150, "1 in 2000 hit " + hits + " times in " + draws + " draws");
        helper.succeed();
    }

    /**
     * The transmutation itself, with a forced chance (1 in 1) and a seeded random source: every host
     * block becomes an ore of its own table, in the world; a block that is no host stays as it is and
     * does not even draw from the random source; with the chance missed the host stays too.
     *
     * <p><strong>What breaks this test:</strong> a transmutation that places the wrong host's ore,
     * touches non-host blocks, or leaves the world unchanged although it reported an ore.
     */
    public static void coreTransmutationTurnsOnlyHostBlocksIntoTheirOres(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockPos absolute = helper.absolutePos(pos);
        RandomSource random = RandomSource.create(42L);
        Block[] hosts = {Blocks.STONE, Blocks.ANDESITE, Blocks.DEEPSLATE, Blocks.TUFF, Blocks.NETHERRACK, Blocks.END_STONE};
        for (Block hostBlock : hosts) {
            CoreOreTransmutation.Host host = CoreOreTransmutation.hostOf(hostBlock.defaultBlockState()).orElseThrow();
            List<Block> table = CoreOreTransmutation.ores(host).stream().map(CoreOreTransmutation.WeightedOre::ore).toList();
            for (int i = 0; i < 20; i++) {
                helper.setBlock(pos, hostBlock);
                Optional<Block> ore = CoreOreTransmutation.tryTransmute(level, absolute, 1, random);
                helper.assertTrue(ore.isPresent() && table.contains(ore.get()),
                        BuiltInRegistries.BLOCK.getKey(hostBlock) + " became " + ore + ", not an ore of " + host);
                helper.assertTrue(helper.getBlockState(pos).is(ore.get()), "the world does not show " + ore.get());
            }
            helper.setBlock(pos, hostBlock);
            helper.assertTrue(CoreOreTransmutation.tryTransmute(level, absolute, 0, random).isEmpty(), "a missed chance transmuted");
            helper.assertTrue(helper.getBlockState(pos).is(hostBlock), "a missed chance changed " + hostBlock);
        }
        RandomSource twin = RandomSource.create(99L);
        RandomSource probe = RandomSource.create(99L);
        for (Block other : List.of(Blocks.COBBLESTONE, Blocks.DIRT, Blocks.OBSIDIAN, Blocks.BLACKSTONE)) {
            helper.setBlock(pos, other);
            helper.assertTrue(CoreOreTransmutation.tryTransmute(level, absolute, 1, probe).isEmpty(), other + " was transmuted");
            helper.assertTrue(helper.getBlockState(pos).is(other), other + " changed");
        }
        helper.assertValueEqual(probe.nextLong(), twin.nextLong(), "random draws for non-host blocks");
        helper.succeed();
    }

    /**
     * Owner rule: gadgets show no text. A block click that transmutes (the real click path with the
     * chance forced to 1 in 1) turns the stone into ore but sends nothing to chat or the action bar,
     * and the removed message key stays out of both language files.
     *
     * <p><strong>What breaks this test:</strong> any {@code sendOverlayMessage}/{@code sendSystemMessage}
     * on the transmutation path, or the key {@code message.simplebuilding.core.ore_transmuted} coming back.
     */
    public static void coreTransmutationShowsNoText(GameTestHelper helper) {
        List<Component> texts = new ArrayList<>();
        ServerPlayer player = textRecordingPlayer(helper, texts);
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockPos absolute = helper.absolutePos(pos);
        List<Block> stoneTable = CoreOreTransmutation.ores(CoreOreTransmutation.Host.STONE).stream()
                .map(CoreOreTransmutation.WeightedOre::ore).toList();
        ItemStack core = new ItemStack(ModItems.ENDERITE_CORE);
        player.setItemInHand(InteractionHand.MAIN_HAND, core);
        for (int i = 0; i < 5; i++) {
            helper.setBlock(pos, Blocks.STONE);
            BuildingCoreItem.transmuteOnClick(helper.getLevel(), player, core, absolute, Direction.UP, 1);
            Block after = helper.getBlockState(pos).getBlock();
            helper.assertTrue(stoneTable.contains(after), "a forced click left " + after + " instead of a stone ore");
        }
        helper.assertTrue(texts.isEmpty(), "the transmutation showed text: " + texts);
        for (String code : List.of("en_us", "de_de")) {
            try (java.io.InputStream in = BuildingCoreTests.class.getResourceAsStream("/assets/simplebuilding/lang/" + code + ".json")) {
                helper.assertTrue(in != null, "no " + code + ".json on the classpath");
                String json = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                helper.assertFalse(json.contains("message.simplebuilding.core.ore_transmuted"), code + " still has the ore message");
            } catch (java.io.IOException e) {
                throw new IllegalStateException("cannot read " + code + ".json", e);
            }
        }
        helper.succeed();
    }

    /**
     * A player in the level (like {@code GameTestHelper#makeMockServerPlayerInLevel}) that records every
     * action-bar line and every chat line of the mod: {@code sendOverlayMessage} and
     * {@code sendSystemMessage} both end in {@code sendSystemMessage(Component, boolean)}.
     */
    private static ServerPlayer textRecordingPlayer(GameTestHelper helper, List<Component> texts) {
        ServerLevel level = helper.getLevel();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "core-texts"), false);
        ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public GameType gameMode() {
                return GameType.CREATIVE;
            }

            @Override
            public void sendSystemMessage(Component message, boolean overlay) {
                if (overlay || (message.getContents() instanceof TranslatableContents key && key.getKey().contains("simplebuilding"))) {
                    texts.add(message);
                }
            }
        };
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 2, 2.5));
        player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
        helper.runBeforeTestEnd(() -> level.getServer().getPlayerList().remove(player));
        return player;
    }

    private static ItemStack one(Ingredient ingredient) {
        // Prefer the core if the slot takes one, so the core itself is what is tested.
        Optional<Holder<Item>> core = ingredient.items().filter(BuildingCoreTests::isCore).findFirst();
        return new ItemStack(core.orElseGet(() -> ingredient.items().findFirst().orElseThrow()));
    }
}
