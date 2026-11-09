package com.simplegadgets.test;

import com.simplegadgets.GadgetEnchantments;
import com.simplegadgets.GadgetsRegistry;
import com.simplegadgets.SimpleGadgets;
import com.simplegadgets.attractor.AttractorFilter;
import com.simplegadgets.attractor.AttractorItem;
import com.simplegadgets.attractor.PlacedAttractor;
import com.simplegadgets.config.GadgetsConfig;
import com.simplelib.api.PlacedGadgets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Attractor in Simple Gadgets (OW24-2 G3, b-gadg14): held pull and its gates, the reach, the filter, what it leaves alone,
 * the placed form through SimpleLib {@link PlacedGadgets} (a test plate, so it runs without SimpleBuilding too), laying down
 * (only with a partner), the recipes and the config takeover. SimpleBuilding's {@code MagnetTests} keep the full behaviour for
 * 26.2. Every test runs standalone and with SimpleBuilding; parts that need its enchantments check for them.
 */
public final class AttractorTests {
    private static final Identifier IRON_POLE = Identifier.fromNamespaceAndPath(SimpleGadgets.OLD_NAMESPACE, "iron_pole");
    private static final double AT_REST = 1.0E-8;

    private AttractorTests() {
    }

    private static boolean withSimpleBuilding() {
        return BuiltInRegistries.ITEM.containsKey(IRON_POLE);
    }

    /** Held in the main hand it pulls a loose item towards the player; sneaking, not holding it or a non-player stop it. */
    public static void heldAttractorPullsAndSneakingPausesIt(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        ItemStack attractor = new ItemStack(GadgetsRegistry.MAGNET);
        player.setItemInHand(InteractionHand.MAIN_HAND, attractor);

        ItemEntity near = loose(helper, new Vec3(4.5, 1.0, 1.5), Items.DIAMOND);
        GadgetsRegistry.MAGNET.inventoryTick(attractor, helper.getLevel(), player, EquipmentSlot.MAINHAND);
        helper.assertTrue(near.getDeltaMovement().x < -AT_REST, "the held Attractor did not pull the item towards the player: " + near.getDeltaMovement());

        ItemEntity sneaking = loose(helper, new Vec3(1.5, 1.0, 4.5), Items.EMERALD);
        player.setShiftKeyDown(true);
        GadgetsRegistry.MAGNET.inventoryTick(attractor, helper.getLevel(), player, EquipmentSlot.MAINHAND);
        player.setShiftKeyDown(false);
        helper.assertTrue(sneaking.getDeltaMovement().lengthSqr() < AT_REST, "sneaking did not pause the Attractor");

        ItemStack pocket = new ItemStack(GadgetsRegistry.MAGNET);
        GadgetsRegistry.MAGNET.inventoryTick(attractor, helper.getLevel(), player, EquipmentSlot.MAINHAND); // moves both again
        Vec3 before = sneaking.getDeltaMovement();
        GadgetsRegistry.MAGNET.inventoryTick(pocket, helper.getLevel(), player, null);
        helper.assertTrue(sneaking.getDeltaMovement().equals(before), "an Attractor in the pocket (not held) pulled as well");

        boolean wasOn = GadgetsConfig.attractorEnabled;
        try {
            GadgetsConfig.attractorEnabled = false;
            ItemEntity off = loose(helper, new Vec3(3.5, 1.0, 3.5), Items.GOLD_INGOT);
            GadgetsRegistry.MAGNET.inventoryTick(attractor, helper.getLevel(), player, EquipmentSlot.MAINHAND);
            helper.assertTrue(off.getDeltaMovement().lengthSqr() < AT_REST, "attractorEnabled=false still pulled");
        } finally {
            GadgetsConfig.attractorEnabled = wasOn;
        }
        helper.succeed();
    }

    /** 4 blocks, +2 per Range level up to 9, times attractorRangeMultiplier, never beyond 15; 0 switches it off. */
    public static void reachIsFourBlocksAndRangeWidensItUpToItsCap(GameTestHelper helper) {
        helper.assertValueEqual(AttractorItem.pullRange(0, 1.0), 4.0, "reach without Range");
        helper.assertValueEqual(AttractorItem.pullRange(1, 1.0), 6.0, "reach with Range I");
        helper.assertValueEqual(AttractorItem.pullRange(3, 1.0), 9.0, "reach with Range III (cap)");
        helper.assertValueEqual(AttractorItem.pullRange(5, 1.0), 9.0, "reach above the cap");
        helper.assertValueEqual(AttractorItem.pullRange(3, 4.0), AttractorItem.HARD_MAX_RANGE, "reach with the largest factor");
        helper.assertValueEqual(AttractorItem.pullRange(2, 0.0), 0.0, "factor 0 switches it off");
        double old = GadgetsConfig.attractorRangeMultiplier;
        try {
            GadgetsConfig.attractorRangeMultiplier = 2.0;
            helper.assertValueEqual(AttractorItem.rangeMultiplier(), 2.0, "the held Attractor reads attractorRangeMultiplier");
            helper.assertValueEqual(PlacedAttractor.range(), PlacedAttractor.RANGE * 2.0, "the placed Attractor reads attractorRangeMultiplier");
        } finally {
            GadgetsConfig.attractorRangeMultiplier = old;
        }
        helper.succeed();
    }

    /** The filter is stored under "MagnetFilter" (old stacks keep theirs), matches the full id, and only works with Constructor's Touch. */
    public static void theFilterNeedsConstructorsTouchAndMatchesTheFullId(GameTestHelper helper) {
        ItemStack attractor = new ItemStack(GadgetsRegistry.MAGNET);
        AttractorItem.setFilter(attractor, "minecraft:diamond");
        helper.assertValueEqual(AttractorItem.filterOf(attractor), "minecraft:diamond", "stored filter");
        helper.assertTrue(attractor.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag().contains("MagnetFilter"),
                "the filter is no longer stored under MagnetFilter - every owned Attractor would lose its filter");
        ItemEntity diamond = loose(helper, new Vec3(2.5, 1.0, 2.5), Items.DIAMOND);
        ItemEntity block = loose(helper, new Vec3(3.5, 1.0, 2.5), Items.DIAMOND_BLOCK);
        helper.assertTrue(AttractorItem.passesFilter(diamond, "minecraft:diamond") && !AttractorItem.passesFilter(block, "minecraft:diamond"),
                "the filter does not match the full registry id only");
        helper.assertTrue(AttractorItem.passesFilter(block, null), "no filter lets everything through");
        helper.assertTrue(AttractorItem.effectiveFilter(attractor, helper.getLevel()) == null, "a filter without Constructor's Touch works");
        var touch = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(GadgetEnchantments.CONSTRUCTORS_TOUCH);
        if (touch.isPresent()) {
            attractor.enchant(touch.get(), 1);
            helper.assertValueEqual(AttractorItem.effectiveFilter(attractor, helper.getLevel()), "minecraft:diamond", "filter with Constructor's Touch");
            helper.assertTrue(PlacedAttractor.INSTANCE.placesOnPlainClick(attractor, helper.getLevel()),
                    "with Constructor's Touch the Attractor no longer lays down with a plain click");
        } else {
            helper.assertTrue(!withSimpleBuilding(), "SimpleBuilding is loaded but its Constructor's Touch is not");
        }
        AttractorItem.setFilter(attractor, null);
        helper.assertTrue(AttractorItem.filterOf(attractor) == null, "clearing left a filter");
        helper.succeed();
    }

    /** Display items, items reserved for another player and another player's death drops stay; the owner's own drops come. */
    public static void itLeavesDisplayReservedAndForeignDeathDropsAlone(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        ItemEntity plain = loose(helper, new Vec3(2.5, 1.0, 2.5), Items.APPLE);
        helper.assertTrue(AttractorFilter.mayAttract(plain, player), "a plain loose item is not attracted");
        ItemEntity display = loose(helper, new Vec3(3.5, 1.0, 2.5), Items.APPLE);
        display.setNeverPickUp();
        helper.assertTrue(!AttractorFilter.mayAttract(display, player), "a display item (never picked up) is attracted");
        ItemEntity reserved = loose(helper, new Vec3(4.5, 1.0, 2.5), Items.APPLE);
        reserved.setTarget(UUID.randomUUID());
        helper.assertTrue(!AttractorFilter.mayAttract(reserved, player), "an item reserved for another player is attracted");
        ItemEntity foreign = loose(helper, new Vec3(2.5, 1.0, 4.5), Items.APPLE);
        foreign.addTag(AttractorFilter.DEATH_DROP_TAG_PREFIX + UUID.randomUUID());
        helper.assertTrue(!AttractorFilter.mayAttract(foreign, player), "another player's death drop is attracted");
        ItemEntity own = loose(helper, new Vec3(3.5, 1.0, 4.5), Items.APPLE);
        AttractorFilter.beginDeathDrops(player);
        AttractorFilter.markIfDeathDrop(player, own);
        AttractorFilter.endDeathDrops();
        helper.assertTrue(own.entityTags().contains(AttractorFilter.DEATH_DROP_TAG_PREFIX + player.getUUID()), "the death drop was not marked");
        helper.assertTrue(AttractorFilter.mayAttract(own, player), "the owner's Attractor does not collect the owner's death drop");
        ItemEntity later = loose(helper, new Vec3(4.5, 1.0, 4.5), Items.APPLE);
        AttractorFilter.markIfDeathDrop(player, later);
        helper.assertTrue(later.entityTags().isEmpty(), "an item dropped outside the death loot was marked");

        // A real death marks the dropped inventory (PlayerDeathDropsMixin + DeathDropMarkMixin).
        ServerPlayer victim = mockPlayer(helper, new Vec3(5.5, 1.0, 5.5));
        victim.getInventory().clearContent();
        // Main inventory (slot 9): Simple Tweaks' partial keep inventory (default hotbar + armor) would keep slot 0.
        victim.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 3));
        victim.die(victim.damageSources().generic());
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds(), e -> e.getItem().is(Items.DIAMOND));
        try {
            helper.assertTrue(!drops.isEmpty(), "the dying player dropped no diamonds (is keepInventory on?)");
            for (ItemEntity entity : drops) {
                helper.assertTrue(victim.getUUID().equals(AttractorFilter.deathDropOwner(entity)), "a death drop carries no mark: " + entity.entityTags());
                helper.assertTrue(!AttractorFilter.mayAttract(entity, player), "another player's Attractor may take the death drop");
                helper.assertTrue(AttractorFilter.mayAttract(entity, victim), "the owner's Attractor may not take its own death drop");
            }
        } finally {
            drops.forEach(ItemEntity::discard);
        }
        helper.succeed();
    }

    /**
     * Laid down (a test plate standing in for SimpleBuilding's placed template): it pulls loose items to the plate, holds a
     * floating item without gravity at the target and lets it fall once no Attractor holds it any more.
     */
    public static void thePlacedAttractorPullsToThePlateAndLetsGo(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        TestPlate plate = new TestPlate(new ItemStack(GadgetsRegistry.MAGNET), Vec3.atCenterOf(pos));
        helper.assertTrue(PlacedGadgets.placeable(plate.stack()), "the Attractor is not a placeable gadget");
        ItemEntity item = loose(helper, new Vec3(5.5, 1.5, 1.5), Items.IRON_INGOT);
        int pulled = PlacedAttractor.pull(level, pos, plate);
        helper.assertTrue(pulled >= 1, "the placed Attractor pulled nothing");
        helper.assertTrue(item.getDeltaMovement().x < -AT_REST, "the item was not pulled towards the plate: " + item.getDeltaMovement());

        ItemEntity hovering = loose(helper, new Vec3(1.5, 1.5, 1.5), Items.GOLD_INGOT);
        hovering.setPos(PlacedAttractor.target(plate).add(0, 0.1, 0));
        hovering.setOnGround(false);
        PlacedAttractor.pull(level, pos, plate);
        helper.assertTrue(hovering.isNoGravity() && hovering.entityTags().contains(PlacedAttractor.HOVER_TAG),
                "the item at the target does not hover");
        plate.allowed = false;
        // No Attractor holds it any more: after the grace time it falls again.
        helper.startSequence().thenExecuteAfter(2 * PlacedAttractor.INTERVAL + 2, () -> {
            PlacedAttractor.releaseIfAbandoned(hovering);
            helper.assertTrue(!hovering.isNoGravity() && !hovering.entityTags().contains(PlacedAttractor.HOVER_TAG),
                    "the abandoned item keeps hovering");
            // The plate's claim answer counts: a plate that may not move the item leaves it alone.
            ItemEntity guarded = loose(helper, new Vec3(4.5, 1.5, 3.5), Items.COPPER_INGOT);
            PlacedAttractor.pull(level, pos, plate);
            helper.assertTrue(guarded.getDeltaMovement().lengthSqr() < AT_REST, "the plate pulled an item its claim check refused");
        }).thenSucceed();
    }

    /** Laying down needs a partner with a plate: alone the click does nothing; with SimpleBuilding a placed template stands there. */
    public static void layingDownNeedsAPlatePartner(GameTestHelper helper) {
        ServerPlayer player = mockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        BlockPos floor = new BlockPos(2, 1, 2);
        helper.setBlock(floor, Blocks.STONE);
        helper.setBlock(floor.above(), Blocks.AIR);
        ItemStack attractor = new ItemStack(GadgetsRegistry.MAGNET);
        player.setItemInHand(InteractionHand.MAIN_HAND, attractor);
        player.setShiftKeyDown(true);
        BlockPos abs = helper.absolutePos(floor);
        UseOnContext click = new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false));
        InteractionResult result = attractor.getItem().useOn(click);
        player.setShiftKeyDown(false);
        if (PlacedGadgets.hasPlacer()) {
            helper.assertTrue(result.consumesAction(), "with a plate partner the sneak click did not lay the Attractor down: " + result);
            helper.assertTrue(!helper.getBlockState(floor.above()).isAir(), "no plate stands above the clicked block");
        } else {
            helper.assertTrue(!withSimpleBuilding(), "SimpleBuilding is loaded but registered no plate partner");
            helper.assertTrue(result == InteractionResult.PASS, "alone the sneak click did something: " + result);
            helper.assertTrue(helper.getBlockState(floor.above()).isAir(), "alone a block appeared above the clicked one");
        }
        helper.succeed();
    }

    /** " R " / "I L" / "CI ": with SimpleBuilding its iron core and iron rods, alone iron ingots. */
    public static void theAttractorRecipeKeepsItsShape(GameTestHelper helper) {
        boolean sb = withSimpleBuilding();
        Item core = sb ? BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simplelib", "iron_core")) : Items.IRON_INGOT;
        Item rod = sb ? BuiltInRegistries.ITEM.getValue(IRON_POLE) : Items.IRON_INGOT;
        helper.assertTrue(core != Items.AIR, "the iron core is not registered");
        String id = sb ? "simplegadgets:magnet" : "simplegadgets:magnet_without_simplebuilding";
        CraftingInput grid = grid(null, Items.REDSTONE, null, rod, null, Items.LAPIS_LAZULI, core, rod, null);
        Optional<RecipeHolder<CraftingRecipe>> match = helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());
        helper.assertTrue(match.isPresent(), "the Attractor pattern matches no recipe");
        helper.assertValueEqual(match.get().id().identifier().toString(), id, "Attractor recipe");
        helper.assertTrue(match.get().value().assemble(grid).is(GadgetsRegistry.MAGNET), "the recipe makes something else");
        helper.succeed();
    }

    /** First start: SimpleBuilding's Attractor and Detector settings move over; a file of an earlier stage gets only the missing keys. */
    public static void theConfigTakesOverSimpleBuildingsValues(GameTestHelper helper) {
        String old = "{\"tools\":{\"magnetRangeMultiplier\":2.5},\"server\":{\"features\":{\"attractor\":false,\"oreDetector\":false},"
                + "\"tools\":{\"attractorMinimumDistance\":0.75},\"oreDetector\":{\"rangeMultiplier\":0.5,\"scanIntervalTicks\":40}}}";
        com.google.gson.JsonObject taken = com.google.gson.JsonParser.parseString(GadgetsConfig.fromSimpleBuilding(old)).getAsJsonObject();
        helper.assertTrue(!taken.get("attractorEnabled").getAsBoolean() && !taken.get("detectorEnabled").getAsBoolean(), "switches not taken: " + taken);
        helper.assertValueEqual(taken.get("attractorRangeMultiplier").getAsDouble(), 2.5, "attractorRangeMultiplier");
        helper.assertValueEqual(taken.get("attractorMinimumDistance").getAsDouble(), 0.75, "attractorMinimumDistance");
        helper.assertValueEqual(taken.get("detectorRangeMultiplier").getAsDouble(), 0.5, "detectorRangeMultiplier");
        helper.assertValueEqual(taken.get("detectorScanIntervalTicks").getAsInt(), 40, "detectorScanIntervalTicks");
        try {
            java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("simplegadgets-config");
            java.nio.file.Files.writeString(dir.resolve(GadgetsConfig.LEGACY_FILE), old);
            com.google.gson.JsonObject existing = new com.google.gson.JsonObject();
            existing.addProperty("rotatorMaxCharge", 512);
            existing.addProperty("attractorEnabled", true); // already in the file: stays
            GadgetsConfig.adoptMissing(existing, dir.resolve(GadgetsConfig.LEGACY_FILE));
            helper.assertTrue(existing.get("attractorEnabled").getAsBoolean(), "a key the file had was overwritten");
            helper.assertTrue(!existing.get("detectorEnabled").getAsBoolean(), "a missing key was not taken over: " + existing);
            helper.assertValueEqual(existing.get("rotatorMaxCharge").getAsInt(), 512, "an unrelated key changed");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        helper.succeed();
    }

    // ---- helpers

    /** A plate standing in for a partner's block: stack, centre, upward face, a switchable claim answer. */
    static final class TestPlate implements PlacedGadgets.Plate {
        private ItemStack stack;
        private final Vec3 centre;
        boolean allowed = true;
        int changes;

        TestPlate(ItemStack stack, Vec3 centre) {
            this.stack = stack;
            this.centre = centre;
        }

        @Override
        public ItemStack stack() {
            return stack;
        }

        @Override
        public void setStack(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public void changed() {
            changes++;
        }

        @Override
        public Vec3 surfaceCentre() {
            return centre;
        }

        @Override
        public Direction normal() {
            return Direction.UP;
        }

        @Override
        public boolean mayAutomate(Entity entity) {
            return allowed;
        }
    }

    static ItemEntity loose(GameTestHelper helper, Vec3 relative, Item item) {
        Vec3 pos = helper.absoluteVec(relative);
        ItemEntity entity = new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, new ItemStack(item), 0.0, 0.0, 0.0);
        entity.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(entity);
        helper.runBeforeTestEnd(entity::discard);
        return entity;
    }

    @SuppressWarnings("removal")
    static ServerPlayer mockPlayer(GameTestHelper helper, Vec3 relative) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(relative);
        player.snapTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));
        return player;
    }

    static CraftingInput grid(Item... items) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Item item : items) stacks.add(item == null ? ItemStack.EMPTY : new ItemStack(item));
        return CraftingInput.of(3, 3, stacks);
    }
}
