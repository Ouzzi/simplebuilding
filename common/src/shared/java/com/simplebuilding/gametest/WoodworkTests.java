package com.simplebuilding.gametest;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.version.McVersion;
import com.simplebuilding.woodwork.CarvedLogBlock;
import com.simplebuilding.woodwork.CrateBlockEntity;
import com.simplebuilding.woodwork.HollowLogBlock;
import com.simplebuilding.woodwork.HollowLogCrawl;
import com.simplebuilding.woodwork.SherdMotif;
import com.simplebuilding.woodwork.WoodBlocks;
import com.simplebuilding.woodwork.WoodKind;
import com.simplebuilding.woodwork.WoodenCauldronBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Holzwerk (docs/ai/PLAN-HOLZWERK-2026-10-09.md): hollow logs (tube, crawling, small mobs), sheets (connect, let light
 * through), wooden cauldrons (Vanilla interactions, burning with lava, Nether wood holds it), crates (food only, eight
 * stacks, comparator, hopper, drops) and carving sherd motifs with the chisel. Loader-neutral; without
 * {@link McVersion#WOODWORK} the tests pass without checking.
 */
public final class WoodworkTests {
    public static final int CAULDRON_MAX_TICKS = WoodenCauldronBlock.BURN_TICKS + 60;

    private WoodworkTests() {
    }

    private static WoodBlocks.Family oak() {
        return WoodBlocks.family(WoodKind.OAK);
    }

    private static ServerPlayer player(GameTestHelper helper, GameType mode) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(mode);
        return player;
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos pos, Direction face) {
        Vec3 centre = Vec3.atCenterOf(helper.absolutePos(pos)).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        return new BlockHitResult(centre, face, helper.absolutePos(pos), false);
    }

    /** Right-click on the block at {@code pos} with {@code stack} in the main hand (the block's own use). */
    private static void useBlock(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos pos) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockState state = helper.getBlockState(pos);
        var result = state.useItemOn(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit(helper, pos, Direction.UP));
        if (!result.consumesAction() && stack.isEmpty()) {
            state.useWithoutItem(helper.getLevel(), player, hit(helper, pos, Direction.UP));
        }
    }

    private static void check(boolean ok, List<String> fails, String what) {
        if (!ok) {
            fails.add(what);
        }
    }

    private static void finish(GameTestHelper helper, List<String> fails) {
        if (fails.isEmpty()) {
            helper.succeed();
        } else {
            helper.fail(String.join("; ", fails));
        }
    }

    public static void hollowLogsLetSmallMobsAndCrawlersThrough(GameTestHelper helper) {
        if (!McVersion.WOODWORK) {
            helper.succeed();
            return;
        }
        List<String> fails = new ArrayList<>();
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(2, 1, 2);
        helper.setBlock(rel, oak().hollow().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        BlockPos pos = helper.absolutePos(rel);
        double cx = pos.getX() + 0.5;
        double cz = pos.getZ() + 0.5;
        double floor = pos.getY() + HollowLogBlock.WALL / 16.0;
        AABB crawling = new AABB(cx - 0.3, floor, cz - 0.3, cx + 0.3, floor + 0.6, cz + 0.3);
        AABB crouching = new AABB(cx - 0.3, floor, cz - 0.3, cx + 0.3, floor + 1.5, cz + 0.3);
        check(level.noCollision(crawling), fails, "a crawling player fits inside");
        check(!level.noCollision(crouching), fails, "a crouching player does not fit inside");
        // Chicken-sized (0.4 x 0.7) and cat-sized (0.6 x 0.7) boxes fit through the opening.
        check(level.noCollision(new AABB(cx - 0.2, floor, cz - 0.2, cx + 0.2, floor + 0.7, cz + 0.2)), fails, "a chicken fits inside");
        check(level.noCollision(new AABB(cx - 0.3, floor, cz - 0.3, cx + 0.3, floor + 0.7, cz + 0.3)), fails, "a cat fits inside");
        check(!level.noCollision(new AABB(cx - 0.45, floor, cz - 0.45, cx + 0.45, floor + 1.4, cz + 0.45)), fails, "a cow-sized mob does not fit");
        check(helper.getBlockState(rel).isPathfindable(net.minecraft.world.level.pathfinder.PathComputationType.LAND), fails, "lying tube is pathfindable");

        ServerPlayer player = player(helper, GameType.SURVIVAL);
        player.setPos(pos.getX() - 0.5, pos.getY(), cz);
        player.setYRot(-90.0F); // facing east, along the tube
        player.setShiftKeyDown(true);
        check(HollowLogCrawl.shouldCrawl(player), fails, "sneaking in front of the tube along its axis crawls");
        player.setShiftKeyDown(false);
        check(!HollowLogCrawl.shouldCrawl(player), fails, "not sneaking does not crawl");
        player.setShiftKeyDown(true);
        player.setYRot(0.0F); // facing south, across the tube
        check(!HollowLogCrawl.shouldCrawl(player), fails, "facing across the tube does not crawl");
        finish(helper, fails);
    }

    public static void sheetsConnectAndLetLightThrough(GameTestHelper helper) {
        if (!McVersion.WOODWORK) {
            helper.succeed();
            return;
        }
        List<String> fails = new ArrayList<>();
        BlockPos a = new BlockPos(1, 1, 1);
        BlockPos b = new BlockPos(2, 1, 1);
        BlockPos glass = new BlockPos(1, 1, 2);
        helper.setBlock(glass, Blocks.GLASS_PANE);
        helper.setBlock(b, oak().strippedSheet());
        helper.setBlock(a, oak().sheet());
        // Neighbour updates connect the shapes (placing on its own does not run getStateForPlacement).
        BlockState sa = Block.updateFromNeighbourShapes(helper.getBlockState(a), helper.getLevel(), helper.absolutePos(a));
        check(sa.getValue(CrossCollisionBlock.EAST), fails, "sheet connects to the stripped sheet");
        check(sa.getValue(CrossCollisionBlock.SOUTH), fails, "sheet connects to a glass pane");
        check(!sa.getValue(CrossCollisionBlock.WEST), fails, "no connection to air");
        check(!sa.canOcclude() && sa.propagatesSkylightDown(), fails, "sheet lets light through");
        BlockState hollow = oak().hollow().defaultBlockState();
        check(!hollow.canOcclude(), fails, "hollow log does not occlude");
        finish(helper, fails);
    }

    public static void woodenCauldronHoldsWaterAndBurnsWithLava(GameTestHelper helper) {
        if (!McVersion.WOODWORK) {
            helper.succeed();
            return;
        }
        WoodenCauldronBlock oak = oak().cauldron();
        WoodenCauldronBlock crimson = WoodBlocks.family(WoodKind.CRIMSON).cauldron();
        BlockPos water = new BlockPos(1, 1, 1);
        BlockPos lava = new BlockPos(3, 1, 1);
        BlockPos nether = new BlockPos(5, 1, 1);
        helper.setBlock(water, oak);
        helper.setBlock(lava, oak);
        helper.setBlock(nether, crimson);
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        List<String> fails = new ArrayList<>();
        useBlock(helper, player, new ItemStack(Items.WATER_BUCKET), water);
        BlockState filled = helper.getBlockState(water);
        check(filled.is(oak) && filled.getValue(WoodenCauldronBlock.CONTENT) == WoodenCauldronBlock.Content.WATER
                && WoodenCauldronBlock.levelOf(filled) == 3, fails, "water bucket fills it (" + filled + ")");
        check(player.getMainHandItem().is(Items.BUCKET), fails, "the bucket comes back empty");
        useBlock(helper, player, new ItemStack(Items.GLASS_BOTTLE), water);
        check(WoodenCauldronBlock.levelOf(helper.getBlockState(water)) == 2, fails, "a bottle takes one level");
        useBlock(helper, player, new ItemStack(Items.LAVA_BUCKET), lava);
        useBlock(helper, player, new ItemStack(Items.LAVA_BUCKET), nether);
        check(helper.getBlockState(lava).getValue(WoodenCauldronBlock.CONTENT) == WoodenCauldronBlock.Content.LAVA, fails, "lava goes in");
        if (!fails.isEmpty()) {
            finish(helper, fails);
            return;
        }
        helper.runAfterDelay(WoodenCauldronBlock.BURN_TICKS + 10, () -> {
            check(helper.getBlockState(lava).is(Blocks.LAVA), fails, "the oak cauldron burns away and leaves lava ("
                    + helper.getBlockState(lava) + ")");
            BlockState kept = helper.getBlockState(nether);
            check(kept.is(crimson) && kept.getValue(WoodenCauldronBlock.CONTENT) == WoodenCauldronBlock.Content.LAVA, fails,
                    "a crimson cauldron keeps its lava");
            helper.setBlock(lava, Blocks.AIR);
            helper.setBlock(lava.above(), Blocks.AIR);
            finish(helper, fails);
        });
    }

    public static void cratesStoreFoodForHoppersAndComparators(GameTestHelper helper) {
        if (!McVersion.WOODWORK) {
            helper.succeed();
            return;
        }
        List<String> fails = new ArrayList<>();
        BlockPos rel = new BlockPos(2, 2, 2);
        helper.setBlock(rel, oak().crate());
        CrateBlockEntity crate = (CrateBlockEntity) helper.getBlockEntity(rel, CrateBlockEntity.class);
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        useBlock(helper, player, new ItemStack(Items.DIRT, 10), rel);
        check(crate.isEmpty(), fails, "dirt stays out");
        check(player.getMainHandItem().getCount() == 10, fails, "dirt stays in the hand");
        useBlock(helper, player, new ItemStack(Items.BREAD, 40), rel);
        check(crate.countItem(Items.BREAD) == 40 && player.getMainHandItem().isEmpty(), fails, "the bread stack goes in");
        ItemStack rest = crate.insert(new ItemStack(Items.APPLE, 64 * 8));
        check(crate.countItem(Items.APPLE) == 64 * 7 && rest.getCount() == 64, fails, "eight stacks in all (apples " + crate.countItem(Items.APPLE) + ")");
        int signal = helper.getBlockState(rel).getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(rel), Direction.NORTH);
        check(signal >= 14, fails, "comparator reads it nearly full (" + signal + ")");
        useBlock(helper, player, ItemStack.EMPTY, rel);
        check(player.getInventory().countItem(Items.APPLE) == 64, fails, "empty hand takes the top stack");
        player.setShiftKeyDown(true);
        useBlock(helper, player, ItemStack.EMPTY, rel);
        check(player.getInventory().countItem(Items.APPLE) == 65, fails, "sneaking takes one");
        crate.clearContent();
        crate.insert(new ItemStack(Items.CARROT, 5));
        helper.setBlock(rel.below(), Blocks.HOPPER);
        if (!fails.isEmpty()) {
            finish(helper, fails);
            return;
        }
        helper.runAfterDelay(60, () -> {
            check(crate.isEmpty(), fails, "a hopper empties the crate");
            var hopper = (net.minecraft.world.Container) helper.getBlockEntity(rel.below(), net.minecraft.world.level.block.entity.HopperBlockEntity.class);
            check(hopper.countItem(Items.CARROT) == 5, fails, "the hopper got the carrots");
            hopper.clearContent();
            helper.setBlock(rel.below(), Blocks.STONE);
            crate.insert(new ItemStack(Items.COOKED_BEEF, 3));
            helper.getLevel().destroyBlock(helper.absolutePos(rel), true);
            AABB box = new AABB(helper.absolutePos(rel)).inflate(2.0);
            int beef = 0;
            boolean crateItem = false;
            for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, box)) {
                if (entity.getItem().is(Items.COOKED_BEEF)) beef += entity.getItem().getCount();
                if (entity.getItem().is(oak().crate().asItem())) crateItem = true;
                entity.discard();
            }
            check(beef == 3, fails, "breaking drops the contents (" + beef + ")");
            check(crateItem, fails, "breaking drops the crate");
            finish(helper, fails);
        });
    }

    public static void chiselCarvesSherdMotifsAndHollowsLogs(GameTestHelper helper) {
        if (!McVersion.WOODWORK) {
            helper.succeed();
            return;
        }
        List<String> fails = new ArrayList<>();
        BlockPos rel = new BlockPos(2, 1, 2);
        BlockPos plain = new BlockPos(4, 1, 2);
        helper.setBlock(rel, Blocks.STRIPPED_OAK_LOG);
        helper.setBlock(plain, Blocks.STRIPPED_OAK_LOG);
        ServerPlayer player = player(helper, GameType.CREATIVE);
        ItemStack chisel = new ItemStack(ModItems.STONE_CHISEL);
        player.setItemInHand(InteractionHand.MAIN_HAND, chisel);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.ANGLER_POTTERY_SHERD));
        chisel.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, rel, Direction.EAST)));
        BlockState carved = helper.getBlockState(rel);
        check(carved.is(oak().carved()) && carved.getValue(CarvedLogBlock.FACING) == Direction.EAST
                && carved.getValue(CarvedLogBlock.MOTIF) == SherdMotif.ANGLER, fails, "angler carved east (" + carved + ")");
        check(player.getOffhandItem().is(Items.ANGLER_POTTERY_SHERD), fails, "the sherd stays");
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.HEART_POTTERY_SHERD));
        chisel.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, rel, Direction.EAST)));
        check(helper.getBlockState(rel).getValue(CarvedLogBlock.MOTIF) == SherdMotif.HEART, fails, "re-carving changes the motif");
        chisel.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, plain, Direction.UP)));
        check(helper.getBlockState(plain).is(Blocks.STRIPPED_OAK_LOG), fails, "with a sherd the top face does nothing");
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        chisel.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, plain, Direction.UP)));
        check(helper.getBlockState(plain).is(oak().hollowStripped()), fails, "without a sherd the chisel hollows the log ("
                + helper.getBlockState(plain) + ")");
        List<ItemStack> drops = Block.getDrops(helper.getBlockState(rel), helper.getLevel(), helper.absolutePos(rel), null);
        boolean keepsMotif = drops.size() == 1 && drops.get(0).is(oak().carved().asItem())
                && drops.get(0).has(DataComponents.BLOCK_STATE)
                && "heart".equals(drops.get(0).get(DataComponents.BLOCK_STATE).properties().get("motif"));
        check(keepsMotif, fails, "carved wood drops with its motif (" + drops + ")");
        for (SherdMotif motif : SherdMotif.values()) {
            check(SherdMotif.of(new ItemStack(motif.sherd())) == motif, fails, "sherd for " + motif.getSerializedName());
        }
        finish(helper, fails);
    }

    public static void woodworkRecipesExistForEveryWood(GameTestHelper helper) {
        if (!McVersion.WOODWORK) {
            helper.succeed();
            return;
        }
        List<String> missing = new ArrayList<>();
        var recipes = helper.getLevel().getServer().getRecipeManager();
        for (WoodBlocks.Family family : WoodBlocks.families()) {
            for (Block block : List.of(family.hollow(), family.hollowStripped(), family.sheet(), family.strippedSheet(), family.cauldron(), family.crate())) {
                Identifier id = BuiltInRegistries.BLOCK.getKey(block);
                if (recipes.byKey(ResourceKey.create(Registries.RECIPE, id)).isEmpty()) {
                    missing.add(id.getPath());
                }
            }
            if (family.wood().nether() == (family.hollow().defaultBlockState().ignitedByLava())) {
                missing.add("lava ignition " + family.wood().id());
            }
        }
        if (missing.isEmpty()) {
            helper.succeed();
        } else {
            helper.fail("missing: " + missing);
        }
    }
}
