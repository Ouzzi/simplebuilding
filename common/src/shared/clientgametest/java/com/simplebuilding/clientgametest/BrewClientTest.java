package com.simplebuilding.clientgametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.custom.StorageCraftingTableBlockEntity;
import com.simplebuilding.client.gui.StorageCraftingScreen;
import com.simplebuilding.screen.StorageCraftingMenu;
import com.simplebuilding.version.McVersion;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Brewing wave 2026-10-09 (docs/ai/PLAN-BRAUEN-WERKBANK-2026-10-09.md, 26.3): documentary pictures of the perception
 * effects - a pig, a cow, a chicken and a zombie as they are, under Mirage, under Reverse Mirage, the scene Faded
 * (also with the inventory and with the Esc menu, which keeps its colours) and the crosshair under Shivering II - and
 * of the Storage Crafting Table with its grid lying on top and its screen.
 *
 * <p>Asserted is only that the scene exists and the effects reach the client; how it looks is for a human.
 */
public final class BrewClientTest {
    private static final double Z = 18.5;
    private static final BlockPos TABLE = new BlockPos(10, 0, 16);

    private BrewClientTest() {
    }

    public static void inWorld(Script script) {
        if (!McVersion.BREWING_EFFECTS || !McVersion.STORAGE_CRAFTING_TABLE) {
            return;
        }
        TestScene.build(script, "minecraft:stone", "creative");
        onServer(script, "put four mobs and a storage crafting table in front of the camera", server -> {
            ServerLevel level = server.overworld();
            spawn(level, EntityTypes.CHICKEN, 7.5);
            spawn(level, EntityTypes.PIG, 9.0);
            spawn(level, EntityTypes.COW, 11.0);
            spawn(level, EntityTypes.ZOMBIE, 13.0);
            level.setBlockAndUpdate(TABLE, ModBlocks.STORAGE_CRAFTING_TABLE.defaultBlockState());
            if (level.getBlockEntity(TABLE) instanceof StorageCraftingTableBlockEntity table) {
                table.grid().setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
                table.grid().setItem(1, new ItemStack(Items.OAK_PLANKS, 4));
                table.grid().setItem(3, new ItemStack(Items.STICK, 2));
                table.grid().setItem(4, new ItemStack(Items.DIAMOND));
                table.grid().setItem(8, new ItemStack(Items.REDSTONE, 3));
            }
        });
        script.command("tp @a 10.5 0.0 13.0 0.0 10.0");
        script.awaitPackets();
        script.await("the client sees the four mobs", 100,
                c -> c.level.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(5, -1, 16, 16, 3, 21)).size() == 4,
                c -> "the client sees " + c.level.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(5, -1, 16, 16, 3, 21)).size() + " mobs");
        script.idle("let the scene settle", 20);
        script.shot("brew-plain");

        script.command("effect give @a simplebuilding:mirage 60 0 true");
        script.await("the client has Mirage", 60, c -> c.player.hasEffect(com.simplebuilding.effect.ModEffects.MIRAGE),
                c -> "no Mirage on the client");
        script.idle("let Mirage draw", 5);
        script.shot("brew-mirage");
        script.command("effect clear @a");
        script.command("effect give @a simplebuilding:reverse_mirage 60 0 true");
        script.await("the client has Reverse Mirage", 60, c -> c.player.hasEffect(com.simplebuilding.effect.ModEffects.REVERSE_MIRAGE),
                c -> "no Reverse Mirage on the client");
        script.idle("let Reverse Mirage draw", 5);
        script.shot("brew-reverse-mirage");
        script.command("effect clear @a");

        script.command("effect give @a simplebuilding:faded 60 0 true");
        script.await("the client has Faded", 60, c -> c.player.hasEffect(com.simplebuilding.effect.ModEffects.FADED),
                c -> "no Faded on the client");
        script.idle("let Faded draw", 5);
        script.shot("brew-faded");
        script.act("open the inventory", c -> c.gui.setScreen(new InventoryScreen(c.player)));
        script.idle("let the inventory render", 3);
        script.shot("brew-faded-inventory");
        script.act("open the Esc menu", c -> c.gui.setScreen(new PauseScreen(true)));
        script.idle("let the Esc menu render", 3);
        script.shot("brew-faded-pause");
        script.act("close the Esc menu", c -> c.gui.setScreen(null));
        script.command("effect clear @a");

        script.command("effect give @a simplebuilding:shivering 60 1 true");
        script.await("the client has Shivering", 60, c -> c.player.hasEffect(com.simplebuilding.effect.ModEffects.SHIVERING),
                c -> "no Shivering on the client");
        script.idle("let the crosshair shake", 5);
        script.shot("brew-shivering");
        script.command("effect clear @a");

        script.command("tp @a 10.5 0.0 14.6 0.0 55.0");
        script.awaitPackets();
        script.check("the client has the table's grid", c -> c.level.getBlockEntity(TABLE) instanceof StorageCraftingTableBlockEntity table
                && table.items().get(4).is(Items.DIAMOND));
        script.idle("let the table render", 10);
        script.shot("brew-storage-table");
        script.act("open the storage crafting table screen", c -> {
            Inventory inv = c.player.getInventory();
            StorageCraftingMenu menu = new StorageCraftingMenu(0, inv);
            menu.getSlot(1).set(new ItemStack(Items.OAK_PLANKS, 4));
            menu.getSlot(2).set(new ItemStack(Items.OAK_PLANKS, 4));
            menu.getSlot(4).set(new ItemStack(Items.OAK_PLANKS, 4));
            menu.getSlot(5).set(new ItemStack(Items.OAK_PLANKS, 4));
            menu.getSlot(StorageCraftingMenu.RESULT_SLOT).set(new ItemStack(Items.CRAFTING_TABLE));
            c.gui.setScreen(new StorageCraftingScreen(menu, inv, Component.translatable("container.simplebuilding.storage_crafting_table")));
        });
        script.idle("let the screen render", 3);
        script.shot("brew-storage-table-screen");
        script.act("close the screen", c -> c.gui.setScreen(null));
        script.command("setblock 10 0 16 minecraft:air");
        script.command("kill @e[type=!minecraft:player]", true);
        script.awaitPackets();
    }

    private static void spawn(ServerLevel level, EntityType<? extends Mob> type, double x) {
        Mob mob = type.create(level, EntitySpawnReason.COMMAND);
        mob.snapTo(x, 0.0, Z, 180.0F, 0.0F);
        mob.setYBodyRot(180.0F);
        mob.setYHeadRot(180.0F);
        mob.setNoAi(true);
        mob.setSilent(true);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
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
