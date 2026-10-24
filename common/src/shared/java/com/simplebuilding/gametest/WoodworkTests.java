package com.simplebuilding.gametest;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.version.McVersion;
import com.simplebuilding.woodwork.CarvedLogBlock;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
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
 * through), wooden cauldrons (Vanilla interactions, burning with lava, Nether wood holds it) and carving sherd motifs with
 * the chisel (the crate tests moved with the crates to Simple Storage's StorageTests, split stage 8a). Loader-neutral; without
 * {@link McVersion#WOODWORK} the tests pass without checking.
 */
public final class WoodworkTests {
    public static final int CAULDRON_MAX_TICKS = WoodenCauldronBlock.BURN_TICKS + 60;

    private WoodworkTests() {
    }

    /** AUDM24-4: Ashwood/Duskwood woodwork exists (hence tab, JEI, recipe book) only while Simple End is loaded. */
    public static void endWoodsOnlyWithSimpleEnd(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        for (WoodKind wood : WoodKind.values()) {
            if (!wood.external()) continue;
            for (String path : WoodBlocks.blockIds(wood)) {
                Identifier id = Identifier.fromNamespaceAndPath("simplebuilding", path);
                if (BuiltInRegistries.BLOCK.containsKey(id) != wood.available()) problems.add("block " + path);
                if (BuiltInRegistries.ITEM.containsKey(id) != wood.available()) problems.add("item " + path);
            }
        }
        helper.assertTrue(problems.isEmpty(), "End-wood woodwork must match Simple End being loaded: " + problems);
        helper.succeed();
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
        BlockPos strippedPos = new BlockPos(7, 1, 1);
        helper.setBlock(strippedPos, oak().strippedCauldron());
        useBlock(helper, player, new ItemStack(Items.WATER_BUCKET), strippedPos);
        check(helper.getBlockState(strippedPos).getValue(WoodenCauldronBlock.CONTENT) == WoodenCauldronBlock.Content.WATER, fails,
                "the stripped cauldron holds water too");
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

    /** N34: copper, Enderite and ceramic buckets work on the wooden cauldron like on the Vanilla one (no item identity checks). */
    public static void woodenCauldronTakesCrucibleBuckets(GameTestHelper helper) {
        if (!McVersion.WOODWORK || !com.simplebuilding.util.BucketRefs.present()) {
            helper.succeed();
            return;
        }
        WoodenCauldronBlock oak = oak().cauldron();
        WoodenCauldronBlock crimson = WoodBlocks.family(WoodKind.CRIMSON).cauldron(); // never burns: lava stays for the test
        BlockPos a = new BlockPos(1, 1, 1);
        BlockPos b = new BlockPos(3, 1, 1);
        BlockPos c = new BlockPos(5, 1, 1);
        helper.setBlock(a, oak);
        helper.setBlock(b, crimson);
        helper.setBlock(c, crimson);
        ServerPlayer player = player(helper, GameType.SURVIVAL);
        List<String> fails = new ArrayList<>();
        WoodenCauldronBlock.Content water = WoodenCauldronBlock.Content.WATER;
        WoodenCauldronBlock.Content lava = WoodenCauldronBlock.Content.LAVA;
        WoodenCauldronBlock.Content empty = WoodenCauldronBlock.Content.EMPTY;
        // full Enderite water bucket: pours one of its two buckets (half stays), the half one takes it back (full again)
        useBlock(helper, player, new ItemStack(com.simplebuilding.util.BucketRefs.item("enderite_water_bucket_full")), a);
        check(helper.getBlockState(a).getValue(WoodenCauldronBlock.CONTENT) == water && WoodenCauldronBlock.levelOf(helper.getBlockState(a)) == 3,
                fails, "full Enderite water bucket fills the oak cauldron (" + helper.getBlockState(a) + ")");
        check(player.getMainHandItem().is(com.simplebuilding.util.BucketRefs.item("enderite_water_bucket")), fails, "half Enderite water bucket stays");
        useBlock(helper, player, player.getMainHandItem(), a);
        check(helper.getBlockState(a).getValue(WoodenCauldronBlock.CONTENT) == empty
                && player.getMainHandItem().is(com.simplebuilding.util.BucketRefs.item("enderite_water_bucket_full")), fails,
                "the half bucket takes the full cauldron and becomes full (" + player.getMainHandItem() + ")");
        // empty Enderite bucket scoops a full water cauldron; copper water bucket pours
        useBlock(helper, player, new ItemStack(com.simplebuilding.util.BucketRefs.item("copper_water_bucket")), a);
        check(helper.getBlockState(a).getValue(WoodenCauldronBlock.CONTENT) == water && player.getMainHandItem().is(com.simplebuilding.util.BucketRefs.item("copper_bucket")),
                fails, "copper water bucket fills it, the empty copper bucket stays");
        useBlock(helper, player, new ItemStack(com.simplebuilding.util.BucketRefs.item("enderite_bucket")), a);
        check(helper.getBlockState(a).getValue(WoodenCauldronBlock.CONTENT) == empty
                && player.getMainHandItem().is(com.simplebuilding.util.BucketRefs.item("enderite_water_bucket")), fails, "the empty Enderite bucket scoops it (half)");
        // lava: full Enderite lava, copper lava breaks, ceramic lava
        useBlock(helper, player, new ItemStack(com.simplebuilding.util.BucketRefs.item("enderite_lava_bucket_full")), b);
        check(helper.getBlockState(b).getValue(WoodenCauldronBlock.CONTENT) == lava && player.getMainHandItem().is(com.simplebuilding.util.BucketRefs.item("enderite_lava_bucket")),
                fails, "full Enderite lava bucket fills the crimson cauldron (" + helper.getBlockState(b) + ")");
        useBlock(helper, player, player.getMainHandItem(), b);
        check(helper.getBlockState(b).getValue(WoodenCauldronBlock.CONTENT) == empty
                && player.getMainHandItem().is(com.simplebuilding.util.BucketRefs.item("enderite_lava_bucket_full")), fails, "the half lava bucket takes lava back");
        useBlock(helper, player, new ItemStack(com.simplebuilding.util.BucketRefs.item("copper_lava_bucket")), b);
        check(helper.getBlockState(b).getValue(WoodenCauldronBlock.CONTENT) == lava && player.getMainHandItem().isEmpty(), fails,
                "copper lava bucket pours and breaks (" + player.getMainHandItem() + ")");
        useBlock(helper, player, new ItemStack(com.simplebuilding.util.BucketRefs.item("enderite_bucket")), b);
        check(helper.getBlockState(b).getValue(WoodenCauldronBlock.CONTENT) == empty
                && player.getMainHandItem().is(com.simplebuilding.util.BucketRefs.item("enderite_lava_bucket")), fails, "the empty Enderite bucket scoops lava (half)");
        useBlock(helper, player, new ItemStack(com.simplebuilding.util.BucketRefs.item("ceramic_lava_bucket")), c);
        check(helper.getBlockState(c).getValue(WoodenCauldronBlock.CONTENT) == lava, fails, "ceramic lava bucket pours");
        // soul lava never goes into a wooden cauldron
        useBlock(helper, player, new ItemStack(com.simplebuilding.util.BucketRefs.item("enderite_soul_lava_bucket_full")), a);
        check(helper.getBlockState(a).getValue(WoodenCauldronBlock.CONTENT) == empty, fails, "soul lava is refused");
        finish(helper, fails);
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
        // N40: one click carves the whole block all round
        check(carved.is(oak().carved()) && CarvedLogBlock.carving(carved) == CarvedLogBlock.Carving.SHERD
                && carved.getValue(CarvedLogBlock.MOTIF) == SherdMotif.ANGLER, fails, "angler carved all round (" + carved + ")");
        check(player.getOffhandItem().is(Items.ANGLER_POTTERY_SHERD), fails, "the sherd stays");
        // another sherd on another side changes the motif of the whole block
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.HEART_POTTERY_SHERD));
        chisel.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, rel, Direction.WEST)));
        BlockState two = helper.getBlockState(rel);
        check(CarvedLogBlock.carving(two) == CarvedLogBlock.Carving.SHERD && two.getValue(CarvedLogBlock.MOTIF) == SherdMotif.HEART,
                fails, "a second sherd re-carves the whole block with its motif (" + two + ")");
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        chisel.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, plain, Direction.NORTH)));
        check(helper.getBlockState(plain).is(oak().carved()) && CarvedLogBlock.carving(helper.getBlockState(plain)) == CarvedLogBlock.Carving.WOOD,
                fails, "a plain stripped log carved without a sherd shows the wood motif all round (" + helper.getBlockState(plain) + ")");
        // sneaking takes the whole carving off at once
        player.setShiftKeyDown(true);
        chisel.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, plain, Direction.SOUTH)));
        check(helper.getBlockState(plain).is(Blocks.STRIPPED_OAK_LOG), fails, "sneaking takes the carving off the whole block ("
                + helper.getBlockState(plain) + ")");
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.ANGLER_POTTERY_SHERD));
        chisel.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, plain, Direction.UP)));
        check(helper.getBlockState(plain).is(Blocks.STRIPPED_OAK_LOG), fails, "with a sherd the top face does nothing");
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        chisel.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, plain, Direction.UP)));
        check(helper.getBlockState(plain).is(oak().hollowStripped()), fails, "without a sherd the chisel still hollows the log from the top ("
                + helper.getBlockState(plain) + ")");
        List<ItemStack> drops = Block.getDrops(helper.getBlockState(rel), helper.getLevel(), helper.absolutePos(rel), null);
        var props = drops.size() == 1 && drops.get(0).has(DataComponents.BLOCK_STATE) ? drops.get(0).get(DataComponents.BLOCK_STATE).properties() : null;
        boolean keepsCarving = props != null && drops.get(0).is(oak().carved().asItem()) && "heart".equals(props.get("motif"))
                && "sherd".equals(props.get("carving"));
        check(keepsCarving, fails, "carved wood drops with its carving and motif (" + drops + ")");
        // pre-N40 states (one carving per side) migrate: any sherd side -> sherd all round, else wood; side keys go
        net.minecraft.nbt.CompoundTag oldSherd = new net.minecraft.nbt.CompoundTag();
        oldSherd.putString("north", "none");
        oldSherd.putString("east", "sherd");
        oldSherd.putString("south", "wood");
        oldSherd.putString("west", "none");
        oldSherd.putString("motif", "skull");
        check(CarvedLogBlock.migrate(oldSherd) && "sherd".equals(oldSherd.getStringOr("carving", "")) && "skull".equals(oldSherd.getStringOr("motif", ""))
                && !oldSherd.contains("north") && !oldSherd.contains("east"), fails, "an old sherd-carved state migrates (" + oldSherd + ")");
        net.minecraft.nbt.CompoundTag oldWood = new net.minecraft.nbt.CompoundTag();
        oldWood.putString("north", "wood");
        oldWood.putString("east", "none");
        oldWood.putString("south", "none");
        oldWood.putString("west", "none");
        oldWood.putString("motif", "angler");
        check(CarvedLogBlock.migrate(oldWood) && "wood".equals(oldWood.getStringOr("carving", "")), fails, "an old wood-carved state migrates (" + oldWood + ")");
        check(!CarvedLogBlock.migrate(oldWood), fails, "a migrated state is left alone");
        net.minecraft.nbt.CompoundTag palette = new net.minecraft.nbt.CompoundTag();
        palette.putString("east", "sherd");
        palette.putString("motif", "heart");
        net.minecraft.nbt.CompoundTag entry = new net.minecraft.nbt.CompoundTag();
        entry.putString("id", "simplebuilding:carved_oak_log");
        entry.put("properties", palette);
        com.simplebuilding.datafix.ModDataFixer.walk(null, entry, 1, 1);
        check("sherd".equals(entry.getCompound("properties").map(t -> t.getStringOr("carving", "")).orElse("")), fails,
                "the data fixer walk migrates a saved palette entry (" + entry + ")");
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
            for (Block block : List.of(family.hollow(), family.hollowStripped(), family.sheet(), family.strippedSheet(), family.cauldron(), family.strippedCauldron())) {
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

    /**
     * N49: the carved log's inventory model shows the carving on all four sides like the placed block (an item model
     * cannot be turned like a blockstate part, so it uses the {@code _all} overlay models), for every wood.
     */
    public static void carvedLogItemShowsAllFourSides(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
            if (!id.getNamespace().equals(Simplebuilding.MOD_ID) || !id.getPath().startsWith("carved_")) continue;
            checked++;
            String text = resource("assets/simplebuilding/items/" + id.getPath() + ".json");
            if (text == null) { problems.add(id + ": no item definition"); continue; }
            if (!text.contains("_wood_all") || !text.contains("_angler_all") || text.contains("_wood\"") || text.contains("_angler\"")) {
                problems.add(id + ": item definition does not use the all-sides overlay models");
            }
            for (String overlay : new String[] {"wood", "angler"}) {
                String model = resource("assets/simplebuilding/models/block/carved/" + id.getPath() + "_" + overlay + "_all.json");
                if (model == null) problems.add(id + ": missing model " + overlay + "_all");
                else if (!(model.contains("\"north\"") && model.contains("\"east\"") && model.contains("\"south\"") && model.contains("\"west\"")
                        || model.contains("template_carved_"))) problems.add(id + ": " + overlay + "_all has no parent");
            }
        }
        helper.assertTrue(checked >= 12, "expected a carved log item per wood, found " + checked);
        helper.assertTrue(problems.isEmpty(), "carved log item models: " + problems);
        helper.succeed();
    }

    private static String resource(String path) {
        try (java.io.InputStream in = WoodworkTests.class.getClassLoader().getResourceAsStream(path)) {
            return in == null ? null : new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            return null;
        }
    }
}
