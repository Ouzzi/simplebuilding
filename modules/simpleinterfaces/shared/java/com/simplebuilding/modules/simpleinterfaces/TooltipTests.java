package com.simplebuilding.modules.simpleinterfaces;

import com.simplebuilding.modules.simpleinterfaces.network.EnderChestSnapshotPayload;
import com.simplebuilding.modules.simpleinterfaces.tooltip.BundleView;
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
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

/** Shulker box / ender chest / bundle tooltips (data side; the drawing itself needs a client and is checked by hand). */
public final class TooltipTests {
    public static final java.util.Map<String, Consumer<GameTestHelper>> ALL = new java.util.LinkedHashMap<>();

    static {
        ALL.put("tooltip_ender_snapshot", TooltipTests::enderSnapshot);
        ALL.put("tooltip_snapshot_codec", TooltipTests::snapshotCodec);
        ALL.put("tooltip_shulker_grid", TooltipTests::shulkerGrid);
        ALL.put("tooltip_tier_shulker", TooltipTests::tierShulker);
        ALL.put("tooltip_config_off", TooltipTests::configOff);
        ALL.put("tooltip_ender_close", TooltipTests::enderClose);
        ALL.put("bundle_view_pages", TooltipTests::bundleViewPages);
        ALL.put("bundle_view_off", TooltipTests::bundleViewOff);
        ALL.put("bundle_selection_beyond_twelve", TooltipTests::bundleSelectionBeyondTwelve);
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
        var red = box(Items.DYED_SHULKER_BOX.pick(net.minecraft.world.item.DyeColor.RED), slotsWith(0, new ItemStack(Items.STONE)));
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

    /** Simple Storage tier box (only when that module is loaded): real counts above 99 and the dye from BASE_COLOR. */
    public static void tierShulker(GameTestHelper h) {
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("simplestorage:netherite_shulker_box"));
        var type = net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(net.minecraft.resources.Identifier.parse("simplestorage:container_counts"));
        if (item == Items.AIR || type == null) {
            h.succeed();
            return;
        }
        var tier = box(item, slotsWith(2, new ItemStack(Items.COBBLESTONE, 64)));
        var json = new com.google.gson.JsonArray();
        var entry = new com.google.gson.JsonObject();
        entry.addProperty("slot", 2);
        entry.addProperty("count", 128);
        json.add(entry);
        setComponent(tier, type, json);
        tier.set(DataComponents.BASE_COLOR, net.minecraft.world.item.DyeColor.RED);
        var local = new ContainersConfig();
        ContainerPreview grid = ContainerTooltips.preview(tier, true, local, true, null).orElseThrow();
        h.assertTrue(grid.grid() && grid.slots().get(2).getCount() == 128, "tier box shows the real count above the vanilla cap: " + grid.slots().get(2).getCount());
        h.assertTrue(grid.face() != ContainerTooltips.VANILLA_FACE, "tier box takes its dye from BASE_COLOR");
        h.succeed();
    }

    private static <T> void setComponent(ItemStack stack, net.minecraft.core.component.DataComponentType<T> type, com.google.gson.JsonElement json) {
        stack.set(type, type.codec().parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow());
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
            h.assertTrue(to.contains(p), "login sends the snapshot");
            to.clear();
            sent.clear();
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

    /** {@code n} distinct bundle entries, entry i holding i+1 stone (custom names keep them apart). */
    private static BundleContents entries(int n) {
        List<ItemStackTemplate> items = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            var stack = new ItemStack(Items.STONE, i + 1);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("e" + i));
            items.add(ItemStackTemplate.fromNonEmptyStack(stack));
        }
        return new BundleContents(items);
    }

    /** Layout for n entries: all up to maxRows*4, beyond that pages of maxRows*4-1 plus "+n", following the selection. */
    public static void bundleViewPages(GameTestHelper h) {
        var c = new ContainersConfig();
        h.assertTrue(c.bundleShowAll && c.bundleMaxRows == 6, "defaults: on, six rows");
        h.assertTrue(BundleView.of(0, -1, c) == null && BundleView.of(12, 5, c) == null, "<= 12 entries: Vanilla");
        h.assertTrue(new BundleView(0, 13, false, 13).equals(BundleView.of(13, -1, c)), "13 entries: all, no +n");
        h.assertTrue(new BundleView(0, 24, false, 24).equals(BundleView.of(24, 23, c)), "24 entries: all six rows");
        h.assertTrue(new BundleView(0, 23, true, 24).equals(BundleView.of(25, -1, c)), "25 entries: page of 23 + n");
        h.assertTrue(new BundleView(23, 2, true, 3).equals(BundleView.of(25, 24, c)), "selection 24 turns to page 2");
        h.assertTrue(new BundleView(23, 17, true, 18).equals(BundleView.of(40, 30, c)), "40 entries, selection 30: page 2 (17)");
        h.assertTrue(new BundleView(92, 8, true, 9).equals(BundleView.of(100, 99, c)), "100 entries, last: page 5");
        h.assertTrue(BundleView.of(40, 99, c).start() == 0 && BundleView.of(40, -1, c).start() == 0, "no / invalid selection: page 1");
        var contents = entries(30);
        var page2 = BundleView.of(30, 25, c);
        int total = 30 * 31 / 2, onPage = 0;
        for (int i = page2.start(); i < page2.start() + page2.shown(); i++) onPage += i + 1;
        h.assertTrue(BundleView.hidden(contents, page2) == total - onPage, "+n counts the items off the page (both sides)");
        h.assertTrue(BundleView.selectable(30, 11, c) == 30 && BundleView.selectable(12, 12, c) == 12, "wheel reaches every entry");
        c.bundleMaxRows = 0;
        h.assertTrue(BundleView.rows(c) == BundleView.MIN_ROWS && BundleView.of(13, -1, c).overflow(), "rows clamp to 3 (Vanilla size)");
        c.bundleMaxRows = 99;
        h.assertTrue(BundleView.rows(c) == BundleView.MAX_ROWS && !BundleView.of(64, 63, c).overflow(), "rows clamp to 16: 64 entries at once");
        h.succeed();
    }

    /** Switch off: no view and the wheel keeps Vanilla's count; old config files get the defaults. */
    public static void bundleViewOff(GameTestHelper h) {
        var c = new ContainersConfig();
        c.bundleShowAll = false;
        h.assertTrue(BundleView.of(40, 30, c) == null && BundleView.selectable(40, 11, c) == 11, "off: Vanilla view and wheel");
        h.assertTrue(BundleView.of(40, 30, null) == null, "no config: Vanilla");
        var old = new com.google.gson.Gson().fromJson("{\"tooltipShulker\":true}", ContainersConfig.class);
        h.assertTrue(old.bundleShowAll && old.bundleMaxRows == 6, "missing keys: defaults");
        h.assertTrue(entries(20).getNumberOfItemsToShow() <= 12, "server side: Vanilla count untouched (client-only mixin)");
        h.succeed();
    }

    /** Server: a bundle entry past the twelfth can be selected and taken out; out-of-range indices select nothing. */
    public static void bundleSelectionBeyondTwelve(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        var bundle = new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.BUNDLE_CONTENTS, entries(20));
        p.getInventory().setItem(9, bundle);
        p.containerMenu.setSelectedBundleItemIndex(9, 15);
        var held = p.getInventory().getItem(9);
        h.assertTrue(BundleItem.getSelectedItemIndex(held) == 15, "index 15 accepted");
        var mutable = held.get(DataComponents.BUNDLE_CONTENTS).asMutable();
        var out = mutable.removeOne();
        h.assertTrue(out != null && "e15".equals(out.getHoverName().getString()) && out.getCount() == 16, "entry 15 comes out");
        p.containerMenu.setSelectedBundleItemIndex(9, 20);
        h.assertTrue(BundleItem.getSelectedItemIndex(held) == -1, "index = entry count rejected");
        p.containerMenu.setSelectedBundleItemIndex(9, -2);
        h.assertTrue(BundleItem.getSelectedItemIndex(held) == -1, "negative index rejected");
        p.getInventory().clearContent();
        h.succeed();
    }
}
