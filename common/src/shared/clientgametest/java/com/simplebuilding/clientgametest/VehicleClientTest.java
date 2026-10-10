package com.simplebuilding.clientgametest;

import com.simplebuilding.entity.ModEntities;
import com.simplebuilding.entity.vehicle.BoatWoods;
import com.simplebuilding.entity.vehicle.TieredChestBoat;
import com.simplebuilding.entity.vehicle.TieredFurnaceMinecart;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.version.McVersion;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;

/**
 * Tiered vehicles (Queue N19/N23, 26.3): one documentary picture of the nine tier carts on a rail and the tier chest
 * boats (cherry boat and bamboo raft) in front, a close-up of the boats, and the item sprites in item frames. Cleans up behind itself.
 */
public final class VehicleClientTest {
    private static final int Z = 19;

    private VehicleClientTest() {
    }

    public static void inWorld(Script script) {
        if (!McVersion.TIERED_VEHICLES) {
            return;
        }
        TestScene.build(script, "minecraft:smooth_stone", "creative");
        onServer(script, "place the vehicles", server -> {
            ServerLevel level = server.overworld();
            for (int x = 3; x <= 17; x++) {
                level.setBlock(new BlockPos(x, 0, Z), Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST), 3);
            }
            List<EntityType<?>> carts = List.of(ModEntities.REINFORCED_CHEST_MINECART, ModEntities.NETHERITE_CHEST_MINECART,
                    ModEntities.ENDERITE_CHEST_MINECART, ModEntities.REINFORCED_FURNACE_MINECART, ModEntities.NETHERITE_FURNACE_MINECART,
                    ModEntities.ENDERITE_FURNACE_MINECART, ModEntities.REINFORCED_HOPPER_MINECART, ModEntities.NETHERITE_HOPPER_MINECART,
                    ModEntities.ENDERITE_HOPPER_MINECART);
            for (int i = 0; i < carts.size(); i++) {
                Entity cart = carts.get(i).create(level, EntitySpawnReason.COMMAND);
                cart.setPos(4.5 + i * 1.5, 0.0625, Z + 0.5);
                if (cart instanceof TieredFurnaceMinecart furnace && i == 4) {
                    furnace.addFuel(furnace.position(), new ItemStack(Items.COAL));
                }
                level.addFreshEntity(cart);
            }
            List<EntityType<TieredChestBoat>> boats = List.of(ModEntities.REINFORCED_CHEST_BOAT, ModEntities.NETHERITE_CHEST_BOAT,
                    ModEntities.ENDERITE_CHEST_BOAT);
            for (int i = 0; i < 6; i++) {
                TieredChestBoat boat = boats.get(i % 3).create(level, EntitySpawnReason.COMMAND);
                boat.setWood(i < 3 ? "cherry" : "bamboo");
                boat.setPos(4.5 + i * 2.2, 0.0, Z - 4.0);
                boat.setYRot(90.0F);
                level.addFreshEntity(boat);
            }
            // Item frames on the wall behind the rail: the nine cart sprites, then the boats in several woods.
            List<Item> cartItems = List.of(ModItems.REINFORCED_CHEST_MINECART, ModItems.NETHERITE_CHEST_MINECART, ModItems.ENDERITE_CHEST_MINECART,
                    ModItems.REINFORCED_FURNACE_MINECART, ModItems.NETHERITE_FURNACE_MINECART, ModItems.ENDERITE_FURNACE_MINECART,
                    ModItems.REINFORCED_HOPPER_MINECART, ModItems.NETHERITE_HOPPER_MINECART, ModItems.ENDERITE_HOPPER_MINECART);
            List<Item> boatItems = List.of(ModItems.REINFORCED_CHEST_BOAT, ModItems.NETHERITE_CHEST_BOAT, ModItems.ENDERITE_CHEST_BOAT);
            String[] woods = {"oak", "spruce", "cherry", "mangrove", "pale_oak", "dark_oak", "bamboo", "bamboo", "bamboo"};
            for (int i = 0; i < 9; i++) {
                frame(level, 6 + i, 2, cartItems.get(i) == null ? ItemStack.EMPTY : new ItemStack(cartItems.get(i)));
                frame(level, 6 + i, 3, BoatWoods.stack(boatItems.get(i % 3), woods[i]));
            }
        });
        script.awaitPackets();
        script.command("tp @a 10.5 4.5 10.0 0.0 25.0");
        script.awaitPackets();
        script.idle("let the vehicles render", 30);
        script.shot("vehicles-overview");
        script.command("tp @a 7.0 2.0 12.0 0.0 20.0");
        script.awaitPackets();
        script.idle("let the boat close-up settle", 20);
        script.shot("vehicles-boats");
        script.command("tp @a 10.5 2.6 15.6 0.0 5.0");
        script.awaitPackets();
        script.idle("let the frames render", 20);
        script.shot("vehicles-item-frames");
        script.command("clear @a");
        script.command("kill @e[type=!minecraft:player]", true);
        script.command("fill 3 0 " + Z + " 17 0 " + Z + " minecraft:air", true);
        script.command("tp @a 10.5 0.0 16.5 0.0 0.0");
        script.awaitPackets();
    }

    private static void frame(ServerLevel level, int x, int y, ItemStack stack) {
        net.minecraft.world.entity.decoration.ItemFrame frame = new net.minecraft.world.entity.decoration.ItemFrame(level,
                new BlockPos(x, y, Z), net.minecraft.core.Direction.NORTH);
        frame.setItem(stack, false);
        level.addFreshEntity(frame);
    }

    private static void onServer(Script script, String name, Consumer<MinecraftServer> work) {
        script.act(name, client -> {
            MinecraftServer server = client.getSingleplayerServer();
            if (server == null) {
                throw new AssertionError("There is no integrated server");
            }
            server.execute(() -> work.accept(server));
        });
    }
}
