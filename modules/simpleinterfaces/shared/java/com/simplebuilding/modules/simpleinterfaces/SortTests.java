package com.simplebuilding.modules.simpleinterfaces;

import com.simplebuilding.modules.simpleinterfaces.network.SortPayload;
import com.simplebuilding.modules.simpleinterfaces.sort.ContainerSort;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import com.simplelib.api.TabSort;
import net.minecraft.world.item.Items;

/** Server-side inventory sorting (WE25-3): the pure order, the player's inventory, a chest, refused areas and the payload. */
public final class SortTests {
    public static final java.util.Map<String, Consumer<GameTestHelper>> ALL = new java.util.LinkedHashMap<>();

    static {
        ALL.put("sort_merge", SortTests::merge);
        ALL.put("sort_inventory", SortTests::inventory);
        ALL.put("sort_chest", SortTests::chest);
        ALL.put("sort_refused", SortTests::refused);
        ALL.put("sort_payload", SortTests::payload);
        ALL.put("sort_families", SortTests::families);
    }

    private SortTests() {}

    private static int slotOf(AbstractContainerMenu menu, Object container, int containerSlot) {
        for (var s : menu.slots) if (s.container == container && s.getContainerSlot() == containerSlot) return s.index;
        throw new IllegalStateException("no slot " + containerSlot);
    }

    /** Equal stacks merge to the limit, order follows the item id, empties go last, the input stays untouched. */
    public static void merge(GameTestHelper h) {
        List<ItemStack> in = List.of(new ItemStack(Items.DIAMOND, 5), ItemStack.EMPTY, new ItemStack(Items.STONE, 40),
                new ItemStack(Items.DIAMOND, 3), new ItemStack(Items.STONE, 40), new ItemStack(Items.DIAMOND_SWORD));
        List<ItemStack> out = ContainerSort.sorted(in, 99);
        h.assertTrue(out.size() == 6, "same size");
        h.assertTrue(out.get(0).is(Items.DIAMOND) && out.get(0).getCount() == 8, "diamonds merged (id order)");
        h.assertTrue(out.get(1).is(Items.DIAMOND_SWORD) && out.get(2).is(Items.STONE) && out.get(2).getCount() == 64
                && out.get(3).is(Items.STONE) && out.get(3).getCount() == 16 && out.get(4).isEmpty() && out.get(5).isEmpty(), "stone 64 + 16, unstackable kept, empties last");
        h.assertTrue(in.get(0).getCount() == 5 && in.get(2).getCount() == 40, "input untouched");
        h.assertTrue(sameCounts(ContainerSort.sorted(out, 99), out), "sorting twice changes nothing");
        h.succeed();
    }

    private static boolean sameCounts(List<ItemStack> a, List<ItemStack> b) {
        for (int i = 0; i < a.size(); i++) if (!ItemStack.matches(a.get(i), b.get(i))) return false;
        return true;
    }

    /** Own inventory: slots 9..35 sorted, hotbar and armor stay, a click on the hotbar does nothing. */
    public static void inventory(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        Inventory inv = p.getInventory();
        inv.setItem(0, new ItemStack(Items.TORCH, 3));
        inv.setItem(9, new ItemStack(Items.DIAMOND, 5));
        inv.setItem(11, new ItemStack(Items.STONE, 10));
        inv.setItem(20, new ItemStack(Items.DIAMOND, 4));
        inv.setItem(35, new ItemStack(Items.STONE, 60));
        inv.setItem(39, new ItemStack(Items.IRON_HELMET));
        var menu = p.inventoryMenu;
        h.assertTrue(!ContainerSort.sort(p, menu.containerId, slotOf(menu, inv, 0)), "hotbar slot is no sort target");
        h.assertTrue(inv.getItem(9).is(Items.DIAMOND), "hotbar click left the main area alone");
        h.assertTrue(ContainerSort.sort(p, menu.containerId, slotOf(menu, inv, 20)), "main slot sorts");
        h.assertTrue(inv.getItem(9).is(Items.DIAMOND) && inv.getItem(9).getCount() == 9, "diamonds first, merged");
        h.assertTrue(inv.getItem(10).is(Items.STONE) && inv.getItem(10).getCount() == 64 && inv.getItem(11).is(Items.STONE) && inv.getItem(11).getCount() == 6
                && inv.getItem(12).isEmpty() && inv.getItem(35).isEmpty(), "stone merged, rest empty");
        h.assertTrue(inv.getItem(0).is(Items.TORCH) && inv.getItem(0).getCount() == 3 && inv.getItem(39).is(Items.IRON_HELMET), "hotbar and armor untouched");
        h.succeed();
    }

    /** Chest: the container is sorted, the player's part of the menu is a separate area. */
    public static void chest(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        var box = new SimpleContainer(27);
        box.setItem(2, new ItemStack(Items.DIAMOND, 7));
        box.setItem(20, new ItemStack(Items.COBBLESTONE, 64));
        box.setItem(26, new ItemStack(Items.DIAMOND, 2));
        p.getInventory().setItem(12, new ItemStack(Items.DIAMOND, 1));
        var menu = ChestMenu.threeRows(77, p.getInventory(), box);
        p.containerMenu = menu;
        h.assertTrue(!ContainerSort.sort(p, 78, 0), "wrong container id refused");
        h.assertTrue(ContainerSort.sort(p, 77, slotOf(menu, box, 5)), "chest sorts");
        h.assertTrue(box.getItem(0).is(Items.COBBLESTONE) && box.getItem(1).is(Items.DIAMOND) && box.getItem(1).getCount() == 9 && box.getItem(2).isEmpty(), "chest ordered and merged");
        h.assertTrue(p.getInventory().getItem(12).getCount() == 1, "player part untouched");
        h.assertTrue(ContainerSort.sort(p, 77, slotOf(menu, p.getInventory(), 12)), "player part of the chest menu sorts on its own");
        h.assertTrue(box.getItem(1).getCount() == 9, "chest untouched by that");
        h.succeed();
    }

    /** The crafting grid and menus that are no chest are refused; sub-mod off refuses all. */
    public static void refused(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        var menu = p.inventoryMenu;
        h.assertTrue(!ContainerSort.eligible(new net.minecraft.world.inventory.FurnaceMenu(5, p.getInventory())), "machines are not eligible");
        h.assertTrue(ContainerSort.region(menu, 1, p.getInventory()).isEmpty(), "crafting grid slot is no area");
        h.assertTrue(ContainerSort.region(menu, 5, p.getInventory()).isEmpty(), "armor slot is no area");
        h.assertTrue(ContainerSort.region(menu, 999, p.getInventory()).isEmpty() && ContainerSort.region(menu, -1, p.getInventory()).isEmpty(), "bad index refused");
        com.simplebuilding.framework.api.SubMods.set(SimpleInterfaces.MOD_ID, false);
        try {
            p.getInventory().setItem(10, new ItemStack(Items.STONE));
            h.assertTrue(!ContainerSort.sort(p, menu.containerId, slotOf(menu, p.getInventory(), 10)), "sub-mod off: no sorting");
        } finally {
            com.simplebuilding.framework.api.SubMods.set(SimpleInterfaces.MOD_ID, true);
        }
        h.succeed();
    }

    /** WE25-3 (06.11.): families stay together and the order is deterministic, whatever the input order. */
    public static void families(GameTestHelper h) {
        String[] expected = {"white_wool", "white_carpet", "orange_wool", "orange_carpet", "stone", "stone_stairs", "stone_slab", "stone_bricks",
                "stone_brick_stairs", "stone_brick_slab", "stone_brick_wall", "cracked_stone_bricks", "mossy_stone_bricks", "chiseled_stone_bricks"};
        List<ItemStack> in = new java.util.ArrayList<>();
        for (String id : expected) in.add(new ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(id))));
        java.util.Collections.shuffle(in, new java.util.Random(6));
        List<ItemStack> a = ContainerSort.sorted(in, 99);
        java.util.Collections.shuffle(in, new java.util.Random(11));
        List<ItemStack> b = ContainerSort.sorted(in, 99);
        h.assertTrue(sameCounts(a, b), "same result for any input order");
        List<String> names = new java.util.ArrayList<>();
        for (ItemStack s : a) names.add(BuiltInRegistries.ITEM.getKey(s.getItem()).getPath());
        int stone = names.indexOf("stone"), stoneEnd = names.indexOf("chiseled_stone_bricks");
        h.assertTrue(stone >= 0 && stoneEnd - stone == 9, "stone family is contiguous: " + names);
        h.assertTrue(names.indexOf("stone_stairs") == stone + 1 && names.indexOf("stone_slab") == stone + 2, "block, stairs, slab in order: " + names);
        h.assertTrue(names.indexOf("white_carpet") == names.indexOf("white_wool") + 1, "carpet follows its wool: " + names);
        h.assertTrue(names.indexOf("cracked_stone_bricks") > names.indexOf("stone_brick_wall"), "variants after the plain bricks: " + names);
        h.assertTrue(TabSort.compareItems("compressed_cobblestone", "cobblestone_slab") > 0 && TabSort.compareItems("cobblestone", "cobblestone_octet") < 0, "tier and octet order");
        h.assertTrue(ContainerSort.compareStacks(new ItemStack(Items.DIAMOND), new ItemStack(Items.STONE)) < 0, "Vanilla compares by id path");
        h.succeed();
    }

    public static void payload(GameTestHelper h) {
        var buf = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
        SortPayload.CODEC.encode(buf, new SortPayload(7, 31));
        var back = SortPayload.CODEC.decode(buf);
        h.assertTrue(back.containerId() == 7 && back.slotIndex() == 31 && buf.readableBytes() == 0, "codec roundtrip");
        h.succeed();
    }
}
