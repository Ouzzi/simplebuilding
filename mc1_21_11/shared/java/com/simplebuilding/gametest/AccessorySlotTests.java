package com.simplebuilding.gametest;

import com.simplebuilding.compat.accessory.AccessorySlots;
import com.simplebuilding.component.BackpackContents;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.BackpackItem;
import com.simplebuilding.items.custom.BackpackTier;
import com.simplebuilding.items.custom.QuiverItem;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.screen.BackpackMenu;
import com.simplebuilding.screen.BackpackMenuProviders;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.Vec3;

/**
 * Optional accessory slots (Curios on NeoForge, Trinkets Updated on Fabric): a backpack or quiver in
 * the back/belt slot counts as worn, between the chest slot and the inventory.
 *
 * <p>The test servers run without an accessory mod, so these tests drive the mod's own seam,
 * {@link AccessorySlots.Hook}, with a hook that answers only for the test's own player (tests run in
 * parallel on one server; everyone else sees no accessories). What the real Curios/Trinkets hooks
 * add on top - handing out the live slot stacks - is a two-line mapping in {@code CuriosCompat} and
 * {@code TrinketsCompat}.
 */
public final class AccessorySlotTests {
    private AccessorySlotTests() {
    }

    /**
     * The backpack lookup goes chest slot, accessory slot, inventory - for the backpack key
     * ({@link BackpackItem#carriedBackpackSlot}) and for everything that reads "the worn backpack"
     * ({@link BackpackItem#wornBackpack}: Funnel, Constructor's Touch, Master Builder).
     *
     * <ul>
     *   <li><b>Inventory only:</b> the key picks inventory slot 20, nothing counts as worn.</li>
     *   <li><b>Accessory + inventory:</b> the accessory backpack wins, as {@link BackpackItem#ACCESSORY_SLOT},
     *       and both lookups hand out the very stack the accessory slot holds.</li>
     *   <li><b>Chest + accessory + inventory:</b> the chest slot wins.</li>
     * </ul>
     *
     * <p>What breaks this test: skipping the accessory hook, asking it before the chest slot or after
     * the inventory, or handing out a copy instead of the slot's stack.
     */
    public static void backpackLookupGoesChestThenAccessoryThenInventory(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        TestHook hook = hookFor(helper, player);

        ItemStack inInventory = new ItemStack(ModItems.BACKPACK);
        player.getInventory().setItem(20, inInventory);
        Assertions.valueEqual(helper, BackpackItem.carriedBackpackSlot(player), 20,
                "slot the backpack key picks with a backpack only in the inventory");
        helper.assertTrue(BackpackItem.wornBackpack(player).isEmpty(),
                "a backpack lying in the inventory counted as worn");

        ItemStack inAccessory = new ItemStack(ModItems.REINFORCED_BACKPACK);
        hook.stacks.add(inAccessory);
        Assertions.valueEqual(helper, BackpackItem.carriedBackpackSlot(player), BackpackItem.ACCESSORY_SLOT,
                "slot the backpack key picks with a backpack in an accessory slot and one in the inventory");
        helper.assertTrue(BackpackItem.carriedStack(player, BackpackItem.ACCESSORY_SLOT) == inAccessory,
                "the accessory slot index resolved to something other than the accessory slot's stack");
        helper.assertTrue(BackpackItem.wornBackpack(player) == inAccessory,
                "the worn backpack is " + BackpackItem.wornBackpack(player) + ", not the one in the accessory slot");

        ItemStack inChest = new ItemStack(ModItems.NETHERITE_BACKPACK);
        player.setItemSlot(EquipmentSlot.CHEST, inChest);
        Assertions.valueEqual(helper, BackpackItem.carriedBackpackSlot(player), BackpackItem.CHEST_INVENTORY_SLOT,
                "slot the backpack key picks with backpacks in the chest slot, an accessory slot and the inventory");
        helper.assertTrue(BackpackItem.wornBackpack(player) == inChest,
                "the worn backpack is " + BackpackItem.wornBackpack(player) + ", not the one in the chest slot");

        succeed(helper);
    }

    /**
     * The backpack key opens a backpack worn in an accessory slot, and the menu writes into that very
     * stack.
     *
     * <ul>
     *   <li>The key may open it ({@link BackpackMenuProviders#canOpenCarried}), the menu has the
     *       accessory backpack's tier and locks no inventory slot.</li>
     *   <li>Stone put into the first backpack slot lands in the accessory stack's contents at once.</li>
     *   <li>Once the backpack leaves the accessory slot, the menu is no longer valid.</li>
     * </ul>
     *
     * <p>What breaks this test: a key gate that only knows inventory indices ({@code >= 0}), a menu
     * built from an inventory slot, a write-back that checks an inventory slot, and a validity check
     * that never notices the backpack is gone.
     */
    public static void backpackKeyOpensAndWritesBackTheAccessoryBackpack(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        TestHook hook = hookFor(helper, player);
        ItemStack backpack = new ItemStack(ModItems.REINFORCED_BACKPACK);
        hook.stacks.add(backpack);

        helper.assertTrue(BackpackMenuProviders.canOpenCarried(player),
                "the backpack key may not open a backpack worn in an accessory slot");
        BackpackMenu menu = (BackpackMenu) BackpackMenuProviders.carried(player).provider()
                .createMenu(1, player.getInventory(), player);
        Assertions.valueEqual(helper, menu.tier(), BackpackTier.REINFORCED, "tier of the accessory backpack the key opened");
        Assertions.valueEqual(helper, menu.lockedSlot(), BackpackItem.ACCESSORY_SLOT, "slot index the accessory backpack's menu carries");

        menu.slots.get(BackpackMenu.BACKPACK_SLOT_START).set(new ItemStack(Items.STONE, 5));
        BackpackContents contents = backpack.getOrDefault(ModDataComponentTypes.BACKPACK_CONTENTS, BackpackContents.EMPTY);
        Assertions.valueEqual(helper, BackpackItem.findEntry(backpack, s -> s.is(Items.STONE)), 0,
                "entry of the stone in the accessory backpack right after the click (contents: " + contents + ")");
        helper.assertTrue(menu.stillValid(player), "the menu of the backpack in the accessory slot is not valid");

        hook.stacks.clear();
        helper.assertFalse(menu.stillValid(player), "the menu stayed valid after the backpack left the accessory slot");
        player.containerMenu = player.inventoryMenu;

        succeed(helper);
    }

    /**
     * The quiver lookup for bow and crossbow goes chest slot, accessory slot, hotbar, and a shot pays
     * with the arrow of the quiver that supplied it. Funnel fills a quiver in an accessory slot.
     *
     * <ul>
     *   <li>Plain arrows in a worn quiver, spectral arrows in an accessory quiver, tipped arrows in a
     *       hotbar quiver: the bow gets the plain arrow.</li>
     *   <li>Without the worn quiver it gets the spectral arrow, and one shot changes only the accessory
     *       quiver.</li>
     *   <li>A picked up arrow goes into an accessory quiver with Funnel II, not into the inventory.</li>
     * </ul>
     *
     * <p>What breaks this test: skipping the accessory slot, asking it before the chest slot or after
     * the hotbar, paying from a different quiver than the one that supplied the arrow, and a Funnel
     * that never looks at accessory slots.
     */
    public static void quiverLookupGoesChestThenAccessoryThenHotbar(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        TestHook hook = hookFor(helper, player);

        ItemStack worn = filledQuiver(helper, player, ModItems.QUIVER, Items.ARROW);
        ItemStack accessory = filledQuiver(helper, player, ModItems.QUIVER, Items.SPECTRAL_ARROW);
        ItemStack hotbar = filledQuiver(helper, player, ModItems.QUIVER, Items.TIPPED_ARROW);
        player.setItemSlot(EquipmentSlot.CHEST, worn);
        hook.stacks.add(accessory);
        player.getInventory().setItem(3, hotbar);
        assertBowFinds(helper, player, Items.ARROW, "with quivers in the chest slot, an accessory slot and the hotbar");

        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        assertBowFinds(helper, player, Items.SPECTRAL_ARROW, "with quivers in an accessory slot and the hotbar");
        Object accessoryBefore = accessory.get(DataComponents.BUNDLE_CONTENTS);
        Object hotbarBefore = hotbar.get(DataComponents.BUNDLE_CONTENTS);
        QuiverItem.consumeProjectileForBow(player);
        helper.assertFalse(accessory.get(DataComponents.BUNDLE_CONTENTS).equals(accessoryBefore),
                "a shot supplied from the accessory quiver left that quiver untouched");
        helper.assertTrue(hotbar.get(DataComponents.BUNDLE_CONTENTS).equals(hotbarBefore),
                "a shot supplied from the accessory quiver took an arrow out of the hotbar quiver");

        // --- Funnel II on the accessory quiver ---
        hook.stacks.clear();
        player.getInventory().clearContent();
        ItemStack funnel = new ItemStack(ModItems.QUIVER);
        funnel.enchant(enchantment(helper, ModEnchantments.FUNNEL), 2);
        hook.stacks.add(funnel);
        touchArrow(helper, player);
        helper.assertTrue(QuiverItem.findProjectileForBow(player).is(Items.ARROW),
                "the Funnel II quiver in the accessory slot did not take the picked up arrow");
        Assertions.valueEqual(helper, player.getInventory().countItem(Items.ARROW), 0,
                "loose arrows although the Funnel II quiver in the accessory slot could take it");

        succeed(helper);
    }

    /**
     * Master Builder and Funnel read a backpack worn in an accessory slot like one in the chest slot:
     * a quiver inside a Master Builder backpack feeds the bow, and a Funnel II backpack takes items
     * from the ground.
     *
     * <p>What breaks this test: Master Builder or Funnel reading only the chest slot.
     */
    public static void masterBuilderAndFunnelReadTheAccessoryBackpack(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        TestHook hook = hookFor(helper, player);

        ItemStack quiver = filledQuiver(helper, player, ModItems.QUIVER, Items.SPECTRAL_ARROW);
        ItemStack backpack = new ItemStack(ModItems.BACKPACK);
        backpack.set(ModDataComponentTypes.BACKPACK_CONTENTS,
                new BackpackContents(List.of(BackpackContents.Entry.of(7, quiver))));
        hook.stacks.add(backpack);
        helper.assertTrue(QuiverItem.findProjectileForBow(player).isEmpty(),
                "a quiver inside an accessory backpack without Master Builder fed the bow");
        backpack.enchant(enchantment(helper, ModEnchantments.MASTER_BUILDER), 1);
        assertBowFinds(helper, player, Items.SPECTRAL_ARROW, "with a quiver inside a Master Builder backpack in an accessory slot");
        helper.assertTrue(BackpackItem.wornBackpackWith(player, ModEnchantments.MASTER_BUILDER) == backpack,
                "Master Builder did not find the backpack in the accessory slot");

        ItemStack funnel = new ItemStack(ModItems.BACKPACK);
        funnel.enchant(enchantment(helper, ModEnchantments.FUNNEL), 2);
        hook.stacks.set(0, funnel);
        ItemStack cobble = new ItemStack(Items.COBBLESTONE, 8);
        helper.assertTrue(BackpackItem.tryFunnelPickup(player, cobble),
                "a Funnel II backpack in an accessory slot turned down cobblestone from the ground");
        helper.assertTrue(cobble.isEmpty() && BackpackItem.findEntry(funnel, s -> s.is(Items.COBBLESTONE)) >= 0,
                "the cobblestone did not end up in the accessory backpack (left on the ground: " + cobble + ")");

        succeed(helper);
    }

    /**
     * An accessory mod whose API changed under the mod (a {@link LinkageError} from the hook) does
     * not crash the lookup: the hook is dropped after its first failure, and the query carries on as
     * if no accessory mod were there.
     *
     * <p>What breaks this test: letting the error through, or asking a broken hook again and again.
     */
    public static void anIncompatibleAccessoryModIsDroppedInsteadOfCrashing(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper);
        AtomicInteger calls = new AtomicInteger();
        AccessorySlots.Hook broken = entity -> {
            if (entity != player) {
                return List.of();
            }
            calls.incrementAndGet();
            throw new NoSuchMethodError("test: accessory API changed");
        };
        AccessorySlots.register(broken);
        cleanup(helper, () -> AccessorySlots.unregister(broken));

        player.getInventory().setItem(5, new ItemStack(ModItems.BACKPACK));
        Assertions.valueEqual(helper, BackpackItem.carriedBackpackSlot(player), 5,
                "slot the backpack key picks while the accessory hook is broken");
        Assertions.valueEqual(helper, BackpackItem.carriedBackpackSlot(player), 5,
                "slot the backpack key picks on the second ask");
        Assertions.valueEqual(helper, calls.get(), 1, "times the broken accessory hook was asked");

        succeed(helper);
    }

    // =====================================================================================
    // HELPERS
    // =====================================================================================

    /** Accessory slots of one player only - a list the test fills in accessory slot order. */
    private static final class TestHook implements AccessorySlots.Hook {
        private final LivingEntity owner;
        final List<ItemStack> stacks = new ArrayList<>();

        TestHook(LivingEntity owner) {
            this.owner = owner;
        }

        @Override
        public List<ItemStack> worn(LivingEntity entity) {
            return entity == this.owner ? this.stacks : List.of();
        }
    }

    private static TestHook hookFor(GameTestHelper helper, ServerPlayer player) {
        TestHook hook = new TestHook(player);
        AccessorySlots.register(hook);
        cleanup(helper, () -> AccessorySlots.unregister(hook));
        return hook;
    }

    /** The in-level mock player in the room, survival abilities, empty inventory. */
    @SuppressWarnings("removal")
    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(1.5, 2.0, 1.5));
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.getAbilities().instabuild = false;
        player.getInventory().clearContent();
        cleanup(helper, () -> {
            player.containerMenu = player.inventoryMenu;
            helper.getLevel().getServer().getPlayerList().remove(player);
        });
        return player;
    }

    private static void cleanup(GameTestHelper helper, Runnable action) {
        TestCleanup.before(helper, action);
    }

    private static void succeed(GameTestHelper helper) {
        TestCleanup.succeed(helper);
    }

    /** A quiver of {@code item} holding four {@code arrow}, filled through the world pickup path. */
    private static ItemStack filledQuiver(GameTestHelper helper, ServerPlayer player, Item item, Item arrow) {
        ItemStack quiver = new ItemStack(item);
        helper.assertTrue(((ReinforcedBundleItem) item).tryInsertStackFromWorld(quiver, new ItemStack(arrow, 4), player),
                "test setup broken: " + item + " refused " + arrow);
        return quiver;
    }

    private static void assertBowFinds(GameTestHelper helper, ServerPlayer player, Item expected, String what) {
        ItemStack found = QuiverItem.findProjectileForBow(player);
        helper.assertTrue(found.is(expected), what + ": the bow was handed " + found + " instead of " + expected);
    }

    /** A pickable plain arrow lying in the room, touched by {@code player} (vanilla's pickup path). */
    private static void touchArrow(GameTestHelper helper, ServerPlayer player) {
        Vec3 pos = helper.absoluteVec(new Vec3(1.5, 2.0, 1.5));
        ServerLevel level = helper.getLevel();
        AbstractArrow arrow = new net.minecraft.world.entity.projectile.arrow.Arrow(level, pos.x, pos.y, pos.z,
                new ItemStack(Items.ARROW), null);
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        arrow.setNoPhysics(true);
        level.addFreshEntity(arrow);
        arrow.playerTouch(player);
        if (!arrow.isRemoved()) {
            arrow.discard();
            helper.fail("the player could not pick up the arrow at all");
        }
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }
}
