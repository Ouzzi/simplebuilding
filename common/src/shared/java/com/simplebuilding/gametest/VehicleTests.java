package com.simplebuilding.gametest;

import com.simplebuilding.blocks.custom.ChestTier;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.entity.ModEntities;
import com.simplebuilding.entity.vehicle.BoatWoods;
import com.simplebuilding.entity.vehicle.TieredChestBoat;
import com.simplebuilding.entity.vehicle.TieredChestMinecart;
import com.simplebuilding.entity.vehicle.TieredFurnaceMinecart;
import com.simplebuilding.entity.vehicle.TieredHopperMinecart;
import com.simplebuilding.entity.vehicle.VehicleTiers;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.screen.TieredChestMenu;
import com.simplebuilding.util.HopperFilterMode;
import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * Tiered vehicles (Queue N19/N23, docs/ai/PLAN-FAHRZEUG-STUFEN-2026-10-10.md). The expected numbers are this file's
 * own constants (the defaults of server.machines), not read from the mod.
 */
public final class VehicleTests {
    public static final int MOVE_MAX_TICKS = 200;

    private static final ChestTier[] TIERS = ChestTier.values();
    private static final int[] SLOTS = {36, 45, 54};
    private static final int[] COBBLE_STACK = {64, 128, 256};
    private static final int[] FUEL_PER_COAL = {7200, 14400, 28800};
    private static final double[] SPEED_SHARE = {0.625, 0.75, 1.0};
    private static final int[] ITEMS_PER_TICK = {2, 4, 8};

    private VehicleTests() {
    }

    private static boolean active(GameTestHelper helper) {
        if (!McVersion.TIERED_VEHICLES) {
            helper.succeed();
            return false;
        }
        return true;
    }

    private static List<EntityType<TieredChestMinecart>> chestCarts() {
        return List.of(ModEntities.REINFORCED_CHEST_MINECART, ModEntities.NETHERITE_CHEST_MINECART, ModEntities.ENDERITE_CHEST_MINECART);
    }

    private static List<EntityType<TieredFurnaceMinecart>> furnaceCarts() {
        return List.of(ModEntities.REINFORCED_FURNACE_MINECART, ModEntities.NETHERITE_FURNACE_MINECART, ModEntities.ENDERITE_FURNACE_MINECART);
    }

    private static List<EntityType<TieredHopperMinecart>> hopperCarts() {
        return List.of(ModEntities.REINFORCED_HOPPER_MINECART, ModEntities.NETHERITE_HOPPER_MINECART, ModEntities.ENDERITE_HOPPER_MINECART);
    }

    private static List<EntityType<TieredChestBoat>> chestBoats() {
        return List.of(ModEntities.REINFORCED_CHEST_BOAT, ModEntities.NETHERITE_CHEST_BOAT, ModEntities.ENDERITE_CHEST_BOAT);
    }

    // =====================================================================================
    // INVENTORY SIZE, STACKS, SAVING, MENU
    // =====================================================================================

    /**
     * Chest carts and chest boats hold the slots and stack size of their tier's chest (36/45/54; cobblestone 64/128/256),
     * open the tier chest menu with all slots, and keep an oversized stack (above 99) and the boat's wood through saving.
     */
    public static void chestVehiclesHoldTheTierSlotsAndKeepOversizedStacks(GameTestHelper helper) {
        if (!active(helper)) return;
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        for (int i = 0; i < TIERS.length; i++) {
            List<Container> vehicles = new ArrayList<>();
            vehicles.add(helper.spawn(chestCarts().get(i), new BlockPos(1, 2, 1)));
            TieredChestBoat boat = helper.spawn(chestBoats().get(i), new BlockPos(4, 2, 4));
            boat.setWood("cherry");
            vehicles.add(boat);
            for (Container vehicle : vehicles) {
                String name = ((Entity) vehicle).getType().toShortString();
                helper.assertValueEqual(vehicle.getContainerSize(), SLOTS[i], name + " slots");
                helper.assertValueEqual(vehicle.getMaxStackSize(new ItemStack(Items.COBBLESTONE)), COBBLE_STACK[i], name + " cobblestone stack");
                helper.assertValueEqual(vehicle.getMaxStackSize(new ItemStack(Items.DIAMOND_SWORD)), 1, name + " sword stack");
                vehicle.setItem(SLOTS[i] - 1, new ItemStack(Items.COBBLESTONE, COBBLE_STACK[i]));
                Entity entity = (Entity) vehicle;
                TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
                entity.saveWithoutId(output);
                Entity loaded = entity.getType().create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
                loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
                ItemStack back = ((Container) loaded).getItem(SLOTS[i] - 1);
                helper.assertTrue(back.is(Items.COBBLESTONE) && back.getCount() == COBBLE_STACK[i],
                        name + " saved " + COBBLE_STACK[i] + " cobblestone in its last slot and loaded " + back);
                if (loaded instanceof TieredChestBoat loadedBoat) {
                    helper.assertValueEqual(loadedBoat.wood(), "cherry", name + " wood after loading");
                }
                net.minecraft.world.inventory.AbstractContainerMenu menu = ((net.minecraft.world.MenuProvider) vehicle)
                        .createMenu(1, player.getInventory(), player);
                helper.assertTrue(menu instanceof TieredChestMenu tiered && tiered.chestSlotCount() == SLOTS[i]
                                && tiered.tier() == TIERS[i],
                        name + " opens " + (menu == null ? "nothing" : menu.getClass().getSimpleName()) + " instead of the tier chest menu");
                vehicle.clearContent();
                entity.discard();
            }
        }
        helper.succeed();
    }

    // =====================================================================================
    // FURNACE CARTS
    // =====================================================================================

    /**
     * One coal burns 7200/14400/28800 ticks, the cart holds eight like vanilla's (scaled with the tier); the top speed
     * is 5/8, 3/4 and all of a plain cart's (vanilla 1/2).
     */
    public static void furnaceCartsBurnLongerAndRunFaster(GameTestHelper helper) {
        if (!active(helper)) return;
        ServerLevel level = helper.getLevel();
        for (int i = 0; i < TIERS.length; i++) {
            TieredFurnaceMinecart cart = helper.spawn(furnaceCarts().get(i), new BlockPos(1, 2, 1));
            String name = cart.getType().toShortString();
            helper.assertTrue(cart.addFuel(cart.position().subtract(1, 0, 0), new ItemStack(Items.COAL)), name + " refused coal");
            helper.assertValueEqual(cart.fuel(), FUEL_PER_COAL[i], name + " fuel of one coal");
            double plain = cart.getBehavior().getMaxSpeed(level);
            helper.assertTrue(Math.abs(cart.topSpeed(level) - plain * SPEED_SHARE[i]) < 1e-9,
                    name + " top speed " + cart.topSpeed(level) + " instead of " + SPEED_SHARE[i] + " x " + plain);
            int coal = 1;
            while (cart.addFuel(cart.position(), new ItemStack(Items.COAL))) coal++;
            helper.assertValueEqual(coal, 8, name + " coal it takes before full (32000 x factor / 3600 x factor)");
            cart.discard();
        }
        helper.succeed();
    }

    /** On a straight rail a fuelled Enderite furnace cart moves faster per tick than vanilla's furnace cart can. */
    public static void enderiteFurnaceCartOutrunsVanilla(GameTestHelper helper) {
        if (!active(helper)) return;
        for (int x = 0; x <= 7; x++) for (int z = 1; z <= 3; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        for (int x = 1; x <= 7; x++) {
            helper.setBlock(new BlockPos(x, 1, 1), Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
            helper.setBlock(new BlockPos(x, 1, 3), Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
        }
        TieredFurnaceMinecart tiered = helper.spawn(ModEntities.ENDERITE_FURNACE_MINECART, new BlockPos(1, 1, 1));
        net.minecraft.world.entity.vehicle.minecart.MinecartFurnace vanilla = helper.spawn(
                net.minecraft.world.entity.EntityTypes.FURNACE_MINECART, new BlockPos(1, 1, 3));
        tiered.addFuel(tiered.position().subtract(1, 0, 0), new ItemStack(Items.COAL));
        vanilla.addFuel(vanilla.position().subtract(1, 0, 0), new ItemStack(Items.COAL));
        double[] last = {tiered.getX(), vanilla.getX()};
        double[] fastest = {0, 0};
        helper.onEachTick(() -> {
            fastest[0] = Math.max(fastest[0], Math.abs(tiered.getX() - last[0]));
            fastest[1] = Math.max(fastest[1], Math.abs(vanilla.getX() - last[1]));
            last[0] = tiered.getX();
            last[1] = vanilla.getX();
        });
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(fastest[0] > 0.3,
                "the enderite furnace cart's fastest step is " + fastest[0] + " (vanilla's " + fastest[1] + ")"))
                .thenExecute(() -> {
                    helper.assertTrue(fastest[1] <= 0.2 + 1e-3, "vanilla's furnace cart ran " + fastest[1] + " per tick");
                    tiered.discard();
                    vanilla.discard();
                }).thenSucceed();
    }

    // =====================================================================================
    // HOPPER CARTS
    // =====================================================================================

    /**
     * Under a full chest each tier cart takes in 2/4/8 times what vanilla's hopper cart takes in the same ticks.
     */
    public static void hopperCartsTakeInAtTheHopperSpeed(GameTestHelper helper) {
        if (!active(helper)) return;
        List<net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer> carts = new ArrayList<>();
        List<BlockPos> chests = new ArrayList<>();
        for (int lane = 0; lane < 4; lane++) {
            BlockPos rail = new BlockPos(1 + lane * 2, 1, 2);
            helper.setBlock(rail.below(), Blocks.STONE);
            helper.setBlock(rail, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH));
            helper.setBlock(rail.above(), Blocks.CHEST);
            ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(rail.above(), ChestBlockEntity.class);
            for (int slot = 0; slot < 27; slot++) chest.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
            chests.add(rail.above());
            if (lane == 0) {
                MinecartHopper vanilla = helper.spawn(net.minecraft.world.entity.EntityTypes.HOPPER_MINECART, rail);
                vanilla.setEnabled(false);
                carts.add(vanilla);
            } else {
                TieredHopperMinecart cart = helper.spawn(hopperCarts().get(lane - 1), rail);
                cart.setEnabled(false);
                carts.add(cart);
            }
        }
        helper.startSequence().thenIdle(2).thenExecute(() -> carts.forEach(c -> setEnabled(c, true)))
                .thenIdle(3).thenExecute(() -> {
                    carts.forEach(c -> setEnabled(c, false));
                    int[] taken = new int[4];
                    for (int lane = 0; lane < 4; lane++) {
                        for (int slot = 0; slot < 5; slot++) taken[lane] += carts.get(lane).getItem(slot).getCount();
                    }
                    helper.assertTrue(taken[0] > 0, "vanilla's hopper cart took nothing");
                    for (int i = 0; i < 3; i++) {
                        helper.assertValueEqual(taken[i + 1], taken[0] * ITEMS_PER_TICK[i],
                                carts.get(i + 1).getType().toShortString() + " items taken while vanilla took " + taken[0]);
                    }
                    carts.forEach(c -> {
                        c.clearContent();
                        c.discard();
                    });
                }).thenSucceed();
    }

    private static void setEnabled(Entity cart, boolean enabled) {
        if (cart instanceof MinecartHopper vanilla) vanilla.setEnabled(enabled);
        if (cart instanceof TieredHopperMinecart tiered) tiered.setEnabled(enabled);
    }

    /**
     * Filter principle on the cart: with Exact Match on, only the slot holding cobblestone fills - from a chest of
     * cobblestone and dirt - no dirt comes in, and a hopper below may take everything but the last cobblestone.
     */
    public static void hopperCartFiltersLikeTheHopperBlock(GameTestHelper helper) {
        if (!active(helper)) return;
        BlockPos rail = new BlockPos(2, 2, 2);
        helper.setBlock(rail.below(), Blocks.STONE);
        helper.setBlock(rail, Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH));
        helper.setBlock(rail.above(), Blocks.CHEST);
        ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(rail.above(), ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.DIRT, 64));
        chest.setItem(1, new ItemStack(Items.COBBLESTONE, 16));
        TieredHopperMinecart cart = helper.spawn(ModEntities.NETHERITE_HOPPER_MINECART, rail);
        cart.setEnabled(false);
        cart.setItem(0, new ItemStack(Items.COBBLESTONE, 1));
        cart.toggleFilterMode();
        helper.assertTrue(cart.getFilterMode() == HopperFilterMode.WHITELIST, "the filter key did not reach Exact Match");
        helper.assertTrue(!cart.canPlaceItem(1, new ItemStack(Items.DIRT)), "an empty slot takes dirt with the filter on");
        helper.assertTrue(cart.canTakeItem(chest, 0, cart.getItem(0)) == false, "a hopper could take the last (filter) item");
        helper.assertTrue(cart.mayPlayerPlace(1, new ItemStack(Items.DIRT)) && !cart.mayPlayerPlace(0, new ItemStack(Items.DIRT)),
                "a player may set a filter in an empty slot but not put dirt onto the cobblestone filter");
        helper.startSequence().thenExecute(() -> cart.setEnabled(true)).thenIdle(10).thenExecute(() -> {
            cart.setEnabled(false);
            helper.assertTrue(cart.getItem(0).is(Items.COBBLESTONE) && cart.getItem(0).getCount() == 17,
                    "the filter slot holds " + cart.getItem(0) + " instead of all 17 cobblestone");
            for (int slot = 1; slot < 5; slot++) {
                helper.assertTrue(cart.getItem(slot).isEmpty(), "slot " + slot + " took " + cart.getItem(slot));
            }
            helper.assertValueEqual(chest.getItem(0).getCount(), 64, "dirt left in the chest");
            helper.assertTrue(cart.canTakeItem(chest, 0, cart.getItem(0)), "hoppers may take the cobblestone above the filter item");
            cart.clearContent();
            cart.discard();
        }).thenSucceed();
    }

    // =====================================================================================
    // DROPS AND RECIPES
    // =====================================================================================

    /** Broken vehicles drop their own tier item (the boat with its wood, a name kept) and their contents. */
    public static void brokenVehiclesDropTheirTierItemAndContents(GameTestHelper helper) {
        if (!active(helper)) return;
        ServerLevel level = helper.getLevel();
        List<Entity> vehicles = new ArrayList<>();
        vehicles.add(helper.spawn(ModEntities.NETHERITE_CHEST_MINECART, new BlockPos(1, 2, 1)));
        vehicles.add(helper.spawn(ModEntities.ENDERITE_FURNACE_MINECART, new BlockPos(3, 2, 1)));
        vehicles.add(helper.spawn(ModEntities.REINFORCED_HOPPER_MINECART, new BlockPos(5, 2, 1)));
        TieredChestBoat boat = helper.spawn(ModEntities.ENDERITE_CHEST_BOAT, new BlockPos(3, 2, 5));
        boat.setWood("cherry");
        boat.setCustomName(net.minecraft.network.chat.Component.literal("Ferry"));
        vehicles.add(boat);
        Item[] expected = {ModItems.NETHERITE_CHEST_MINECART, ModItems.ENDERITE_FURNACE_MINECART, ModItems.REINFORCED_HOPPER_MINECART,
                ModItems.ENDERITE_CHEST_BOAT};
        for (int i = 0; i < vehicles.size(); i++) {
            Entity vehicle = vehicles.get(i);
            if (vehicle instanceof Container container) container.setItem(0, new ItemStack(Items.EMERALD, 3));
            vehicle.hurtServer(level, level.damageSources().generic(), 100.0F);
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, vehicle.getBoundingBox().inflate(2.0));
            Item item = expected[i];
            Optional<ItemEntity> own = drops.stream().filter(d -> d.getItem().is(item)).findFirst();
            helper.assertTrue(own.isPresent(), vehicle.getType().toShortString() + " dropped " + drops.stream().map(d -> d.getItem().toString()).toList());
            if (vehicle instanceof Container) {
                helper.assertTrue(drops.stream().anyMatch(d -> d.getItem().is(Items.EMERALD)), vehicle.getType().toShortString() + " lost its contents");
            }
            if (vehicle == boat) {
                ItemStack stack = own.get().getItem();
                helper.assertValueEqual(stack.get(ModDataComponentTypes.BOAT_WOOD), "cherry", "wood of the dropped boat");
                helper.assertTrue(stack.getHoverName().getString().equals("Ferry"), "the boat lost its name: " + stack.getHoverName().getString());
            }
            drops.forEach(Entity::discard);
        }
        helper.succeed();
    }

    /**
     * Crafting: cart + tier block, boat + tier chest (wood carried over); smithing reinforced -> netherite -> enderite,
     * the boat keeping its wood.
     */
    public static void vehicleRecipesCraftAndUpgrade(GameTestHelper helper) {
        if (!active(helper)) return;
        ServerLevel level = helper.getLevel();
        List<String> wrong = new ArrayList<>();
        for (ChestTier tier : TIERS) {
            craft(helper, wrong, List.of(new ItemStack(Items.MINECART), new ItemStack(VehicleTiers.chest(tier))), VehicleTiers.chestMinecart(tier), null);
            craft(helper, wrong, List.of(new ItemStack(VehicleTiers.furnace(tier)), new ItemStack(Items.MINECART)), VehicleTiers.furnaceMinecart(tier), null);
            craft(helper, wrong, List.of(new ItemStack(Items.MINECART), new ItemStack(VehicleTiers.hopper(tier))), VehicleTiers.hopperMinecart(tier), null);
            for (String wood : BoatWoods.ALL) {
                craft(helper, wrong, List.of(new ItemStack(BoatWoods.boat(wood)), new ItemStack(VehicleTiers.chest(tier))), VehicleTiers.chestBoat(tier), wood);
            }
        }
        ItemStack base = BoatWoods.stack(ModItems.REINFORCED_CHEST_BOAT, "mangrove");
        ItemStack netherite = smith(helper, new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), base,
                new ItemStack(Items.NETHERITE_INGOT)));
        if (!netherite.is(ModItems.NETHERITE_CHEST_BOAT) || !"mangrove".equals(netherite.get(ModDataComponentTypes.BOAT_WOOD))) {
            wrong.add("smithing reinforced chest boat (mangrove) gave " + netherite + " wood " + netherite.get(ModDataComponentTypes.BOAT_WOOD));
        }
        ItemStack enderite = smith(helper, new SmithingRecipeInput(new ItemStack(ModItems.ENDERITE_UPGRADE_TEMPLATE),
                new ItemStack(ModItems.NETHERITE_HOPPER_MINECART), new ItemStack(ModItems.ENDERITE_INGOT)));
        if (!enderite.is(ModItems.ENDERITE_HOPPER_MINECART)) {
            wrong.add("smithing netherite hopper minecart gave " + enderite);
        }
        helper.assertTrue(wrong.isEmpty(), "vehicle recipes: " + wrong);
        helper.succeed();
    }

    private static void craft(GameTestHelper helper, List<String> wrong, List<ItemStack> ingredients, Item result, String wood) {
        CraftingInput grid = CraftingInput.of(2, 1, ingredients);
        ItemStack got = helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel())
                .map(m -> m.value().assemble(grid)).orElse(ItemStack.EMPTY);
        boolean ok = got.is(result) && (wood == null || wood.equals(got.get(ModDataComponentTypes.BOAT_WOOD)));
        if (!ok) {
            wrong.add(BuiltInRegistries.ITEM.getKey(result).getPath() + (wood == null ? "" : " (" + wood + ")") + " got " + got
                    + (got.isEmpty() ? "" : " wood " + got.get(ModDataComponentTypes.BOAT_WOOD)));
        }
    }

    private static ItemStack smith(GameTestHelper helper, SmithingRecipeInput input) {
        return helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, helper.getLevel())
                .map(m -> m.value().assemble(input)).orElse(ItemStack.EMPTY);
    }
}
