package com.simpleshops.test;

import static com.simpleshops.test.ShopsTests.player;

import com.simpleshops.block.EarningsStore;
import com.simpleshops.logic.ShopActions;
import com.simpleshops.menu.SalesOwnerMenu;
import com.simpleshops.menu.StockMenu;
import com.simpleshops.menu.StorageMenu;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Shops rework stage 1 (KONZ20 S4 t): the sales block and the sales storage accept transactions only. Every click type the
 * server knows (pickup left/right, shift-click, number keys and off hand, double-click collect, drag in all three modes,
 * middle click, throw) is fired at the earnings slots of every owner screen; the earnings never grow and no item appears or
 * vanishes. The display case stays the goods input (counter check). Hoppers beside or below the shop move nothing.
 */
final class TransactionTests {
    private static final Item[] POOL = {Items.STICK, Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT};

    static void register(Map<String, Consumer<GameTestHelper>> all) {
        all.put("recipes_ender_pearl", h -> {
            // KONZ20: shop and shop storage are crafted with an ender pearl (the contents live in the ender data, like an ender chest).
            var recipes = h.getLevel().getServer().getRecipeManager();
            for (String id : List.of("sales_block", "sales_storage")) {
                var holder = recipes.byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, com.simpleshops.SimpleShops.id(id)));
                h.assertTrue(holder.isPresent(), "recipe " + id);
                var ingredients = holder.get().value().placementInfo().ingredients();
                h.assertTrue(ingredients.stream().filter(i -> i.test(new ItemStack(Items.ENDER_PEARL))).count() == 1, "one ender pearl in " + id);
                h.assertTrue(ingredients.stream().filter(i -> i.test(new ItemStack(Items.IRON_INGOT))).count() == 2, "two iron ingots in " + id);
            }
            h.succeed();
        });
        all.put("no_insert_every_click_type", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            StorageTests.Column c = StorageTests.column(h, owner, 1);
            stock(owner);
            c.sales().earnings().set(0, new ItemStack(Items.GOLD_INGOT, 5));
            c.storages().get(0).earnings().set(0, new ItemStack(Items.GOLD_INGOT, 5));
            int[] clicks = {0};
            // Shared owner screen (goods, price, earnings).
            ShopActions.useSales(owner, h.getLevel(), c.sales);
            h.assertTrue(owner.containerMenu instanceof SalesOwnerMenu m && m.isCombined(), "shared owner screen");
            SalesOwnerMenu both = (SalesOwnerMenu) owner.containerMenu;
            hammer(h, c, owner, both, both.earningsStart(), both.earningsEnd(), clicks);
            // Counter check: the goods row of the same screen takes goods (the display case is the goods input).
            owner.getInventory().setItem(8, new ItemStack(Items.DIAMOND, 2));
            both.clicked(both.earningsEnd() + 27 + 8, 0, ContainerInput.QUICK_MOVE, owner);
            h.assertTrue(c.box().goods().stream().anyMatch(s -> s.is(Items.DIAMOND)), "the goods row takes goods");
            owner.closeContainer();
            // Sales storage screen.
            ShopActions.useStorage(owner, h.getLevel(), c.storagePos(0));
            h.assertTrue(owner.containerMenu instanceof StorageMenu, "storage screen");
            hammer(h, c, owner, owner.containerMenu, 0, StorageMenu.SLOTS, clicks);
            owner.closeContainer();
            // The sales block alone (no case above): its own price + earnings screen.
            h.getLevel().setBlock(c.box, Blocks.AIR.defaultBlockState(), 3);
            for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, AABB.ofSize(Vec3.atCenterOf(c.sales), 24, 24, 24))) e.discard();
            ShopActions.useSales(owner, h.getLevel(), c.sales);
            h.assertTrue(owner.containerMenu instanceof SalesOwnerMenu m && !m.isCombined(), "sales block screen");
            SalesOwnerMenu alone = (SalesOwnerMenu) owner.containerMenu;
            hammer(h, c, owner, alone, alone.earningsStart(), alone.earningsEnd(), clicks);
            owner.closeContainer();
            // The display case alone still takes goods.
            h.getLevel().setBlock(c.box, com.simpleshops.ShopsRegistry.DISPLAY_CASE.defaultBlockState(), 3);
            ((com.simpleshops.block.DisplayCaseBlockEntity) h.getLevel().getBlockEntity(c.box)).initFor(owner);
            h.getLevel().setBlock(c.sales, Blocks.AIR.defaultBlockState(), 3);
            ShopActions.useCase(owner, h.getLevel(), c.box);
            h.assertTrue(owner.containerMenu instanceof StockMenu, "goods screen");
            owner.getInventory().setItem(7, new ItemStack(Items.EMERALD, 3));
            owner.containerMenu.clicked(StockMenu.GOODS_END + 27 + 7, 0, ContainerInput.QUICK_MOVE, owner);
            h.assertTrue(c.box().goods().stream().anyMatch(s -> s.is(Items.EMERALD)), "the display case takes goods");
            com.simpleshops.SimpleShops.LOG.info("transaction-only clicks fired: {}", clicks[0]);
            h.assertTrue(clicks[0] > 300, "all click types fired: " + clicks[0]);
            h.succeed();
        });
        all.put("no_insert_hoppers", h -> {
            ServerPlayer owner = player(h, GameType.SURVIVAL);
            StorageTests.Column c = StorageTests.column(h, owner, 1);
            // A hopper beside the sales block and one beside the storage (other side, so they never feed each other), both facing
            // into the shop, full of sticks.
            List<BlockPos> hoppers = List.of(c.sales.east(), c.storagePos(0).west());
            for (BlockPos p : hoppers) {
                Direction into = p.getX() > c.sales.getX() ? Direction.WEST : Direction.EAST;
                h.getLevel().setBlock(p, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, into), 3);
                ((HopperBlockEntity) h.getLevel().getBlockEntity(p)).setItem(0, new ItemStack(Items.STICK, 5));
            }
            h.runAfterDelay(60, () -> {
                for (BlockPos p : hoppers) {
                    HopperBlockEntity hop = (HopperBlockEntity) h.getLevel().getBlockEntity(p);
                    h.assertTrue(hop.getItem(0).is(Items.STICK) && hop.getItem(0).getCount() == 5, "the hopper pushed nothing into the shop at " + p);
                }
                h.assertTrue(StorageTests.total(h, c, Items.STICK) == 0, "no stick inside the shop");
                h.succeed();
            });
        });
        ShopsTests.MAX_TICKS.put("no_insert_hoppers", 120);
    }

    /** Fresh inventory: sticks in hotbar 0, diamonds in hotbar 1, gold in the main inventory, emeralds in the off hand. */
    private static void stock(ServerPlayer p) {
        p.getInventory().clearContent();
        p.getInventory().setItem(0, new ItemStack(Items.STICK, 16));
        p.getInventory().setItem(1, new ItemStack(Items.DIAMOND, 3));
        p.getInventory().setItem(9, new ItemStack(Items.GOLD_INGOT, 2));
        p.getInventory().setItem(40, new ItemStack(Items.EMERALD, 4));
    }

    /** Fires every click type at the earnings slots [{@code start}, {@code end}) and checks the rules after each click. */
    private static void hammer(GameTestHelper h, StorageTests.Column c, ServerPlayer p, AbstractContainerMenu menu, int start, int end, int[] clicks) {
        Map<Item, Integer> universe = universe(h, c, p, menu);
        Map<Item, Integer> earnings = earnings(c);
        int invStart = end;
        if (menu instanceof SalesOwnerMenu) invStart = ((SalesOwnerMenu) menu).earningsEnd();
        int[] targets = {start, start + 1, end - 1};
        for (int slot : targets) {
            for (Item cursor : new Item[] {null, Items.STICK, Items.GOLD_INGOT}) {
                // Cursor from the inventory (so the universe stays the same).
                carry(p, menu, cursor);
                for (int button = 0; button <= 1; button++) fire(h, c, p, menu, slot, button, ContainerInput.PICKUP, universe, earnings, clicks);
                carry(p, menu, cursor);
                fire(h, c, p, menu, slot, 0, ContainerInput.CLONE, universe, earnings, clicks);
                // Double click collect: gathers matching items into the cursor (taking out is fine, nothing goes in).
                fire(h, c, p, menu, slot, 0, ContainerInput.PICKUP_ALL, universe, earnings, clicks);
                fire(h, c, p, menu, invStart, 0, ContainerInput.PICKUP_ALL, universe, earnings, clicks);
                // Drag over the earnings and one inventory slot, all three modes (even, one each, clone).
                for (int mode = 0; mode <= 2; mode++) {
                    carry(p, menu, cursor);
                    fire(h, c, p, menu, AbstractContainerMenu.SLOT_CLICKED_OUTSIDE, AbstractContainerMenu.getQuickcraftMask(0, mode), ContainerInput.QUICK_CRAFT, universe, earnings, clicks);
                    for (int s : new int[] {slot, start + 2, invStart + 3}) {
                        fire(h, c, p, menu, s, AbstractContainerMenu.getQuickcraftMask(1, mode), ContainerInput.QUICK_CRAFT, universe, earnings, clicks);
                    }
                    fire(h, c, p, menu, AbstractContainerMenu.SLOT_CLICKED_OUTSIDE, AbstractContainerMenu.getQuickcraftMask(2, mode), ContainerInput.QUICK_CRAFT, universe, earnings, clicks);
                }
            }
            carry(p, menu, null);
            // Number keys 1-9 and the off hand key (40) on the earnings slot.
            for (int key = 0; key < 9; key++) fire(h, c, p, menu, slot, key, ContainerInput.SWAP, universe, earnings, clicks);
            fire(h, c, p, menu, slot, 40, ContainerInput.SWAP, universe, earnings, clicks);
            fire(h, c, p, menu, slot, 0, ContainerInput.THROW, universe, earnings, clicks);
            fire(h, c, p, menu, slot, 0, ContainerInput.QUICK_MOVE, universe, earnings, clicks);
        }
        // Shift-click from every inventory slot of the screen.
        for (int s = invStart; s < menu.slots.size(); s++) {
            stockIfEmpty(p);
            universe = universe(h, c, p, menu);
            fire(h, c, p, menu, s, 0, ContainerInput.QUICK_MOVE, universe, earnings, clicks);
        }
        carry(p, menu, null);
    }

    /** Puts a stack of {@code item} from the inventory on the cursor ({@code null}: cursor back into the inventory). */
    private static void carry(ServerPlayer p, AbstractContainerMenu menu, Item item) {
        if (!menu.getCarried().isEmpty()) {
            ItemStack back = menu.getCarried();
            menu.setCarried(ItemStack.EMPTY);
            p.getInventory().add(back);
            if (!back.isEmpty()) p.drop(back, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        if (item == null) return;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack at = p.getInventory().getItem(i);
            if (at.is(item)) {
                menu.setCarried(at.copy());
                p.getInventory().setItem(i, ItemStack.EMPTY);
                return;
            }
        }
    }

    private static void stockIfEmpty(ServerPlayer p) {
        boolean any = false;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) any |= !p.getInventory().getItem(i).isEmpty();
        if (!any) p.getInventory().setItem(0, new ItemStack(Items.STICK, 16));
    }

    private static void fire(GameTestHelper h, StorageTests.Column c, ServerPlayer p, AbstractContainerMenu menu, int slot, int button, ContainerInput input,
                             Map<Item, Integer> universe, Map<Item, Integer> earnings, int[] clicks) {
        try {
            menu.clicked(slot, button, input, p);
        } catch (RuntimeException e) {
            throw h.assertionException(input + " slot " + slot + " button " + button + " threw " + e);
        }
        menu.broadcastChanges();
        clicks[0]++;
        Map<Item, Integer> now = earnings(c);
        for (Item it : POOL) {
            h.assertTrue(now.get(it) <= earnings.get(it), input + " slot " + slot + " button " + button + ": earnings grew " + earnings + " -> " + now);
            earnings.put(it, now.get(it));
        }
        Map<Item, Integer> u = universe(h, c, p, menu);
        h.assertTrue(u.equals(universe), input + " slot " + slot + " button " + button + ": items changed " + universe + " -> " + u);
    }

    private static Map<Item, Integer> earnings(StorageTests.Column c) {
        Map<Item, Integer> out = new LinkedHashMap<>();
        for (Item it : POOL) out.put(it, 0);
        List<EarningsStore> stores = new ArrayList<>();
        if (c.h.getLevel().getBlockEntity(c.sales) instanceof EarningsStore s) stores.add(s);
        stores.addAll(c.storages());
        for (EarningsStore s : stores) for (ItemStack st : s.earnings()) if (out.containsKey(st.getItem())) out.merge(st.getItem(), st.getCount(), Integer::sum);
        return out;
    }

    private static Map<Item, Integer> universe(GameTestHelper h, StorageTests.Column c, ServerPlayer p, AbstractContainerMenu menu) {
        Map<Item, Integer> out = earnings(c);
        Consumer<ItemStack> add = s -> {
            if (!s.isEmpty() && out.containsKey(s.getItem())) out.merge(s.getItem(), s.getCount(), Integer::sum);
        };
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) add.accept(p.getInventory().getItem(i));
        add.accept(menu.getCarried());
        if (h.getLevel().getBlockEntity(c.box) instanceof com.simpleshops.block.DisplayCaseBlockEntity box) box.goods().forEach(add);
        for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, AABB.ofSize(Vec3.atCenterOf(c.sales), 24, 24, 24))) add.accept(e.getItem());
        return out;
    }

    private TransactionTests() {}
}
