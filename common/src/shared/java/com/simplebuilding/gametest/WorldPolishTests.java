package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.PlacedSmallPartsBlock;
import com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.fluid.ModFluids;
import com.simplebuilding.util.PlacedSmallParts;
import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Queue N37, world part (owner 2026-10-11): placeables break part by part, buckets draw no durability bar and the
 * Enderite bucket shows only its name, the sculk jaw needs a full block top.
 */
public final class WorldPolishTests {
    private WorldPolishTests() {
    }

    /**
     * Breaking a pile of small parts (the real {@code ServerPlayerGameMode#destroyBlock} of a survival player) takes only
     * the part the player looks at: it drops, the others stay where they lay (the block, its candle count and its
     * facing). The last part breaks the block as before.
     */
    public static void pilesBreakOnlyTheLookedAtPart(GameTestHelper helper) {
        if (!McVersion.SMALL_PLACEABLES) {
            helper.succeed();
            return;
        }
        ServerPlayer player = survivalPlayer(helper);
        BlockPos rel = new BlockPos(2, 2, 2);
        helper.setBlock(rel.below(), Blocks.STONE);
        helper.setBlock(rel, ModBlocks.PLACED_SMALL_PARTS.defaultBlockState().setValue(PlacedSmallPartsBlock.FACING, Direction.EAST));
        PlacedSmallPartsBlockEntity pile = helper.getBlockEntity(rel, PlacedSmallPartsBlockEntity.class);
        pile.setParts(List.of(new ItemStack(Items.CANDLE), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.SEA_PICKLE)));
        BlockPos pos = helper.absolutePos(rel);
        helper.assertValueEqual(helper.getBlockState(rel).getValue(PlacedSmallPartsBlock.CANDLES), 1, "candles before");

        breakLookingAt(helper, player, pos, 1);
        helper.assertTrue(helper.getBlockState(rel).is(ModBlocks.PLACED_SMALL_PARTS), "the whole pile broke: " + helper.getBlockState(rel));
        helper.assertValueEqual(items(helper, rel), List.of(Items.CANDLE, Items.SEA_PICKLE), "parts left after breaking the iron ingot");
        helper.assertValueEqual(helper.getBlockState(rel).getValue(PlacedSmallPartsBlock.CANDLES), 1, "candles after");
        helper.assertValueEqual(helper.getBlockState(rel).getValue(PlacedSmallPartsBlock.FACING), Direction.EAST, "facing after");
        helper.assertValueEqual(dropped(helper, rel, Items.IRON_INGOT), 1, "dropped iron ingots");
        helper.assertValueEqual(dropped(helper, rel, Items.CANDLE) + dropped(helper, rel, Items.SEA_PICKLE), 0, "other parts dropped");

        breakLookingAt(helper, player, pos, 1);
        helper.assertValueEqual(items(helper, rel), List.of(Items.CANDLE), "parts left after breaking the sea pickle");
        helper.assertValueEqual(dropped(helper, rel, Items.SEA_PICKLE), 1, "dropped sea pickles");

        breakLookingAt(helper, player, pos, 0);
        helper.assertTrue(helper.getBlockState(rel).isAir(), "the last part did not break the block: " + helper.getBlockState(rel));
        helper.assertValueEqual(dropped(helper, rel, Items.CANDLE), 1, "dropped candles");
        helper.succeed();
    }

    /** Puts the player's eye one block above part {@code index} looking straight down, then breaks the block. */
    private static void breakLookingAt(GameTestHelper helper, ServerPlayer player, BlockPos pos, int index) {
        PlacedSmallPartsBlockEntity pile = (PlacedSmallPartsBlockEntity) helper.getLevel().getBlockEntity(pos);
        List<ItemStack> parts = pile.parts();
        Direction facing = helper.getLevel().getBlockState(pos).getValue(PlacedSmallPartsBlock.FACING);
        AABB part = PlacedSmallParts.partShape(parts.get(index), facing, parts.size(), index,
                PlacedSmallParts.ingotStack(parts)).bounds().move(pos);
        Vec3 top = new Vec3(part.getCenter().x, part.maxY, part.getCenter().z);
        player.snapTo(top.x, top.y + 1.0 - player.getEyeHeight(), top.z, 0.0F, 90.0F);
        helper.assertValueEqual(PlacedSmallParts.lookedAtPart(player, parts, facing, pos), index, "looked-at part");
        player.gameMode.destroyBlock(pos);
    }

    private static List<Item> items(GameTestHelper helper, BlockPos rel) {
        return helper.getLevel().getBlockEntity(helper.absolutePos(rel)) instanceof PlacedSmallPartsBlockEntity be
                ? be.parts().stream().map(ItemStack::getItem).toList() : List.of();
    }

    private static int dropped(GameTestHelper helper, BlockPos rel, Item item) {
        AABB around = new AABB(helper.absolutePos(rel)).inflate(2.0);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).stream()
                .filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
    }

    /** A real survival player (the mock player is always creative and drops nothing). */
    private static ServerPlayer survivalPlayer(GameTestHelper helper) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "n37-world"), false);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        helper.runBeforeTestEnd(() -> level.getServer().getPlayerList().remove(player));
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    /**
     * No bucket draws a durability bar (a worn ceramic one shows its wear in the texture), and every Enderite bucket -
     * empty, half, full, milk - has no tooltip line below its name.
     */
    public static void bucketsDrawNoBarAndTheEnderiteBucketShowsOnlyItsName(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        ItemStack worn = stack(ModFluids.CERAMIC_WATER_BUCKET);
        if (!worn.isEmpty()) {
            worn.set(ModDataComponentTypes.CERAMIC_USES, 1);
        }
        for (ItemStack stack : List.of(worn, stack(ModFluids.CERAMIC_BUCKET), stack(ModFluids.COPPER_WATER_BUCKET),
                stack(ModFluids.ENDERITE_WATER_BUCKET), stack(ModFluids.SOUL_LAVA_BUCKET))) {
            if (!stack.isEmpty() && stack.isBarVisible()) {
                problems.add(stack.getItem() + " draws a durability bar");
            }
        }
        for (Item item : new Item[]{ModFluids.ENDERITE_BUCKET, ModFluids.ENDERITE_WATER_BUCKET, ModFluids.FULL_ENDERITE_WATER_BUCKET,
                ModFluids.ENDERITE_LAVA_BUCKET, ModFluids.FULL_ENDERITE_LAVA_BUCKET, ModFluids.ENDERITE_SOUL_LAVA_BUCKET,
                ModFluids.FULL_ENDERITE_SOUL_LAVA_BUCKET, ModFluids.ENDERITE_MILK_BUCKET, ModFluids.FULL_ENDERITE_MILK_BUCKET}) {
            if (item == null) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            List<Component> lines = new ArrayList<>();
            item.appendHoverText(stack, Item.TooltipContext.of(helper.getLevel()), TooltipDisplay.DEFAULT, lines::add, TooltipFlag.NORMAL);
            if (!lines.isEmpty()) {
                problems.add(item + " has tooltip lines " + lines.stream().map(Component::getString).toList());
            }
            if (stack.isBarVisible()) {
                problems.add(item + " draws a durability bar");
            }
        }
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    private static ItemStack stack(Item item) {
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /**
     * The sculk jaw stands only on a block with a solid, full top: stone and an upper slab yes; a lower slab, a fence,
     * a glass pane or air no. Taking the block away under a jaw removes it.
     */
    public static void sculkJawNeedsAFullBlockTop(GameTestHelper helper) {
        if (!McVersion.NATURE_VARIANTS || ModBlocks.SCULK_JAW == null) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        var jaw = ModBlocks.SCULK_JAW.defaultBlockState();
        Object[][] floors = {
                {Blocks.STONE.defaultBlockState(), true},
                {Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP), true},
                {Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM), false},
                {Blocks.OAK_FENCE.defaultBlockState(), false},
                {Blocks.GLASS_PANE.defaultBlockState(), false},
                {Blocks.AIR.defaultBlockState(), false},
        };
        for (int i = 0; i < floors.length; i++) {
            BlockPos floor = new BlockPos(1 + i, 1, 1);
            helper.setBlock(floor, (net.minecraft.world.level.block.state.BlockState) floors[i][0]);
            boolean survives = jaw.canSurvive(level, helper.absolutePos(floor.above()));
            helper.assertValueEqual(survives, floors[i][1], "jaw on " + floors[i][0]);
        }
        BlockPos floor = new BlockPos(1, 1, 3);
        helper.setBlock(floor, Blocks.STONE);
        helper.setBlock(floor.above(), jaw);
        helper.setBlock(floor, Blocks.AIR);
        helper.assertTrue(helper.getBlockState(floor.above()).isAir(), "the jaw stayed without its floor: " + helper.getBlockState(floor.above()));
        helper.succeed();
    }
}
