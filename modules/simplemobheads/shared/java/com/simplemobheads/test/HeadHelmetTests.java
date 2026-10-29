package com.simplemobheads.test;

import com.simplemobheads.MobHeads;
import com.simplemobheads.helmet.HeadHelmets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Game tests of BS25-17 (giant heads next to each other) and BS28b-1 (helmets on heads); registered in {@link MobHeadTests}. */
final class HeadHelmetTests {
    private HeadHelmetTests() {
    }

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        player.snapTo(h.absoluteVec(new Vec3(2.5, 2.0, 2.5)));
        return player;
    }

    private static int dropped(GameTestHelper h, net.minecraft.world.item.Item item) {
        int n = 0;
        for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, h.getBounds().inflate(8.0), e -> e.getItem().is(item))) {
            n += e.getItem().getCount();
        }
        return n;
    }

    /** Right-click with the main hand like the game does: the block first, then the item. */
    private static InteractionResult click(GameTestHelper h, ServerPlayer player, BlockPos rel) {
        BlockPos at = h.absolutePos(rel);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false);
        var state = h.getLevel().getBlockState(at);
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        InteractionResult result = held.isEmpty() ? state.useWithoutItem(h.getLevel(), player, hit)
                : state.useItemOn(held, h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        if (result == InteractionResult.TRY_WITH_EMPTY_HAND) {
            result = state.useWithoutItem(h.getLevel(), player, hit);
        }
        return result;
    }

    /** BS25-17: a giant head is not placed when another stands in the 3x3x3 around it; ordinary heads never block. */
    static void giantHeadsKeepApart(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        BlockPos centre = new BlockPos(2, 2, 2);
        h.setBlock(centre, MobHeads.standing("giant"));
        BlockPos abs = h.absolutePos(centre);
        h.assertTrue(!MobHeads.crowded(level, abs), "a giant head is crowded by itself");
        for (BlockPos near : new BlockPos[] {new BlockPos(1, 0, 0), new BlockPos(0, 0, -1), new BlockPos(1, 0, 1), new BlockPos(0, 1, 0), new BlockPos(-1, -1, 1)}) {
            h.assertTrue(MobHeads.crowded(level, abs.offset(near)), "a giant head at +" + near + " would overlap");
        }
        for (BlockPos far : new BlockPos[] {new BlockPos(2, 0, 0), new BlockPos(0, 0, 2), new BlockPos(0, 2, 0), new BlockPos(-3, 0, 1)}) {
            h.assertTrue(!MobHeads.crowded(level, abs.offset(far)), "a giant head " + far + " away does not overlap");
        }
        // through the item: the second giant head next to it is refused, one two blocks away is placed
        ServerPlayer player = player(h);
        ItemStack giant = new ItemStack(MobHeads.item("giant"), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, giant);
        BlockPos floor = h.absolutePos(new BlockPos(3, 1, 2));
        InteractionResult next = giant.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(floor), Direction.UP, floor, false)));
        h.assertTrue(!next.consumesAction() && level.getBlockState(floor.above()).isAir(), "a giant head was placed next to a giant head: " + next);
        floor = h.absolutePos(new BlockPos(5, 1, 2));
        giant.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(floor), Direction.UP, floor, false)));
        h.assertTrue(level.getBlockState(floor.above()).is(MobHeads.standing("giant")), "no giant head two blocks away");
        // an ordinary head next to a giant head is fine
        ItemStack cat = new ItemStack(MobHeads.item("cat"));
        player.setItemInHand(InteractionHand.MAIN_HAND, cat);
        floor = h.absolutePos(new BlockPos(3, 1, 3));
        cat.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(floor), Direction.UP, floor, false)));
        h.assertTrue(level.getBlockState(floor.above()).is(MobHeads.standing("cat")), "a cat head next to a giant head was refused");
        h.succeed();
    }

    /** BS28b-1: helmet on, second helmet refused, off again, drops when the head breaks, not on heads without a face. */
    static void helmetOnHead(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos rel = new BlockPos(2, 2, 2);
        BlockPos abs = h.absolutePos(rel);
        ServerPlayer player = player(h);
        h.setBlock(rel, Blocks.SKELETON_SKULL);
        ItemStack iron = new ItemStack(Items.IRON_HELMET, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, iron);
        h.assertTrue(click(h, player, rel) == InteractionResult.SUCCESS, "the click did nothing");
        h.assertTrue(HeadHelmets.get(level, abs) != null && HeadHelmets.get(level, abs).is(Items.IRON_HELMET), "the helmet is not on the head");
        h.assertTrue(iron.getCount() == 1, "the helmet was not used up: " + iron.getCount());
        h.assertTrue(level.getBlockEntity(abs) instanceof SkullBlockEntity skull
                && skull.getUpdateTag(level.registryAccess()).contains("components"), "clients would not see the helmet");
        // one helmet per head
        click(h, player, rel);
        h.assertTrue(iron.getCount() == 1 && HeadHelmets.get(level, abs).is(Items.IRON_HELMET), "a second helmet went on");
        // empty hand: off again, into the inventory
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.assertTrue(click(h, player, rel) == InteractionResult.SUCCESS && HeadHelmets.get(level, abs) == null, "the helmet is still on");
        h.assertTrue(player.getInventory().contains(s -> s.is(Items.IRON_HELMET)), "the helmet is not in the inventory");
        // dyed leather keeps its colour, the head keeps its name
        ItemStack dyed = new ItemStack(Items.LEATHER_HELMET);
        dyed.set(DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(0x3366CC));
        player.setItemInHand(InteractionHand.MAIN_HAND, dyed);
        click(h, player, rel);
        ItemStack worn = HeadHelmets.get(level, abs);
        h.assertTrue(worn != null && worn.get(DataComponents.DYED_COLOR) != null && worn.get(DataComponents.DYED_COLOR).rgb() == 0x3366CC, "the dye got lost");
        // broken head: the helmet falls out
        level.setBlock(abs, Blocks.AIR.defaultBlockState(), 3);
        h.assertTrue(dropped(h, Items.LEATHER_HELMET) == 1, "the broken head did not drop its helmet: " + dropped(h, Items.LEATHER_HELMET));
        // heads without a human face and things that are no helmets
        h.setBlock(rel, Blocks.CREEPER_HEAD);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_HELMET));
        click(h, player, rel);
        h.assertTrue(HeadHelmets.get(level, abs) == null, "a creeper head took a helmet");
        h.setBlock(rel, MobHeads.standing("cat"));
        click(h, player, rel);
        h.assertTrue(HeadHelmets.get(level, abs) == null, "a cat head took a helmet");
        h.setBlock(rel, MobHeads.standing("enderman"));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        click(h, player, rel);
        h.assertTrue(HeadHelmets.get(level, abs) == null, "a stone went on the head");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_HELMET));
        click(h, player, rel);
        h.assertTrue(HeadHelmets.get(level, abs) != null, "an Enderman head took no helmet");
        h.assertTrue(HeadHelmets.size(level.getBlockState(abs)) == 1.0F && HeadHelmets.size(level.getBlockState(abs)) > 0, "helmet size");
        h.setBlock(rel, Blocks.AIR);
        h.succeed();
    }

    /** BS28b-2: wither skulls take helmets up to Diamond; the Netherite helmet belongs to the crown of Simple Mobs when it is there. */
    static void helmetOnWitherSkull(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos rel = new BlockPos(2, 2, 2);
        BlockPos abs = h.absolutePos(rel);
        ServerPlayer player = player(h);
        for (var helmet : new net.minecraft.world.item.Item[] {Items.LEATHER_HELMET, Items.COPPER_HELMET, Items.CHAINMAIL_HELMET,
                Items.IRON_HELMET, Items.GOLDEN_HELMET, Items.DIAMOND_HELMET}) {
            h.setBlock(rel, Blocks.AIR); // a fresh skull each time: one helmet per head
            h.setBlock(rel, Blocks.WITHER_SKELETON_SKULL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(helmet));
            click(h, player, rel);
            h.assertTrue(HeadHelmets.get(level, abs) != null && HeadHelmets.get(level, abs).is(helmet), "the wither skull took no " + helmet);
        }
        h.setBlock(rel, Blocks.AIR);
        h.setBlock(rel, Blocks.WITHER_SKELETON_SKULL);
        ItemStack netherite = new ItemStack(Items.NETHERITE_HELMET);
        player.setItemInHand(InteractionHand.MAIN_HAND, netherite);
        InteractionResult result = h.getLevel().getBlockState(abs).useItemOn(netherite, level, player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        boolean taken = HeadHelmets.get(level, abs) != null;
        h.assertTrue(taken != HeadHelmets.crownsAvailable() && (taken || !result.consumesAction()),
                "Netherite helmet on a wither skull: taken " + taken + " with Simple Mobs " + HeadHelmets.crownsAvailable());
        h.setBlock(rel, Blocks.AIR);
        h.succeed();
    }
}
