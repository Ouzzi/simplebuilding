package com.simplelib.test;

import com.simplelib.api.DiscSides;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** N47 M6: the shared A/B-side block - the cycle rule, the turned stack and the right click with its cooldown. */
final class DiscSidesTests {
    private DiscSidesTests() {}

    static void register() {
        LibTests.ALL.put("disc_sides_cycle_and_turn", DiscSidesTests::cycleAndTurn);
        LibTests.ALL.put("disc_sides_right_click_turns", DiscSidesTests::rightClick);
    }

    /** next walks the cycle and wraps; a stranger or a lone item has none; the turned stack keeps the name. */
    static void cycleAndTurn(GameTestHelper h) {
        List<Item> cycle = List.of(Items.MUSIC_DISC_13, Items.MUSIC_DISC_CAT, Items.MUSIC_DISC_BLOCKS);
        h.assertTrue(DiscSides.next(cycle, Items.MUSIC_DISC_13) == Items.MUSIC_DISC_CAT, "1 -> 2");
        h.assertTrue(DiscSides.next(cycle, Items.MUSIC_DISC_BLOCKS) == Items.MUSIC_DISC_13, "last wraps to first");
        h.assertTrue(DiscSides.next(cycle, Items.STONE) == null, "stranger has no side");
        h.assertTrue(DiscSides.next(List.of(Items.MUSIC_DISC_13), Items.MUSIC_DISC_13) == null, "a single side does not turn");
        ItemStack named = new ItemStack(Items.MUSIC_DISC_13);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("mine"));
        ItemStack turned = DiscSides.turned(named, Items.MUSIC_DISC_CAT);
        h.assertTrue(turned.is(Items.MUSIC_DISC_CAT) && "mine".equals(turned.getHoverName().getString()), "name kept: " + turned);
        h.succeed();
    }

    /** The right click turns A to B, starts the cooldown and passes when the turner has nothing. */
    static void rightClick(GameTestHelper h) {
        java.util.function.UnaryOperator<ItemStack> turner =
                s -> s.is(Items.MUSIC_DISC_13) ? DiscSides.turned(s, Items.MUSIC_DISC_CAT) : ItemStack.EMPTY;
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MUSIC_DISC_13));
        InteractionResult result = DiscSides.turnInHand(h.getLevel(), player, InteractionHand.MAIN_HAND, turner);
        h.assertTrue(result instanceof InteractionResult.Success, "turned: " + result);
        h.assertTrue(player.getCooldowns().isOnCooldown(new ItemStack(Items.MUSIC_DISC_CAT)), "cooldown after turning");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        h.assertTrue(DiscSides.turnInHand(h.getLevel(), player, InteractionHand.MAIN_HAND, turner) instanceof InteractionResult.Pass, "nothing to turn passes");
        h.succeed();
    }
}
