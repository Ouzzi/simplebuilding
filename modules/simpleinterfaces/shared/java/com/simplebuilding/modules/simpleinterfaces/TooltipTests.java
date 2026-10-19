package com.simplebuilding.modules.simpleinterfaces;

import com.simplebuilding.modules.simpleinterfaces.network.EnderChestSnapshotPayload;
import com.simplebuilding.modules.simpleinterfaces.tooltip.ContainerPreview;
import com.simplebuilding.modules.simpleinterfaces.tooltip.ContainerTooltips;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;

/** Shulker box / ender chest tooltips (data side; the drawing itself needs a client and is checked by hand). */
public final class TooltipTests {
    public static final java.util.Map<String, Consumer<GameTestHelper>> ALL = new java.util.LinkedHashMap<>();

    static {
        ALL.put("tooltip_ender_snapshot", TooltipTests::enderSnapshot);
        ALL.put("tooltip_snapshot_codec", TooltipTests::snapshotCodec);
        ALL.put("tooltip_shulker_grid", TooltipTests::shulkerGrid);
        ALL.put("tooltip_config_off", TooltipTests::configOff);
        ALL.put("tooltip_ender_close", TooltipTests::enderClose);
    }

    private TooltipTests() {}

    private static ItemStack box(net.minecraft.world.item.Item item, List<ItemStack> items) {
        ItemStack box = new ItemStack(item);
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        return box;
    }

    private static List<ItemStack> slotsWith(int index, ItemStack stack) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 27; i++) items.add(i == index ? stack : ItemStack.EMPTY);
        return items;
    }

    /** The snapshot holds the ender inventory in slot order; an empty one is Vanilla's EMPTY. */
    public static void enderSnapshot(GameTestHelper h) {
        var ender = new PlayerEnderChestContainer();
        h.assertTrue(ContainerTooltips.snapshot(ender).equals(ItemContainerContents.EMPTY), "empty ender chest -> EMPTY");
        ender.setItem(0, new ItemStack(Items.EMERALD, 5));
        ender.setItem(26, new ItemStack(Items.DIAMOND, 2));
        var snapshot = ContainerTooltips.snapshot(ender);
        var copies = snapshot.itemCopies().toList();
        h.assertTrue(copies.size() == 27 && copies.get(0).is(Items.EMERALD) && copies.get(0).getCount() == 5
                && copies.get(26).is(Items.DIAMOND) && copies.get(13).isEmpty(), "slot order kept");
        ender.getItem(0).setCount(1);
        h.assertTrue(snapshot.itemCopies().toList().get(0).getCount() == 5, "snapshot is a copy");
        h.succeed();
    }

    /** The payload survives the network codec (also custom names). */
    public static void snapshotCodec(GameTestHelper h) {
        var named = new ItemStack(Items.GOLD_INGOT, 7);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Saved"));
        var payload = new EnderChestSnapshotPayload(ItemContainerContents.fromItems(slotsWith(4, named)));
        var buf = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        EnderChestSnapshotPayload.CODEC.encode(buf, payload);
        var back = EnderChestSnapshotPayload.CODEC.decode(buf);
        h.assertTrue(back.contents().equals(payload.contents()) && buf.readableBytes() == 0, "codec roundtrip");
        var item = back.contents().itemCopies().toList().get(4);
        h.assertTrue(item.is(Items.GOLD_INGOT) && item.getCount() == 7 && "Saved".equals(item.getHoverName().getString()), "stack details kept");
        var empty = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        EnderChestSnapshotPayload.CODEC.encode(empty, new EnderChestSnapshotPayload(ItemContainerContents.EMPTY));
        h.assertTrue(EnderChestSnapshotPayload.CODEC.decode(empty).contents().equals(ItemContainerContents.EMPTY), "empty roundtrip");
        h.succeed();
    }

    /** Shulker grid from the container component: Shift rule, dye color, Vanilla lines replaced, big tiers capped. */
    public static void shulkerGrid(GameTestHelper h) {
        var local = new ContainersConfig();
        var shulker = box(Items.SHULKER_BOX, slotsWith(3, new ItemStack(Items.COBBLESTONE, 64)));
        ContainerPreview grid = ContainerTooltips.preview(shulker, true, local, true, null).orElseThrow();
        h.assertTrue(grid.grid() && grid.slots().size() == 27 && grid.rows() == 3 && grid.replacesList(), "Shift: 9x3 grid instead of the lines");
        h.assertTrue(grid.slots().get(3).is(Items.COBBLESTONE) && grid.slots().get(3).getCount() == 64 && grid.slots().get(0).isEmpty(), "slot positions kept");
        h.assertTrue(grid.face() == ContainerTooltips.VANILLA_FACE, "undyed box: Vanilla gray");
        var hint = ContainerTooltips.preview(shulker, false, local, true, null).orElseThrow();
        h.assertTrue(!hint.grid() && !hint.replacesList() && hint.note() != null, "no Shift: Vanilla lines plus hint");
        local.tooltipNeedsShift = false;
        h.assertTrue(ContainerTooltips.preview(shulker, false, local, true, null).orElseThrow().grid(), "always mode: grid without Shift");
        var red = box(Items.RED_SHULKER_BOX, slotsWith(0, new ItemStack(Items.STONE)));
        h.assertTrue(ContainerTooltips.preview(red, false, local, true, null).orElseThrow().face() != ContainerTooltips.VANILLA_FACE, "dyed box tinted");
        local.tooltipColorByDye = false;
        h.assertTrue(ContainerTooltips.preview(red, false, local, true, null).orElseThrow().face() == ContainerTooltips.VANILLA_FACE, "dye tint switch");
        h.assertTrue(ContainerTooltips.preview(new ItemStack(Items.SHULKER_BOX), true, local, true, null).isEmpty(), "empty box: Vanilla only");
        List<ItemStack> many = new ArrayList<>();
        for (int i = 0; i < 60; i++) many.add(new ItemStack(Items.STONE, i + 1));
        var big = ContainerTooltips.preview(box(Items.SHULKER_BOX, many), true, local, true, null).orElseThrow();
        h.assertTrue(big.slots().size() == ContainerTooltips.MAX_CELLS && big.more() == 60 - ContainerTooltips.MAX_CELLS + 1 && big.rows() == 6, "big tier: 6 rows, +n cell");
        h.succeed();
    }

    /** Every switch off -> no image (Vanilla tooltip unchanged); unknown ender chest -> nothing. */
    public static void configOff(GameTestHelper h) {
        var shulker = box(Items.SHULKER_BOX, slotsWith(0, new ItemStack(Items.STONE)));
        var ender = ItemContainerContents.fromItems(slotsWith(2, new ItemStack(Items.EMERALD)));
        var enderItem = new ItemStack(Items.ENDER_CHEST);
        var local = new ContainersConfig();
        var shown = ContainerTooltips.preview(enderItem, true, local, true, ender).orElseThrow();
        h.assertTrue(shown.grid() && shown.slots().get(2).is(Items.EMERALD) && !shown.replacesList(), "ender chest grid from the snapshot");
        h.assertTrue(ContainerTooltips.preview(enderItem, true, local, true, null).isEmpty(), "no snapshot (no server part): nothing");
        h.assertTrue(ContainerTooltips.preview(enderItem, true, local, false, ender).isEmpty(), "server switch off: nothing");
        var empty = ContainerTooltips.preview(enderItem, true, local, true, ItemContainerContents.EMPTY).orElseThrow();
        h.assertTrue(!empty.grid() && empty.note() != null, "empty ender chest: one note");
        h.assertTrue(ContainerTooltips.preview(new ItemStack(Items.CHEST), true, local, true, ender).isEmpty(), "other containers: later stage");
        local.tooltipShulker = false;
        local.tooltipEnderChest = false;
        h.assertTrue(ContainerTooltips.preview(shulker, true, local, true, ender).isEmpty(), "shulker switch off");
        h.assertTrue(ContainerTooltips.preview(enderItem, true, local, true, ender).isEmpty(), "ender switch off");
        var copy = new com.google.gson.Gson().fromJson("{}", ContainersConfig.class);
        h.assertTrue(copy.tooltipShulker && copy.tooltipEnderChest && copy.tooltipNeedsShift && copy.tooltipColorByDye, "defaults on, Shift needed");
        h.assertTrue(new LinkedConfig().enableEnderChestTooltip, "server default on");
        h.succeed();
    }

    /** Closing the own ender inventory sends a snapshot to that player only; server switch off sends nothing. */
    public static void enderClose(GameTestHelper h) {
        var sender = ContainerTooltips.sender;
        boolean before = SimpleInterfaces.config().enableEnderChestTooltip;
        List<ServerPlayer> to = new ArrayList<>();
        List<ItemContainerContents> sent = new ArrayList<>();
        try {
            ContainerTooltips.sender = (p, payload) -> {
                to.add(p);
                sent.add(payload.contents());
                return true;
            };
            SimpleInterfaces.config().enableEnderChestTooltip = true;
            ServerPlayer p = h.makeMockServerPlayerInLevel();
            ServerPlayer other = h.makeMockServerPlayerInLevel();
            p.getEnderChestInventory().setItem(5, new ItemStack(Items.DIAMOND, 3));
            p.getEnderChestInventory().startOpen(p);
            p.getEnderChestInventory().stopOpen(p);
            h.assertTrue(to.size() == 1 && to.get(0) == p, "closing sends one snapshot to the owner");
            h.assertTrue(sent.get(0).itemCopies().toList().get(5).is(Items.DIAMOND), "snapshot holds the contents");
            p.getEnderChestInventory().stopOpen(other);
            h.assertTrue(to.size() == 1, "another player closing someone else's chest gets nothing");
            SimpleInterfaces.config().enableEnderChestTooltip = false;
            p.getEnderChestInventory().stopOpen(p);
            h.assertTrue(to.size() == 1 && !ContainerTooltips.send(p), "server switch off: no snapshot");
            p.getEnderChestInventory().clearContent();
        } finally {
            ContainerTooltips.sender = sender;
            SimpleInterfaces.config().enableEnderChestTooltip = before;
        }
        h.succeed();
    }
}
