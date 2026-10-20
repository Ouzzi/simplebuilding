package com.simplestorage.test;

import com.simplestorage.SimpleStorage;
import com.simplestorage.chest.ChestTier;
import com.simplestorage.chest.TieredChestMenu;
import com.simplestorage.registry.StorageComponents;
import com.simplestorage.registry.StorageEntities;
import com.simplestorage.registry.StorageItems;
import com.simplestorage.vehicle.BoatWoods;
import com.simplestorage.vehicle.TieredChestBoat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Chest carts and chest boats (split stage 8b-2; bodies from SimpleBuilding's {@code VehicleTests} and
 * {@code WorldPolishTests}, unchanged in what they check). The expected numbers are this file's own constants.
 */
final class VehicleTests {
    private static final ChestTier[] TIERS = ChestTier.values();
    private static final int[] SLOTS = {36, 45, 54};
    private static final int[] COBBLE_STACK = {64, 128, 256};

    private VehicleTests() {
    }

    /**
     * Chest carts and chest boats hold the slots and stack size of their tier's chest (36/45/54; cobblestone 64/128/256),
     * open the tier chest menu with all slots, and keep an oversized stack (above 99) and the boat's wood through saving.
     */
    static void chestVehiclesHoldTheTierSlotsAndKeepOversizedStacks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (int i = 0; i < TIERS.length; i++) {
            List<Container> vehicles = new ArrayList<>();
            vehicles.add(helper.spawn(StorageEntities.CHEST_MINECARTS.get(TIERS[i]), new BlockPos(1, 2, 1)));
            TieredChestBoat boat = helper.spawn(StorageEntities.CHEST_BOATS.get(TIERS[i]), new BlockPos(4, 2, 4));
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
                Entity loaded = entity.getType().create(level, EntitySpawnReason.LOAD);
                loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
                ItemStack back = ((Container) loaded).getItem(SLOTS[i] - 1);
                helper.assertTrue(back.is(Items.COBBLESTONE) && back.getCount() == COBBLE_STACK[i],
                        name + " saved " + COBBLE_STACK[i] + " cobblestone in its last slot and loaded " + back);
                if (loaded instanceof TieredChestBoat loadedBoat) {
                    helper.assertValueEqual(loadedBoat.wood(), "cherry", name + " wood after loading");
                }
                net.minecraft.world.inventory.AbstractContainerMenu menu = ((net.minecraft.world.MenuProvider) vehicle)
                        .createMenu(1, player.getInventory(), player);
                helper.assertTrue(menu instanceof TieredChestMenu tiered && tiered.chestSlotCount() == SLOTS[i] && tiered.tier() == TIERS[i],
                        name + " opens " + (menu == null ? "nothing" : menu.getClass().getSimpleName()) + " instead of the tier chest menu");
                vehicle.clearContent();
                entity.discard();
            }
        }
        helper.succeed();
    }

    /** Broken chest vehicles drop their own tier item (the boat with its wood, a name kept) and their contents. */
    static void brokenChestVehiclesDropTheirTierItemAndContents(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Entity cart = helper.spawn(StorageEntities.CHEST_MINECARTS.get(ChestTier.NETHERITE), new BlockPos(1, 2, 1));
        TieredChestBoat boat = helper.spawn(StorageEntities.CHEST_BOATS.get(ChestTier.ENDERITE), new BlockPos(4, 2, 5));
        boat.setWood("cherry");
        boat.setCustomName(net.minecraft.network.chat.Component.literal("Ferry"));
        List<Entity> vehicles = List.of(cart, boat);
        Item[] expected = {StorageItems.CHEST_MINECARTS.get(ChestTier.NETHERITE), StorageItems.CHEST_BOATS.get(ChestTier.ENDERITE)};
        for (int i = 0; i < vehicles.size(); i++) {
            Entity vehicle = vehicles.get(i);
            ((Container) vehicle).setItem(0, new ItemStack(Items.EMERALD, 3));
            vehicle.hurtServer(level, level.damageSources().generic(), 100.0F);
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, vehicle.getBoundingBox().inflate(2.0));
            Item item = expected[i];
            Optional<ItemEntity> own = drops.stream().filter(d -> d.getItem().is(item)).findFirst();
            helper.assertTrue(own.isPresent(), vehicle.getType().toShortString() + " dropped " + drops.stream().map(d -> d.getItem().toString()).toList());
            helper.assertTrue(drops.stream().anyMatch(d -> d.getItem().is(Items.EMERALD)), vehicle.getType().toShortString() + " lost its contents");
            if (vehicle == boat) {
                ItemStack stack = own.get().getItem();
                helper.assertValueEqual(stack.get(StorageComponents.BOAT_WOOD), "cherry", "wood of the dropped boat");
                helper.assertTrue(stack.getHoverName().getString().equals("Ferry"), "the boat lost its name: " + stack.getHoverName().getString());
            }
            drops.forEach(Entity::discard);
        }
        helper.succeed();
    }

    /**
     * Crafting: minecart + tier chest, boat of a wood + tier chest (wood carried over, End woods with Simple End);
     * smithing Reinforced -> Netherite (Vanilla template) -> Enderite (SimpleLib's template and ingot), the boat keeping
     * its wood.
     */
    static void chestVehicleRecipesCraftAndUpgrade(GameTestHelper helper) {
        List<String> wrong = new ArrayList<>();
        for (ChestTier tier : TIERS) {
            ItemStack chest = new ItemStack(Refs.chest(tier.textureName()));
            craft(helper, wrong, List.of(new ItemStack(Items.MINECART), chest), StorageItems.CHEST_MINECARTS.get(tier), null);
            for (String wood : BoatWoods.all()) {
                craft(helper, wrong, List.of(new ItemStack(BoatWoods.boat(wood)), chest), StorageItems.CHEST_BOATS.get(tier), wood);
            }
        }
        ItemStack base = BoatWoods.stack(StorageItems.CHEST_BOATS.get(ChestTier.REINFORCED), "mangrove");
        ItemStack netherite = smith(helper, new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), base,
                new ItemStack(Items.NETHERITE_INGOT)));
        if (!netherite.is(StorageItems.CHEST_BOATS.get(ChestTier.NETHERITE)) || !"mangrove".equals(netherite.get(StorageComponents.BOAT_WOOD))) {
            wrong.add("smithing reinforced chest boat (mangrove) gave " + netherite + " wood " + netherite.get(StorageComponents.BOAT_WOOD));
        }
        ItemStack enderite = smith(helper, new SmithingRecipeInput(new ItemStack(Refs.material("enderite_upgrade_template")),
                new ItemStack(StorageItems.CHEST_MINECARTS.get(ChestTier.NETHERITE)), new ItemStack(Refs.material("enderite_ingot"))));
        if (!enderite.is(StorageItems.CHEST_MINECARTS.get(ChestTier.ENDERITE))) {
            wrong.add("smithing netherite chest minecart gave " + enderite);
        }
        helper.assertTrue(wrong.isEmpty(), "chest vehicle recipes: " + wrong);
        helper.succeed();
    }

    /**
     * A tier chest boat floating in still water keeps its height on the 1/4096 grid of the move packets
     * ({@code TieredChestBoat#tick}), so the client's deck lies exactly where the server's does and a player standing on it
     * is not set back by the server (Queue N37).
     */
    static void standersStayOnTheChestBoatDeck(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 1, z), Blocks.WATER);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.WATER);
            }
        }
        ServerLevel level = helper.getLevel();
        TieredChestBoat boat = StorageEntities.CHEST_BOATS.get(ChestTier.ENDERITE).create(level, EntitySpawnReason.COMMAND);
        Vec3 at = helper.absoluteVec(new Vec3(2.5, 2.6123, 2.5));
        boat.setPos(at.x, at.y, at.z);
        level.addFreshEntity(boat);
        helper.runAfterDelay(40, () -> {
            double grid = boat.getY() * TieredChestBoat.PACKET_GRID;
            helper.assertTrue(boat.isInWater() || boat.getY() > at.y - 1.0, "the boat sank: " + boat.getY());
            helper.assertTrue(grid == Math.rint(grid), "the boat height " + boat.getY() + " is off the packet grid");
            helper.succeed();
        });
    }

    /**
     * Old worlds: a chest cart and a chest boat saved by SimpleBuilding ({@code simplebuilding:<tier>_chest_*}, the boat
     * item with {@code simplebuilding:boat_wood}) load as Simple Storage's, contents, oversized stacks and wood kept.
     */
    static void oldVehiclesLoadAsSimpleStorages(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> problems = new ArrayList<>();
        for (String path : SimpleStorage.movedVehicles()) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(SimpleStorage.old(path));
            if (type != BuiltInRegistries.ENTITY_TYPE.getValue(SimpleStorage.id(path))) problems.add("entity " + path + " -> " + type);
            if (BuiltInRegistries.ITEM.getValue(SimpleStorage.old(path)) != BuiltInRegistries.ITEM.getValue(SimpleStorage.id(path))) problems.add("item " + path);
        }
        // A saved Netherite chest cart: id, one stack of 128 cobblestone (99 + extra count, as the chest saves it).
        CompoundTag cart = new CompoundTag();
        cart.putString("id", SimpleStorage.old("netherite_chest_minecart").toString());
        net.minecraft.nbt.ListTag items = new net.minecraft.nbt.ListTag();
        CompoundTag slot = new CompoundTag();
        slot.putByte("Slot", (byte) 0);
        slot.putString("id", "minecraft:cobblestone");
        slot.putInt("count", 99);
        items.add(slot);
        cart.put("Items", items);
        net.minecraft.nbt.ListTag extra = new net.minecraft.nbt.ListTag();
        CompoundTag count = new CompoundTag();
        count.putInt("Slot", 0);
        count.putInt("Count", 128);
        extra.add(count);
        cart.put(com.simplestorage.chest.TieredChestBlockEntity.EXTRA_COUNTS, extra);
        Optional<Entity> loaded = EntityType.create(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), cart), level, EntitySpawnReason.LOAD);
        if (loaded.isEmpty() || loaded.get().getType() != StorageEntities.CHEST_MINECARTS.get(ChestTier.NETHERITE)) {
            problems.add("old cart loaded as " + loaded.map(e -> e.getType().toShortString()).orElse("nothing"));
        } else {
            ItemStack back = ((Container) loaded.get()).getItem(0);
            if (!back.is(Items.COBBLESTONE) || back.getCount() != 128) problems.add("old cart contents " + back);
        }
        // The boat item with the old wood component.
        CompoundTag stack = new CompoundTag();
        stack.putString("id", SimpleStorage.old("reinforced_chest_boat").toString());
        stack.putInt("count", 1);
        CompoundTag components = new CompoundTag();
        components.putString(SimpleStorage.old(StorageComponents.BOAT_WOOD_ID).toString(), "spruce");
        stack.put("components", components);
        ItemStack boat = ItemStack.CODEC.parse(level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), stack)
                .result().orElse(ItemStack.EMPTY);
        if (!boat.is(StorageItems.CHEST_BOATS.get(ChestTier.REINFORCED)) || !"spruce".equals(boat.get(StorageComponents.BOAT_WOOD))) {
            problems.add("old boat item " + boat + " wood " + boat.get(StorageComponents.BOAT_WOOD));
        }
        helper.assertTrue(problems.isEmpty(), "old vehicles: " + problems);
        helper.succeed();
    }

    private static void craft(GameTestHelper helper, List<String> wrong, List<ItemStack> ingredients, Item result, String wood) {
        CraftingInput grid = CraftingInput.of(2, 1, ingredients);
        ItemStack got = helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel())
                .map(m -> m.value().assemble(grid)).orElse(ItemStack.EMPTY);
        boolean ok = got.is(result) && (wood == null || wood.equals(got.get(StorageComponents.BOAT_WOOD)));
        if (!ok) {
            wrong.add(BuiltInRegistries.ITEM.getKey(result).getPath() + (wood == null ? "" : " (" + wood + ")") + " got " + got
                    + (got.isEmpty() ? "" : " wood " + got.get(StorageComponents.BOAT_WOOD)));
        }
    }

    private static ItemStack smith(GameTestHelper helper, SmithingRecipeInput input) {
        return helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, helper.getLevel())
                .map(m -> m.value().assemble(input)).orElse(ItemStack.EMPTY);
    }
}
