package com.simplebuilding.modules.simpletools;

import com.simplebuilding.framework.api.SubMods;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Better item frames (docs/ai/PLAN-B-ITEMFRAMES-2026-10-19.md): real {@code ItemFrame.interact} calls through the mixin. */
public final class FrameTests {
    private FrameTests() {}

    private static final BlockPos SPOT = new BlockPos(1, 2, 1);

    /** A frame on a stone wall (facing south) with a diamond, and a survival player in front of it. */
    private static ItemFrame frame(GameTestHelper h, boolean glow, boolean filled) {
        BlockPos pos = h.absolutePos(SPOT);
        h.getLevel().setBlockAndUpdate(pos.north(), Blocks.STONE.defaultBlockState());
        ItemFrame frame = glow ? new GlowItemFrame(h.getLevel(), pos, Direction.SOUTH) : new ItemFrame(h.getLevel(), pos, Direction.SOUTH);
        h.getLevel().addFreshEntity(frame);
        if (filled) frame.setItem(new ItemStack(Items.DIAMOND), false);
        return frame;
    }

    private static ServerPlayer player(GameTestHelper h, GameType mode, ItemStack held, boolean sneak) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.setGameMode(mode);
        BlockPos pos = h.absolutePos(SPOT);
        p.snapTo(pos.getX() + 0.5, pos.getY() - 0.5, pos.getZ() + 2.0);
        p.setItemInHand(InteractionHand.MAIN_HAND, held);
        p.setShiftKeyDown(sneak);
        return p;
    }

    /** With SimpleBuilding its older frame hook runs first: shears hide a visible frame, sneaking shows a hidden one. */
    private static boolean sb() {
        return ToolsTests.isModLoaded("simplebuilding");
    }

    private static InteractionResult click(ItemFrame frame, ServerPlayer p) {
        return frame.interact(p, InteractionHand.MAIN_HAND, Vec3.ZERO);
    }

    /** Fresh default config, the sub-mod switched on; frames discarded afterwards. */
    private static void run(GameTestHelper h, Consumer<GameTestHelper> body) {
        ToolsConfig old = SimpleTools.CONFIG;
        try {
            SimpleTools.CONFIG = new ToolsConfig();
            SubMods.set(SimpleTools.MOD_ID, true);
            body.accept(h);
        } finally {
            SimpleTools.CONFIG = old;
            SubMods.set(SimpleTools.MOD_ID, true);
            h.getLevel().getEntitiesOfClass(ItemFrame.class, new net.minecraft.world.phys.AABB(h.absolutePos(SPOT)).inflate(2)).forEach(ItemFrame::discard);
        }
        h.succeed();
    }

    /** Sneak + shears: hidden, shears -1, again: shown; without sneaking Vanilla turns the item. */
    public static void invisible(GameTestHelper h) {
        run(h, t -> {
            ItemFrame frame = frame(h, false, true);
            ServerPlayer p = player(h, GameType.SURVIVAL, new ItemStack(Items.SHEARS), true);
            h.assertTrue(click(frame, p).consumesAction() && frame.isInvisible(), "sneak + shears hides a filled frame");
            // SimpleBuilding hides first and asks isCreative(), which the mock server player answers with true
            int wear = p.getMainHandItem().getDamageValue();
            h.assertTrue(sb() || wear == 1, "shears lose 1 durability: " + p.getMainHandItem().getDamageValue() + " invisible=" + frame.isInvisible() + " rot=" + frame.getRotation());
            h.assertTrue(frame.getRotation() == 0 && frame.getItem().is(Items.DIAMOND), "item and rotation untouched");
            click(frame, p);
            h.assertTrue(!frame.isInvisible() && p.getMainHandItem().getDamageValue() == (sb() ? wear : 2), "again: shown, shears -1 again (SimpleBuilding shows for free): " + p.getMainHandItem().getDamageValue());
            p.setShiftKeyDown(false);
            click(frame, p);
            h.assertTrue(!frame.isInvisible() && frame.getRotation() == 1, "without sneaking: Vanilla rotation");
            ServerPlayer creative = player(h, GameType.CREATIVE, new ItemStack(Items.SHEARS), true);
            click(frame, creative);
            h.assertTrue(frame.isInvisible() && creative.getMainHandItem().getDamageValue() == 0, "creative: no wear");
        });
    }

    /** Without an item nothing of ours happens: Vanilla puts the held item into the frame. */
    public static void empty(GameTestHelper h) {
        run(h, t -> {
            ItemFrame frame = frame(h, false, false);
            ServerPlayer p = player(h, GameType.SURVIVAL, new ItemStack(Items.SHEARS), true);
            click(frame, p);
            h.assertTrue(!frame.isInvisible(), "an empty frame never turns invisible");
            h.assertTrue(frame.getItem().is(Items.SHEARS) && p.getMainHandItem().isEmpty(), "Vanilla: the shears went into the frame");
            ServerPlayer q = player(h, GameType.SURVIVAL, new ItemStack(Items.HONEYCOMB), true);
            ItemFrame second = frame(h, false, false);
            click(second, q);
            h.assertTrue(!ItemFrames.fixed(second) && second.getItem().is(Items.HONEYCOMB), "honeycomb on an empty frame: Vanilla placing, no lock");
        });
    }

    /** Sneak + right-click turns backwards (0 -> 7 -> 6), a plain right-click forwards. */
    public static void reverse(GameTestHelper h) {
        run(h, t -> {
            ItemFrame frame = frame(h, false, true);
            ServerPlayer p = player(h, GameType.SURVIVAL, ItemStack.EMPTY, true);
            click(frame, p);
            h.assertTrue(frame.getRotation() == 7, "sneaking turns backwards: " + frame.getRotation());
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
            click(frame, p);
            h.assertTrue(frame.getRotation() == 6 && p.getMainHandItem().getCount() == 1, "with any other item too, nothing used: " + frame.getRotation());
            p.setShiftKeyDown(false);
            click(frame, p);
            h.assertTrue(frame.getRotation() == 7, "plain click turns forwards");
            h.assertTrue(frame.getItem().is(Items.DIAMOND), "the item stays");
        });
    }

    /** Honeycomb locks (Vanilla Fixed): no turning, taking out, swapping or hiding; adventure cannot unlock; an axe unlocks. */
    public static void lock(GameTestHelper h) {
        run(h, t -> lockOn(h, frame(h, false, true)));
    }

    private static void lockOn(GameTestHelper h, ItemFrame frame) {
        ServerPlayer p = player(h, GameType.SURVIVAL, new ItemStack(Items.HONEYCOMB, 2), true);
        h.assertTrue(click(frame, p).consumesAction() && ItemFrames.fixed(frame), "sneak + honeycomb locks");
        h.assertTrue(p.getMainHandItem().getCount() == 1 && frame.getRotation() == 0, "one honeycomb used, no turn");
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        p.setShiftKeyDown(false);
        click(frame, p);
        p.setShiftKeyDown(true);
        click(frame, p);
        h.assertTrue(frame.getRotation() == 0, "locked: no turning either way");
        if (!sb()) {
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
            click(frame, p);
            h.assertTrue(!frame.isInvisible() && p.getMainHandItem().getDamageValue() == 0, "locked: shears do nothing");
        }
        frame.hurtServer(h.getLevel(), p.damageSources().playerAttack(p), 1.0F);
        h.assertTrue(frame.getItem().is(Items.DIAMOND) && !frame.isRemoved(), "locked: a survival hit takes nothing out");
        ServerPlayer adventure = player(h, GameType.ADVENTURE, new ItemStack(Items.IRON_AXE), true);
        click(frame, adventure);
        h.assertTrue(ItemFrames.fixed(frame) && adventure.getMainHandItem().getDamageValue() == 0, "adventure mode cannot unlock");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        h.assertTrue(click(frame, p).consumesAction() && !ItemFrames.fixed(frame), "sneak + axe unlocks");
        h.assertTrue(p.getMainHandItem().getDamageValue() == 1 && frame.getRotation() == 0, "axe -1, no turn while unlocking");
        click(frame, p);
        h.assertTrue(frame.getRotation() == 7, "unlocked: sneak + axe now turns backwards");
    }

    /** Glow item frames share the behaviour (same Vanilla method). */
    public static void glow(GameTestHelper h) {
        run(h, t -> {
            ItemFrame frame = frame(h, true, true);
            h.assertTrue(frame instanceof GlowItemFrame, "glow frame");
            ServerPlayer p = player(h, GameType.SURVIVAL, new ItemStack(Items.SHEARS), true);
            click(frame, p);
            h.assertTrue(frame.isInvisible(), "glow frame hidden with shears");
            click(frame, p);
            lockOn(h, frame);
        });
    }

    /** Knocking the item out of a hidden frame shows the frame again (an empty hidden frame would be lost). */
    public static void reveal(GameTestHelper h) {
        run(h, t -> {
            ItemFrame frame = frame(h, false, true);
            ServerPlayer p = player(h, GameType.SURVIVAL, new ItemStack(Items.SHEARS), true);
            click(frame, p);
            h.assertTrue(frame.isInvisible(), "hidden");
            frame.hurtServer(h.getLevel(), p.damageSources().playerAttack(p), 1.0F);
            h.assertTrue(frame.getItem().isEmpty() && !frame.isRemoved(), "the hit took the item out, the frame stays");
            h.assertTrue(!frame.isInvisible(), "emptied frame visible again");
        });
    }

    /** Every option off, and the super-mod switch off: Vanilla behaviour. */
    public static void configOff(GameTestHelper h) {
        run(h, t -> {
            ToolsConfig c = SimpleTools.config();
            c.enableItemFrameInvisibility = false;
            c.enableItemFrameReverseRotate = false;
            c.enableItemFrameLock = false;
            ItemFrame frame = frame(h, false, true);
            ServerPlayer p = player(h, GameType.SURVIVAL, ItemStack.EMPTY, true);
            click(frame, p);
            h.assertTrue(frame.getRotation() == 1, "reverse off: Vanilla forward turn");
            if (!sb()) {
                p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
                click(frame, p);
                h.assertTrue(!frame.isInvisible() && frame.getRotation() == 2 && p.getMainHandItem().getDamageValue() == 0, "shears off: Vanilla forward turn");
                frame.setRotation(1);
            }
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HONEYCOMB));
            click(frame, p);
            h.assertTrue(!ItemFrames.fixed(frame) && frame.getRotation() == 2 && p.getMainHandItem().getCount() == 1, "lock off: Vanilla forward turn");
            ItemFrames.setFixed(frame, true);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
            click(frame, p);
            h.assertTrue(ItemFrames.fixed(frame), "lock off: an axe does not unlock a Fixed frame");
            ItemFrames.setFixed(frame, false);
            frame.setInvisible(true);
            frame.hurtServer(h.getLevel(), p.damageSources().playerAttack(p), 1.0F);
            h.assertTrue(frame.isInvisible(), "invisibility off: an emptied hidden frame stays hidden (Vanilla)");
            SimpleTools.CONFIG = new ToolsConfig();
            SubMods.set(SimpleTools.MOD_ID, false);
            ItemFrame second = frame(h, false, true);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HONEYCOMB));
            click(second, p);
            h.assertTrue(!ItemFrames.fixed(second) && second.getRotation() == 1 && p.getMainHandItem().getCount() == 1, "Enable Simple Tools off: Vanilla");
        });
    }
}
