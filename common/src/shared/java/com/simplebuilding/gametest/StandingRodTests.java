package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.HammockLayout;
import com.simplebuilding.blocks.custom.PlacedSmallPartsBlock;
import com.simplebuilding.blocks.custom.StandingRodBlock;
import com.simplebuilding.blocks.custom.StandingRodBlock.Rod;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.version.McVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Standing rods (docs/ai/PLAN-SENKRECHTE-RODS-2026-10-04.md): sneak + right-click on a top stands a stick, bone, blaze,
 * breeze or diamond rod up; it drops itself, stacks, needs a floor, only the blaze rod glows, it holds water, a pile still
 * takes rods lying, the server options gate it. Loader-neutral; without {@link McVersion#STANDING_RODS} the tests pass.
 */
public final class StandingRodTests {
    private StandingRodTests() {
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 corner = helper.absoluteVec(new Vec3(7.5, 1.0, 7.5));
        player.setPos(corner.x, corner.y, corner.z);
        player.setShiftKeyDown(true);
        return player;
    }

    private static InteractionResult use(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos on, Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = helper.absolutePos(on);
        Vec3 hit = Vec3.atCenterOf(abs).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        return stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, abs, false)));
    }

    private static boolean standing(GameTestHelper helper, BlockPos pos, Rod rod) {
        BlockState state = helper.getBlockState(pos);
        return state.is(ModBlocks.STANDING_ROD) && state.getValue(StandingRodBlock.ROD) == rod;
    }

    private static int dropped(GameTestHelper helper, Item item) {
        AABB box = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8.0, 6.0, 8.0);
        int count = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, box, e -> e.getItem().is(item))) {
            count += entity.getItem().getCount();
        }
        return count;
    }

    /** Every rod stands up on a top when sneaking and uses one item; not without sneaking, not on a side. */
    public static void everyRodStandsUpOnTopWhenSneaking(GameTestHelper helper) {
        if (!McVersion.STANDING_RODS) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper);
        int x = 0;
        for (Rod rod : Rod.values()) {
            helper.assertTrue(rod.item() != null, rod + " has no item");
            BlockPos floor = new BlockPos(x, 1, 1);
            helper.setBlock(floor, Blocks.STONE);
            ItemStack stack = new ItemStack(rod.item(), 2);
            player.setShiftKeyDown(false);
            use(helper, player, stack, floor, Direction.UP);
            helper.assertFalse(helper.getBlockState(floor.above()).is(ModBlocks.STANDING_ROD), rod + " stood up without sneaking");
            player.setShiftKeyDown(true);
            InteractionResult result = use(helper, player, stack, floor, Direction.UP);
            helper.assertTrue(result.consumesAction(), rod + ": sneak + right-click answered " + result);
            helper.assertTrue(standing(helper, floor.above(), rod), rod + " does not stand: " + helper.getBlockState(floor.above()));
            helper.assertValueEqual(stack.getCount(), 1, rod + " items left");
            helper.assertValueEqual(Rod.of(new ItemStack(rod.item())), rod, rod + " from its item");
            // on a side it is no standing rod (small parts lie there like a template)
            BlockPos side = new BlockPos(x, 1, 3);
            helper.setBlock(side, Blocks.STONE);
            use(helper, player, new ItemStack(rod.item()), side, Direction.NORTH);
            helper.assertFalse(helper.getBlockState(side.north()).is(ModBlocks.STANDING_ROD), rod + " stands on a side");
            x++;
        }
        helper.assertTrue(Rod.of(new ItemStack(Items.ARROW)) == null, "an arrow is a standing rod");
        helper.assertTrue(Rod.of(new ItemStack(Items.END_ROD)) == null, "the end rod (a block) is a standing rod");
        helper.succeed();
    }

    /** Breaking or losing the floor drops the rod itself, once; rods stack on rods and fall together. */
    public static void rodsDropThemselvesAndStackIntoPosts(GameTestHelper helper) {
        if (!McVersion.STANDING_RODS) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper);
        BlockPos floor = new BlockPos(1, 1, 1);
        helper.setBlock(floor, Blocks.STONE);
        helper.assertTrue(use(helper, player, new ItemStack(Items.STICK), floor, Direction.UP).consumesAction(), "the stick did not stand");
        // sneak + right-click on the top of a standing rod stacks the next one
        helper.assertTrue(use(helper, player, new ItemStack(Items.BONE), floor.above(), Direction.UP).consumesAction(), "the bone did not stack");
        helper.assertTrue(standing(helper, floor.above(2), Rod.BONE), "no bone on the stick: " + helper.getBlockState(floor.above(2)));
        // without the stone both fall and drop themselves
        helper.setBlock(floor, Blocks.AIR);
        helper.assertTrue(helper.getBlockState(floor.above()).isAir() && helper.getBlockState(floor.above(2)).isAir(), "the post still stands");
        helper.assertValueEqual(dropped(helper, Items.STICK), 1, "sticks dropped");
        helper.assertValueEqual(dropped(helper, Items.BONE), 1, "bones dropped");
        // broken by a player: the diamond rod drops itself
        if (ModItems.DIAMOND_ROD != null) {
            BlockPos other = new BlockPos(4, 1, 1);
            helper.setBlock(other, Blocks.STONE);
            helper.setBlock(other.above(), ModBlocks.STANDING_ROD.defaultBlockState().setValue(StandingRodBlock.ROD, Rod.DIAMOND_ROD));
            helper.getLevel().destroyBlock(helper.absolutePos(other.above()), true, player);
            helper.assertValueEqual(dropped(helper, ModItems.DIAMOND_ROD), 1, "diamond rods dropped");
            BlockState state = ModBlocks.STANDING_ROD.defaultBlockState().setValue(StandingRodBlock.ROD, Rod.DIAMOND_ROD);
            helper.assertTrue(state.getCloneItemStack(helper.getLevel(), helper.absolutePos(other.above()), false).is(ModItems.DIAMOND_ROD),
                    "pick block is not the diamond rod");
        }
        // nothing to stand on in the air
        BlockPos air = new BlockPos(6, 3, 1);
        helper.assertFalse(ModBlocks.STANDING_ROD.defaultBlockState().canSurvive(helper.getLevel(), helper.absolutePos(air)), "a rod stands on air");
        helper.succeed();
    }

    /** Only the blaze rod glows; in water the rod is waterlogged; the thin shape collides (a hammock anchor). */
    public static void blazeRodGlowsAndRodsHoldWater(GameTestHelper helper) {
        if (!McVersion.STANDING_RODS) {
            helper.succeed();
            return;
        }
        for (Rod rod : Rod.values()) {
            BlockState state = ModBlocks.STANDING_ROD.defaultBlockState().setValue(StandingRodBlock.ROD, rod);
            helper.assertValueEqual(state.getLightEmission(), rod == Rod.BLAZE_ROD ? 5 : 0, rod + " light");
            helper.assertFalse(state.getCollisionShape(helper.getLevel(), helper.absolutePos(BlockPos.ZERO)).isEmpty(), rod + " has no collision");
            helper.assertValueEqual((int) Math.round(state.getShape(helper.getLevel(), helper.absolutePos(BlockPos.ZERO)).max(Direction.Axis.Y) * 16),
                    rod.height(), rod + " height");
        }
        ServerPlayer player = player(helper);
        BlockPos floor = new BlockPos(1, 1, 1);
        helper.setBlock(floor, Blocks.STONE);
        helper.setBlock(floor.above(), Blocks.WATER);
        helper.assertTrue(use(helper, player, new ItemStack(Items.BREEZE_ROD), floor, Direction.UP).consumesAction(), "no breeze rod in water");
        BlockState wet = helper.getBlockState(floor.above());
        helper.assertTrue(wet.is(ModBlocks.STANDING_ROD) && wet.getValue(StandingRodBlock.WATERLOGGED), "not waterlogged: " + wet);
        helper.assertTrue(HammockLayout.isAnchor(helper.getLevel(), helper.absolutePos(floor.above())), "a standing rod is no hammock anchor");
        helper.succeed();
    }

    /** A rod clicked on a pile of small parts is added lying; the server options switch standing rods off too. */
    public static void pilesTakeRodsAndTheServerOptionsGateThem(GameTestHelper helper) {
        if (!McVersion.STANDING_RODS) {
            helper.succeed();
            return;
        }
        var features = com.simplebuilding.config.ServerTuning.get().features;
        boolean vanilla = features.placeVanillaItems;
        String disabled = features.placeDisabledItems;
        try {
            features.placeVanillaItems = true;
            features.placeDisabledItems = "";
            ServerPlayer player = player(helper);
            BlockPos floor = new BlockPos(1, 1, 1);
            helper.setBlock(floor, Blocks.STONE);
            helper.assertTrue(use(helper, player, new ItemStack(Items.FLINT), floor, Direction.UP).consumesAction(), "no flint pile");
            helper.assertTrue(use(helper, player, new ItemStack(Items.STICK), floor, Direction.UP).consumesAction(), "the stick was refused");
            helper.assertTrue(helper.getBlockState(floor.above()).getBlock() instanceof PlacedSmallPartsBlock, "the pile is gone: "
                    + helper.getBlockState(floor.above()));
            helper.assertTrue(helper.getBlockState(floor.above(2)).isAir(), "a stick stood on the pile");
            BlockPos other = new BlockPos(4, 1, 1);
            helper.setBlock(other, Blocks.STONE);
            features.placeDisabledItems = "stick";
            use(helper, player, new ItemStack(Items.STICK), other, Direction.UP);
            helper.assertFalse(helper.getBlockState(other.above()).is(ModBlocks.STANDING_ROD), "a listed stick stood up");
            helper.assertTrue(StandingRodBlock.allowed(new ItemStack(Items.BONE)), "the unlisted bone is off");
            features.placeDisabledItems = "";
            features.placeVanillaItems = false;
            helper.assertFalse(StandingRodBlock.allowed(new ItemStack(Items.BLAZE_ROD)), "vanilla items off: the blaze rod still stands");
            if (ModItems.DIAMOND_ROD != null) {
                helper.assertTrue(StandingRodBlock.allowed(new ItemStack(ModItems.DIAMOND_ROD)), "vanilla items off: the diamond rod is off");
            }
        } finally {
            features.placeVanillaItems = vanilla;
            features.placeDisabledItems = disabled;
        }
        helper.succeed();
    }
}
