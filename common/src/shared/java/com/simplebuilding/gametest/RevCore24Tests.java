package com.simplebuilding.gametest;

import com.simplebuilding.api.WorldPermissions;
import com.simplebuilding.data.ModDataTables;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.version.McVersion;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Review 24, core findings F-07, F-08, F-09, F-15 (b-revcore24).
 *
 * <p>Not covered by a game test: F-19 (needs several unloaded chunks of loaders whose owner returns), F-20 (needs a
 * container below the crafter whose {@code addItem} deviates from {@code fits}), F-12 (the guide migration keeps its
 * own tests in {@code GuideBookTests}; the change is an earlier prefix test), F-16 (documentation only).
 */
public final class RevCore24Tests {

    private RevCore24Tests() {
    }

    /**
     * F-07: a provider whose claim is one block column inside a chunk, away from the chunk corner. Before the fix the
     * footprint check only asked the chunk corner and let the box through.
     */
    @SuppressWarnings("removal")
    public static void claimsSmallerThanAChunkAreAskedAtTheRealFootprint(GameTestHelper helper) {
        try {
            claimsBody(helper);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("removal")
    private static void claimsBody(GameTestHelper helper) throws ReflectiveOperationException {
        Class<?> protection;
        try {
            protection = Class.forName("com.simplebuilding.framework.api.Protection");
        } catch (ClassNotFoundException e) {
            helper.succeed(); // this version has no framework providers
            return;
        }
        ServerLevel level = helper.getLevel();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos claimed = helper.absolutePos(new BlockPos(2, 1, 2));
        if ((claimed.getX() & 15) == 0 && (claimed.getZ() & 15) == 0) {
            claimed = claimed.offset(1, 0, 1);
        }
        final int cx = claimed.getX();
        final int cz = claimed.getZ();
        Class<?> check = Class.forName("com.simplebuilding.framework.api.Protection$Check");
        Class<?> target = Class.forName("com.simplebuilding.framework.api.Protection$Target");
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            Object t = args[0];
            int x = (Integer) target.getMethod("x").invoke(t);
            int z = (Integer) target.getMethod("z").invoke(t);
            return !(x == cx && z == cz);
        };
        Object provider = Proxy.newProxyInstance(check.getClassLoader(), new Class<?>[]{check}, handler);
        Object server = level.getServer();
        String id = "revcore24_claim";
        protection.getMethod("register", Object.class, String.class, check).invoke(null, server, id, provider);
        try {
            helper.assertTrue(WorldPermissions.active(level), "test setup broken: the provider is not active");
            helper.assertTrue(!WorldPermissions.mayOccupy(level, player, new AABB(claimed)),
                    "a box inside the claimed column was allowed: only the chunk corner was asked");
            helper.assertTrue(!WorldPermissions.mayOccupy(level, player, new AABB(claimed).inflate(0.4, 0, 0.4)),
                    "a box overlapping the claimed column was allowed");
            BlockPos free = claimed.offset(1, 0, 0);
            helper.assertTrue(WorldPermissions.mayOccupy(level, player, new AABB(free)),
                    "a box beside the claim was refused");
            ItemEntity item = new ItemEntity(level, claimed.getX() + 0.5, claimed.getY(), claimed.getZ() + 0.5, new ItemStack(Items.STONE));
            helper.assertTrue(!WorldPermissions.mayAutomateEntity(level, claimed.offset(3, 0, 0), item),
                    "automation reached an entity in the claimed column");
        } finally {
            protection.getMethod("unregister", Object.class, String.class).invoke(null, server, id);
        }
        helper.succeed();
    }

    /**
     * F-08: the held Attractor pulls through {@code push()}, so the pull marks the item for vanilla's own sync;
     * an item that does not change speed is not marked (no packet).
     */
    @SuppressWarnings("removal")
    public static void heldAttractorMarksMovingItemsForSyncAndLeavesRestingOnesAlone(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 spot = helper.absoluteVec(new Vec3(1.5, 1.0, 1.5));
        player.teleportTo(spot.x, spot.y, spot.z);
        ItemStack magnet = new ItemStack(ModItems.MAGNET);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, magnet);
        ItemEntity far = helper.spawnItem(Items.DIAMOND, new Vec3(4.5, 1.0, 1.5));
        far.setDeltaMovement(Vec3.ZERO);
        far.setOnGround(false);
        far.needsSync = false;
        magnet.getItem().inventoryTick(magnet, level, player, EquipmentSlot.MAINHAND);
        helper.assertTrue(far.getDeltaMovement().lengthSqr() > 0 && far.needsSync,
                "the pulled item was not marked for the vanilla motion sync: " + far.getDeltaMovement() + " " + far.needsSync);
        helper.runBeforeTestEnd(() -> level.getServer().getPlayerList().remove(player));
        helper.succeed();
    }

    /** F-09: hammer + hammer drops an enchantment the left hammer excludes, like vanilla. */
    @SuppressWarnings("removal")
    public static void anvilHammerCombineSkipsIncompatibleEnchantments(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        var hammer = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(McVersion.HAMMERS_NAMESPACE, "iron_sledgehammer"));
        var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> silk = registry.getOrThrow(Enchantments.SILK_TOUCH);
        Holder<Enchantment> fortune = registry.getOrThrow(Enchantments.FORTUNE);
        Holder<Enchantment> unbreaking = registry.getOrThrow(Enchantments.UNBREAKING);
        ItemStack left = new ItemStack(hammer);
        left.enchant(silk, 1);
        ItemStack right = new ItemStack(hammer);
        right.enchant(fortune, 2);
        right.enchant(unbreaking, 1);
        AnvilMenu menu = new AnvilMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        menu.getSlot(AnvilMenu.INPUT_SLOT).set(left);
        menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(right);
        ItemStack result = menu.getSlot(AnvilMenu.RESULT_SLOT).getItem();
        helper.assertTrue(!result.isEmpty(), "the anvil made no result for hammer + hammer");
        var enchants = EnchantmentHelper.getEnchantmentsForCrafting(result);
        helper.assertTrue(enchants.getLevel(silk) == 1, "silk touch got lost: " + enchants);
        helper.assertTrue(enchants.getLevel(fortune) == 0, "fortune was combined with silk touch: " + enchants);
        helper.assertTrue(enchants.getLevel(unbreaking) == 1, "a compatible enchantment was dropped: " + enchants);
        helper.succeed();
    }

    /** F-15: the sync payload is trimmed below the packet limit, largest files first. */
    public static void dataTablesAreTrimmedToTheSyncPacketLimit(GameTestHelper helper) {
        Map<String, String> chisel = new HashMap<>();
        Map<String, String> upgrades = new HashMap<>();
        chisel.put("a:small", "{}");
        upgrades.put("a:small", "{}");
        chisel.put("a:big", "x".repeat(600_000));
        upgrades.put("a:also_big", "y".repeat(600_000));
        ModDataTables.trimToPacket(chisel, upgrades);
        helper.assertTrue(chisel.size() + upgrades.size() == 3 && chisel.containsKey("a:small") && upgrades.containsKey("a:small"),
                "trimming dropped more than one big file or a small one: " + chisel.keySet() + " " + upgrades.keySet());
        Map<String, String> fine = new HashMap<>(Map.of("a:one", "{}"));
        Map<String, String> none = new HashMap<>();
        ModDataTables.trimToPacket(fine, none);
        helper.assertTrue(fine.size() == 1, "a small table was trimmed");
        helper.succeed();
    }
}
